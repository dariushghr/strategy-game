package org.strategygame.model.map;

public enum TerrainType {
    PLAINS(1,   "دشت"),
    FOREST(2,   "جنگل"),
    MOUNTAIN(4, "کوهستان"),
    GRASSLAND(1,"سبزه‌زار");

    private final int movementCost;
    private final String label;

    TerrainType(int movementCost, String label) {
        this.movementCost = movementCost;
        this.label = label;
    }

    public int getMovementCost() { return movementCost; }
    public String getLabel()     { return label; }
}
