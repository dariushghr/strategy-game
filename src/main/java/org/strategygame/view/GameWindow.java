package org.strategygame.view;

import org.strategygame.controller.*;
import org.strategygame.model.GameState;
import org.strategygame.model.building.Building;
import org.strategygame.model.building.BuildingType;
import org.strategygame.model.building.TownHall;
import org.strategygame.model.map.HexCell;
import org.strategygame.model.unit.*;
import org.strategygame.view.hud.HUDView;
import org.strategygame.view.map.HexMapView;
import org.strategygame.view.panel.TownHallPanel;
import org.strategygame.view.panel.UnitInfoPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

public class GameWindow extends JFrame {

    private final GameState         state;
    private final TurnController    turnCtrl;
    private final UnitController    unitCtrl;
    private final BuildingController bldCtrl;

    private final HexMapView    mapView  = new HexMapView();
    private final HUDView       hud      = new HUDView();
    private final UnitInfoPanel unitPanel= new UnitInfoPanel();
    private final TownHallPanel thPanel;

    private Unit    selected         = null;
    private boolean waitingForTarget = false;

    public GameWindow(GameState state,
                      TurnController turnCtrl,
                      UnitController unitCtrl,
                      BuildingController bldCtrl) {
        this.state    = state;
        this.turnCtrl = turnCtrl;
        this.unitCtrl = unitCtrl;
        this.bldCtrl  = bldCtrl;

        setTitle("بازی استراتژیک");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(1150, 740);
        setLocationRelativeTo(null);

        thPanel = new TownHallPanel(this);
        buildLayout();
        bindActions();

        mapView.setMapData(state.getMap(), state.getFog());
        hud.refresh(state);

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

    private void bindActions() {

        hud.setOnEndTurn(() -> {
            List<Unit> idle = state.idleUnitsWithAP();
            if (!idle.isEmpty()) {
                hud.showIdleWarning(this::doEndTurn);
            } else {
                doEndTurn();
            }
        });

        mapView.setCellClickListener(this::handleClick);

        unitPanel.setMoveAction(() -> {
            if (selected == null) return;
            waitingForTarget = true;
            mapView.setHighlight(unitCtrl.reachable(selected));
        });

        unitPanel.setStationAction(() -> {
            if (!(selected instanceof Worker w)) return;
            Building b = w.getPosition().getBuilding();
            if (b != null) unitCtrl.station(w, b);
        });

        unitPanel.setUnstationAction(() -> {
            if (selected instanceof Worker w) unitCtrl.unstation(w);
        });

        unitPanel.setExpandAction(() -> {
            if (selected instanceof BorderExpander be)
                unitCtrl.expand(be, selected.getPosition());
            selected = null; unitPanel.clear();
        });

        unitPanel.setBuildAction(t -> {
            if (!(selected instanceof Builder builder)) return;
            unitCtrl.build(builder, t, selected.getPosition());
            if (selected != null && !selected.isAlive()) {
                selected = null; unitPanel.clear();
            } else if (selected != null) {
                unitPanel.show(selected);
            }
        });

        unitPanel.setAutoExploreAction(v -> {
            if (selected instanceof Explorer explorer) explorer.setAutoExplore(v);
        });

        thPanel.setUnitCB(t -> {
            bldCtrl.queueUnit(t);
            refresh();
        });

        thPanel.setUpgradeCB(t -> {
            bldCtrl.queueUpgrade(t);
            refresh();
        });
    }

    private void doEndTurn() {
        turnCtrl.execute();
    }

    private void handleClick(HexCell cell) {

        if (waitingForTarget && selected != null) {
            unitCtrl.move(selected, cell);
            mapView.clearHighlight();
            waitingForTarget = false;
            if (selected.isAlive()) unitPanel.show(selected);
            else { selected = null; unitPanel.clear(); }
            return;
        }

        if (cell.hasBuilding() && cell.getBuilding() instanceof TownHall townHall) {
            thPanel.open(townHall, state.getStorage(), state.getDoneUpgrades());
            return;
        }

        if (!cell.getUnits().isEmpty()) {
            selected = cell.getUnits().getFirst();
            mapView.setSelected(cell);
            unitPanel.show(selected);
            return;
        }

        selected = null;
        waitingForTarget = false;
        mapView.clearHighlight();
        mapView.setSelected(null);
        unitPanel.clear();
    }

    public void refresh() {
        mapView.setMapData(state.getMap(), state.getFog());
        hud.refresh(state);
        if (selected != null) {
            if (selected.isAlive()) unitPanel.show(selected);
            else { selected = null; unitPanel.clear(); }
        }
        mapView.repaint();
    }
}
