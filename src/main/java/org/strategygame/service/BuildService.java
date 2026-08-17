package org.strategygame.service;

import org.strategygame.common.ActionResult;
import org.strategygame.config.GameConfig;
import org.strategygame.model.GameState;
import org.strategygame.model.building.Building;
import org.strategygame.model.building.BuildingFactory;
import org.strategygame.model.building.BuildingType;
import org.strategygame.model.building.Bazaar;
import org.strategygame.model.building.Settlement;
import org.strategygame.model.event.NotificationKind;
import org.strategygame.model.map.HexCell;
import org.strategygame.model.map.HexEdge;
import org.strategygame.model.map.TerrainType;
import org.strategygame.model.map.Wall;
import org.strategygame.model.resource.ResourceStorage;
import org.strategygame.model.unit.Builder;
import org.strategygame.model.upgrade.UpgradeType;

import java.util.Set;

/**
 * تمام قواعد ساخت‌وساز: سازه، جاده، دیوار و تخریب اختیاری. هر اکشن دو سطح دارد:
 * یک متد «مشکل» برای غیرفعال کردن دکمه و Tooltip، و یک متد اجرا.
 */
public class BuildService {

    // ------------------------------------------------------------------ سازه
    /** دلیل غیرقابل‌ساخت بودن یک سازه روی یک هکس؛ {@code null} یعنی مجاز. */
    public String buildProblem(GameState state, Builder builder, BuildingType type, HexCell cell) {
        if (builder == null)              return "اول یک بیلدر انتخاب کنید";
        if (type == null || cell == null) return "انتخاب نامعتبر است";
        if (!type.isPlayerBuildable())    return type.getLabel() + " قابل ساخت نیست";
        if (!builder.hasCharges())        return "ظرفیت ساخت این بیلدر تمام شده است";

        if (builder.getPosition() == null || !builder.getPosition().equals(cell))
            return "بیلدر روی این هکس نیست";
        if (cell.hasBuilding())
            return "روی این هکس از قبل «" + cell.getBuilding().getType().getLabel() + "» ساخته شده است";
        if (!cell.isExplored())  return "این هکس هنوز کشف نشده است";
        if (!cell.isInBorder())  return "این هکس داخل مرز شما نیست";

        if (cell.getTerrain() == TerrainType.MOUNTAIN_RANGE)
            return "روی رشته‌کوه نمی‌توان ساخت";
        if (cell.isWater())
            return "روی دریا نمی‌توان ساخت";

        int level = state.getTownHall() == null ? 1 : state.getTownHall().getLevel();
        if (level < type.getRequiredTownHallLevel())
            return type.getLabel() + " به تان هال سطح " + type.getRequiredTownHallLevel()
                    + " نیاز دارد (سطح فعلی: " + level + ")";

        if (!cell.terrainAllows(type)) return terrainProblem(type, cell);

        if (type == BuildingType.DOCK && !state.getMap().isCoastal(cell))
            return "اسکله فقط روی هکس ساحلی ساخته می‌شود (باید حداقل یک همسایه‌ی دریایی داشته باشد)";
        if (type == BuildingType.BAZAAR && state.findBazaar() != null)
            return "شما از قبل یک بازار دارید";

        if (!isTechUnlocked(state, type))
            return "تکنولوژی لازم را ندارید (" + techName(type) + ")";

        if (!builder.hasAP(type.getBuildAP()))
            return "AP کافی نیست (نیاز: " + type.getBuildAP()
                    + "، موجود: " + builder.getCurrentAP() + ")";

        int[] cost = effectiveCost(state, type);
        if (!state.getStorage().canAffordAll(cost))
            return "منابع کافی نیست (نیاز: " + BuildingType.formatCost(cost) + ")";
        return null;
    }

    /** هزینه‌ی نهایی ساخت با درنظر گرفتن تخفیف‌های جایزه‌ی مأموریت. */
    public int[] effectiveCost(GameState state, BuildingType type) {
        int[] base = type.getBuildCost();
        if (type != BuildingType.DOCK || !state.isDockDiscountAvailable()) return base;

        int[] discounted = new int[base.length];
        for (int i = 0; i < base.length; i++) discounted[i] = (int) Math.floor(base[i] * 0.5);
        return discounted;
    }

