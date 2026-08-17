package org.strategygame.model.building;

import org.strategygame.config.GameConfig;
import org.strategygame.model.resource.ResourceType;

public enum BuildingType {

    LUMBER_MILL ("چوب‌بری",   1, new int[]{0, 5, 0, 0},   new int[]{0,1,0,0}, 3, ResourceType.WOOD,  3,  80, 1, true),
    STONE_MINE  ("معدن سنگ",  1, new int[]{0, 8, 0, 0},   new int[]{0,1,0,0}, 3, ResourceType.STONE, 2,  80, 1, true),
    IRON_MINE   ("معدن آهن",  2, new int[]{0,10, 5, 0},   new int[]{0,1,0,0}, 2, ResourceType.IRON,  1,  80, 1, true),
    FARM        ("مزرعه",     1, new int[]{0, 6, 0, 0},   new int[]{0,1,0,0}, 4, ResourceType.FOOD,  4,  60, 1, true),
    STABLE      ("طویله",     1, new int[]{0, 7, 0, 0},   new int[]{0,1,0,0}, 2, ResourceType.FOOD,  3,  80, 1, true),
    DOCK        ("اسکله",     1, GameConfig.DOCK_COST,    new int[]{0,1,0,0}, 2, ResourceType.FOOD,  3,  80, 2, true),
    BAZAAR      ("بازار",     1, GameConfig.BAZAAR_COST,  new int[]{0,1,0,0}, 0, null,               0, 100, 2, true),
    MONUMENT    ("بنای یادبود",2, GameConfig.MONUMENT_COST,new int[]{0,0,0,0}, 0, null,              0, 200, 1, true),
    SETTLEMENT  ("شهرک",      3, new int[]{0,15,10, 5},   new int[]{0,1,1,0}, 0, null,               0, 150, 1, true),
    TRADING_POST("پاسگاه تجاری",0,new int[]{0,0,0,0},     new int[]{0,0,0,0}, 0, null,               0, 120, 1, false),
    OUTPOST     ("پاسگاه",    0, new int[]{0,0,0,0},      new int[]{0,0,0,0}, 0, null,               0, 150, 1, false),
    TOWN_HALL   ("تان هال",   0, new int[]{0,0,0,0},      new int[]{0,0,0,0}, 0, ResourceType.FOOD,  0,
                 GameConfig.TOWN_HALL_MAX_HP, 1, false);

    private final String       label;
    private final int          buildAP;
    private final int[]        buildCost;
    private final int[]        upkeepCost;
    private final int          maxWorkers;
    private final ResourceType yieldType;
    private final int          yieldPerWorker;
    private final int          maxHp;
    private final int          requiredTownHallLevel;
    private final boolean      demolishable;

    BuildingType(String label, int buildAP, int[] buildCost, int[] upkeepCost,
                 int maxWorkers, ResourceType yieldType, int yieldPerWorker,
                 int maxHp, int requiredTownHallLevel, boolean demolishable) {
        this.label                 = label;
        this.buildAP               = buildAP;
        this.buildCost             = buildCost;
        this.upkeepCost            = upkeepCost;
        this.maxWorkers            = maxWorkers;
        this.yieldType             = yieldType;
        this.yieldPerWorker        = yieldPerWorker;
        this.maxHp                 = maxHp;
        this.requiredTownHallLevel = requiredTownHallLevel;
        this.demolishable          = demolishable;
    }

    public String getLabel()          { return label; }
    public int getBuildAP()           { return buildAP; }
    public int[] getBuildCost()       { return buildCost; }
    public int[] getUpkeepCost()      { return upkeepCost; }
    public int getMaxWorkers()        { return maxWorkers; }
    public ResourceType getYieldType(){ return yieldType; }
    public int getYieldPerWorker()    { return yieldPerWorker; }
    public int getMaxHp()             { return maxHp; }
    public int getRequiredTownHallLevel() { return requiredTownHallLevel; }
    public boolean isDemolishable()   { return demolishable; }

    /** سازه‌هایی که بازیکن می‌تواند با بیلدر بسازد. */
    public boolean isPlayerBuildable() {
        return this != TOWN_HALL && this != TRADING_POST && this != OUTPOST;
    }

    /** بیشترین تولید ممکن در هر نوبت وقتی همه‌ی جای کارگرها پر باشد. */
    public int getMaxYield() { return maxWorkers * yieldPerWorker; }

    /** هزینه ساخت به صورت متن؛ مثلا «چوب ۵ + سنگ ۸». */
    public String costText() { return formatCost(buildCost); }

    /** هزینه نگهداری هر نوبت به صورت متن. */
    public String upkeepText() {
        String txt = formatCost(upkeepCost);
        return txt.equals("رایگان") ? "ندارد" : txt + " در هر نوبت";
    }

    /** توضیح منابعی که این سازه بعد از ساخت می‌دهد. */
    public String yieldText() {
        return switch (this) {
            case TOWN_HALL -> "غذا +" + GameConfig.TOWN_HALL_BASE_FOOD
                    + " و چوب +" + GameConfig.TOWN_HALL_BASE_WOOD + " در هر نوبت";
            case SETTLEMENT -> "منبع تولید نمی‌کند، سقف یونیت +" + Settlement.CAP_BONUS;
            case BAZAAR -> "امکان تبدیل منابع با نرخ بازار (یک معامله در هر نوبت)";
            case MONUMENT -> "منبع تولید نمی‌کند، شادی +" + GameConfig.MONUMENT_HAPPINESS;
            case TRADING_POST -> "تبدیل منابع با نرخ "
                    + (int) (GameConfig.TRADING_POST_RATE * 100) + "٪";
            case OUTPOST -> "قلمرو را گسترش می‌دهد";
            default -> {
                if (yieldType == null || yieldPerWorker <= 0) yield "—";
                yield yieldType.getLabel() + " +" + yieldPerWorker + " به ازای هر کارگر"
                        + " (تا " + maxWorkers + " کارگر: +" + getMaxYield() + " در هر نوبت)";
            }
        };
    }

    public static String formatCost(int[] foodWoodStoneIron) {
        ResourceType[] types = ResourceType.values();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < types.length && i < foodWoodStoneIron.length; i++) {
            if (foodWoodStoneIron[i] <= 0) continue;
            if (sb.length() > 0) sb.append(" + ");
            sb.append(types[i].getLabel()).append(' ').append(foodWoodStoneIron[i]);
        }
        return sb.length() == 0 ? "رایگان" : sb.toString();
    }
}
