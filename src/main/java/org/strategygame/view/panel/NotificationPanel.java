package org.strategygame.view.panel;

import org.strategygame.model.event.Notification;
import org.strategygame.model.event.NotificationCenter;
import org.strategygame.model.event.NotificationKind;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/** گزارش رویدادها؛ آخرین اعلان‌ها با فیلتر دسته. */
public class NotificationPanel extends JDialog {

    private static final Color BG = new Color(18, 24, 38);
    private static final int SHOWN = 60;

    private final NotificationCenter center;
    private final JPanel list = new JPanel();
    private final JComboBox<String> filterBox = new JComboBox<>();

    public NotificationPanel(JFrame parent, NotificationCenter center) {
        super(parent, "گزارش رویدادها", false);
        this.center = center;
        setSize(520, 460);
        setLocationRelativeTo(parent);

        getContentPane().setBackground(BG);
        getContentPane().setLayout(new BorderLayout());

        filterBox.addItem("همه");
        for (NotificationKind kind : NotificationKind.values()) filterBox.addItem(kind.getLabel());
        filterBox.addActionListener(e -> refresh());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        top.setBackground(BG);
        JLabel label = new JLabel("دسته:");
        label.setForeground(new Color(255, 200, 60));
        top.add(label);
        top.add(filterBox);
        getContentPane().add(top, BorderLayout.NORTH);

        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(BG);
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        getContentPane().add(scroll, BorderLayout.CENTER);
    }

    public void open() {
        refresh();
        setVisible(true);
        center.drainPending();
    }

    public void refresh() {
        list.removeAll();

        String selected = (String) filterBox.getSelectedItem();
        List<Notification> all = center.latest(SHOWN);

        int shown = 0;
        for (int i = all.size() - 1; i >= 0; i--) {
            Notification n = all.get(i);
            if (selected != null && !selected.equals("همه")
                    && !n.kind().getLabel().equals(selected)) continue;
            list.add(row(n));
            shown++;
        }

        if (shown == 0) {
            JLabel empty = new JLabel("رویدادی ثبت نشده است");
            empty.setForeground(new Color(150, 165, 190));
            empty.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            list.add(empty);
        }

        list.revalidate();
        list.repaint();
        if (isVisible()) center.drainPending();
    }

    private static JPanel row(Notification n) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(new Color(26, 34, 52));
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 1, 0,
                        kindColor(n.kind())),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel head = new JLabel("نوبت " + n.turn() + " • " + n.kind().getLabel());
        head.setForeground(kindColor(n.kind()));
        head.setFont(new Font("Dialog", Font.BOLD, 11));

        JLabel body = new JLabel(n.message());
        body.setForeground(new Color(220, 228, 242));
        body.setFont(new Font("Dialog", Font.PLAIN, 12));

        row.add(head, BorderLayout.NORTH);
        row.add(body, BorderLayout.CENTER);
        return row;
    }

    private static Color kindColor(NotificationKind kind) {
        return switch (kind) {
            case WAR, TRIBE_ATTACK, COMBAT -> new Color(235, 95, 95);
            case DISASTER                  -> new Color(240, 150, 60);
            case MISSION                   -> new Color(150, 190, 255);
            case TRADE                     -> new Color(120, 220, 180);
            case RELATION, GUARD_SPAWNED   -> new Color(230, 200, 110);
            case HAPPINESS                 -> new Color(215, 150, 235);
            case SEASON                    -> new Color(130, 210, 235);
            default                        -> new Color(180, 195, 215);
        };
    }
}