    public ActionResult build(GameState state, Builder builder, BuildingType type, HexCell cell) {
        String problem = buildProblem(state, builder, type, cell);
        if (problem != null) return ActionResult.fail(problem);

        int[] cost = effectiveCost(state, type);
        ResourceStorage storage = state.getStorage();
        storage.deductAll(cost);
        builder.spendAP(type.getBuildAP());

        if (type == BuildingType.DOCK && state.isDockDiscountAvailable()) {
            state.setDockDiscountAvailable(false);
        }

        Building building = BuildingFactory.create(type);
        building.setLocation(cell);
        cell.setBuilding(building);
        state.addBuilding(building);

        if (building instanceof Bazaar bazaar && state.getTownHall() != null) {
            bazaar.setTradeLevel(state.getTownHall().getLevel());
        }
        if (building instanceof Settlement) {
            state.addUnitCap(Settlement.CAP_BONUS);
            state.setSettlementUnlocked(true);
        }

        builder.useCharge();
        if (!builder.isAlive()) state.removeUnit(builder);

        state.getFog().update(state.getUnits(), state.getBuildings());
        state.notify(NotificationKind.PRODUCTION,
                type.getLabel() + " روی هکس (" + cell.getQ() + "," + cell.getR() + ") ساخته شد");

        return ActionResult.ok(builtMessage(state, type, cell, cost));
    }

    private String builtMessage(GameState state, BuildingType type, HexCell cell, int[] cost) {
        StringBuilder sb = new StringBuilder();
        sb.append("✔ «").append(type.getLabel()).append("» روی هکس (")
          .append(cell.getQ()).append(",").append(cell.getR()).append(") ساخته شد.\n\n");
        sb.append("هزینه‌ی پرداخت‌شده: ").append(BuildingType.formatCost(cost))
          .append("  |  AP مصرف‌شده: ").append(type.getBuildAP()).append('\n');
        sb.append("منابعی که می‌دهد: ").append(type.yieldText()).append('\n');
        sb.append("هزینه‌ی نگهداری: ").append(type.upkeepText()).append('\n');

        if (type == BuildingType.SETTLEMENT) {
            sb.append("\nسقف یونیت اکنون ").append(state.getUnitCap()).append(" است.");
        } else if (type.getMaxWorkers() > 0) {
            sb.append("\nبرای شروع تولید، یک کارگر را روی این هکس ببرید و «استقرار» را بزنید")
              .append(" (تا ").append(type.getMaxWorkers()).append(" کارگر).");
        }
        return sb.toString();
    }

    private String terrainProblem(BuildingType type, HexCell cell) {
        String where = switch (type) {
            case LUMBER_MILL -> "جنگل دارای منبع چوب";
            case STONE_MINE  -> "کوهستان دارای منبع سنگ";
            case IRON_MINE   -> "کوهستان دارای منبع آهن";
            case FARM        -> "سبزه‌زار دارای منبع غذا";
            case STABLE      -> "دشت";
            case MONUMENT    -> "دشت داخل قلمرو";
            case DOCK        -> "هکس ساحلی";
            case BAZAAR      -> "هکس زمینی داخل قلمرو";
            case SETTLEMENT  -> "هکس بدون منبع";
            default          -> "—";
        };
        return type.getLabel() + " فقط روی " + where + " ساخته می‌شود (اینجا: "
                + cell.getTerrain().getLabel()
                + (cell.hasResource() ? " با منبع" : " بدون منبع") + ")";
    }

