package org.strategygame.view.panel;

import org.strategygame.model.building.TownHall;
import org.strategygame.model.resource.ResourceStorage;
import org.strategygame.model.unit.UnitType;
import org.strategygame.model.upgrade.UpgradeType;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

public class TownHallPanel extends JDialog {

    public interface UnitCB    { void go(UnitType t); }
    public interface UpgradeCB { void go(UpgradeType t); }

    private UnitCB    unitCB;
    private UpgradeCB upgradeCB;

    public TownHallPanel(JFrame parent) {
        super(parent, "Town Hall", false);
        setSize(340, 420);
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    public void open(TownHall th, ResourceStorage storage, final Set<UpgradeType> done) {
        getContentPane().removeAll();
        getContentPane().setBackground(new Color(22, 30, 48));

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(22, 30, 48));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        addSection(panel, "تولید یونیت");
        for (UnitType t : UnitType.values()) {
            JButton b = dialogBtn(t.getLabel());
            b.addActionListener(e -> { if (unitCB != null) unitCB.go(t); });
            panel.add(b); panel.add(Box.createVerticalStrut(4));
        }

        addSection(panel, "آپگریدها");
        for (UpgradeType u : UpgradeType.values()) {
            boolean unlocked = u.isUnlocked(done);
            boolean finished = done.contains(u);
            String lbl = u.getLabel()
                    + (finished ? "  ✓" : (!unlocked ? "  🔒" : ""));
            JButton b = dialogBtn(lbl);
            b.setEnabled(unlocked && !finished);
            b.addActionListener(e -> { if (upgradeCB != null) upgradeCB.go(u); });
            panel.add(b); panel.add(Box.createVerticalStrut(4));
        }

        JScrollPane scroll = new JScrollPane(panel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(new Color(22, 30, 48));
        getContentPane().add(scroll);
        revalidate(); repaint();
        setVisible(true);
    }

    private void addSection(JPanel p, String title) {
        JLabel l = new JLabel(title);
        l.setForeground(new Color(255, 200, 60));
        l.setFont(new Font("Dialog", Font.BOLD, 13));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(Box.createVerticalStrut(8));
        p.add(l);
        p.add(Box.createVerticalStrut(4));
    }

    private static JButton dialogBtn(String txt) {
        JButton b = new JButton(txt);
        b.setMaximumSize(new Dimension(300, 30));
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setBackground(new Color(48, 65, 90));
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(new Font("Dialog", Font.PLAIN, 12));
        return b;
    }

    public void setUnitCB(UnitCB cb)       { this.unitCB = cb; }
    public void setUpgradeCB(UpgradeCB cb) { this.upgradeCB = cb; }
}
