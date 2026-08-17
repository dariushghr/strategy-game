package org.strategygame.config;

/**
 * تمام عددهای قابل تنظیم بازی در همین یک کلاس جمع شده‌اند تا هیچ عدد جادویی
 * داخل منطق پراکنده نشود. ترتیب آرایه‌های هزینه همیشه
 * {@code {FOOD, WOOD, STONE, IRON}} است، مطابق ترتیب
 * {@link org.strategygame.model.resource.ResourceType}.
 */
public final class GameConfig {

    private GameConfig() { }

    // ---------------------------------------------------------------- تان هال
    /** غذایی که تان هال مستقل از سطح در هر نوبت می‌دهد. */
    public static final int TOWN_HALL_BASE_FOOD = 1;
    /** چوبی که تان هال مستقل از سطح در هر نوبت می‌دهد. */
    public static final int TOWN_HALL_BASE_WOOD = 1;

    /** ظرفیت انبار به ازای هر سطح تان هال (سطح ۱ در ایندکس صفر). */
    public static final int[] STORAGE_CAPACITY_BY_LEVEL = {100, 250, 500};

    public static final int[] TOWN_HALL_LEVEL_2_COST  = {0, 50, 50, 0};
    public static final int   TOWN_HALL_LEVEL_2_TURNS = 3;
    public static final int[] TOWN_HALL_LEVEL_3_COST  = {0, 0, 100, 50};
    public static final int   TOWN_HALL_LEVEL_3_TURNS = 5;

    /** رسیدن به سطح ۲ این مقدار HP تان هال را ترمیم می‌کند. */
    public static final int TOWN_HALL_LEVEL_2_HEAL = 50;

    public static final int TOWN_HALL_MAX_HP  = 300;
    public static final int TOWN_HALL_DEFENSE = 10;

    /** سقف یونیت نظامی بر اساس سطح تان هال. */
    public static final int[] MILITARY_CAP_BY_LEVEL = {4, 8, 14};

    // ------------------------------------------------------------ تکنولوژی‌ها
    /** ضریب تولید معدن سنگ/آهن بعد از ابزار فولادی. */
    public static final double STEEL_TOOLS_MINING_MULTIPLIER = 1.5;

    /** با معماری دفاعی، دیوار دفاعی تان هال ساخته می‌شود. */
    public static final int DEFENSIVE_ARCHITECTURE_DEFENSE = 30;
    public static final int DEFENSIVE_ARCHITECTURE_MAX_HP  = 350;

    // ------------------------------------------------------------------ نقشه
    public static final int MOUNTAIN_MOVEMENT_COST = 3;
    public static final int RIVER_CROSSING_COST    = 2;
    /** عبور از رودخانه وقتی هر دو طرف جاده دارند این‌قدر هزینه دارد. */
    public static final int RIVER_CROSSING_COST_WITH_ROAD = 0;
    /** هزینه حرکت بین دو هکسِ دارای جاده. */
    public static final int ROAD_MOVEMENT_COST = 1;

    public static final int[] ROAD_COST     = {0, 5, 5, 0};
    public static final int   ROAD_BUILD_AP = 1;

    public static final int[] WALL_COST     = {0, 10, 20, 0};
    public static final int   WALL_BUILD_AP = 1;
    public static final int   WALL_MAX_HP   = 100;
    /** امتیازی که دیوار به تاس دفاع اضافه می‌کند. */
    public static final int   WALL_DEFENSE_BONUS = 2;

    public static final int[] MONUMENT_COST      = {0, 40, 60, 10};
    public static final int   MONUMENT_HAPPINESS = 2;

    public static final int[] DOCK_COST = {0, 30, 10, 0};

    public static final int DEMOLISH_AP = 1;

    // ------------------------------------------------------------ یونیت نظامی
    /** HP واقعی یونیت‌ها؛ بلایای طبیعی و حمله‌ی حیوانات روی همین کار می‌کنند. */
    public static final int CIVILIAN_HP  = 50;
    public static final int SWORDSMAN_HP = 100;
    public static final int ARCHER_HP    = 80;
    public static final int CAVALRY_HP   = 120;

