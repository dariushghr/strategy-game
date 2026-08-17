package org.strategygame.model.unit;

import org.strategygame.config.GameConfig;
import org.strategygame.model.building.BuildingType;

/**
 * انواع یونیت به همراه هزینه، مدت آموزش و پیش‌نیازهایشان.
 * نگه داشتن این اعداد در یک جا باعث می‌شود پنل تان هال و منطق تولید
 * همیشه از یک منبع واحد بخوانند.
 */
public enum UnitType {

    EXPLORER       ("اکسپلورر", new int[]{10,  5, 0, 0}, 2, 1, false, false, true),
    BUILDER        ("بیلدر",    new int[]{10, 10, 0, 0}, 1, 1, false, false, true),
    WORKER         ("کارگر",    new int[]{ 8,  0, 0, 0}, 1, 1, false, false, true),
    BORDER_EXPANDER("مرزگشا",   new int[]{15,  5, 5, 0}, 2, 1, false, false, true),

    SWORDSMAN("شمشیرزن",  GameConfig.SWORDSMAN_COST, GameConfig.SWORDSMAN_TRAIN_TURNS, 1, true, false, true),
    ARCHER   ("کماندار",  GameConfig.ARCHER_COST,    GameConfig.ARCHER_TRAIN_TURNS,    2, true, false, true),
    CAVALRY  ("سوارنظام", GameConfig.CAVALRY_COST,   GameConfig.CAVALRY_TRAIN_TURNS,   2, true, true,  true),

    /** خرس فقط با رویداد حمله‌ی خرس روی نقشه می‌آید و قابل آموزش نیست. */
    BEAR("خرس", new int[]{0, 0, 0, 0}, 0, 1, false, false, false);

    private final String  label;
    private final int[]   cost;
    private final int     trainTurns;
    private final int     requiredTownHallLevel;
    private final boolean military;
    private final boolean requiresStable;
    private final boolean trainable;

    UnitType(String label, int[] cost, int trainTurns, int requiredTownHallLevel,
             boolean military, boolean requiresStable, boolean trainable) {
        this.label                 = label;
        this.cost                  = cost;
        this.trainTurns            = trainTurns;
        this.requiredTownHallLevel = requiredTownHallLevel;
        this.military              = military;
        this.requiresStable        = requiresStable;
        this.trainable             = trainable;
    }

    public String  getLabel()                 { return label; }
    public int[]   getCost()                  { return cost; }
    public int     getTrainTurns()            { return trainTurns; }
    public int     getRequiredTownHallLevel() { return requiredTownHallLevel; }
    public boolean isMilitary()               { return military; }
    public boolean requiresStable()           { return requiresStable; }
    public boolean isTrainable()              { return trainable; }

    public String costText() { return BuildingType.formatCost(cost); }
}
