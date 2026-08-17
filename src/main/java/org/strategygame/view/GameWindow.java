package org.strategygame.view;

import org.strategygame.common.ActionResult;
import org.strategygame.controller.BuildingController;
import org.strategygame.controller.CombatController;
import org.strategygame.controller.TradeController;
import org.strategygame.controller.TribeController;
import org.strategygame.controller.TurnController;
import org.strategygame.controller.UnitController;
import org.strategygame.model.GameState;
import org.strategygame.model.building.Building;
import org.strategygame.model.building.BuildingType;
import org.strategygame.model.building.TownHall;
import org.strategygame.model.disaster.DisasterEvent;
import org.strategygame.model.map.HexCell;
import org.strategygame.model.unit.BorderExpander;
import org.strategygame.model.unit.Builder;
import org.strategygame.model.unit.Explorer;
import org.strategygame.model.unit.MilitaryUnit;
import org.strategygame.model.unit.Unit;
import org.strategygame.model.unit.Worker;
import org.strategygame.view.hud.HUDView;
import org.strategygame.view.map.HexMapView;
import org.strategygame.view.panel.DebugPanel;
import org.strategygame.view.panel.NotificationPanel;
import org.strategygame.view.panel.TownHallPanel;
import org.strategygame.view.panel.TradePanel;
import org.strategygame.view.panel.TribePanel;
import org.strategygame.view.panel.UnitInfoPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

/**
 * پنجره‌ی اصلی بازی. تنها وظیفه‌ی آن نمایش وضعیت و تبدیل کلیک‌ها به فراخوانی
 * کنترلرهاست؛ هیچ قاعده‌ی بازی اینجا نوشته نمی‌شود.
 */
public class GameWindow extends JFrame {

    /** کلیک بعدی روی نقشه چه معنایی دارد. */
    private enum ClickMode { SELECT, MOVE, ATTACK, WALL }

    private final GameState          state;
    private final TurnController     turnCtrl;
    private final UnitController     unitCtrl;
    private final BuildingController bldCtrl;
    private final CombatController   combatCtrl;
    private final TribeController    tribeCtrl;
    private final TradeController    tradeCtrl;

    private final HexMapView    mapView   = new HexMapView();
    private final HUDView       hud       = new HUDView();
    private final UnitInfoPanel unitPanel = new UnitInfoPanel();

    private final TownHallPanel     thPanel;
    private final TribePanel        tribePanel;
    private final TradePanel        tradePanel;
    private final NotificationPanel logPanel;
    private final DebugPanel        debugPanel;

    private Unit      selected  = null;
    private ClickMode clickMode = ClickMode.SELECT;
    private boolean   animating = false;