    /** HP نبرد؛ تعداد ضربه‌ی تاسی که یونیت تحمل می‌کند. */
    public static final int SWORDSMAN_BATTLE_HP = 1;
    public static final int ARCHER_BATTLE_HP    = 1;
    public static final int CAVALRY_BATTLE_HP   = 2;

    public static final int SWORDSMAN_AP = 2, ARCHER_AP = 2, CAVALRY_AP = 4;
    public static final int SWORDSMAN_RANGE = 1, ARCHER_RANGE = 2, CAVALRY_RANGE = 1;
    public static final int MILITARY_VISION = 2;

    /** هزینه‌ی AP هر حمله. */
    public static final int ATTACK_AP_COST = 1;

    /** حداقل تعداد تاس دفاع برای مدافع معمولی. */
    public static final int DEFENDER_MIN_DICE = 2;

    public static final int[] SWORDSMAN_COST = {20, 10, 0, 0};
    public static final int[] ARCHER_COST    = {15, 25, 0, 0};
    public static final int[] CAVALRY_COST   = {30, 10, 0, 20};

    public static final int SWORDSMAN_TRAIN_TURNS = 2;
    public static final int ARCHER_TRAIN_TURNS    = 2;
    public static final int CAVALRY_TRAIN_TURNS   = 3;

    /** حداکثر تعداد هر نوع یونیت نظامی در یک هکس. */
    public static final int HEX_CAP_SWORDSMAN = 2;
    public static final int HEX_CAP_ARCHER    = 2;
    public static final int HEX_CAP_CAVALRY   = 1;

    public static final int STRUCTURE_DAMAGE_SWORDSMAN = 10;
    public static final int STRUCTURE_DAMAGE_ARCHER    = 6;
    public static final int STRUCTURE_DAMAGE_CAVALRY   = 8;

    public static final int DICE_SIDES     = 6;
    public static final int DICE_MAX_VALUE = 6;

    public static final int BARBARIAN_DEFENSE_DICE   = 2;
    public static final int WILD_ANIMAL_DEFENSE_DICE = 1;

    // ------------------------------------------------------------------ تجارت
    /** سطح‌های بازار: حداکثر مقدار فروش و نرخ تبدیل. */
    public static final int[]    BAZAAR_MAX_AMOUNT = {10, 100, 500};
    public static final double[] BAZAAR_RATE       = {0.50, 0.60, 0.70};
    public static final int[]    BAZAAR_COST       = {0, 40, 20, 0};

    public static final double TRADING_POST_RATE       = 0.80;
    public static final int    TRADING_POST_MAX_AMOUNT = 500;
    public static final int    TRIBE_TRADE_MAX_AMOUNT  = 200;

    public static final double TRIBE_RATE_FARMER   = 0.75;
    public static final double TRIBE_RATE_MOUNTAIN = 0.75;
    public static final double TRIBE_RATE_MERCHANT = 0.80;
    public static final double TRIBE_RATE_COASTAL  = 0.75;
    public static final double TRIBE_RATE_WARRIOR  = 0.70;

    // -------------------------------------------------------- بونوس همجواری
    public static final int ADJACENCY_FARM_PAIR_FOOD    = 1;
    public static final int ADJACENCY_LUMBER_SEA_WOOD   = 2;
    public static final int ADJACENCY_MINE_MOUNTAIN_OUT = 1;
    public static final int ADJACENCY_MINE_MOUNTAIN_MIN = 2;

    // ------------------------------------------------------------------ شادی
    public static final int HAPPINESS_NEW_SETTLEMENT   = -1;
    public static final int HAPPINESS_UNIT_CAP_REACHED = -1;
    public static final int HAPPINESS_MILITARY_AT_HOME = 1;
    public static final int HAPPINESS_ATTACK_FRIENDLY  = -5;
    public static final int HAPPINESS_ATTACK_ALLIED    = -15;
    public static final int HAPPINESS_DECLARE_WAR      = -1;

