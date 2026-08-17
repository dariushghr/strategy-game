package org.strategygame.view.panel;

import org.strategygame.model.building.BuildingType;
import org.strategygame.model.unit.BorderExpander;
import org.strategygame.model.unit.Builder;
import org.strategygame.model.unit.Explorer;
import org.strategygame.model.unit.MilitaryUnit;
import org.strategygame.model.unit.Unit;
import org.strategygame.model.unit.Worker;
import org.strategygame.view.BuildingPalette;

import javax.swing.*;
import java.awt.*;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** پنل کنار صفحه: اطلاعات یونیت انتخاب‌شده و اکشن‌های آن. */
public class UnitInfoPanel extends JPanel {

    private final JLabel nameLbl = label("یونیتی انتخاب نشده");
    private final JLabel apLbl   = label("AP: —");
    private final JLabel hpLbl   = label("HP: —");
    private final JLabel posLbl  = label("موقعیت: —");
    private final JLabel roleLbl = label(" ");

    private final Map<String, JButton>   actionButtons  = new LinkedHashMap<>();
    private final Map<String, Supplier<String>> actionProblems = new LinkedHashMap<>();

    private final JCheckBox autoExploreBox = new JCheckBox("کاوش خودکار");

    private final JLabel buildTitle = sectionLabel("سازه‌های قابل ساخت روی این هکس");
    private final JPanel buildPanel = new JPanel();
    private final JScrollPane buildScroll;
    private final Map<BuildingType, JButton> buildButtons = new EnumMap<>(BuildingType.class);

    private Consumer<BuildingType>          buildAction;
    private Function<BuildingType, String>  buildStatus = t -> null;
    private Consumer<Boolean>               autoExploreAction;