    public GameWindow(GameState state,
                      TurnController turnCtrl,
                      UnitController unitCtrl,
                      BuildingController bldCtrl,
                      CombatController combatCtrl,
                      TribeController tribeCtrl,
                      TradeController tradeCtrl) {
        this.state      = state;
        this.turnCtrl   = turnCtrl;
        this.unitCtrl   = unitCtrl;
        this.bldCtrl    = bldCtrl;
        this.combatCtrl = combatCtrl;
        this.tribeCtrl  = tribeCtrl;
        this.tradeCtrl  = tradeCtrl;

        setTitle("بازی استراتژیک");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(1250, 780);
        setLocationRelativeTo(null);

        thPanel    = new TownHallPanel(this);
        tribePanel = new TribePanel(this, tribeCtrl);
        tradePanel = new TradePanel(this, tradeCtrl);
        logPanel   = new NotificationPanel(this, state.getNotifications());
        debugPanel = new DebugPanel(this, state, turnCtrl);

        buildLayout();
        bindHud();
        bindUnitPanel();
        bindTownHallPanel();
        bindTribePanel();
        bindTradePanel();
        debugPanel.setOnChange(this::refresh);

        mapView.setState(state);
        mapView.setMapData(state.getMap(), state.getFog());
        mapView.setBuildOptions(this::buildHint);
        mapView.setShowBuildOverlay(hud.isOverlayOn());
        mapView.syncWeather(state.getSeason());
        refresh();

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                int r = JOptionPane.showConfirmDialog(GameWindow.this,
                        "از بازی خارج شوید؟", "خروج", JOptionPane.YES_NO_OPTION);
                if (r == JOptionPane.YES_OPTION) System.exit(0);
            }
        });
    }

    private void buildLayout() {
        setLayout(new BorderLayout());
        add(hud,       BorderLayout.NORTH);
        add(mapView,   BorderLayout.CENTER);
        add(unitPanel, BorderLayout.EAST);
    }

    // ------------------------------------------------------------------- HUD
    private void bindHud() {
        hud.setOnEndTurn(() -> {
            if (state.idleUnitsWithAP().isEmpty()) doEndTurn();
            else hud.showIdleWarning(this::doEndTurn);
        });

        hud.setOnOverlayToggle(mapView::setShowBuildOverlay);
        hud.setOnTownHall(this::openTownHall);
        hud.setOnTribes(() -> tribePanel.open());
        hud.setOnTrade(() -> tradePanel.open(null));
        hud.setOnLog(logPanel::open);
        hud.setOnDebug(debugPanel::open);
        mapView.setCellClickListener(this::handleClick);
    }

    private void openTownHall() {
        if (state.getTownHall() == null) {
            hud.showMessage("تان هالی روی نقشه نیست", false);
            return;
        }
        thPanel.open(state);
    }

    // ------------------------------------------------------- پنل یونیت
    private void bindUnitPanel() {
        unitPanel.setAction("move", () -> {
            if (selected == null) return;
            clickMode = ClickMode.MOVE;
            mapView.setHighlight(unitCtrl.reachable(selected));
            hud.showMessage("هکس مقصد را انتخاب کنید", true);
        });
        unitPanel.setActionProblem("move", () -> selected == null
                ? "یونیتی انتخاب نشده"
                : selected.getCurrentAP() <= 0 ? "AP کافی نیست" : null);

        unitPanel.setAction("attack", () -> {
            if (!(selected instanceof MilitaryUnit)) return;
            clickMode = ClickMode.ATTACK;
            mapView.setHighlight(attackTargets(selected));
            hud.showMessage("هدف حمله را انتخاب کنید", true);
        });
        unitPanel.setActionProblem("attack", () -> {
            if (!(selected instanceof MilitaryUnit)) return "فقط یونیت نظامی می‌تواند حمله کند";
            if (selected.getCurrentAP() <= 0) return "AP کافی نیست";
            return attackTargets(selected).isEmpty() ? "هدفی در برد حمله نیست" : null;
        });

        unitPanel.setAction("station", () -> {
            if (!(selected instanceof Worker w)) return;
            Building b = w.getPosition().getBuilding();
            if (b != null && unitCtrl.station(w, b)) {
                hud.showMessage("کارگر در " + b.getType().getLabel() + " مستقر شد", true);
            }
            refresh();
        });
        unitPanel.setActionProblem("station", () -> {
            if (!(selected instanceof Worker w)) return "فقط کارگر می‌تواند مستقر شود";
            if (w.isStationed()) return "این کارگر همین حالا مستقر است";
            Building b = w.getPosition().getBuilding();
            if (b == null) return "روی این هکس سازه‌ای نیست";
            if (!b.isFunctional()) return "این سازه فعال نیست";
            return b.canTakeWorker() ? null : "ظرفیت این سازه پر است";
        });

        unitPanel.setAction("unstation", () -> {
            if (selected instanceof Worker w) unitCtrl.unstation(w);
            refresh();
        });
        unitPanel.setActionProblem("unstation", () -> selected instanceof Worker w
                ? (w.isStationed() ? null : "این کارگر مستقر نیست")
                : "فقط کارگر");

        unitPanel.setAction("expand", () -> {
            if (selected instanceof BorderExpander be) {
                unitCtrl.expand(be, selected.getPosition());
                hud.showMessage("مرز امپراتوری گسترش یافت", true);
            }
            selected = null;
            unitPanel.clear();
            refresh();
        });
        unitPanel.setActionProblem("expand", () -> selected instanceof BorderExpander
                ? (selected.getCurrentAP() <= 0 ? "AP کافی نیست" : null)
                : "فقط توسعه‌دهنده مرز");

        unitPanel.setAction("road", () -> {
            if (!(selected instanceof Builder builder)) return;
            report(unitCtrl.buildRoad(builder, builder.getPosition()), "ساخت جاده");
            refresh();
        });
        unitPanel.setActionProblem("road", () -> selected instanceof Builder builder
                ? unitCtrl.roadProblem(builder, builder.getPosition())
                : "فقط بیلدر می‌تواند جاده بسازد");

        unitPanel.setAction("wall", () -> {
            if (!(selected instanceof Builder)) return;
            clickMode = ClickMode.WALL;
            mapView.setHighlight(wallNeighbors(selected));
            hud.showMessage("هکس مجاور را انتخاب کنید تا دیوار روی یال مشترک ساخته شود", true);
        });
        unitPanel.setActionProblem("wall", () -> {
            if (!(selected instanceof Builder)) return "فقط بیلدر می‌تواند دیوار بسازد";
            return wallNeighbors(selected).isEmpty()
                    ? "یال مناسبی برای دیوار در اطراف نیست" : null;
        });

        unitPanel.setAction("demolish", () -> {
            if (!(selected instanceof Builder builder)) return;
            HexCell cell = builder.getPosition();
            String label = cell.hasBuilding() ? cell.getBuilding().getType().getLabel() : "سازه";
            int confirm = JOptionPane.showConfirmDialog(this,
                    label + " تخریب شود؟ نیمی از هزینه‌ی ساخت برمی‌گردد.",
                    "تخریب", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
            report(unitCtrl.demolish(builder, cell), "تخریب");
            refresh();
        });
        unitPanel.setActionProblem("demolish", () -> selected instanceof Builder builder
                ? unitCtrl.demolishProblem(builder, builder.getPosition())
                : "فقط بیلدر می‌تواند تخریب کند");

        unitPanel.setBuildStatus(t -> selected instanceof Builder builder
                ? unitCtrl.buildProblem(builder, t, builder.getPosition())
                : "اول یک بیلدر انتخاب کنید");

        unitPanel.setBuildAction(t -> {
            if (!(selected instanceof Builder builder)) return;
            HexCell cell = builder.getPosition();
            ActionResult result = unitCtrl.build(builder, t, cell);

            if (result.success()) {
                mapView.flashBuilt(cell);
                hud.showMessage(t.getLabel() + " ساخته شد — " + t.yieldText(), true);
            } else {
                hud.showMessage(result.message(), false);
            }
            report(result, result.success() ? "سازه ساخته شد" : "امکان ساخت نیست");
            refresh();
        });

        unitPanel.setAutoExploreAction(v -> {
            if (selected instanceof Explorer explorer) explorer.setAutoExplore(v);
        });
    }

    // -------------------------------------------------------- پنل تان هال
    private void bindTownHallPanel() {
        thPanel.setUnitCB(t -> {
            report(bldCtrl.queueTrain(t), "آموزش یونیت");
            afterTownHallAction();
        });
        thPanel.setUpgradeCB(t -> {
            report(bldCtrl.queueUpgrade(t), "ارتقا");
            afterTownHallAction();
        });
        thPanel.setTechCB(t -> {
            report(bldCtrl.queueTechnology(t), "تحقیق فناوری");
            afterTownHallAction();
        });
        thPanel.setTownHallUpgradeCB(() -> {
            report(bldCtrl.queueTownHallUpgrade(), "ارتقای تان هال");
            afterTownHallAction();
        });
        thPanel.setCancelCB(() -> {
            report(bldCtrl.cancelQueue(), "لغو صف");
            afterTownHallAction();
        });

        thPanel.setUnitProblem(bldCtrl::trainProblem);
        thPanel.setUpgradeProblem(bldCtrl::upgradeProblem);
        thPanel.setTechProblem(bldCtrl::technologyProblem);
        thPanel.setTownHallUpgradeProblem(bldCtrl::townHallUpgradeProblem);
    }

    private void afterTownHallAction() {
        thPanel.refresh();
        refresh();
    }

    // ---------------------------------------------------------- پنل قبیله‌ها
    private void bindTribePanel() {
        tribePanel.setOnGift((tribe, type, amount) -> {
            report(tribeCtrl.gift(tribe, type, amount), "هدیه");
            afterTribeAction();
        });
        tribePanel.setOnWar(tribe -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "اعلام جنگ به " + tribe.getName() + "؟ شادی امپراتوری کم می‌شود.",
                    "اعلام جنگ", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
            report(tribeCtrl.declareWar(tribe), "اعلام جنگ");
            afterTribeAction();
        });
        tribePanel.setOnPeace(tribe -> {
            report(tribeCtrl.makePeace(tribe), "صلح");
            afterTribeAction();
        });
        tribePanel.setOnAlliance(tribe -> {
            report(tribeCtrl.formAlliance(tribe), "اتحاد");
            afterTribeAction();
        });
        tribePanel.setOnAcceptMission(tribe -> {
            report(tribeCtrl.acceptMission(tribe), "مأموریت");
            afterTribeAction();
        });
        tribePanel.setOnTurnIn(tribe -> {
            report(tribeCtrl.turnInMission(tribe), "تحویل مأموریت");
            afterTribeAction();
        });
        tribePanel.setOnCancelMission(tribe -> {
            report(tribeCtrl.cancelMission(tribe), "لغو مأموریت");
            afterTribeAction();
        });
        tribePanel.setOnTrade(tradePanel::open);
        tribePanel.setTradeProblemProvider(tradeCtrl::tribeProblem);
    }

    private void afterTribeAction() {
        tribePanel.refresh();
        refresh();
    }

    private void bindTradePanel() {
        tradePanel.setOnTrade((strategy, source, target, amount) -> {
            report(tradeCtrl.trade(strategy, source, target, amount), "تجارت");
            tradePanel.refresh();
            refresh();
        });
    }

    // ------------------------------------------------------- کلیک روی نقشه
    private void handleClick(HexCell cell) {
        if (animating) return;

        switch (clickMode) {
            case MOVE   -> { doMove(cell);   return; }
            case ATTACK -> { doAttack(cell); return; }
            case WALL   -> { doWall(cell);   return; }
            case SELECT -> { }
        }

        if (cell.hasBuilding() && cell.getBuilding() instanceof TownHall) {
            thPanel.open(state);
            return;
        }

        if (state.tribeAt(cell) != null) {
            tribePanel.open();
            return;
        }

        Unit own = firstOwnUnit(cell);
        if (own != null) {
            selected = own;
            mapView.setSelected(cell);
            unitPanel.show(own);
            return;
        }

        clearSelection();
    }

    private void doMove(HexCell cell) {
        List<HexCell> path = unitCtrl.path(selected, cell);
        mapView.clearHighlight();
        clickMode = ClickMode.SELECT;

        if (path.isEmpty()) {
            String problem = unitCtrl.stepProblem(selected, cell);
            hud.showMessage(problem == null ? "مسیری به این هکس نیست" : problem, false);
            return;
        }
        animateAlong(selected, path, 0);
    }

    private void doAttack(HexCell cell) {
        HexCell from = selected == null ? null : selected.getPosition();
        mapView.clearHighlight();
        clickMode = ClickMode.SELECT;
        if (from == null) return;

        ActionResult result = combatCtrl.hasHostileTarget(cell)
                ? combatCtrl.attack(from, cell)
                : combatCtrl.attackWall(from, cell);

        hud.showMessage(result.message(), result.success());
        report(result, result.success() ? "نتیجه نبرد" : "حمله انجام نشد");
        refresh();
    }

    private void doWall(HexCell cell) {
        HexCell from = selected == null ? null : selected.getPosition();
        mapView.clearHighlight();
        clickMode = ClickMode.SELECT;
        if (!(selected instanceof Builder builder) || from == null) return;

        report(unitCtrl.buildWall(builder, from, cell), "ساخت دیوار");
        refresh();
    }

    private void clearSelection() {
        selected  = null;
        clickMode = ClickMode.SELECT;
        mapView.clearHighlight();
        mapView.setSelected(null);
        unitPanel.clear();
    }

    /** فقط یونیت‌های خودی انتخاب می‌شوند؛ کلیک روی خرس یا قبیله انتخاب نمی‌کند. */
    private Unit firstOwnUnit(HexCell cell) {
        for (Unit u : cell.getUnits()) {
            if (u.isAlive() && u.getOwner().isPlayer()) return u;
        }
        return null;
    }

    private List<HexCell> attackTargets(Unit unit) {
        List<HexCell> targets = new java.util.ArrayList<>();
        if (!(unit instanceof MilitaryUnit military)) return targets;

        HexCell from = military.getPosition();
        for (HexCell cell : state.getMap().getCellsInRadius(from, military.getAttackRange())) {
            if (cell == from) continue;
            if (combatCtrl.attackProblem(from, cell) == null) targets.add(cell);
        }
        return targets;
    }

    private List<HexCell> wallNeighbors(Unit unit) {
        List<HexCell> cells = new java.util.ArrayList<>();
        if (!(unit instanceof Builder builder)) return cells;

        HexCell from = builder.getPosition();
        for (HexCell neighbor : state.getMap().getNeighbors(from)) {
            if (unitCtrl.wallProblem(builder, from, neighbor) == null) cells.add(neighbor);
        }
        return cells;
    }

    // ------------------------------------------------------------ انیمیشن
    private void animateAlong(Unit unit, List<HexCell> path, int index) {
        if (index >= path.size()) {
            animating = false;
            if (selected == unit) {
                if (unit.isAlive()) unitPanel.show(unit);
                else clearSelection();
            }
            refresh();
            return;
        }

        animating = true;
        HexCell from = unit.getPosition();
        HexCell to   = path.get(index);

        mapView.animMove(unit, from, to, () -> {
            boolean moved = unitCtrl.stepOnce(unit, to);
            refresh();
            if (!moved) {
                animating = false;
                hud.showMessage("حرکت نیمه‌کاره ماند", false);
                refresh();
                return;
            }
            animateAlong(unit, path, index + 1);
        });
    }

    // --------------------------------------------------- قلاب‌های کنترلر
    /** پس از پایان نوبت: همه‌ی پنل‌های باز و نقشه هم‌گام می‌شوند. */
    public void onTurnAdvanced() {
        mapView.syncWeather(state.getSeason());
        if (selected != null && !selected.isAlive()) clearSelection();
        refresh();

        if (thPanel.isVisible())    thPanel.refresh();
        if (tribePanel.isVisible()) tribePanel.refresh();
        if (tradePanel.isVisible()) tradePanel.refresh();
        if (logPanel.isVisible())   logPanel.refresh();
    }

    /** بلای طبیعی: اگر داخل دید بازیکن باشد انیمیشن، وگرنه فقط گزارش. */
    public void onDisaster(DisasterEvent event) {
        if (event == null) return;
        if (event.isVisible()) mapView.playDisaster(event);
        hud.showMessage(event.headline(), false);
        JOptionPane.showMessageDialog(this, event.report(),
                event.getType().getLabel(), JOptionPane.WARNING_MESSAGE);
        refresh();
    }

    /** سازه‌ای که روی این هکس قابل ساخت است، برای رنگ‌آمیزی نقشه. */
    private HexMapView.BuildHint buildHint(HexCell cell) {
        BuildingType type = unitCtrl.buildableTypeAt(cell);
        if (type == null) return null;
        return new HexMapView.BuildHint(
                type,
                unitCtrl.isTechUnlocked(type),
                state.getStorage().canAffordAll(unitCtrl.effectiveCost(type)));
    }

    private void report(ActionResult result, String title) {
        if (result == null) return;
        hud.showMessage(result.message(), result.success());
        JOptionPane.showMessageDialog(this, result.message(), title,
                result.success() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
    }

    private void doEndTurn() {
        clearSelection();
        turnCtrl.execute();
    }

    public void refresh() {
        mapView.setMapData(state.getMap(), state.getFog());
        hud.refresh(state);
        hud.setPendingCount(state.getNotifications().pendingCount());
        if (selected != null && !animating) {
            if (selected.isAlive()) unitPanel.show(selected);
            else clearSelection();
        }
        mapView.repaint();
    }
}
