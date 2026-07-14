package org.strategygame.controller;

import org.strategygame.model.GameState;
import org.strategygame.model.building.Building;
import org.strategygame.model.building.BuildingFactory;
import org.strategygame.model.building.BuildingType;
import org.strategygame.model.building.Settlement;
import org.strategygame.model.map.HexCell;
import org.strategygame.model.map.HexMap;
import org.strategygame.model.resource.ResourceStorage;
import org.strategygame.model.unit.*;
import org.strategygame.model.upgrade.UpgradeType;

import java.util.*;

public class UnitController {

    private static final int STATION_AP = 1;

    private final GameState state;

    public UnitController(GameState state) {
        this.state = state;
    }

    public List<HexCell> reachable(Unit u) {
        if (u == null || u.getPosition() == null || u.getCurrentAP() <= 0) return List.of();

        HexMap map = state.getMap();
        HexCell start = u.getPosition();

        Map<HexCell, Integer> bestCost = new HashMap<>();
        bestCost.put(start, 0);

        PriorityQueue<HexCell> frontier =
                new PriorityQueue<>(Comparator.comparingInt(bestCost::get));
        frontier.add(start);

        Set<HexCell> result = new LinkedHashSet<>();

        while (!frontier.isEmpty()) {
            HexCell current = frontier.poll();
            int currentCost = bestCost.get(current);

            for (HexCell next : map.getNeighbors(current)) {
                int stepCost = next.getTerrain().getMovementCost();
                int newCost  = currentCost + stepCost;
                if (newCost > u.getCurrentAP()) continue;
                Integer known = bestCost.get(next);
                if (known == null || newCost < known) {
                    bestCost.put(next, newCost);
                    frontier.add(next);
                    result.add(next);
                }
            }
        }
        return new ArrayList<>(result);
    }

    public List<HexCell> path(Unit u, HexCell dest) {
        if (u == null || dest == null || u.getPosition() == null) return List.of();
        HexCell start = u.getPosition();
        if (start.equals(dest)) return List.of();

        HexMap map = state.getMap();
        Map<HexCell, Integer>  bestCost = new HashMap<>();
        Map<HexCell, HexCell>  parent   = new HashMap<>();
        bestCost.put(start, 0);

        PriorityQueue<HexCell> frontier =
                new PriorityQueue<>(Comparator.comparingInt(bestCost::get));
        frontier.add(start);

        while (!frontier.isEmpty()) {
            HexCell current = frontier.poll();
            if (current.equals(dest)) break;
            int currentCost = bestCost.get(current);

            for (HexCell next : map.getNeighbors(current)) {
                int newCost = currentCost + next.getTerrain().getMovementCost();
                Integer known = bestCost.get(next);
                if (known == null || newCost < known) {
                    bestCost.put(next, newCost);
                    parent.put(next, current);
                    frontier.add(next);
                }
            }
        }

        Integer totalCost = bestCost.get(dest);
        if (totalCost == null || totalCost > u.getCurrentAP()) return List.of();

        LinkedList<HexCell> result = new LinkedList<>();
        HexCell cur = dest;
        while (!cur.equals(start)) {
            result.addFirst(cur);
            cur = parent.get(cur);
        }
        return result;
    }

    public boolean stepOnce(Unit u, HexCell next) {
        if (u == null || next == null || u.getPosition() == null) return false;
        if (!state.getMap().getNeighbors(u.getPosition()).contains(next)) return false;

        int cost = next.getTerrain().getMovementCost();
        if (!u.hasAP(cost)) return false;

        HexCell from = u.getPosition();
        u.spendAP(cost);
        from.removeUnit(u);
        next.addUnit(u);
        u.setPosition(next);

        state.getFog().update(state.getUnits(), state.getBuildings());
        return true;
    }

    public boolean move(Unit u, HexCell dest) {
        List<HexCell> steps = path(u, dest);
        if (steps.isEmpty()) return false;
        for (HexCell step : steps) {
            if (!stepOnce(u, step)) return false;
        }
        return true;
    }

    public boolean station(Worker w, Building b) {
        if (w == null || b == null) return false;
        if (w.isStationed()) return false;
        if (w.getPosition() == null || !w.getPosition().equals(b.getLocation())) return false;
        if (!b.canTakeWorker() || !w.hasAP(STATION_AP)) return false;

        w.station(b, STATION_AP);
        return true;
    }

    public boolean unstation(Worker w) {
        if (w == null || !w.isStationed()) return false;
        w.unstation();
        return true;
    }

    public boolean expand(BorderExpander be, HexCell cell) {
        if (be == null || cell == null) return false;
        if (!state.getFog().isExplored(cell.getQ(), cell.getR())) return false;

        cell.setInBorder(true);
        for (HexCell n : state.getMap().getNeighbors(cell)) {
            if (state.getFog().isExplored(n.getQ(), n.getR())) n.setInBorder(true);
        }

        be.consume();
        state.removeUnit(be);
        return true;
    }

    public boolean build(Builder builder, BuildingType type, HexCell cell) {
        if (builder == null || type == null || cell == null) return false;
        if (!builder.hasCharges()) return false;
        if (builder.getPosition() == null || !builder.getPosition().equals(cell)) return false;
        if (!cell.canBuild(type)) return false;
        if (!builder.hasAP(type.getBuildAP())) return false;
        if (!techUnlocked(type)) return false;

        ResourceStorage storage = state.getStorage();
        if (!storage.canAffordAll(type.getBuildCost())) return false;

        storage.deductAll(type.getBuildCost());
        builder.spendAP(type.getBuildAP());

        Building building = BuildingFactory.create(type);
        building.setLocation(cell);
        cell.setBuilding(building);
        state.addBuilding(building);

        if (building instanceof Settlement) {
            state.addUnitCap(Settlement.CAP_BONUS);
            state.setSettlementUnlocked(true);
        }

        builder.useCharge();
        if (!builder.isAlive()) state.removeUnit(builder);

        state.getFog().update(state.getUnits(), state.getBuildings());
        return true;
    }

    private boolean techUnlocked(BuildingType type) {
        Set<UpgradeType> done = state.getDoneUpgrades();
        return switch (type) {
            case STONE_MINE -> done.contains(UpgradeType.STONE_MINE_TECH);
            case IRON_MINE  -> done.contains(UpgradeType.IRON_MINE_TECH);
            case SETTLEMENT -> done.contains(UpgradeType.SETTLEMENT_TECH);
            default -> true;
        };
    }
}
