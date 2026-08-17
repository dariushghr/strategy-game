package org.strategygame.service;

import org.strategygame.config.GameConfig;
import org.strategygame.model.GameState;
import org.strategygame.model.building.Building;
import org.strategygame.model.building.BuildingType;
import org.strategygame.model.building.TownHall;
import org.strategygame.model.map.HexCell;
import org.strategygame.model.map.TerrainType;
import org.strategygame.model.resource.ResourceRate;
import org.strategygame.model.resource.ResourceType;
import org.strategygame.model.season.Season;

/**
 * تک مسیر محاسبه‌ی تولید. ترتیب اعمال مودیفایرها فقط همین‌جا تعریف شده است:
 *
 * <pre>
 * تولید پایه‌ی کارگر → مودیفایر تکنولوژی → بونوس همجواری
 * → مودیفایر فصل → مودیفایر شادی → گرد کردن به پایین → انبار
 * </pre>
 */
public class ProductionService {

    /** تولید نهایی یک سازه در این نوبت. */
    public int finalOutput(GameState state, Building building) {
        if (building == null || !building.isFunctional()) return 0;
        if (building instanceof TownHall) return GameConfig.TOWN_HALL_BASE_FOOD;
        if (building.getResourceType() == null) return 0;
        if (building.getType() == BuildingType.DOCK && !hasUsableFish(state, building)) return 0;

        BuildingType type = building.getType();

        // ۱) تولید پایه‌ی کارگرها
        double value = building.workerCount() * (double) type.getYieldPerWorker();
        if (value <= 0 && adjacencyBonus(state, building) <= 0) return 0;

        // ۲) مودیفایر تکنولوژی (ابزار فولادی روی معادن سنگ و آهن)
        if (type == BuildingType.STONE_MINE || type == BuildingType.IRON_MINE) {
            value *= state.getMiningMultiplier();
        }

        // ۳) بونوس همجواری روی خروجی نهایی سازه
        value += adjacencyBonus(state, building);

        // ۴) مودیفایر فصل
        if (building.getResourceType() == ResourceType.FOOD) {
            value += state.getSeason().foodModifierFor(type);
        }

        // ۵) مودیفایر شادی
        value *= state.getHappiness().productionMultiplier();
        value -= state.getHappiness().workerPenalty() * building.workerCount();

        // ۶) گرد کردن به پایین و کف صفر
        return Math.max(0, (int) Math.floor(value));
    }

    /**
     * بونوس همجواری. هر جفت مزرعه‌ی مجاور فقط یک بار شمرده می‌شود؛ برای این کار
     * بونوس به مزرعه‌ای داده می‌شود که مختصات کوچک‌تری دارد.
     */
    public int adjacencyBonus(GameState state, Building building) {
        HexCell loc = building.getLocation();
        if (loc == null) return 0;

        return switch (building.getType()) {
            case FARM -> GameConfig.ADJACENCY_FARM_PAIR_FOOD * countCanonicalFarmPairs(state, loc);
            case LUMBER_MILL -> state.getMap().isCoastal(loc)
                    ? GameConfig.ADJACENCY_LUMBER_SEA_WOOD : 0;
            case STONE_MINE, IRON_MINE ->
                    countMountainNeighbors(state, loc) >= GameConfig.ADJACENCY_MINE_MOUNTAIN_MIN
                            ? GameConfig.ADJACENCY_MINE_MOUNTAIN_OUT : 0;
            default -> 0;
        };
    }

    private int countCanonicalFarmPairs(GameState state, HexCell loc) {
        int pairs = 0;
        for (HexCell n : state.getMap().getNeighbors(loc)) {
            if (!n.hasBuilding() || n.getBuilding().getType() != BuildingType.FARM) continue;
            if (!n.getBuilding().isFunctional()) continue;
            if (isCanonicalFirst(loc, n)) pairs++;
        }
        return pairs;
    }

    /** برای این‌که یک جفت دو بار شمرده نشود، فقط یک طرف جفت را «صاحب بونوس» می‌گیریم. */
    private boolean isCanonicalFirst(HexCell a, HexCell b) {
        return a.getQ() < b.getQ() || (a.getQ() == b.getQ() && a.getR() < b.getR());
    }

    private int countMountainNeighbors(GameState state, HexCell loc) {
        int count = 0;
        for (HexCell n : state.getMap().getNeighbors(loc)) {
            if (n.getTerrain() == TerrainType.MOUNTAIN
                    || n.getTerrain() == TerrainType.MOUNTAIN_RANGE) count++;
        }
        return count;
    }