    public static final int    HAPPINESS_GOLDEN_AGE_MIN = 3;
    public static final int    HAPPINESS_UNREST_MAX     = -3;
    public static final int    HAPPINESS_REBELLION_MAX  = -5;
    public static final double GOLDEN_AGE_PRODUCTION_BONUS = 0.10;
    /** در نارضایتی، هر کارگر یک واحد کمتر تولید می‌کند. */
    public static final int UNREST_WORKER_PENALTY = 1;
    /** در شورش، حرکت یونیت‌ها یک AP کمتر می‌شود. */
    public static final int REBELLION_AP_PENALTY = 1;

    // ------------------------------------------------------------------ قبیله
    /** حداقل فاصله‌ی اردوگاه قبیله از تان هال. */
    public static final int TRIBE_MIN_DISTANCE_FROM_TOWN_HALL = 6;
    public static final int TRIBE_TERRITORY_RADIUS = 1;

    public static final int RELATION_MIN = -100;
    public static final int RELATION_MAX = 100;
    public static final int RELATION_TRADE_MIN    = 20;
    public static final int RELATION_ALLIANCE_MIN = 70;
    public static final int RELATION_WAR_VALUE    = -100;
    public static final int RELATION_AFTER_PEACE  = -10;

    public static final int[] PEACE_COST = {30, 30, 0, 30};

    /** هدیه: هر چند واحد از هر منبع، چند امتیاز رابطه می‌دهد. */
    public static final int GIFT_FOOD_UNIT   = 10, GIFT_FOOD_GAIN   = 2;
    public static final int GIFT_WOOD_UNIT   = 10, GIFT_WOOD_GAIN   = 2;
    public static final int GIFT_STONE_UNIT  = 10, GIFT_STONE_GAIN  = 3;
    public static final int GIFT_IRON_UNIT   = 5,  GIFT_IRON_GAIN   = 3;

    public static final int MISSION_FAIL_RELATION      = -10;
    public static final int MISSION_CANCEL_RELATION    = -5;
    public static final int MISSION_FAIL_COOLDOWN      = 5;
    public static final int FRIENDLY_MISSION_INTERVAL  = 5;

    public static final int TRIBE_WARNING_RADIUS   = 6;
    public static final int TRIBE_TRESPASS_PENALTY = -5;
    public static final int TRIBE_GUARD_INTERVAL   = 3;
    public static final int TRIBE_GUARD_CAP        = 3;
    public static final int TRIBE_GUARD_CAP_WARRIOR = 5;
    public static final int TRIBE_AGGRESSION_RADIUS = 5;

    // ------------------------------------------------------------------- فصل
    public static final int SEASON_LENGTH = 10;
    public static final int SPRING_FARM_BONUS    = 1;
    public static final int WINTER_FARM_PENALTY  = 1;
    public static final int WINTER_LAND_AP_COST  = 1;
    public static final int AUTUMN_WATER_AP_BONUS = 1;

    // -------------------------------------------------------------- بلای طبیعی
    public static final double DISASTER_CHANCE = 0.05;

    public static final int EARTHQUAKE_RADIUS         = 2;
    public static final int EARTHQUAKE_UNIT_DAMAGE    = 10;
    public static final int EARTHQUAKE_TOWN_HALL_DAMAGE = 50;
    public static final int EARTHQUAKE_BUILDING_DAMAGE  = 25;

    public static final int FLOOD_UNIT_DAMAGE     = 20;
    public static final int FLOOD_BUILDING_DAMAGE = 30;
    public static final int FLOOD_RIVER_DISTANCE  = 1;
    /** سازه‌ی تولیدی آسیب‌دیده از سیل تا پایان نوبت بعد متوقف است. */
    public static final int FLOOD_DISABLE_TURNS = 2;

    public static final int BEAR_HP           = 120;
    public static final int BEAR_ATTACK       = 35;
    public static final int BEAR_RANGE        = 1;
    public static final int BEAR_AP           = 2;
    public static final int BEAR_MAX_COUNT    = 2;
    public static final int BEAR_SECOND_BEAR_UNIT_THRESHOLD = 3;
    public static final int BEAR_TARGET_RADIUS = 3;
    public static final int BEAR_COOLDOWN      = 5;
}
