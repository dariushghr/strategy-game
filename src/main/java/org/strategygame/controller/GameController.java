package org.strategygame.controller;

import org.strategygame.model.GameState;
import org.strategygame.save.SaveException;
import org.strategygame.save.SaveSlot;
import org.strategygame.view.GameWindow;
import org.strategygame.view.menu.AudioManager;
import org.strategygame.view.menu.MainMenuView;
import org.strategygame.view.panel.SavePanel;

import javax.swing.JOptionPane;

public class GameController {

    private MainMenuView   menu;
    private GameWindow     window;
    private SaveController saveCtrl;

    public void showMenu() {
        menu = new MainMenuView();
        menu.setStartCB(this::startGame);
        menu.setLoadCB(this::openMenuLoad);
        menu.setExitCB(this::confirmExit);
        menu.setVisible(true);
    }

    private void startGame() {
        GameState state = new GameState();
        state.initialize();
        openSession(state, true);
    }

    private void openMenuLoad() {
        SaveController loader = new SaveController(new GameServices().saves());
        new SavePanel(menu, loader, SavePanel.Mode.LOAD, slot -> {
            try {
                GameState loaded = loader.load(slot);
                menu.setVisible(false);
                openSession(loaded, false);
            } catch (SaveException e) {
                JOptionPane.showMessageDialog(menu, e.getMessage(), "بارگذاری",
                        JOptionPane.WARNING_MESSAGE);
            }
        }).setVisible(true);
    }

    private void openSession(GameState state, boolean spawnTribes) {
        GameServices services = new GameServices();
        if (spawnTribes) {
            services.tribe().spawnInitialTribes(state);
            services.tribe().updateDiscovery(state);
        }

        saveCtrl = new SaveController(services.saves());
        saveCtrl.bind(state);

        UnitController     unitCtrl   = new UnitController(state, services);
        BuildingController bldCtrl    = new BuildingController(state);
        CombatController   combatCtrl = new CombatController(state, services);
        TribeController    tribeCtrl  = new TribeController(state, services);
        TradeController    tradeCtrl  = new TradeController(state, services);
        TurnController     turnCtrl   = new TurnController(state, services);

        if (window != null) window.dispose();
        window = new GameWindow(state, turnCtrl, unitCtrl, bldCtrl,
                combatCtrl, tribeCtrl, tradeCtrl, saveCtrl, this::loadIntoCurrentWindow);
        saveCtrl.setBusyReason(window::busyMessage);
        turnCtrl.setWindow(window);
        turnCtrl.setSaveController(saveCtrl);
        turnCtrl.setEventBus(services.events());
        window.setVisible(true);

        AudioManager.getInstance().play("/assets/theme.wav");
    }

    private void loadIntoCurrentWindow(SaveSlot slot) {
        if (saveCtrl == null) return;
        try {
            GameState loaded = saveCtrl.load(slot);
            openSession(loaded, false);
        } catch (SaveException e) {
            JOptionPane.showMessageDialog(window, e.getMessage(), "بارگذاری",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private void confirmExit() {
        int r = JOptionPane.showConfirmDialog(menu,
                "از بازی خارج شوید؟", "خروج", JOptionPane.YES_NO_OPTION);
        if (r == JOptionPane.YES_OPTION) System.exit(0);
    }
}
