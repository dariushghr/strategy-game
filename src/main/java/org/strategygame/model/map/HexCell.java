package org.strategygame.model.map;

import org.strategygame.model.building.Building;
import org.strategygame.model.building.BuildingType;
import org.strategygame.model.resource.ResourceType;
import org.strategygame.model.unit.Unit;

import java.util.ArrayList;
import java.util.List;

public class HexCell {
    private final int q, r;
    private final TerrainType terrain;
    private ResourceDeposit deposit;
    private Building building;
    private final List<Unit> units = new ArrayList<>();
    private boolean explored = false;
    private boolean inBorder = false;

    public HexCell(int q, int r, TerrainType terrain) {
        this.q = q; this.r = r; this.terrain = terrain;
    }

    public int getQ()              { return q; }
    public int getR()              { return r; }
    public TerrainType getTerrain(){ return terrain; }
    public ResourceDeposit getDeposit() { return deposit; }
    public Building getBuilding()  { return building; }
    public List<Unit> getUnits()   { return units; }
    public boolean isExplored()    { return explored; }
    public boolean isInBorder()    { return inBorder; }
    public boolean hasBuilding()   { return building != null; }
    public boolean hasResource()   { return deposit != null && !deposit.isDepleted(); }

    public void setDeposit(ResourceDeposit d)  { this.deposit = d; }
    public void setBuilding(Building b)         { this.building = b; }
    public void setExplored(boolean v)          { this.explored = v; }
    public void setInBorder(boolean v)          { this.inBorder = v; }

    public void addUnit(Unit u)    { if (!units.contains(u)) units.add(u); }
    public void removeUnit(Unit u) { units.remove(u); }

    public boolean canBuild(BuildingType type) {
        if (building != null || !inBorder) return false;
        return switch (type) {
            case LUMBER_MILL -> terrain == TerrainType.FOREST && hasResource();
            case STONE_MINE  -> terrain == TerrainType.MOUNTAIN
                    && deposit != null && deposit.getType() == ResourceType.STONE;
            case IRON_MINE   -> terrain == TerrainType.MOUNTAIN
                    && deposit != null && deposit.getType() == ResourceType.IRON;
            case FARM        -> terrain == TerrainType.GRASSLAND && hasResource();
            case STABLE      -> terrain == TerrainType.PLAINS && hasResource();
            case SETTLEMENT  -> !hasResource();
            case TOWN_HALL   -> false;
        };
    }
}
