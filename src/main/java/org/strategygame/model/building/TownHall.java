package org.strategygame.model.building;

import org.strategygame.model.resource.ResourceType;

public class TownHall extends Building {
    public static final int SAFEGUARD_FOOD = 1;
    public static final int SAFEGUARD_WOOD = 1;

    private final ProductionQueue queue = new ProductionQueue();
    private int storageLevel = 0;

    public TownHall() { super(0); }

    @Override public int produce(double m)        { return SAFEGUARD_FOOD; }
    @Override public ResourceType getResourceType(){ return ResourceType.FOOD; }
    @Override public BuildingType getType()        { return BuildingType.TOWN_HALL; }

    public ProductionQueue getQueue()  { return queue; }
    public int getStorageLevel()       { return storageLevel; }
    public boolean canUpgradeStorage() { return storageLevel < 2; }
    public void upgradeStorage()       { if (canUpgradeStorage()) storageLevel++; }
}
