package org.strategygame.controller;

import org.strategygame.model.GameState;
import org.strategygame.model.building.ProductionQueue;
import org.strategygame.model.resource.ResourceStorage;
import org.strategygame.model.unit.UnitType;
import org.strategygame.model.upgrade.UpgradeType;

public class BuildingController {

    private static final int[] EXPLORER_COST        = {10, 5, 0, 0};
    private static final int[] BUILDER_COST         = {10, 10, 0, 0};
    private static final int[] WORKER_COST          = {8, 0, 0, 0};
    private static final int[] BORDER_EXPANDER_COST = {15, 5, 5, 0};

    private final GameState state;

    public BuildingController(GameState state) {
        this.state = state;
    }

    public boolean queueUnit(UnitType type) {
        ProductionQueue queue = state.getTownHall().getQueue();
        if (!queue.isEmpty()) return false;
        if (!state.canSpawnUnit()) return false;

        int[] cost  = unitCost(type);
        int   turns = unitTurns(type);

        ResourceStorage storage = state.getStorage();
        if (!storage.canAffordAll(cost)) return false;

        storage.deductAll(cost);
        queue.start(type, turns);
        return true;
    }

    public boolean queueUpgrade(UpgradeType type) {
        ProductionQueue queue = state.getTownHall().getQueue();
        if (!queue.isEmpty()) return false;
        if (state.getDoneUpgrades().contains(type)) return false;
        if (!type.isUnlocked(state.getDoneUpgrades())) return false;

        ResourceStorage storage = state.getStorage();
        if (!storage.canAffordAll(type.getCost())) return false;

        storage.deductAll(type.getCost());
        queue.start(type, type.getTurns());
        return true;
    }

    private int[] unitCost(UnitType type) {
        return switch (type) {
            case EXPLORER        -> EXPLORER_COST;
            case BUILDER         -> BUILDER_COST;
            case WORKER          -> WORKER_COST;
            case BORDER_EXPANDER -> BORDER_EXPANDER_COST;
        };
    }

    private int unitTurns(UnitType type) {
        return switch (type) {
            case EXPLORER, BORDER_EXPANDER -> 2;
            case BUILDER, WORKER           -> 1;
        };
    }
}
