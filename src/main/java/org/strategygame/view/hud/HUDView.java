package org.strategygame.view.hud;

import org.strategygame.model.GameState;
import org.strategygame.model.building.ProductionQueue;
import org.strategygame.model.building.TownHall;
import org.strategygame.model.resource.ResourceRate;
import org.strategygame.model.resource.ResourceType;
import org.strategygame.model.season.Season;

import javax.swing.*;
import java.awt.*;
import java.util.EnumMap;
import java.util.Map;

/** نوار بالای بازی: منابع، نوبت، فصل، شادی، صف تان هال و دسترسی به پنل‌ها. */
public class HUDView extends JPanel {

    public interface BoolCB { void go(boolean v); }

    private final Map<ResourceType, JLabel> resLabels = new EnumMap<>(ResourceType.class);

    private final JLabel unitLbl      = lbl("یونیت: —");
    private final JLabel turnLbl      = lbl("نوبت: 1");
    private final JLabel seasonLbl    = lbl("فصل: —");
    private final JLabel happinessLbl = lbl("شادی: —");
    private final JLabel queueLbl     = lbl("صف: —");
    private final JLabel townHallLbl  = lbl("تان هال: سطح —");
    private final JLabel warnLbl      = lbl("⚠ قحطی!");
    private final JLabel statusLbl    = lbl(" ");

    private final JCheckBox overlayBox = new JCheckBox("رنگ‌بندی سازه‌های قابل ساخت", true);
    private final JButton endTurnBtn   = accentBtn("پایان نوبت ▶", new Color(40, 110, 45));
    private final JButton townHallBtn  = toolBtn("تان هال");
    private final JButton tribesBtn    = toolBtn("قبیله‌ها");
    private final JButton tradeBtn     = toolBtn("تجارت");
    private final JButton logBtn       = toolBtn("رویدادها");
    private final JButton debugBtn     = toolBtn("دیباگ");

    private Runnable onEndTurn;
    private BoolCB   onOverlayToggle;
    private Timer    statusTimer;

