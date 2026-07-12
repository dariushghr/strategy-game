package org.strategygame.view.menu;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class MainMenuView extends JFrame {
    private Runnable startCB, exitCB;

    public MainMenuView() {
        setTitle("بازی استراتژیک");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(420, 320);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel bg = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(15, 20, 40),
                        0, getHeight(), new Color(25, 40, 70));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        bg.setLayout(new GridBagLayout());

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(8, 50, 8, 50);

        JLabel title = new JLabel("⚔  بازی استراتژیک  ⚔", SwingConstants.CENTER);
        title.setFont(new Font("Dialog", Font.BOLD, 22));
        title.setForeground(new Color(220, 190, 80));
        gc.gridy = 0; gc.insets = new Insets(20, 30, 20, 30);
        bg.add(title, gc);
        gc.insets = new Insets(6, 50, 6, 50);

        gc.gridy = 1; bg.add(menuBtn("شروع بازی", new Color(40, 110, 50), e -> {
            setVisible(false);
            if (startCB != null) startCB.run();
        }), gc);

        gc.gridy = 2; bg.add(menuBtn("تنظیمات", new Color(55, 75, 110), e ->
                new SettingsView(MainMenuView.this).setVisible(true)), gc);

        gc.gridy = 3; bg.add(menuBtn("خروج", new Color(110, 40, 40), e -> {
            if (exitCB != null) exitCB.run();
        }), gc);

        setContentPane(bg);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                if (exitCB != null) exitCB.run();
            }
        });
    }

    private JButton menuBtn(String txt, Color bg, ActionListener al) {
        JButton b = new JButton(txt);
        b.setFont(new Font("Dialog", Font.BOLD, 15));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bg.brighter(), 1),
                BorderFactory.createEmptyBorder(9, 20, 9, 20)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(al);
        return b;
    }

    public void setStartCB(Runnable r) { this.startCB = r; }
    public void setExitCB(Runnable r)  { this.exitCB  = r; }
}
