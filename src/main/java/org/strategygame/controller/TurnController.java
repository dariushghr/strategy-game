package org.strategygame.controller;

import org.strategygame.model.GameState;
import org.strategygame.model.building.*;
import org.strategygame.model.map.HexCell;
import org.strategygame.model.resource.ResourceStorage;
import org.strategygame.model.resource.ResourceType;
import org.strategygame.model.unit.*;
import org.strategygame.model.upgrade.UpgradeType;
import org.strategygame.view.GameWindow;

import java.util.ArrayList;
import java.util.List;

public class TurnController {

    private final GameState     state;
    private final UnitController unitCtrl;
    private GameWindow window;

    public TurnController(GameState state, UnitController unitCtrl) {
        this.state    = state;
        this.unitCtrl = unitCtrl;
    }

    public void setWindow(GameWindow window) { this.window = window; }

    public void execute() {
        refreshAP();
        produceResources();
        advanceProductionQueue();
        applyUpkeep();
        applyFoodConsumption();

        state.getFog().update(state.getUnits(), state.getBuildings());
        state.nextTurn();

        if (window != null) window.refresh();
    }

    private void refreshAP() {
        for (Unit u : state.getUnits()) {
            if (u.isAlive()) u.refreshAP();
        }
    }

    private void produceResources() {
        ResourceStorage storage = state.getStorage();
        double mult = state.getMiningMultiplier();

        for (Building b : state.getBuildings()) {
            if (!b.isFunctional()) continue;
            ResourceType rt = b.getResourceType();
            if (rt == null) continue;
            double m = (b instanceof TownHall) ? 1.0 : mult;
            storage.add(rt, b.produce(m));
        }

        storage.add(ResourceType.WOOD, TownHall.SAFEGUARD_WOOD);
    }

    private void advanceProductionQueue() {
        ProductionQueue queue = state.getTownHall().getQueue();
        if (queue.isEmpty()) return;

        if (queue.advance()) {
            Object item = queue.take();
            if (item instanceof UnitType type) {
                spawnProducedUnit(type);
            } else if (item instanceof UpgradeType type) {
                applyUpgrade(type);
            }
        }
    }

    private void spawnProducedUnit(UnitType type) {
        if (!state.canSpawnUnit()) return;

        HexCell home = state.getTownHall().getLocation();
        List<HexCell> neighbors = state.getMap().getNeighbors(home);
        HexCell spot = neighbors.isEmpty() ? home : neighbors.getFirst();

        Unit unit = switch (type) {
            case EXPLORER        -> new Explorer();
            case BUILDER         -> new Builder();
            case WORKER          -> new Worker();
            case BORDER_EXPANDER -> new BorderExpander();
        };
        state.spawn(unit, spot);
    }

    private void applyUpgrade(UpgradeType type) {
        state.getDoneUpgrades().add(type);
        switch (type) {
            case STORAGE_UPGRADE_1, STORAGE_UPGRADE_2 -> state.getStorage().upgradeCapacity();
            case PROFESSIONAL_TOOLS -> state.setMiningMultiplier(1.5);
            case SETTLEMENT_TECH -> autoPlaceSettlement();
            default -> {  }
        }
    }

    private void autoPlaceSettlement() {
        List<HexCell> candidates = new ArrayList<>();
        for (HexCell c : state.getMap().getAllCells()) {
            if (c.isInBorder() && !c.hasBuilding() && !c.hasResource()) candidates.add(c);
        }
        if (candidates.isEmpty()) return;

        HexCell chosen = candidates.getFirst();
        Building settlement = BuildingFactory.create(BuildingType.SETTLEMENT);
        settlement.setLocation(chosen);
        chosen.setBuilding(settlement);
        state.addBuilding(settlement);
        state.addUnitCap(Settlement.CAP_BONUS);
        state.setSettlementUnlocked(true);
    }

    private void applyUpkeep() {
        ResourceStorage storage = state.getStorage();
        for (Building b : state.getBuildings()) {
            if (!b.isFunctional()) continue;
            int[] upkeep = b.getType().getUpkeepCost();
            if (storage.canAffordAll(upkeep)) {
                storage.deductAll(upkeep);
                b.payUpkeep();
            } else {
                b.missUpkeep();
            }
        }
    }

    private void applyFoodConsumption() {
        ResourceStorage storage = state.getStorage();
        int consumption = state.getUnits().size() * GameState.FOOD_PER_UNIT;
        int available   = storage.get(ResourceType.FOOD);

        boolean starving = available < consumption;
        storage.deduct(ResourceType.FOOD, consumption);
        state.setStarvation(starving);

        if (starving) {
            for (Unit u : state.getUnits()) {
                if (u.isAlive()) u.spendAP(1);
            }
        }
    }
}
