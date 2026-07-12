package org.strategygame.model.building;

import org.strategygame.model.resource.ResourceType;

class LumberMill extends Building {
    public LumberMill() { super(3); }
    @Override public int produce(double m)       { return (int) (workerCount() * 3 * m); }
    @Override public ResourceType getResourceType(){ return ResourceType.WOOD; }
    @Override public BuildingType getType()       { return BuildingType.LUMBER_MILL; }
}

class StoneMine extends Building {
    public StoneMine() { super(3); }
    @Override public int produce(double m)       { return (int) (workerCount() * 2 * m); }
    @Override public ResourceType getResourceType(){ return ResourceType.STONE; }
    @Override public BuildingType getType()       { return BuildingType.STONE_MINE; }
}

class IronMine extends Building {
    public IronMine() { super(2); }
    @Override public int produce(double m)       { return (int) (workerCount() * 1 * m); }
    @Override public ResourceType getResourceType(){ return ResourceType.IRON; }
    @Override public BuildingType getType()       { return BuildingType.IRON_MINE; }
}

class Farm extends Building {
    public Farm() { super(4); }
    @Override public int produce(double m)       { return (int) (workerCount() * 4 * m); }
    @Override public ResourceType getResourceType(){ return ResourceType.FOOD; }
    @Override public BuildingType getType()       { return BuildingType.FARM; }
}

class Stable extends Building {
    public Stable() { super(2); }
    @Override public int produce(double m)       { return (int) (workerCount() * 3 * m); }
    @Override public ResourceType getResourceType(){ return ResourceType.FOOD; }
    @Override public BuildingType getType()       { return BuildingType.STABLE; }
}

public class BuildingFactory {
    public static Building create(BuildingType t) {
        return switch (t) {
            case LUMBER_MILL -> new LumberMill();
            case STONE_MINE  -> new StoneMine();
            case IRON_MINE   -> new IronMine();
            case FARM        -> new Farm();
            case STABLE      -> new Stable();
            case SETTLEMENT  -> new Settlement();
            case TOWN_HALL   -> new TownHall();
        };
    }
}