    public UnitInfoPanel() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(340, 0));
        setBackground(new Color(24, 32, 50));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);

        top.add(nameLbl);
        top.add(Box.createVerticalStrut(4));
        top.add(apLbl);
        top.add(hpLbl);
        top.add(posLbl);
        top.add(roleLbl);
        top.add(Box.createVerticalStrut(10));

        for (String key : new String[]{"move", "attack", "station", "unstation",
                                       "expand", "road", "wall", "demolish"}) {
            JButton b = actionBtn(actionLabel(key));
            actionButtons.put(key, b);
            top.add(b);
            top.add(Box.createVerticalStrut(4));
        }

        autoExploreBox.setOpaque(false);
        autoExploreBox.setForeground(Color.WHITE);
        autoExploreBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        autoExploreBox.addActionListener(e -> {
            if (autoExploreAction != null) autoExploreAction.accept(autoExploreBox.isSelected());
        });
        top.add(autoExploreBox);
        top.add(Box.createVerticalStrut(10));
        top.add(buildTitle);
        top.add(Box.createVerticalStrut(6));

        buildPanel.setLayout(new BoxLayout(buildPanel, BoxLayout.Y_AXIS));
        buildPanel.setOpaque(false);
        for (BuildingType t : BuildingType.values()) {
            if (!t.isPlayerBuildable()) continue;
            JButton b = buildBtn(t);
            b.addActionListener(e -> { if (buildAction != null) buildAction.accept(t); });
            buildButtons.put(t, b);
            buildPanel.add(b);
            buildPanel.add(Box.createVerticalStrut(6));
        }

        buildScroll = new JScrollPane(buildPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        buildScroll.setBorder(null);
        buildScroll.setOpaque(false);
        buildScroll.getViewport().setOpaque(false);
        buildScroll.getVerticalScrollBar().setUnitIncrement(16);

        add(top,         BorderLayout.NORTH);
        add(buildScroll, BorderLayout.CENTER);
        clear();
    }

    private String actionLabel(String key) {
        return switch (key) {
            case "move"      -> "حرکت";
            case "attack"    -> "حمله";
            case "station"   -> "استقرار در سازه";
            case "unstation" -> "خروج از سازه";
            case "expand"    -> "توسعه مرز";
            case "road"      -> "ساخت جاده";
            case "wall"      -> "ساخت دیوار روی یال";
            case "demolish"  -> "تخریب";
            default          -> key;
        };
    }

    public void show(Unit u) {
        if (u == null) { clear(); return; }

        nameLbl.setText(u.getType().getLabel());
        apLbl.setText("AP: " + u.getCurrentAP() + "/" + u.getMaxAP()
                + (u.getApPenalty() > 0 ? "  (جریمه " + u.getApPenalty() + ")" : ""));
        hpLbl.setText("HP: " + u.getHp() + "/" + u.getMaxHp());
        posLbl.setText("موقعیت: (" + u.getPosition().getQ() + "," + u.getPosition().getR() + ")");

        if (u instanceof MilitaryUnit m) {
            roleLbl.setText("نظامی — برد حمله " + m.getAttackRange()
                    + " | آسیب به سازه " + m.getStructureDamage()
                    + " | HP نبرد " + m.getBattleHp());
            roleLbl.setForeground(new Color(255, 200, 120));
        } else {
            roleLbl.setText("غیرنظامی");
            roleLbl.setForeground(new Color(170, 190, 215));
        }

        boolean isWorker   = u instanceof Worker;
        boolean isExpander = u instanceof BorderExpander;
        boolean isBuilder  = u instanceof Builder;
        boolean isExplorer = u instanceof Explorer;
        boolean isMilitary = u instanceof MilitaryUnit;

        setVisible("move", true);
        setVisible("attack", isMilitary);
        setVisible("station", isWorker);
        setVisible("unstation", isWorker);
        setVisible("expand", isExpander);
        setVisible("road", isBuilder);
        setVisible("wall", isBuilder);
        setVisible("demolish", isBuilder);

        autoExploreBox.setVisible(isExplorer);
        if (u instanceof Explorer explorer) autoExploreBox.setSelected(explorer.isAutoExplore());

        refreshActionStates();

        buildTitle.setVisible(isBuilder);
        buildScroll.setVisible(isBuilder);
        if (u instanceof Builder builder) {
            buildTitle.setText("سازه‌های قابل ساخت (ظرفیت بیلدر: " + builder.getCharges() + ")");
            refreshBuildButtons();
        }

        revalidate();
        repaint();
    }

    /** هر دکمه از تابع «مشکل» خودش می‌پرسد که فعال باشد یا نه. */
    private void refreshActionStates() {
        for (Map.Entry<String, JButton> e : actionButtons.entrySet()) {
            JButton button = e.getValue();
            if (!button.isVisible()) continue;

            Supplier<String> supplier = actionProblems.get(e.getKey());
            String problem = supplier == null ? null : supplier.get();
            button.setEnabled(problem == null);
            button.setToolTipText(problem == null ? "قابل انجام است" : problem);
        }
    }

    private void setVisible(String key, boolean visible) {
        JButton b = actionButtons.get(key);
        if (b != null) b.setVisible(visible);
    }

    private void refreshBuildButtons() {
        for (Map.Entry<BuildingType, JButton> e : buildButtons.entrySet()) {
            BuildingType t = e.getKey();
            JButton btn    = e.getValue();
            String problem = buildStatus.apply(t);
            btn.setEnabled(problem == null);
            btn.setToolTipText(problem == null ? "قابل ساخت است" : problem);
            btn.setText(buildBtnText(t, problem));
        }
    }

    private String buildBtnText(BuildingType t, String problem) {
        String swatch = BuildingPalette.hex(BuildingPalette.of(t));
        String state  = problem == null
                ? "<span style='color:#7dff9a'>✔ قابل ساخت</span>"
                : "<span style='color:#ff9a9a'>✖ " + problem + "</span>";
        String v = "<span style='color:#e6edf7'>";
        return "<html><table cellpadding='0' cellspacing='0' width='250'><tr><td>"
                + "<b style='color:" + swatch + "'>■ " + t.getLabel() + "</b>"
                + (t.getRequiredTownHallLevel() > 1
                    ? " <span style='color:#ffd27f'>(تان هال " + t.getRequiredTownHallLevel() + ")</span>"
                    : "")
                + "<br><span style='color:#ffd27f'>هزینه ساخت:</span> " + v + t.costText()
                + " &nbsp;|&nbsp; AP: " + t.getBuildAP() + "</span><br>"
                + "<span style='color:#9fe0ff'>بعد از ساخت می‌دهد:</span> " + v + t.yieldText() + "</span><br>"
                + "<span style='color:#c9c9c9'>نگهداری: " + t.upkeepText()
                + " | HP: " + t.getMaxHp() + "</span><br>"
                + state + "</td></tr></table></html>";
    }

    public void clear() {
        nameLbl.setText("یونیتی انتخاب نشده");
        apLbl.setText("AP: —");
        hpLbl.setText("HP: —");
        posLbl.setText("موقعیت: —");
        roleLbl.setText(" ");
        for (JButton b : actionButtons.values()) b.setVisible(false);
        autoExploreBox.setVisible(false);
        buildTitle.setVisible(false);
        buildScroll.setVisible(false);
        revalidate();
        repaint();
    }

    // ------------------------------------------------------------- اتصال‌ها
    public void setAction(String key, Runnable action) {
        JButton b = actionButtons.get(key);
        if (b != null) b.addActionListener(e -> action.run());
    }

    public void setActionProblem(String key, Supplier<String> problem) {
        actionProblems.put(key, problem);
    }

    public void setBuildAction(Consumer<BuildingType> cb)         { this.buildAction = cb; }
    public void setBuildStatus(Function<BuildingType, String> f)   { this.buildStatus = f; }
    public void setAutoExploreAction(Consumer<Boolean> cb)         { this.autoExploreAction = cb; }

    private static JLabel label(String txt) {
        JLabel l = new JLabel(txt);
        l.setForeground(Color.WHITE);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setFont(new Font("Dialog", Font.PLAIN, 12));
        return l;
    }

    private static JLabel sectionLabel(String txt) {
        JLabel l = label(txt);
        l.setForeground(new Color(255, 200, 60));
        l.setFont(new Font("Dialog", Font.BOLD, 12));
        return l;
    }

    private static JButton actionBtn(String txt) {
        JButton b = new JButton(txt);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(310, 30));
        b.setContentAreaFilled(false);
        b.setOpaque(true);
        b.setBackground(new Color(48, 65, 90));
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(new Font("Dialog", Font.PLAIN, 12));
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 92, 125)),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        return b;
    }

    private static JButton buildBtn(BuildingType t) {
        JButton b = new JButton();
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(310, 240));
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setContentAreaFilled(false);
        b.setOpaque(true);
        b.setBackground(BuildingPalette.dark(t));
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(new Font("Dialog", Font.PLAIN, 11));
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 7, 0, 0, BuildingPalette.of(t)),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        return b;
    }
}
