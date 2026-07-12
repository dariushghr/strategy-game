package org.strategygame.view.panel;

import org.strategygame.model.building.BuildingType;
import org.strategygame.model.unit.*;

import javax.swing.*;
import java.awt.*;

public class UnitInfoPanel extends JPanel {

    public interface BuildCB { void go(BuildingType t); }
    public interface BoolCB  { void go(boolean v); }

    private final JLabel nameLbl = label("یونیتی انتخاب نشده");
    private final JLabel apLbl   = label("AP: —");
    private final JLabel posLbl  = label("موقعیت: —");

    private final JButton moveBtn      = actionBtn("حرکت");
    private final JButton stationBtn   = actionBtn("استقرار");
    private final JButton unstationBtn = actionBtn("خروج از سازه");
    private final JButton expandBtn    = actionBtn("توسعه مرز");
    private final JCheckBox autoExploreBox = new JCheckBox("کاوش خودکار");

    private final JPanel buildPanel = new JPanel();

    private Runnable moveAction, stationAction, unstationAction, expandAction;
    private BuildCB  buildAction;
    private BoolCB   autoExploreAction;

    private Unit current;

    public UnitInfoPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setPreferredSize(new Dimension(220, 0));
        setBackground(new Color(24, 32, 50));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(nameLbl);
        add(Box.createVerticalStrut(4));
        add(apLbl);
        add(posLbl);
        add(Box.createVerticalStrut(10));

        moveBtn.addActionListener(e -> { if (moveAction != null) moveAction.run(); });
        stationBtn.addActionListener(e -> { if (stationAction != null) stationAction.run(); });
        unstationBtn.addActionListener(e -> { if (unstationAction != null) unstationAction.run(); });
        expandBtn.addActionListener(e -> { if (expandAction != null) expandAction.run(); });

        autoExploreBox.setOpaque(false);
        autoExploreBox.setForeground(Color.WHITE);
        autoExploreBox.addActionListener(e -> {
            if (autoExploreAction != null) autoExploreAction.go(autoExploreBox.isSelected());
        });

        add(moveBtn);
        add(Box.createVerticalStrut(4));
        add(stationBtn);
        add(Box.createVerticalStrut(4));
        add(unstationBtn);
        add(Box.createVerticalStrut(4));
        add(expandBtn);
        add(Box.createVerticalStrut(4));
        add(autoExploreBox);
        add(Box.createVerticalStrut(10));

        buildPanel.setLayout(new BoxLayout(buildPanel, BoxLayout.Y_AXIS));
        buildPanel.setOpaque(false);
        for (BuildingType t : BuildingType.values()) {
            if (t == BuildingType.TOWN_HALL) continue;
            JButton b = actionBtn(t.getLabel());
            b.addActionListener(e -> { if (buildAction != null) buildAction.go(t); });
            buildPanel.add(b);
            buildPanel.add(Box.createVerticalStrut(4));
        }
        add(buildPanel);

        add(Box.createVerticalGlue());
        clear();
    }

    public void show(Unit u) {
        this.current = u;
        if (u == null) { clear(); return; }

        nameLbl.setText(u.getType().getLabel());
        apLbl.setText("AP: " + u.getCurrentAP() + "/" + u.getMaxAP());
        posLbl.setText("موقعیت: (" + u.getPosition().getQ() + "," + u.getPosition().getR() + ")");

        moveBtn.setEnabled(u.getCurrentAP() > 0);

        boolean isWorker = u instanceof Worker;
        stationBtn.setVisible(isWorker);
        unstationBtn.setVisible(isWorker);
        if (u instanceof Worker w) {
            stationBtn.setEnabled(!w.isStationed() && w.getPosition().hasBuilding());
            unstationBtn.setEnabled(w.isStationed());
        }

        boolean isExpander = u instanceof BorderExpander;
        expandBtn.setVisible(isExpander);
        expandBtn.setEnabled(isExpander && u.getCurrentAP() > 0);

        boolean isExplorer = u instanceof Explorer;
        autoExploreBox.setVisible(isExplorer);
        if (u instanceof Explorer explorer) autoExploreBox.setSelected(explorer.isAutoExplore());

        boolean isBuilder = u instanceof Builder;
        buildPanel.setVisible(isBuilder);

        revalidate();
        repaint();
    }

    public void clear() {
        current = null;
        nameLbl.setText("یونیتی انتخاب نشده");
        apLbl.setText("AP: —");
        posLbl.setText("موقعیت: —");
        moveBtn.setEnabled(false);
        stationBtn.setVisible(false);
        unstationBtn.setVisible(false);
        expandBtn.setVisible(false);
        autoExploreBox.setVisible(false);
        buildPanel.setVisible(false);
        revalidate();
        repaint();
    }

    public void setMoveAction(Runnable r)           { this.moveAction = r; }
    public void setStationAction(Runnable r)        { this.stationAction = r; }
    public void setUnstationAction(Runnable r)      { this.unstationAction = r; }
    public void setExpandAction(Runnable r)         { this.expandAction = r; }
    public void setBuildAction(BuildCB cb)          { this.buildAction = cb; }
    public void setAutoExploreAction(BoolCB cb)     { this.autoExploreAction = cb; }

    private static JLabel label(String txt) {
        JLabel l = new JLabel(txt);
        l.setForeground(Color.WHITE);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setFont(new Font("Dialog", Font.PLAIN, 12));
        return l;
    }

    private static JButton actionBtn(String txt) {
        JButton b = new JButton(txt);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(200, 28));
        b.setBackground(new Color(48, 65, 90));
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(new Font("Dialog", Font.PLAIN, 12));
        return b;
    }
}
