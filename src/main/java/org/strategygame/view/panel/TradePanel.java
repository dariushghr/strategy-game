package org.strategygame.view.panel;

import org.strategygame.controller.TradeController;
import org.strategygame.model.resource.ResourceType;
import org.strategygame.model.trade.TradeStrategy;
import org.strategygame.model.tribe.Tribe;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/** پنل تجارت: انتخاب کانال، منبع مبدا و مقصد، مقدار و پیش‌نمایش نتیجه. */
public class TradePanel extends JDialog {

    private static final Color BG = new Color(22, 30, 48);

    /** اجرای معامله روی کانال انتخاب‌شده. */
    public interface TradeCB {
        void go(TradeStrategy strategy, ResourceType source, ResourceType target, int amount);
    }

    private final TradeController controller;

    private final JComboBox<String>       channelBox = new JComboBox<>();
    private final JComboBox<ResourceType> sourceBox  = new JComboBox<>(ResourceType.values());
    private final JComboBox<ResourceType> targetBox  = new JComboBox<>(ResourceType.values());
    private final JSpinner amountSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 500, 5));

    private final JLabel channelInfo = info("—");
    private final JLabel preview     = info("—");
    private final JButton tradeBtn   = new JButton("انجام معامله");

    private final List<TradeStrategy> channels = new java.util.ArrayList<>();
    private final List<String>        problems = new java.util.ArrayList<>();

    private TradeCB onTrade;

    public TradePanel(JFrame parent, TradeController controller) {
        super(parent, "تجارت", false);
        this.controller = controller;
        setSize(520, 420);
        setLocationRelativeTo(parent);
        buildLayout();
    }

    private void buildLayout() {
        getContentPane().setBackground(BG);
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        panel.add(title("کانال تجاری"));
        panel.add(channelBox);
        panel.add(channelInfo);
        panel.add(Box.createVerticalStrut(10));

        panel.add(title("منبعی که می‌دهید"));
        panel.add(sourceBox);
        panel.add(Box.createVerticalStrut(6));
        panel.add(title("منبعی که می‌گیرید"));
        panel.add(targetBox);
        panel.add(Box.createVerticalStrut(6));
        panel.add(title("مقدار"));
        panel.add(amountSpinner);
        panel.add(Box.createVerticalStrut(10));

        panel.add(title("نتیجه"));
        panel.add(preview);
        panel.add(Box.createVerticalStrut(10));

        tradeBtn.setBackground(new Color(40, 110, 45));
        tradeBtn.setForeground(Color.WHITE);
        tradeBtn.setFocusPainted(false);
        tradeBtn.setContentAreaFilled(false);
        tradeBtn.setOpaque(true);
        tradeBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 160, 80)),
                BorderFactory.createEmptyBorder(6, 14, 6, 14)));
        tradeBtn.addActionListener(e -> execute());
        panel.add(tradeBtn);

        channelBox.addActionListener(e -> updatePreview());
        sourceBox.addActionListener(e -> updatePreview());
        targetBox.addActionListener(e -> updatePreview());
        amountSpinner.addChangeListener(e -> updatePreview());

        getContentPane().add(panel);
    }

    public void open(Tribe preselected) {
        rebuildChannels(preselected);
        setVisible(true);
    }

    public void refresh() {
        if (isVisible()) rebuildChannels(null);
    }

    /** فهرست کانال‌ها هر بار از نو ساخته می‌شود چون قبیله‌ها و سازه‌ها عوض می‌شوند. */
    private void rebuildChannels(Tribe preselected) {
        String previous = (String) channelBox.getSelectedItem();

        channels.clear();
        problems.clear();
        channelBox.removeAllItems();

        addChannel("بازار امپراتوری", controller.bazaar(), controller.bazaarProblem());
        addChannel("پاسگاه تجاری بی‌طرف", controller.tradingPost(), controller.tradingPostProblem());

        int preselectedIndex = -1;
        for (Tribe tribe : controller.knownTribesForTrade()) {
            if (tribe == preselected) preselectedIndex = channels.size();
            addChannel("قبیله‌ی " + tribe.getName(),
                    controller.tribe(tribe), controller.tribeProblem(tribe));
        }

        if (preselectedIndex >= 0) channelBox.setSelectedIndex(preselectedIndex);
        else if (previous != null) channelBox.setSelectedItem(previous);

        updatePreview();
    }

    private void addChannel(String name, TradeStrategy strategy, String problem) {
        channels.add(strategy);
        problems.add(strategy == null ? (problem == null ? "در دسترس نیست" : problem) : problem);
        channelBox.addItem(name);
    }

    private void updatePreview() {
        int index = channelBox.getSelectedIndex();
        if (index < 0 || index >= channels.size()) {
            channelInfo.setText("کانالی در دسترس نیست");
            preview.setText("—");
            tradeBtn.setEnabled(false);
            return;
        }

        TradeStrategy strategy = channels.get(index);
        String channelProblem  = problems.get(index);

        if (strategy == null || channelProblem != null) {
            channelInfo.setText("✖ " + (channelProblem == null ? "در دسترس نیست" : channelProblem));
            channelInfo.setForeground(new Color(255, 150, 150));
            preview.setText("—");
            tradeBtn.setEnabled(false);
            return;
        }

        channelInfo.setText(strategy.getName() + " — " + strategy.rateText()
                + " | حداکثر " + strategy.getMaxAmount() + " واحد در هر معامله"
                + (strategy.allowedTargets().isEmpty() ? "" : " | فقط خروجی مشخص"));
        channelInfo.setForeground(new Color(160, 225, 175));

        ResourceType source = (ResourceType) sourceBox.getSelectedItem();
        ResourceType target = (ResourceType) targetBox.getSelectedItem();
        int amount = (Integer) amountSpinner.getValue();

        String problem = controller.tradeProblem(strategy, source, target, amount);
        if (problem != null) {
            preview.setText("✖ " + problem);
            preview.setForeground(new Color(255, 150, 150));
            tradeBtn.setEnabled(false);
            return;
        }

        int received = (int) Math.floor(amount * strategy.getRate());
        preview.setText("✔ " + amount + " " + source.getLabel() + " → "
                + received + " " + target.getLabel());
        preview.setForeground(new Color(150, 240, 170));
        tradeBtn.setEnabled(true);
    }

    private void execute() {
        int index = channelBox.getSelectedIndex();
        if (index < 0 || onTrade == null) return;

        onTrade.go(channels.get(index),
                (ResourceType) sourceBox.getSelectedItem(),
                (ResourceType) targetBox.getSelectedItem(),
                (Integer) amountSpinner.getValue());
        updatePreview();
    }

    public void setOnTrade(TradeCB cb) { this.onTrade = cb; }

    private static JLabel title(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(new Color(255, 200, 60));
        l.setFont(new Font("Dialog", Font.BOLD, 12));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private static JLabel info(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(new Color(215, 225, 240));
        l.setFont(new Font("Dialog", Font.PLAIN, 12));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }
}
