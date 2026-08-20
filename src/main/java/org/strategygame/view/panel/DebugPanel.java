package org.strategygame.view.panel;

import org.strategygame.controller.TurnController;
import org.strategygame.model.GameState;
import org.strategygame.model.disaster.DisasterType;
import org.strategygame.model.resource.ResourceType;
import org.strategygame.model.season.Season;

import javax.swing.*;
import java.awt.*;

/**
 * ابزار تست سناریوها. سناریوهای پذیرش (زمین‌لرزه، سیل، حمله خرس، تغییر فصل)
 * شانس تصادفی دارند، پس برای بازبینی دستی از اینجا اجرا می‌شوند.
 */
public class DebugPanel extends JDialog {

    private static final Color BG = new Color(20, 20, 28);

    private final GameState      state;
    private final TurnController turnController;
    private final JLabel         status = new JLabel("—");
    private Runnable onChange;

    public DebugPanel(JFrame parent, GameState state, TurnController turnController) {
        super(parent, "ابزار تست", false);
        this.state          = state;
        this.turnController = turnController;
        setSize(430, 470);
        setLocationRelativeTo(parent);
        buildLayout();
    }

    private void buildLayout() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        panel.add(title("بلایای طبیعی"));
        for (DisasterType type : DisasterType.values()) {
            panel.add(button(type.getLabel(), () -> {
                if (!turnController.canForce(type)) {
                    report("هدف مناسبی برای " + type.getLabel() + " روی نقشه نیست");
                    return;
                }
                var event = turnController.forceDisaster(type);
                report(event == null
                        ? type.getLabel() + " اجرا نشد"
                        : event.headline() + " — " + event.getEffects().size() + " اثر");
            }));
        }

        panel.add(Box.createVerticalStrut(10));
        panel.add(title("فصل"));
        JPanel seasons = new JPanel(new GridLayout(1, 4, 6, 0));
        seasons.setBackground(BG);
        seasons.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (Season season : Season.values()) {
            seasons.add(button(season.getLabel(), () -> {
                turnController.forceSeason(season);
                report("فصل: " + season.getLabel() + " — " + season.getEffectText());
            }));
        }
        panel.add(seasons);

        panel.add(Box.createVerticalStrut(10));
        panel.add(title("منابع"));
        panel.add(button("+۵۰۰ از همه منابع", () -> {
            for (ResourceType type : ResourceType.values()) state.getStorage().add(type, 500);
            report("منابع اضافه شد (تا سقف ظرفیت انبار)");
        }));
        panel.add(button("خالی کردن انبار", () -> {
            for (ResourceType type : ResourceType.values())
                state.getStorage().deduct(type, state.getStorage().get(type));
            report("انبار خالی شد");
        }));

        panel.add(Box.createVerticalStrut(10));
        panel.add(title("نقشه"));
        panel.add(button("برداشتن فوگ از کل نقشه", () -> {
            state.getFog().revealAll();
            report("کل نقشه آشکار شد");
        }));

        panel.add(Box.createVerticalStrut(12));
        status.setForeground(new Color(150, 240, 170));
        status.setFont(new Font("Dialog", Font.PLAIN, 12));
        status.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(status);

        JScrollPane scroll = new JScrollPane(panel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        getContentPane().setBackground(BG);
        getContentPane().add(scroll);
    }

    private void report(String message) {
        status.setText(message);
        if (onChange != null) onChange.run();
    }

    public void setOnChange(Runnable r) { this.onChange = r; }

    public void open() { setVisible(true); }

    private JButton button(String label, Runnable action) {
        JButton b = new JButton(label);
        b.setBackground(new Color(46, 52, 74));
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(true);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(80, 90, 120)),
                BorderFactory.createEmptyBorder(5, 12, 5, 12)));
        b.addActionListener(e -> action.run());
        return b;
    }

    private static JLabel title(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(new Color(255, 200, 60));
        l.setFont(new Font("Dialog", Font.BOLD, 12));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }
}