    /** اسکله فقط وقتی غذا می‌دهد که ماهی قابل استفاده‌ای نزدیکش باشد. */
    public boolean hasUsableFish(GameState state, Building dock) {
        HexCell loc = dock.getLocation();
        if (loc == null) return false;
        if (loc.hasFish()) return true;
        for (HexCell n : state.getMap().getNeighbors(loc)) {
            if (n.hasFish()) return true;
        }
        return false;
    }

    /** ماهی مصرف‌شده توسط اسکله را از منبع کم می‌کند. */
    public void consumeFish(GameState state, Building dock, int amount) {
        if (amount <= 0) return;
        HexCell loc = dock.getLocation();
        if (loc == null) return;

        if (loc.hasFish()) { loc.getDeposit().extract(amount); return; }
        for (HexCell n : state.getMap().getNeighbors(loc)) {
            if (n.hasFish()) { n.getDeposit().extract(amount); return; }
        }
    }

    /** نرخ تولید و مصرف برای نمایش در HUD. */
    public ResourceRate calculate(GameState state) {
        ResourceRate rate = new ResourceRate();
        if (state.getMap() == null) return rate;

        for (Building b : state.getBuildings()) {
            if (!b.isFunctional()) continue;

            ResourceType rt = b.getResourceType();
            if (rt != null) rate.addProduction(rt, finalOutput(state, b));

            int[] upkeep = b.getType().getUpkeepCost();
            ResourceType[] types = ResourceType.values();
            for (int i = 0; i < types.length && i < upkeep.length; i++) {
                if (upkeep[i] > 0) rate.addUpkeep(types[i], upkeep[i]);
            }
        }

        TownHall th = state.getTownHall();
        if (th != null) rate.addProduction(ResourceType.WOOD, GameConfig.TOWN_HALL_BASE_WOOD);

        rate.addUpkeep(ResourceType.FOOD, state.getUnits().size() * GameState.FOOD_PER_UNIT);
        return rate;
    }

    /** ریختن تولید این نوبت داخل انبار. */
    public void applyProduction(GameState state) {
        for (Building b : state.getBuildings()) {
            if (!b.isFunctional()) continue;
            ResourceType rt = b.getResourceType();
            if (rt == null) continue;

            int output = finalOutput(state, b);
            if (output <= 0) continue;

            if (b.getType() == BuildingType.DOCK) consumeFish(state, b, output);
            state.getStorage().add(rt, output);
        }

        if (state.getTownHall() != null) {
            state.getStorage().add(ResourceType.WOOD, GameConfig.TOWN_HALL_BASE_WOOD);
        }
    }

    /**
     * توضیح این‌که خروجی نهایی یک سازه از چه مودیفایرهایی ساخته شده است؛
     * برای Tooltip و توضیح دادن اقتصاد بازی.
     */
    public String explain(GameState state, Building building) {
        if (building == null) return "—";
        if (building instanceof TownHall) {
            return "تان هال پایه: غذا +" + GameConfig.TOWN_HALL_BASE_FOOD
                    + " و چوب +" + GameConfig.TOWN_HALL_BASE_WOOD + " (مستقل از سطح و مودیفایرها)";
        }

        BuildingType type = building.getType();
        StringBuilder sb = new StringBuilder();
        int base = building.workerCount() * type.getYieldPerWorker();
        sb.append("پایه‌ی کارگر: ").append(building.workerCount()).append(" × ")
          .append(type.getYieldPerWorker()).append(" = ").append(base);

        if (type == BuildingType.STONE_MINE || type == BuildingType.IRON_MINE) {
            sb.append("  |  تکنولوژی ×").append(state.getMiningMultiplier());
        }

        int adjacency = adjacencyBonus(state, building);
        if (adjacency != 0) sb.append("  |  همجواری +").append(adjacency);

        Season season = state.getSeason();
        int seasonMod = building.getResourceType() == ResourceType.FOOD
                ? season.foodModifierFor(type) : 0;
        if (seasonMod != 0) {
            sb.append("  |  ").append(season.getLabel())
              .append(seasonMod > 0 ? " +" : " ").append(seasonMod);
        }

        if (state.getHappiness().productionMultiplier() != 1.0) {
            sb.append("  |  شادی ×").append(state.getHappiness().productionMultiplier());
        }
        if (state.getHappiness().workerPenalty() > 0) {
            sb.append("  |  جریمه‌ی شادی -")
              .append(state.getHappiness().workerPenalty() * building.workerCount());
        }

        sb.append("  ⇒  ").append(finalOutput(state, building));
        return sb.toString();
    }
}
