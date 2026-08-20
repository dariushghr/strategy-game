package org.strategygame.view.panel;

import org.strategygame.controller.SaveController;
import org.strategygame.save.SaveInfo;
import org.strategygame.save.SaveSlot;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Consumer;

/** سه اسلات دستی و یک ذخیرهٔ خودکار. */
public class SavePanel extends JDialog {

    public enum Mode { SAVE, LOAD }

    public SavePanel(Window owner, SaveController saves, Mode mode, Consumer<SaveSlot> onSlot) {
        super(owner, mode == Mode.SAVE ? "ذخیره بازی" : "بارگذاری بازی", ModalityType.APPLICATION_MODAL);
        setSize(460, 340);
        setLocationRelativeTo(owner);
        setResizable(false);

        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        list.setBackground(new Color(22, 28, 42));

        for (SaveInfo info : saves.slots()) {
            boolean loadable = info.occupied() && info.problem() == null;
            boolean savable = mode == Mode.SAVE && !info.slot().isAutosave();
            boolean enabled = mode == Mode.LOAD ? loadable : savable;

            JButton row = new JButton(rowText(info));
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
            row.setEnabled(enabled);
            if (info.slot().isAutosave()) {
                row.setToolTipText("ذخیرهٔ خودکار جدا از اسلات‌های دستی است");
            } else if (mode == Mode.SAVE && !enabled) {
                row.setToolTipText("این اسلات برای ذخیرهٔ دستی نیست");
            } else if (mode == Mode.LOAD && info.problem() != null) {
                row.setToolTipText(info.problem());
            }
            SaveSlot slot = info.slot();
            row.addActionListener(e -> {
                dispose();
                if (onSlot != null) onSlot.accept(slot);
            });
            list.add(row);
            list.add(Box.createVerticalStrut(8));
        }

        JButton cancel = new JButton("انصراف");
        cancel.addActionListener(e -> dispose());

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.setBackground(new Color(22, 28, 42));
        south.add(cancel);

        getContentPane().setBackground(new Color(22, 28, 42));
        add(list, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
    }

    private static String rowText(SaveInfo info) {
        if (!info.occupied()) return info.slot().label() + "  —  خالی";
        if (info.problem() != null) return info.slot().label() + "  —  " + info.problem();
        String when = info.savedAt() <= 0 ? ""
                : "  |  " + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(info.savedAt()));
        return info.slot().label() + "  —  نوبت " + info.turn() + when;
    }
}
