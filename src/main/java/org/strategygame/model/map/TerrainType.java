package org.strategygame.model.map;

import org.strategygame.config.GameConfig;

public enum TerrainType {

    PLAINS        (1, "دشت",      false, true,  true),
    FOREST        (2, "جنگل",     false, true,  true),
    GRASSLAND     (1, "سبزه‌زار",  false, true,  true),
    MOUNTAIN      (GameConfig.MOUNTAIN_MOVEMENT_COST, "کوهستان", false, true, true),
    /** رشته‌کوه کاملا غیرقابل عبور و غیرقابل ساخت است. */
    MOUNTAIN_RANGE(0, "رشته‌کوه", false, false, false),
    /** دریا برای یونیت زمینی فقط با تکنولوژی دریانوردی قابل عبور است. */
    SEA           (1, "دریا",      true,  false, false);

    private final int     movementCost;
    private final String  label;
    private final boolean water;
    private final boolean landPassable;
    private final boolean buildable;

    TerrainType(int movementCost, String label,
                boolean water, boolean landPassable, boolean buildable) {
        this.movementCost = movementCost;
        this.label        = label;
        this.water        = water;
        this.landPassable = landPassable;
        this.buildable    = buildable;
    }

    public int getMovementCost() { return movementCost; }
    public String getLabel()     { return label; }
    public boolean isWater()     { return water; }
    public boolean isLand()      { return !water; }

    /** آیا یونیت زمینی بدون تکنولوژی خاص می‌تواند وارد این زمین شود. */
    public boolean isLandPassable() { return landPassable; }

    /** آیا اصولا روی این زمین می‌توان چیزی ساخت. */
    public boolean isBuildable() { return buildable; }
}
