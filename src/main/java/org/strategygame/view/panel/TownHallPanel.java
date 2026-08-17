package org.strategygame.view.panel;

import org.strategygame.model.GameState;
import org.strategygame.model.building.ProductionQueue;
import org.strategygame.model.building.TownHall;
import org.strategygame.model.building.TownHallLevel;
import org.strategygame.model.tech.Technology;
import org.strategygame.model.unit.UnitType;
import org.strategygame.model.upgrade.UpgradeType;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * پنل تان هال. آموزش یونیت، تحقیق تکنولوژی، ارتقاها و سطح تان هال همه از یک
 * صف عبور می‌کنند، پس هر دکمه دلیل غیرفعال بودنش را هم در Tooltip نشان می‌دهد.
 */
public class TownHallPanel extends JDialog {

    private static final Color BG = new Color(22, 30, 48);

    private Consumer<UnitType>    unitCB;
    private Consumer<UpgradeType> upgradeCB;
    private Consumer<Technology>  techCB;
    private Runnable              townHallUpgradeCB;
    private Runnable              cancelCB;

    private Function<UnitType, String>    unitProblem    = t -> null;
    private Function<UpgradeType, String> upgradeProblem = t -> null;
    private Function<Technology, String>  techProblem    = t -> null;
    private java.util.function.Supplier<String> townHallUpgradeProblem = () -> null;

    private GameState state;

    public TownHallPanel(JFrame parent) {
        super(parent, "Town Hall", false);
        setSize(460, 700);
        setLocationRelativeTo(parent);
    }

    public void open(GameState state) {
        this.state = state;
        rebuild();
        setVisible(true);
    }

    /** بعد از هر اکشن، پنل باز دوباره ساخته می‌شود تا وضعیت دکمه‌ها تازه بماند. */
    public void refresh() {
        if (state == null || !isVisible()) return;
        rebuild();
    }

