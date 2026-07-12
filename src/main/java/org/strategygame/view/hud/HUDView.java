package org.strategygame.view.hud;

import org.strategygame.model.GameState;
import org.strategygame.model.building.ProductionQueue;
import org.strategygame.model.resource.ResourceRate;
import org.strategygame.model.resource.ResourceType;

import javax.swing.*;
import java.awt.*;
import java.util.EnumMap;
import java.util.Map;

public class HUDView extends JPanel {

    private final Map<ResourceType, JLabel> resLabels = new EnumMap<>(ResourceType.class);
    private final JLabel unitLbl      = lbl("یونیت: —");
    private final JLabel turnLbl      = lbl("نوبت: 1");
    private final JLabel queueLbl     = lbl("صف: —");
    private final JLabel warnLbl      = lbl("⚠ قحطی!");
    private final JButton endTurnBtn  = new JButton("پایان نوبت ▶");
    private Runnable onEndTurn;

    public HUDView() {
        setLayout(new BorderLayout());
        setBackground(new Color(18, 24, 38));
        setPreferredSize(new Dimension(0, 62));
        setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(60, 80, 120)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 10));
        left.setOpaque(false);
        for (ResourceType rt : ResourceType.values()) {
            JLabel l = lbl(rt.getLabel() + ": —");
            resLabels.put(rt, l);
            left.add(l);
        }
        warnLbl.setForeground(new Color(255, 80, 80));
        warnLbl.setFont(warnLbl.getFont().deriveFont(Font.BOLD));
        warnLbl.setVisible(false);
        left.add(warnLbl);

        JPanel mid = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 10));
        mid.setOpaque(false);
        mid.add(turnLbl);
        mid.add(unitLbl);
        mid.add(queueLbl);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 8));
        right.setOpaque(false);
        endTurnBtn.setFont(new Font("Dialog", Font.BOLD, 13));
        endTurnBtn.setBackground(new Color(40, 110, 45));
        endTurnBtn.setForeground(Color.WHITE);
        endTurnBtn.setFocusPainted(false);
        endTurnBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 160, 80), 1),
                BorderFactory.createEmptyBorder(5, 12, 5, 12)));
        endTurnBtn.addActionListener(e -> {
            if (onEndTurn != null) onEndTurn.run();
        });
        right.add(endTurnBtn);

        add(left,  BorderLayout.WEST);
        add(mid,   BorderLayout.CENTER);
        add(right, BorderLayout.EAST);
    }

    public void refresh(GameState state) {
        ResourceRate rate = state.calcRate();
        for (ResourceType rt : ResourceType.values()) {
            int cur = state.getStorage().get(rt);
            int cap = state.getStorage().getCapacity(rt);
            int net = rate.getNet(rt);
            String txt = rt.getLabel() + ": " + cur + "/" + cap
                    + " (" + (net >= 0 ? "+" : "") + net + ")";
            JLabel l = resLabels.get(rt);
            l.setText(txt);
            l.setForeground(net < 0 ? new Color(255, 90, 90) : new Color(170, 220, 170));
        }
        unitLbl.setText("یونیت: " + state.getUnits().size() + "/" + state.getUnitCap());
        turnLbl.setText("نوبت: " + state.getTurn());

        ProductionQueue q = state.getTownHall().getQueue();
        queueLbl.setText(q.isEmpty() ? "صف: —"
                : "صف: " + q.getItem() + " | " + q.getTurnsLeft() + " نوبت");

        warnLbl.setVisible(state.isStarvation());
    }

    public void showIdleWarning(Runnable confirmed) {
        int r = JOptionPane.showConfirmDialog(this,
                "یونیت‌های بیکار با AP باقی‌مانده وجود دارد.\nآیا نوبت را پایان می‌دهید؟",
                "هشدار", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r == JOptionPane.YES_OPTION) confirmed.run();
    }

    public void setOnEndTurn(Runnable r) { this.onEndTurn = r; }

    private static JLabel lbl(String txt) {
        JLabel l = new JLabel(txt);
        l.setForeground(new Color(175, 215, 175));
        l.setFont(new Font("Dialog", Font.PLAIN, 12));
        return l;
    }
}
