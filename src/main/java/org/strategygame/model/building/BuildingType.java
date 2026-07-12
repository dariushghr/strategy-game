package org.strategygame.model.building;

public enum BuildingType {
    LUMBER_MILL ("چوب‌بری",   1, new int[]{0,5,0,0},  new int[]{0,1,0,0}),
    STONE_MINE  ("معدن سنگ",  1, new int[]{0,8,0,0},  new int[]{0,1,0,0}),
    IRON_MINE   ("معدن آهن",  2, new int[]{0,10,5,0}, new int[]{0,1,0,0}),
    FARM        ("مزرعه",     1, new int[]{0,6,0,0},  new int[]{0,1,0,0}),
    STABLE      ("طویله",     1, new int[]{0,7,0,0},  new int[]{0,1,0,0}),
    SETTLEMENT  ("شهرک",      3, new int[]{0,15,10,5},new int[]{0,1,1,0}),
    TOWN_HALL   ("Town Hall", 0, new int[]{0,0,0,0},  new int[]{0,0,0,0});

    private final String  label;
    private final int     buildAP;
    private final int[]   buildCost;
    private final int[]   upkeepCost;

    BuildingType(String label, int buildAP, int[] buildCost, int[] upkeepCost) {
        this.label      = label;
        this.buildAP    = buildAP;
        this.buildCost  = buildCost;
        this.upkeepCost = upkeepCost;
    }

    public String getLabel()     { return label; }
    public int getBuildAP()      { return buildAP; }
    public int[] getBuildCost()  { return buildCost; }
    public int[] getUpkeepCost() { return upkeepCost; }
}
