package org.strategygame;

import org.strategygame.controller.GameController;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GameController().showMenu());
    }
}