    // ------------------------------------------------------------------ جاده
    public String roadProblem(GameState state, Builder builder, HexCell cell) {
        if (builder == null)  return "اول یک بیلدر انتخاب کنید";
        if (cell == null)     return "انتخاب نامعتبر است";
        if (cell.hasRoad())   return "این هکس از قبل جاده دارد";
        if (cell.isWater())   return "روی دریا نمی‌توان جاده ساخت";
        if (cell.getTerrain() == TerrainType.MOUNTAIN_RANGE)
            return "روی رشته‌کوه نمی‌توان جاده ساخت";
        if (!cell.isExplored()) return "این هکس هنوز کشف نشده است";
        if (!cell.isInBorder()) return "جاده فقط داخل قلمرو شما ساخته می‌شود";

        if (builder.getPosition() == null || !builder.getPosition().equals(cell))
            return "بیلدر روی این هکس نیست";
        if (!builder.hasAP(GameConfig.ROAD_BUILD_AP))
            return "AP کافی نیست (نیاز: " + GameConfig.ROAD_BUILD_AP + ")";
        if (!state.getStorage().canAffordAll(GameConfig.ROAD_COST))
            return "منابع کافی نیست (نیاز: " + BuildingType.formatCost(GameConfig.ROAD_COST) + ")";
        return null;
    }

    public ActionResult buildRoad(GameState state, Builder builder, HexCell cell) {
        String problem = roadProblem(state, builder, cell);
        if (problem != null) return ActionResult.fail(problem);

        state.getStorage().deductAll(GameConfig.ROAD_COST);
        builder.spendAP(GameConfig.ROAD_BUILD_AP);
        cell.setRoad(true);

        return ActionResult.ok("✔ جاده روی هکس (" + cell.getQ() + "," + cell.getR() + ") ساخته شد.\n\n"
                + "هزینه: " + BuildingType.formatCost(GameConfig.ROAD_COST)
                + "  |  AP: " + GameConfig.ROAD_BUILD_AP + "\n"
                + "حرکت بین دو هکس جاده‌دار " + GameConfig.ROAD_MOVEMENT_COST
                + " AP است و جریمه‌ی رودخانه را هم برمی‌دارد.");
    }

    // ----------------------------------------------------------------- دیوار
    public String wallProblem(GameState state, Builder builder, HexCell a, HexCell b) {
        if (builder == null)         return "اول یک بیلدر انتخاب کنید";
        if (a == null || b == null)  return "دو هکس مجاور انتخاب کنید";
        if (!state.getMap().areNeighbors(a, b)) return "دیوار فقط روی یال بین دو هکس مجاور ساخته می‌شود";

        if (!a.isInBorder() && !b.isInBorder())
            return "حداقل یکی از دو هکس باید داخل قلمرو شما باشد";
        if (!a.isExplored() || !b.isExplored())
            return "هر دو هکس باید کشف شده باشند";
        if (a.isWater() || b.isWater())
            return "روی یال دریایی نمی‌توان دیوار ساخت";
        if (a.getTerrain() == TerrainType.MOUNTAIN_RANGE || b.getTerrain() == TerrainType.MOUNTAIN_RANGE)
            return "روی یال رشته‌کوه نمی‌توان دیوار ساخت";
        if (state.getMap().getWall(a, b) != null)
            return "روی این یال از قبل دیوار وجود دارد";

        HexCell pos = builder.getPosition();
        if (pos == null || (!pos.equals(a) && !pos.equals(b)))
            return "بیلدر باید روی یکی از دو هکس این یال باشد";
        if (!builder.hasAP(GameConfig.WALL_BUILD_AP))
            return "AP کافی نیست (نیاز: " + GameConfig.WALL_BUILD_AP + ")";
        if (!state.getStorage().canAffordAll(GameConfig.WALL_COST))
            return "منابع کافی نیست (نیاز: " + BuildingType.formatCost(GameConfig.WALL_COST) + ")";
        return null;
    }

    public ActionResult buildWall(GameState state, Builder builder, HexCell a, HexCell b) {
        String problem = wallProblem(state, builder, a, b);
        if (problem != null) return ActionResult.fail(problem);

        state.getStorage().deductAll(GameConfig.WALL_COST);
        builder.spendAP(GameConfig.WALL_BUILD_AP);

        HexEdge edge = state.getMap().edgeOrCreate(a, b);
        edge.setWall(new Wall());

        return ActionResult.ok("✔ دیوار روی یال بین (" + a.getQ() + "," + a.getR() + ") و ("
                + b.getQ() + "," + b.getR() + ") ساخته شد.\n\n"
                + "هزینه: " + BuildingType.formatCost(GameConfig.WALL_COST)
                + "  |  HP دیوار: " + GameConfig.WALL_MAX_HP + "\n"
                + "حمله از این یال به تاس دفاع +" + GameConfig.WALL_DEFENSE_BONUS
                + " می‌دهد (سقف هر تاس ۶)."
                + (edge.hasRiver() ? "\nاین یال هم‌زمان رودخانه هم دارد." : ""));
    }

