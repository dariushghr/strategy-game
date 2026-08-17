package org.strategygame.controller;

import org.strategygame.model.GameState;
import org.strategygame.view.GameWindow;
import org.strategygame.view.menu.AudioManager;
import org.strategygame.view.menu.MainMenuView;

import javax.swing.JOptionPane;

public class GameController {

    private MainMenuView menu;
    private GameWindow   window;

    public void showMenu() {
        menu = new MainMenuView();
        menu.setStartCB(this::startGame);
        menu.setExitCB(this::confirmExit);
        menu.setVisible(true);
    }

    private void startGame() {
        GameState    state    = new GameState();
        GameServices services = new GameServices();

        state.initialize();
        services.tribe().spawnInitialTribes(state);
        services.tribe().updateDiscovery(state);

        UnitController     unitCtrl   = new UnitController(state, services);
        BuildingController bldCtrl    = new BuildingController(state);
        CombatController   combatCtrl = new CombatController(state, services);
        TribeController    tribeCtrl  = new TribeController(state, services);
        TradeController    tradeCtrl  = new TradeController(state, services);
        TurnController     turnCtrl   = new TurnController(state, services);

        window = new GameWindow(state, turnCtrl, unitCtrl, bldCtrl,
                combatCtrl, tribeCtrl, tradeCtrl);
        turnCtrl.setWindow(window);
        window.setVisible(true);

        AudioManager.getInstance().play("/assets/theme.wav");
    }

    private void confirmExit() {
        int r = JOptionPane.showConfirmDialog(menu,
                "از بازی خارج شوید؟", "خروج", JOptionPane.YES_NO_OPTION);
        if (r == JOptionPane.YES_OPTION) System.exit(0);
    }
}
