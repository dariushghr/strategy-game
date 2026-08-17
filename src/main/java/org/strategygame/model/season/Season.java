package org.strategygame.model.season;

import org.strategygame.config.GameConfig;
import org.strategygame.model.building.BuildingType;

/**
 * چرخه‌ی فصل‌ها؛ هر فصل دقیقا {@link GameConfig#SEASON_LENGTH} نوبت طول می‌کشد
 * و بعد از زمستان چرخه از بهار تکرار می‌شود.
 */
public enum Season {

    SPRING("بهار",     "مزرعه و طویله +" + GameConfig.SPRING_FARM_BONUS + " غذا"),
    SUMMER("تابستان",  "بدون اثر خاص"),
    AUTUMN("پاییز",    "حرکت قایق روی آب +" + GameConfig.AUTUMN_WATER_AP_BONUS + " AP"),
    WINTER("زمستان",   "مزرعه -" + GameConfig.WINTER_FARM_PENALTY + " غذا و حرکت روی زمین +"
            + GameConfig.WINTER_LAND_AP_COST + " AP");

    private final String label;
    private final String effectText;

    Season(String label, String effectText) {
        this.label      = label;
        this.effectText = effectText;
    }

    public String getLabel()      { return label; }
    public String getEffectText() { return effectText; }

    /** فصل مربوط به یک نوبت؛ نوبت‌ها از ۱ شروع می‌شوند. */
    public static Season forTurn(int turn) {
        int index = ((Math.max(1, turn) - 1) / GameConfig.SEASON_LENGTH) % values().length;
        return values()[index];
    }

    /** شماره‌ی نوبت داخل فصل جاری. */
    public static int turnWithinSeason(int turn) {
        return ((Math.max(1, turn) - 1) % GameConfig.SEASON_LENGTH) + 1;
    }

    /** تغییر تولید غذای یک سازه در این فصل. */
    public int foodModifierFor(BuildingType type) {
        return switch (this) {
            case SPRING -> (type == BuildingType.FARM || type == BuildingType.STABLE)
                    ? GameConfig.SPRING_FARM_BONUS : 0;
            case WINTER -> type == BuildingType.FARM ? -GameConfig.WINTER_FARM_PENALTY : 0;
            default -> 0;
        };
    }

    /** هزینه‌ی اضافه‌ی حرکت روی زمین. */
    public int landMovementExtraCost() {
        return this == WINTER ? GameConfig.WINTER_LAND_AP_COST : 0;
    }

    /** AP اضافه‌ی حرکت روی آب. */
    public int waterMovementApBonus() {
        return this == AUTUMN ? GameConfig.AUTUMN_WATER_AP_BONUS : 0;
    }

    /** آیا در این فصل سیل ممکن است. */
    public boolean allowsFlood() { return this == AUTUMN; }
}
