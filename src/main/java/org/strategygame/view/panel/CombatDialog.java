package org.strategygame.view.panel;

import org.strategygame.model.combat.CombatResult;
import org.strategygame.model.combat.DicePair;

import javax.swing.*;
import java.awt.*;

/** نمایش تاس‌ها، جفت‌ها و تلفات نبرد. */
public final class CombatDialog {

    private CombatDialog() { }

    public static void show(Window owner, CombatResult result) {
        if (result == null) return;
        JDialog dialog = new JDialog(owner, "نتیجه نبرد", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(420, 380);
        dialog.setLocationRelativeTo(owner);

        JTextArea area = new JTextArea(result.report());
        area.setEditable(false);
        area.setFont(new Font("Dialog", Font.PLAIN, 13));
        area.setBackground(new Color(24, 30, 46));
        area.setForeground(new Color(220, 230, 240));
        area.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JPanel dice = new JPanel(new GridLayout(0, 1, 4, 4));
        dice.setBackground(new Color(24, 30, 46));
        dice.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        if (!result.isStructureAttack()) {
            dice.add(label("تاس حمله: " + result.getAttackDice()));
            dice.add(label("تاس دفاع: " + result.getDefenseDice()
                    + (result.getWallBonus() > 0 ? "  (+" + result.getWallBonus() + " دیوار)" : "")));
            int i = 1;
            for (DicePair pair : result.getPairs()) {
                dice.add(label("جفت " + i++ + ": " + pair.text()));
            }
        }

        JButton ok = new JButton("باشه");
        ok.addActionListener(e -> dialog.dispose());

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.setBackground(new Color(24, 30, 46));
        south.add(ok);

        dialog.add(dice, BorderLayout.NORTH);
        dialog.add(new JScrollPane(area), BorderLayout.CENTER);
        dialog.add(south, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(new Color(230, 210, 140));
        return l;
    }
}