    public HUDView() {
        setLayout(new BorderLayout());
        setBackground(new Color(18, 24, 38));
        setPreferredSize(new Dimension(0, 116));
        setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(60, 80, 120)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 8));
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

        JPanel mid = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 8));
        mid.setOpaque(false);
        mid.add(turnLbl);
        seasonLbl.setForeground(new Color(255, 225, 150));
        mid.add(seasonLbl);
        mid.add(happinessLbl);
        mid.add(unitLbl);
        mid.add(queueLbl);
        townHallLbl.setForeground(new Color(140, 235, 255));
        townHallLbl.setFont(townHallLbl.getFont().deriveFont(Font.BOLD));
        mid.add(townHallLbl);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        right.setOpaque(false);
        endTurnBtn.addActionListener(e -> { if (onEndTurn != null) onEndTurn.run(); });
        right.add(endTurnBtn);

        JPanel tools = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        tools.setOpaque(false);
        tools.add(townHallBtn);
        tools.add(tribesBtn);
        tools.add(tradeBtn);
        tools.add(logBtn);
        tools.add(debugBtn);

        overlayBox.setOpaque(false);
        overlayBox.setForeground(new Color(200, 215, 235));
        overlayBox.setFont(new Font("Dialog", Font.PLAIN, 12));
        overlayBox.addActionListener(e -> {
            if (onOverlayToggle != null) onOverlayToggle.go(overlayBox.isSelected());
        });

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(0, 14, 4, 14));
        statusLbl.setFont(new Font("Dialog", Font.BOLD, 12));
        bottom.add(tools,     BorderLayout.WEST);
        bottom.add(statusLbl, BorderLayout.CENTER);
        bottom.add(overlayBox, BorderLayout.EAST);

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        topRow.add(left,  BorderLayout.WEST);
        topRow.add(mid,   BorderLayout.CENTER);
        topRow.add(right, BorderLayout.EAST);

        JPanel rows = new JPanel(new BorderLayout());
        rows.setOpaque(false);
        rows.add(topRow, BorderLayout.CENTER);
        rows.add(bottom, BorderLayout.SOUTH);

        add(rows, BorderLayout.CENTER);
    }

    /** پیام کوتاه بالای صفحه؛ بعد از چند ثانیه پاک می‌شود. */
    public void showMessage(String text, boolean success) {
        String oneLine = text.replace('\n', ' ').trim();
        statusLbl.setText((success ? "✔ " : "✖ ") + oneLine);
        statusLbl.setForeground(success ? new Color(120, 240, 150) : new Color(255, 130, 130));

        if (statusTimer != null) statusTimer.stop();
        statusTimer = new Timer(8000, e -> {
            statusLbl.setText(" ");
            statusTimer.stop();
        });
        statusTimer.setRepeats(false);
        statusTimer.start();
    }

    public void refresh(GameState state) {
        ResourceRate rate = state.calcRate();
        for (ResourceType rt : ResourceType.values()) {
            int cur = state.getStorage().get(rt);
            int cap = state.getStorage().getCapacity(rt);
            int net = rate.getNet(rt);
            JLabel l = resLabels.get(rt);
            l.setText(rt.getLabel() + ": " + cur + "/" + cap
                    + " (" + (net >= 0 ? "+" : "") + net + ")");
            l.setForeground(net < 0 ? new Color(255, 90, 90) : new Color(170, 220, 170));
        }

        turnLbl.setText("نوبت: " + state.getTurn());

        Season season = state.getSeason();
        seasonLbl.setText("فصل: " + season.getLabel()
                + " (" + Season.turnWithinSeason(state.getTurn()) + "/10) — "
                + season.getEffectText());

        happinessLbl.setText("شادی: " + state.getHappiness().statusText());
        happinessLbl.setForeground(happinessColor(state.getHappiness().getValue()));

        unitLbl.setText("یونیت: " + state.getUnits().size() + "/" + state.getUnitCap()
                + " | نظامی: " + state.militaryUnitCount() + "/" + state.getMilitaryCap());

        TownHall townHall = state.getTownHall();
        if (townHall == null) {
            townHallLbl.setText("تان هال: نابود شده");
            queueLbl.setText("صف: —");
        } else {
            townHallLbl.setText("تان هال: سطح " + townHall.getLevel() + "/" + townHall.getMaxLevel()
                    + " | HP " + townHall.getHp() + "/" + townHall.getMaxHp());

            ProductionQueue queue = townHall.getQueue();
            queueLbl.setText(queue.isEmpty() ? "صف: —"
                    : "صف: " + queue.getLabel() + " | " + queue.getTurnsLeft() + " نوبت");
        }

        warnLbl.setVisible(state.isStarvation());
    }

    private Color happinessColor(int value) {
        if (value >= 3)  return new Color(140, 255, 170);
        if (value <= -5) return new Color(255, 100, 100);
        if (value <= -3) return new Color(255, 175, 100);
        return new Color(210, 220, 235);
    }

    public void showIdleWarning(Runnable confirmed) {
        int r = JOptionPane.showConfirmDialog(this,
                "یونیت‌های بیکار با AP باقی‌مانده وجود دارد.\nآیا نوبت را پایان می‌دهید؟",
                "هشدار", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r == JOptionPane.YES_OPTION) confirmed.run();
    }

    public void setOnEndTurn(Runnable r)      { this.onEndTurn = r; }
    public void setOnOverlayToggle(BoolCB cb) { this.onOverlayToggle = cb; }
    public boolean isOverlayOn()              { return overlayBox.isSelected(); }

    public void setOnTownHall(Runnable r) { bind(townHallBtn, r); }
    public void setOnTribes(Runnable r)   { bind(tribesBtn, r); }
    public void setOnTrade(Runnable r)    { bind(tradeBtn, r); }
    public void setOnLog(Runnable r)      { bind(logBtn, r); }
    public void setOnDebug(Runnable r)    { bind(debugBtn, r); }

    private void bind(JButton button, Runnable action) {
        button.addActionListener(e -> action.run());
    }

    /** تعداد اعلان‌های جدید روی دکمه‌ی رویدادها. */
    public void setPendingCount(int count) {
        logBtn.setText(count > 0 ? "رویدادها (" + count + ")" : "رویدادها");
        logBtn.setForeground(count > 0 ? new Color(255, 220, 120) : Color.WHITE);
    }

    private static JLabel lbl(String txt) {
        JLabel l = new JLabel(txt);
        l.setForeground(new Color(175, 215, 175));
        l.setFont(new Font("Dialog", Font.PLAIN, 12));
        return l;
    }

    private static JButton toolBtn(String txt) {
        return accentBtn(txt, new Color(48, 65, 90));
    }

    private static JButton accentBtn(String txt, Color background) {
        JButton b = new JButton(txt);
        b.setFont(new Font("Dialog", Font.BOLD, 12));
        b.setBackground(background);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(true);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(background.brighter(), 1),
                BorderFactory.createEmptyBorder(5, 12, 5, 12)));
        return b;
    }
}
