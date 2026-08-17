package org.strategygame.view.panel;

import org.strategygame.controller.TribeController;
import org.strategygame.model.resource.ResourceType;
import org.strategygame.model.tribe.Tribe;
import org.strategygame.model.tribe.mission.TribeMission;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

/** پنل دیپلماسی: وضعیت رابطه، هدیه، جنگ و صلح، اتحاد و مأموریت‌ها. */
public class TribePanel extends JDialog {

    private static final Color BG = new Color(22, 30, 48);

    /** هدیه دادن یک مقدار مشخص از یک منبع به یک قبیله. */
    public interface GiftCB { void go(Tribe tribe, ResourceType type, int amount); }

    private final TribeController controller;

    private Consumer<Tribe> onWar, onPeace, onAlliance, onAcceptMission,
                            onTurnIn, onCancelMission, onTrade;
    private GiftCB onGift;
    private java.util.function.Function<Tribe, String> tradeProblemProvider;

    public TribePanel(JFrame parent, TribeController controller) {
        super(parent, "قبیله‌ها", false);
        this.controller = controller;
        setSize(560, 700);
        setLocationRelativeTo(parent);
    }

    public void open() {
        rebuild();
        setVisible(true);
    }

    public void refresh() {
        if (isVisible()) rebuild();
    }

