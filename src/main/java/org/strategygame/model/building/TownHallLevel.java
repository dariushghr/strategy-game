package org.strategygame.model.building;

import org.strategygame.config.GameConfig;

/**
 * سطح‌های تان هال. هزینه و مدت هر سطح، هزینه و مدت رسیدن به همان سطح است
 * (سطح ۱ سطح شروع بازی است و رایگان).
 */
public enum TownHallLevel {

    LEVEL_1(1, new int[]{0, 0, 0, 0}, 0),
    LEVEL_2(2, GameConfig.TOWN_HALL_LEVEL_2_COST, GameConfig.TOWN_HALL_LEVEL_2_TURNS),
    LEVEL_3(3, GameConfig.TOWN_HALL_LEVEL_3_COST, GameConfig.TOWN_HALL_LEVEL_3_TURNS);

    private final int   number;
    private final int[] upgradeCost;
    private final int   upgradeTurns;

    TownHallLevel(int number, int[] upgradeCost, int upgradeTurns) {
        this.number       = number;
        this.upgradeCost  = upgradeCost;
        this.upgradeTurns = upgradeTurns;
    }

    public int   getNumber()       { return number; }
    public int[] getUpgradeCost()  { return upgradeCost; }
    public int   getUpgradeTurns() { return upgradeTurns; }

    /** ظرفیت انبار در این سطح. */
    public int getStorageCapacity() {
        int[] caps = GameConfig.STORAGE_CAPACITY_BY_LEVEL;
        return caps[Math.min(number - 1, caps.length - 1)];
    }

    /** سقف یونیت نظامی در این سطح. */
    public int getMilitaryCap() {
        int[] caps = GameConfig.MILITARY_CAP_BY_LEVEL;
        return caps[Math.min(number - 1, caps.length - 1)];
    }

    public int getFoodPerTurn() { return GameConfig.TOWN_HALL_BASE_FOOD; }
    public int getWoodPerTurn() { return GameConfig.TOWN_HALL_BASE_WOOD; }

    public String getLabel() { return "سطح " + number; }

    /** خلاصه‌ی مزایای این سطح برای نمایش در لیست. */
    public String bonusText() {
        StringBuilder sb = new StringBuilder();
        sb.append("ظرفیت انبار ").append(getStorageCapacity());
        sb.append(" | سقف یونیت نظامی ").append(getMilitaryCap());
        if (number == 2) {
            sb.append(" | ترمیم ").append(GameConfig.TOWN_HALL_LEVEL_2_HEAL).append(" HP تان هال");
            sb.append(" | آزادسازی کماندار، اسکله و بازار");
        } else if (number == 3) {
            sb.append(" | آزادسازی معماری دفاعی و سوارنظام پیشرفته");
        }
        return sb.toString();
    }

    public String costText() {
        return number == 1 ? "سطح شروع"
                : BuildingType.formatCost(upgradeCost) + " | " + upgradeTurns + " نوبت";
    }

    public TownHallLevel next() {
        TownHallLevel[] all = values();
        return ordinal() + 1 < all.length ? all[ordinal() + 1] : null;
    }

    public static TownHallLevel maxLevel() {
        TownHallLevel[] all = values();
        return all[all.length - 1];
    }
}