    // ----------------------------------------------------------------- تخریب
    public String demolishProblem(GameState state, Builder builder, HexCell cell) {
        if (builder == null) return "اول یک بیلدر انتخاب کنید";
        if (cell == null)    return "انتخاب نامعتبر است";
        if (!cell.hasBuilding() && !cell.hasRoad()) return "روی این هکس چیزی برای تخریب نیست";

        if (cell.hasBuilding()) {
            Building b = cell.getBuilding();
            if (!b.isDemolishable())
                return b.getType().getLabel() + " با این اکشن قابل تخریب نیست";
        }
        if (state.tribeAt(cell) != null)
            return "اردوگاه فعال قبیله با این اکشن حذف نمی‌شود";

        HexCell pos = builder.getPosition();
        boolean onOrNear = pos != null
                && (pos.equals(cell) || state.getMap().areNeighbors(pos, cell));
        if (!onOrNear) return "بیلدر باید روی همان هکس یا هکس مجاور باشد";
        if (!builder.hasAP(GameConfig.DEMOLISH_AP))
            return "AP کافی نیست (نیاز: " + GameConfig.DEMOLISH_AP + ")";
        return null;
    }

    /** تخریب اختیاری؛ منابع برنمی‌گردد، کارگرها آزاد و هکس آزاد می‌شود. */
    public ActionResult demolish(GameState state, Builder builder, HexCell cell) {
        String problem = demolishProblem(state, builder, cell);
        if (problem != null) return ActionResult.fail(problem);

        builder.spendAP(GameConfig.DEMOLISH_AP);
        StringBuilder sb = new StringBuilder();

        if (cell.hasBuilding()) {
            Building b = cell.getBuilding();
            int workers = b.workerCount();
            state.destroyBuilding(b);
            sb.append("«").append(b.getType().getLabel()).append("» تخریب شد.\n");
            if (workers > 0) sb.append(workers).append(" کارگر آزاد شد.\n");
        } else if (cell.hasRoad()) {
            cell.setRoad(false);
            sb.append("جاده برداشته شد.\n");
        }

        sb.append("\nمنابع بازگردانده نمی‌شود و هکس اکنون آزاد است.");
        state.notify(NotificationKind.GENERAL,
                "تخریب روی هکس (" + cell.getQ() + "," + cell.getR() + ")");
        return ActionResult.ok(sb.toString());
    }

    // ---------------------------------------------------------------- کمکی‌ها
    /** سازه‌ای که از نظر زمین/منبع روی این هکس قابل ساخت است. */
    public BuildingType buildableTypeAt(GameState state, HexCell cell) {
        if (cell == null || cell.hasBuilding()) return null;
        for (BuildingType t : BuildingType.values()) {
            if (!t.isPlayerBuildable()) continue;
            if (!cell.terrainAllows(t)) continue;
            if (t == BuildingType.DOCK && !state.getMap().isCoastal(cell)) continue;
            if (t == BuildingType.BAZAAR && state.findBazaar() != null) continue;
            return t;
        }
        return null;
    }

    public boolean isTechUnlocked(GameState state, BuildingType type) {
        Set<UpgradeType> done = state.getDoneUpgrades();
        return switch (type) {
            case STONE_MINE -> done.contains(UpgradeType.STONE_MINE_TECH);
            case IRON_MINE  -> done.contains(UpgradeType.IRON_MINE_TECH);
            case SETTLEMENT -> done.contains(UpgradeType.SETTLEMENT_TECH);
            default -> true;
        };
    }

    private String techName(BuildingType type) {
        return switch (type) {
            case STONE_MINE -> UpgradeType.STONE_MINE_TECH.getLabel();
            case IRON_MINE  -> UpgradeType.IRON_MINE_TECH.getLabel();
            case SETTLEMENT -> UpgradeType.SETTLEMENT_TECH.getLabel();
            default         -> "—";
        };
    }
}
