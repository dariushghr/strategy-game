package org.strategygame.view.menu;

import javax.swing.*;
import java.awt.*;

public class SettingsView extends JDialog {
    private final JSlider volumeSlider = new JSlider(0, 100, 70);

    public SettingsView(JFrame parent) {
        super(parent, "تنظیمات صدا", true);
        setSize(300, 140);
        setLocationRelativeTo(parent);
        setResizable(false);

        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBackground(new Color(22, 30, 48));
        p.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        JLabel lbl = new JLabel("حجم موسیقی:");
        lbl.setForeground(Color.WHITE);
        lbl.setFont(new Font("Dialog", Font.PLAIN, 13));
        p.add(lbl, BorderLayout.NORTH);

        volumeSlider.setMajorTickSpacing(25);
        volumeSlider.setPaintTicks(true);
        volumeSlider.setPaintLabels(true);
        volumeSlider.setOpaque(false);
        volumeSlider.setForeground(Color.LIGHT_GRAY);
        volumeSlider.addChangeListener(e ->
                AudioManager.getInstance().setVolume(volumeSlider.getValue() / 100f));
        p.add(volumeSlider, BorderLayout.CENTER);

        JButton close = new JButton("بستن");
        close.setBackground(new Color(55, 75, 110));
        close.setForeground(Color.WHITE);
        close.setFocusPainted(false);
        close.addActionListener(e -> dispose());
        p.add(close, BorderLayout.SOUTH);

        setContentPane(p);
    }
}
