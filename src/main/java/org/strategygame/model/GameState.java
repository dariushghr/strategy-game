package org.strategygame.model;

import org.strategygame.model.building.*;
import org.strategygame.model.map.*;
import org.strategygame.model.resource.*;
import org.strategygame.model.unit.*;
import org.strategygame.model.upgrade.UpgradeType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GameState {

    public static final int MAP_W  = 20;
    public static final int MAP_H  = 20;
    public static final int FOOD_PER_UNIT = 1;

    private int turn = 1;
    private final ResourceStorage storage = new ResourceStorage();
    private HexMap   map;
    private FogOfWar fog;
    private TownHall townHall;

    private final List<Unit>       units        = new ArrayList<>();
    private final List<Building>   buildings    = new ArrayList<>();
    private final Set<UpgradeType> doneUpgrades = new HashSet<>();

    private int     unitCap            = 10;
    private double  miningMultiplier   = 1.0;
    private boolean settlementUnlocked = false;
    private boolean starvation         = false;

    public void initialize() {
        map = new MapGenerator(MAP_W, MAP_H).generate(System.currentTimeMillis());
        fog = new FogOfWar(map);

        int cx = MAP_W / 2, cy = MAP_H / 2;
        HexCell center = map.getCell(cx, cy);

        if (center.getTerrain() == TerrainType.MOUNTAIN) {
            map.setCell(cx, cy, new HexCell(cx, cy, TerrainType.PLAINS));
            center = map.getCell(cx, cy);
        }

        center.setInBorder(true);
        for (HexCell n : map.getNeighbors(center)) n.setInBorder(true);

        townHall = new TownHall();
        townHall.setLocation(center);
        center.setBuilding(townHall);
        buildings.add(townHall);

        List<HexCell> neighbors = map.getNeighbors(center);
        spawn(new Explorer(),       center);
        spawn(new Builder(),        safeNeighbor(neighbors, 0, center));
        spawn(new Builder(),        safeNeighbor(neighbors, 1, center));
        spawn(new Worker(),         safeNeighbor(neighbors, 2, center));
        spawn(new Worker(),         safeNeighbor(neighbors, 3, center));

        fog.update(units, buildings);
    }

    private HexCell safeNeighbor(List<HexCell> list, int idx, HexCell fallback) {
        return idx < list.size() ? list.get(idx) : fallback;
    }

    public void spawn(Unit u, HexCell cell) {
        u.setPosition(cell);
        cell.addUnit(u);
        units.add(u);
    }

    public ResourceRate calcRate() {
        ResourceRate rate = new ResourceRate();
        for (Building b : buildings) {
            if (!b.isFunctional()) continue;
            ResourceType rt = b.getResourceType();
            if (rt != null) {
                double m = (b instanceof TownHall) ? 1.0 : miningMultiplier;
                rate.addProduction(rt, b.produce(m));
            }
            int[] up = b.getType().getUpkeepCost();
            ResourceType[] types = ResourceType.values();
            for (int i = 0; i < types.length; i++) {
                if (up[i] > 0) rate.addUpkeep(types[i], up[i]);
            }
        }

        rate.addProduction(ResourceType.WOOD, TownHall.SAFEGUARD_WOOD);

        rate.addUpkeep(ResourceType.FOOD, units.size() * FOOD_PER_UNIT);
        return rate;
    }

    public List<Unit> idleUnitsWithAP() {
        List<Unit> list = new ArrayList<>();
        for (Unit u : units) {
            boolean stationed = (u instanceof Worker w) && w.isStationed();
            if (u.getCurrentAP() > 0 && !stationed) list.add(u);
        }
        return list;
    }

    public boolean canSpawnUnit() { return units.size() < unitCap; }

    public void removeUnit(Unit u) {
        units.remove(u);
        if (u.getPosition() != null) u.getPosition().removeUnit(u);
    }

    public void addBuilding(Building b) { buildings.add(b); }

    public int getTurn()                        { return turn; }
    public void nextTurn()                      { turn++; }
    public ResourceStorage getStorage()         { return storage; }
    public HexMap getMap()                      { return map; }
    public FogOfWar getFog()                    { return fog; }
    public TownHall getTownHall()               { return townHall; }
    public List<Unit> getUnits()                { return units; }
    public List<Building> getBuildings()        { return buildings; }
    public Set<UpgradeType> getDoneUpgrades()   { return doneUpgrades; }
    public int getUnitCap()                     { return unitCap; }
    public double getMiningMultiplier()          { return miningMultiplier; }
    public boolean isSettlementUnlocked()        { return settlementUnlocked; }
    public boolean isStarvation()               { return starvation; }

    public void setStarvation(boolean v)        { this.starvation = v; }
    public void setMiningMultiplier(double v)   { this.miningMultiplier = v; }
    public void setSettlementUnlocked(boolean v){ this.settlementUnlocked = v; }
    public void addUnitCap(int bonus)           { this.unitCap += bonus; }
}