    private void rebuild() {
        TownHall townHall = state.getTownHall();
        getContentPane().removeAll();
        getContentPane().setBackground(BG);

        if (townHall == null) {
            setTitle("تان هال نابود شده است");
            getContentPane().add(sectionLabel("تان هال شما نابود شده است"));
            revalidate(); repaint();
            return;
        }

        setTitle("تان هال — سطح " + townHall.getLevel() + " از " + townHall.getMaxLevel());

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        addQueueSection(panel, townHall.getQueue());
        addLevelList(panel, townHall);

        addSection(panel, "آموزش یونیت");
        for (UnitType t : UnitType.values()) {
            if (!t.isTrainable()) continue;
            String problem = unitProblem.apply(t);
            String label = t.getLabel() + " — " + t.costText()
                    + " | " + t.getTrainTurns() + " نوبت"
                    + (t.isMilitary() ? " | نظامی" : "");
            addActionButton(panel, label, problem, () -> {
                if (unitCB != null) unitCB.accept(t);
            });
        }

        addSection(panel, "تکنولوژی‌ها");
        for (Technology tech : Technology.values()) {
            String problem = techProblem.apply(tech);
            boolean done = state.hasTechnology(tech);
            String label = tech.getLabel() + (done ? "  ✓" : "")
                    + " — " + tech.costText() + " | " + tech.getTurns() + " نوبت"
                    + "\n" + tech.getEffectText();
            addActionButton(panel, label, done ? "این تکنولوژی تحقیق شده است" : problem,
                    () -> { if (techCB != null) techCB.accept(tech); });
        }

        addSection(panel, "ارتقاها");
        for (UpgradeType u : UpgradeType.values()) {
            boolean done = state.getDoneUpgrades().contains(u);
            String problem = done ? "این ارتقا انجام شده است" : upgradeProblem.apply(u);
            String label = u.getLabel() + (done ? "  ✓" : "")
                    + " — " + org.strategygame.model.building.BuildingType.formatCost(u.getCost())
                    + " | " + u.getTurns() + " نوبت";
            addActionButton(panel, label, problem,
                    () -> { if (upgradeCB != null) upgradeCB.accept(u); });
        }

        JScrollPane scroll = new JScrollPane(panel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(BG);
        getContentPane().add(scroll);
        revalidate(); repaint();
    }

    private void addQueueSection(JPanel panel, ProductionQueue queue) {
        addSection(panel, "صف تان هال (فقط یک دستور در هر لحظه)");

        JLabel info = new JLabel(queue.isEmpty()
                ? "صف خالی است"
                : queue.getLabel() + " — " + queue.getTurnsLeft() + " نوبت باقی مانده");
        info.setForeground(queue.isEmpty() ? new Color(180, 190, 205) : new Color(140, 235, 255));
        info.setFont(new Font("Dialog", Font.BOLD, 13));
        info.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(info);
        panel.add(Box.createVerticalStrut(4));

        addActionButton(panel, "لغو دستور فعال (منابع برنمی‌گردد)",
                queue.isEmpty() ? "دستوری در صف نیست" : null,
                () -> { if (cancelCB != null) cancelCB.run(); });
    }

    /** لیست سطح‌های تان هال؛ سطح فعلی مشخص شده و سطح بعدی قابل ارتقاست. */
    private void addLevelList(JPanel panel, TownHall townHall) {
        addSection(panel, "سطح‌های تان هال");

        for (TownHallLevel lv : TownHallLevel.values()) {
            boolean isCurrent = lv.getNumber() == townHall.getLevel();
            boolean isDone    = lv.getNumber() < townHall.getLevel();
            String status = isCurrent ? "◀ سطح فعلی"
                          : isDone    ? "✓ گذرانده شده"
                                      : "قفل — " + lv.costText();

            Color fg = isCurrent ? new Color(120, 255, 160)
                     : isDone    ? new Color(160, 200, 160)
                                 : new Color(200, 200, 210);

            JLabel row = new JLabel("<html><div style='width:380px'>"
                    + "<b>" + lv.getLabel() + "</b> — " + status + "<br>"
                    + "<span style='color:#c9d6e6'>" + lv.bonusText() + "</span></div></html>");
            row.setForeground(fg);
            row.setFont(new Font("Dialog", Font.PLAIN, 12));
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.setOpaque(true);
            row.setBackground(isCurrent ? new Color(34, 60, 52) : new Color(28, 38, 58));
            row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 5, 0, 0,
                            isCurrent ? new Color(90, 230, 130)
                                      : isDone ? new Color(80, 120, 90) : new Color(70, 85, 115)),
                    BorderFactory.createEmptyBorder(6, 8, 6, 8)));
            row.setMaximumSize(new Dimension(420, 80));
            panel.add(row);
            panel.add(Box.createVerticalStrut(5));
        }

        TownHallLevel next = townHall.getNextLevel();
        if (next == null) {
            addActionButton(panel, "تان هال در بالاترین سطح است",
                    "سطح بالاتری وجود ندارد", () -> { });
        } else {
            addActionButton(panel, "ارتقا به " + next.getLabel() + " — " + next.costText(),
                    townHallUpgradeProblem.get(),
                    () -> { if (townHallUpgradeCB != null) townHallUpgradeCB.run(); });
        }
    }

    /** دکمه‌ای که وضعیت مجاز بودن اکشن و دلیل آن را هم نشان می‌دهد. */
    private void addActionButton(JPanel panel, String label, String problem, Runnable action) {
        boolean enabled = problem == null;
        String[] lines  = label.split("\n", 2);

        JButton b = new JButton("<html><table width='390'><tr><td>"
                + "<b>" + lines[0] + "</b>"
                + (lines.length > 1 ? "<br><span style='color:#b8c6da'>" + lines[1] + "</span>" : "")
                + "<br><span style='color:" + (enabled ? "#7dff9a" : "#ff9a9a") + "'>"
                + (enabled ? "✔ قابل انجام" : "✖ " + problem)
                + "</span></td></tr></table></html>");

        b.setMaximumSize(new Dimension(420, 120));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setContentAreaFilled(false);
        b.setOpaque(true);
        b.setBackground(enabled ? new Color(40, 60, 88) : new Color(38, 42, 54));
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(new Font("Dialog", Font.PLAIN, 12));
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 92, 125)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        b.setEnabled(enabled);
        b.setToolTipText(enabled ? "قابل انجام است" : problem);
        b.addActionListener(e -> action.run());

        panel.add(b);
        panel.add(Box.createVerticalStrut(5));
    }

    private void addSection(JPanel p, String title) {
        p.add(Box.createVerticalStrut(10));
        p.add(sectionLabel(title));
        p.add(Box.createVerticalStrut(4));
    }

    private JLabel sectionLabel(String title) {
        JLabel l = new JLabel(title);
        l.setForeground(new Color(255, 200, 60));
        l.setFont(new Font("Dialog", Font.BOLD, 13));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    public void setUnitCB(Consumer<UnitType> cb)          { this.unitCB = cb; }
    public void setUpgradeCB(Consumer<UpgradeType> cb)     { this.upgradeCB = cb; }
    public void setTechCB(Consumer<Technology> cb)         { this.techCB = cb; }
    public void setTownHallUpgradeCB(Runnable r)           { this.townHallUpgradeCB = r; }
    public void setCancelCB(Runnable r)                    { this.cancelCB = r; }

    public void setUnitProblem(Function<UnitType, String> f)       { this.unitProblem = f; }
    public void setUpgradeProblem(Function<UpgradeType, String> f) { this.upgradeProblem = f; }
    public void setTechProblem(Function<Technology, String> f)     { this.techProblem = f; }
    public void setTownHallUpgradeProblem(java.util.function.Supplier<String> s) {
        this.townHallUpgradeProblem = s;
    }
}