    private void rebuild() {
        getContentPane().removeAll();
        getContentPane().setBackground(BG);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        List<Tribe> tribes = controller.knownTribes();
        if (tribes.isEmpty()) {
            panel.add(title("هنوز قبیله‌ای کشف نکرده‌اید"));
            panel.add(info("با اکسپلورر نقشه را کشف کنید تا اردوگاه قبیله‌ها پیدا شود."));
        }
        for (Tribe tribe : tribes) panel.add(tribeCard(tribe));

        List<Tribe> conquered = controller.conqueredTribes();
        if (!conquered.isEmpty()) {
            panel.add(Box.createVerticalStrut(10));
            panel.add(title("قبیله‌های فتح‌شده"));
            for (Tribe tribe : conquered) {
                panel.add(info("✖ " + tribe.getName() + " — اردوگاه به پاسگاه شما تبدیل شد"));
            }
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

    private JPanel tribeCard(Tribe tribe) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(new Color(28, 38, 58));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 5, 0, 0, statusColor(tribe)),
                BorderFactory.createEmptyBorder(8, 10, 10, 10)));

        card.add(title(tribe.describe()
                + (tribe.isAtWar() ? "  ⚔ در جنگ" : "")
                + (tribe.isAllied() ? "  ★ متحد" : "")));

        String where = tribe.getCampHex() == null ? "—"
                : "(" + tribe.getCampHex().getQ() + "," + tribe.getCampHex().getR() + ")";
        card.add(info("اردوگاه " + where
                + (controller.isCampVisible(tribe) ? " — در دید شما" : " — خارج از دید (اطلاعات ذخیره‌شده)")
                + " | HP " + tribe.getCamp().getHp() + "/" + tribe.getCamp().getMaxHp()));
        card.add(info("تجارت: " + tribe.getType().tradeText()
                + " | غنیمت در صورت فتح: " + tribe.getType().lootText()));
        card.add(info("نگهبان‌ها: " + tribe.aliveUnits().size()
                + "/" + tribe.getType().getGuardCap()));

        addMissionSection(card, tribe);

        card.add(Box.createVerticalStrut(6));
        card.add(subTitle("هدیه (رابطه را زیاد می‌کند)"));
        for (ResourceType type : ResourceType.values()) {
            int unit = controller.giftUnit(type);
            int amount = unit * 2;
            String label = "هدیه " + amount + " " + type.getLabel()
                    + " (+" + controller.giftGainPerUnit(type) * 2 + " رابطه)";
            card.add(actionButton(label, controller.giftProblem(tribe, type, amount),
                    () -> { if (onGift != null) onGift.go(tribe, type, amount); }));
        }

        card.add(Box.createVerticalStrut(6));
        card.add(subTitle("دیپلماسی"));
        card.add(actionButton("تجارت با این قبیله", tradeProblem(tribe),
                () -> { if (onTrade != null) onTrade.accept(tribe); }));
        card.add(actionButton("اعلام جنگ", controller.declareWarProblem(tribe),
                () -> { if (onWar != null) onWar.accept(tribe); }));
        card.add(actionButton("درخواست صلح", controller.peaceProblem(tribe),
                () -> { if (onPeace != null) onPeace.accept(tribe); }));
        card.add(actionButton("پیشنهاد اتحاد", controller.allianceProblem(tribe),
                () -> { if (onAlliance != null) onAlliance.accept(tribe); }));

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        wrapper.add(card, BorderLayout.CENTER);
        return wrapper;
    }

    private void addMissionSection(JPanel card, Tribe tribe) {
        card.add(Box.createVerticalStrut(6));
        card.add(subTitle("مأموریت"));

        TribeMission mission = tribe.getActiveMission();
        if (mission == null) {
            card.add(info("پیشنهاد فعلی: " + controller.missionPreview(tribe).replace("\n", " | ")));
            card.add(actionButton("پذیرش مأموریت", controller.acceptMissionProblem(tribe),
                    () -> { if (onAcceptMission != null) onAcceptMission.accept(tribe); }));
            return;
        }

        card.add(info(mission.summary() + " — " + mission.getObjectiveText()));
        card.add(info("جایزه: " + mission.rewardText()));
        card.add(actionButton("تحویل مأموریت", controller.turnInProblem(tribe),
                () -> { if (onTurnIn != null) onTurnIn.accept(tribe); }));
        card.add(actionButton("لغو مأموریت (رابطه کم می‌شود)",
                mission.isOpen() ? null : "مأموریت فعالی برای لغو نیست",
                () -> { if (onCancelMission != null) onCancelMission.accept(tribe); }));
    }

    /** دلیل غیرمجاز بودن تجارت با این قبیله را پنل تجارت هم استفاده می‌کند. */
    private String tradeProblem(Tribe tribe) {
        return tradeProblemProvider == null ? null : tradeProblemProvider.apply(tribe);
    }

    public void setTradeProblemProvider(java.util.function.Function<Tribe, String> f) {
        this.tradeProblemProvider = f;
    }

    private Color statusColor(Tribe tribe) {
        if (tribe.isAtWar())  return new Color(220, 80, 80);
        if (tribe.isAllied()) return new Color(110, 240, 180);
        return switch (tribe.getRelationState()) {
            case ENEMY      -> new Color(220, 80, 80);
            case DISPLEASED -> new Color(230, 160, 80);
            case NEUTRAL    -> new Color(160, 175, 200);
            case FRIENDLY   -> new Color(140, 220, 150);
            case ALLIED     -> new Color(110, 240, 180);
        };
    }

    private JButton actionButton(String label, String problem, Runnable action) {
        boolean enabled = problem == null;
        JButton b = new JButton("<html><table width='450'><tr><td><b>" + label + "</b><br>"
                + "<span style='color:" + (enabled ? "#7dff9a" : "#ff9a9a") + "'>"
                + (enabled ? "✔ قابل انجام" : "✖ " + problem) + "</span></td></tr></table></html>");
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(480, 90));
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setContentAreaFilled(false);
        b.setOpaque(true);
        b.setBackground(enabled ? new Color(40, 60, 88) : new Color(38, 42, 54));
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(new Font("Dialog", Font.PLAIN, 12));
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 92, 125)),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)));
        b.setEnabled(enabled);
        b.setToolTipText(enabled ? "قابل انجام است" : problem);
        b.addActionListener(e -> action.run());
        return b;
    }

    private JLabel title(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(new Color(255, 210, 110));
        l.setFont(new Font("Dialog", Font.BOLD, 14));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JLabel subTitle(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(new Color(150, 220, 255));
        l.setFont(new Font("Dialog", Font.BOLD, 12));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JLabel info(String text) {
        JLabel l = new JLabel("<html><div style='width:450px'>" + text + "</div></html>");
        l.setForeground(new Color(210, 220, 235));
        l.setFont(new Font("Dialog", Font.PLAIN, 12));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    public void setOnGift(GiftCB cb)                     { this.onGift = cb; }
    public void setOnWar(Consumer<Tribe> cb)             { this.onWar = cb; }
    public void setOnPeace(Consumer<Tribe> cb)           { this.onPeace = cb; }
    public void setOnAlliance(Consumer<Tribe> cb)        { this.onAlliance = cb; }
    public void setOnAcceptMission(Consumer<Tribe> cb)   { this.onAcceptMission = cb; }
    public void setOnTurnIn(Consumer<Tribe> cb)          { this.onTurnIn = cb; }
    public void setOnCancelMission(Consumer<Tribe> cb)   { this.onCancelMission = cb; }
    public void setOnTrade(Consumer<Tribe> cb)           { this.onTrade = cb; }
}
