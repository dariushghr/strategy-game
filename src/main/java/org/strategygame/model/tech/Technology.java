package org.strategygame.model.tech;

import org.strategygame.config.GameConfig;
import org.strategygame.model.building.BuildingType;

/**
 * تکنولوژی‌های فاز دوم. هر تکنولوژی حداقل سطح تان هال، هزینه و مدت تحقیق دارد
 * و اثرش در قوانین بازی ثبت می‌شود، نه در UI.
 */
public enum Technology {

    SAILING("دریانوردی", 2, new int[]{0, 80, 0, 0}, 4,
            "یونیت زمینی می‌تواند وارد دریا شود؛ ورود به آب AP همان نوبت را صفر می‌کند"),

    STEEL_TOOLS("ابزار فولادی", 2, new int[]{0, 0, 0, 40}, 3,
            "تولید کارگر معدن سنگ و آهن ×" + GameConfig.STEEL_TOOLS_MINING_MULTIPLIER
                    + " (با گرد کردن به پایین)"),

    DEFENSIVE_ARCHITECTURE("معماری دفاعی", 3, new int[]{0, 0, 100, 0}, 4,
            "دیوار دفاعی تان هال: دفاع " + GameConfig.TOWN_HALL_DEFENSE + " → "
                    + GameConfig.DEFENSIVE_ARCHITECTURE_DEFENSE + " و حداکثر HP "
                    + GameConfig.DEFENSIVE_ARCHITECTURE_MAX_HP);

    private final String requiredLabel;
    private final int    requiredTownHallLevel;
    private final int[]  cost;
    private final int    turns;
    private final String effectText;

    Technology(String label, int requiredTownHallLevel, int[] cost, int turns, String effectText) {
        this.requiredLabel         = label;
        this.requiredTownHallLevel = requiredTownHallLevel;
        this.cost                  = cost;
        this.turns                 = turns;
        this.effectText            = effectText;
    }

    public String getLabel()                { return requiredLabel; }
    public int    getRequiredTownHallLevel(){ return requiredTownHallLevel; }
    public int[]  getCost()                 { return cost; }
    public int    getTurns()                { return turns; }
    public String getEffectText()           { return effectText; }

    public String costText() { return BuildingType.formatCost(cost); }
}
