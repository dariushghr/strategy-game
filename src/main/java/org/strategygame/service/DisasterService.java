package org.strategygame.service;

import org.strategygame.config.GameConfig;
import org.strategygame.model.GameState;
import org.strategygame.model.building.Building;
import org.strategygame.model.building.BuildingType;
import org.strategygame.model.combat.CombatResult;
import org.strategygame.model.combat.Combatant;
import org.strategygame.model.disaster.DisasterEvent;
import org.strategygame.model.disaster.DisasterType;
import org.strategygame.model.event.NotificationKind;
import org.strategygame.model.map.HexCell;
import org.strategygame.model.map.HexEdge;
import org.strategygame.model.map.TerrainType;
import org.strategygame.model.map.Wall;
import org.strategygame.model.season.Season;
import org.strategygame.model.unit.Bear;
import org.strategygame.model.unit.Owner;
import org.strategygame.model.unit.Unit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * موتور رویداد بلایای طبیعی. هر بلا یک {@link DisasterEvent} برمی‌گرداند که
 * کاملا مستقل از View است؛ لایه‌ی نمایش با خواندن همان شیء تصمیم می‌گیرد
 * انیمیشن اجرا کند یا فقط هشدار متنی بدهد.
 */
public class DisasterService {

    private final CombatService combatService;

    public DisasterService(CombatService combatService) {
        this.combatService = combatService;
    }

    // ------------------------------------------------------------ قرعه‌ی نوبت
    /**
     * ابتدای هر نوبت یک قرعه با احتمال {@link GameConfig#DISASTER_CHANCE}.
     * @return رویداد رخ‌داده یا {@code null} اگر قرعه نگرفت.
     */
    public DisasterEvent rollForTurn(GameState state) {
        if (!state.getRandom().chance(GameConfig.DISASTER_CHANCE)) return null;

        List<DisasterType> candidates = candidates(state);
        if (candidates.isEmpty()) return null;

        DisasterType chosen = state.getRandom().pick(candidates);
        return trigger(state, chosen);
    }

    /** فقط بلاهایی که با زمین و فصل فعلی سازگارند وارد قرعه می‌شوند. */
    public List<DisasterType> candidates(GameState state) {
        List<DisasterType> list = new ArrayList<>();
        if (!landCells(state).isEmpty())                  list.add(DisasterType.EARTHQUAKE);
        if (!floodCenters(state).isEmpty())               list.add(DisasterType.FLOOD);
        if (!bearDens(state).isEmpty())                   list.add(DisasterType.BEAR_ATTACK);
        return list;
    }

    /** اجرای دستی یک بلا؛ برای دیباگ و تست سناریوها بدون انتظار برای قرعه. */
    public DisasterEvent trigger(GameState state, DisasterType type) {
        DisasterEvent event = switch (type) {
            case EARTHQUAKE  -> earthquake(state, state.getRandom().pick(landCells(state)));
            case FLOOD       -> flood(state, state.getRandom().pick(floodCenters(state)));
            case BEAR_ATTACK -> bearAttack(state, state.getRandom().pick(bearDens(state)));
        };
        if (event != null) publish(state, event);
        return event;
    }

    private void publish(GameState state, DisasterEvent event) {
        state.setLastDisaster(event);
        state.purgeDeadUnits();
        state.notify(NotificationKind.DISASTER, event.headline()
                + (event.isVisible() ? "" : " (خارج از دید شما)"));
    }

    // ---------------------------------------------------------------- زلزله
    /** زلزله روی هر هکس خشکی؛ شعاع ۲ هکس یعنی مجموعا ۱۹ هکس. */
    public DisasterEvent earthquake(GameState state, HexCell center) {
        if (center == null) return null;

        DisasterEvent event = new DisasterEvent(DisasterType.EARTHQUAKE, state.getTurn(), center);
        List<HexCell> area = state.getMap().getCellsInRadius(center, GameConfig.EARTHQUAKE_RADIUS);

        for (HexCell cell : area) {
            event.addAffected(cell);
            damageUnits(cell, GameConfig.EARTHQUAKE_UNIT_DAMAGE, false, event);
            damageBuilding(state, cell, event, false);
            damageWalls(state, cell, event);
        }
        markVisibility(state, event);
        return event;
    }

    /** ساختمان‌های داخل منطقه؛ زلزله تان هال را زیر کف HP آن نمی‌برد. */
    private void damageBuilding(GameState state, HexCell cell, DisasterEvent event, boolean flood) {
        Building building = cell.getBuilding();
        if (building == null) return;

        if (flood && (building.getType() == BuildingType.FARM)) {
            state.destroyBuilding(building);
            event.addEffect("مزرعه در هکس (" + cell.getQ() + "," + cell.getR() + ") نابود شد");
            return;
        }

        int requested = flood
                ? GameConfig.FLOOD_BUILDING_DAMAGE
                : (building.getType() == BuildingType.TOWN_HALL
                    ? GameConfig.EARTHQUAKE_TOWN_HALL_DAMAGE
                    : GameConfig.EARTHQUAKE_BUILDING_DAMAGE);

        int applied = damageStructureSafely(building, requested);
        if (applied <= 0) return;

        event.addEffect(building.getType().getLabel() + " " + applied + " HP آسیب دید ("
                + building.getHp() + "/" + building.getMaxHp() + ")");

        if (building.isDestroyed()) {
            state.destroyBuilding(building);
            event.addEffect(building.getType().getLabel() + " نابود شد");
            return;
        }
        if (flood && building.getType().getYieldType() != null) {
            building.disableFor(GameConfig.FLOOD_DISABLE_TURNS);
            event.addEffect(building.getType().getLabel() + " تا پایان نوبت بعد متوقف است");
        }
    }

    /**
     * آسیب سازه با احترام به کف HP آن؛ تان هال با بلای طبیعی از بین نمی‌رود.
     * @return مقدار آسیبی که واقعا اعمال شد.
     */
    private int damageStructureSafely(Building building, int requested) {
        int room = building.getHp() - building.getMinimumHp();
        int applied = Math.max(0, Math.min(requested, room));
        building.takeDamage(applied);
        return applied;
    }

    private void damageWalls(GameState state, HexCell cell, DisasterEvent event) {
        for (HexEdge edge : state.getMap().getEdgesAround(cell)) {
            Wall wall = edge.getWall();
            if (wall == null || wall.isDestroyed()) continue;

            wall.takeDamage(GameConfig.EARTHQUAKE_BUILDING_DAMAGE);
            if (wall.isDestroyed()) {
                edge.removeWall();
                event.addEffect("یک دیوار فرو ریخت");
            } else {
                event.addEffect("دیوار آسیب دید (" + wall.getHp() + "/" + wall.getMaxHp() + ")");
            }
        }
    }

    // ------------------------------------------------------------------ سیل
    /** سیل فقط در پاییز و فقط نزدیک رودخانه یا ساحل رخ می‌دهد. */
    public DisasterEvent flood(GameState state, HexCell center) {
        if (center == null || !state.getSeason().allowsFlood()) return null;

        DisasterEvent event = new DisasterEvent(DisasterType.FLOOD, state.getTurn(), center);

        List<HexCell> area = new ArrayList<>();
        area.add(center);
        for (HexCell n : state.getMap().getNeighbors(center)) {
            if (isFloodImmune(n)) continue;
            if (n.getTerrain().isLand()) area.add(n);
        }

        for (HexCell cell : area) {
            event.addAffected(cell);
            damageUnits(cell, GameConfig.FLOOD_UNIT_DAMAGE, true, event);

            if (cell.hasRoad()) {
                cell.setRoad(false);
                event.addEffect("جاده در هکس (" + cell.getQ() + "," + cell.getR() + ") از بین رفت");
            }
            damageBuilding(state, cell, event, true);
        }
        markVisibility(state, event);
        return event;
    }

    private boolean isFloodImmune(HexCell cell) {
        TerrainType t = cell.getTerrain();
        return t == TerrainType.MOUNTAIN || t == TerrainType.MOUNTAIN_RANGE || t.isWater();
    }

    /** مرکزهای مجاز سیل: دشت/سبزه‌زار/جنگل نزدیک رودخانه یا ساحل. */
    public List<HexCell> floodCenters(GameState state) {
        List<HexCell> list = new ArrayList<>();
        if (state.getMap() == null || !state.getSeason().allowsFlood()) return list;

        for (HexCell cell : state.getMap().getAllCells()) {
            TerrainType t = cell.getTerrain();
            boolean validTerrain = t == TerrainType.PLAINS
                    || t == TerrainType.GRASSLAND
                    || t == TerrainType.FOREST;
            if (!validTerrain) continue;
            if (nearWater(state, cell)) list.add(cell);
        }
        return list;
    }

    /** فاصله‌ی حداکثر یک هکس از رودخانه یا ساحل. */
    private boolean nearWater(GameState state, HexCell cell) {
        if (state.getMap().touchesRiver(cell) || state.getMap().isCoastal(cell)) return true;
        if (GameConfig.FLOOD_RIVER_DISTANCE < 1) return false;
        for (HexCell n : state.getMap().getNeighbors(cell)) {
            if (state.getMap().touchesRiver(n) || state.getMap().isCoastal(n)) return true;
        }
        return false;
    }

    // ------------------------------------------------------------- حمله‌ی خرس
    /** خرس‌ها از یک لانه‌ی جنگلی بیرون می‌آیند؛ هر لانه ۵ نوبت خنک‌شدن دارد. */
    public DisasterEvent bearAttack(GameState state, HexCell den) {
        if (den == null) return null;

        DisasterEvent event = new DisasterEvent(DisasterType.BEAR_ATTACK, state.getTurn(), den);
        event.addAffected(den);

        int nearby = playerUnitsNear(state, den).size();
        int count = nearby >= GameConfig.BEAR_SECOND_BEAR_UNIT_THRESHOLD ? 2 : 1;
        count = Math.min(count, GameConfig.BEAR_MAX_COUNT);

        int spawned = 0;
        for (int i = 0; i < count; i++) {
            HexCell spot = i == 0 ? den : freeLandNeighbor(state, den);
            if (spot == null) spot = den;
            state.spawn(new Bear(den), spot);
            spawned++;
        }
        state.markBearDenUsed(den);

        event.addEffect(spawned + " خرس از لانه‌ی جنگلی بیرون آمد");
        event.addEffect("نیروهای شما در شعاع " + GameConfig.BEAR_TARGET_RADIUS + " هکس در خطرند");
        markVisibility(state, event);
        return event;
    }

    /** لانه‌های جنگلی معتبر: بدون خنک‌شدن فعال و با هدفی در شعاع ۳ هکس. */
    public List<HexCell> bearDens(GameState state) {
        List<HexCell> list = new ArrayList<>();
        if (state.getMap() == null) return list;

        for (HexCell cell : state.getMap().getAllCells()) {
            if (cell.getTerrain() != TerrainType.FOREST) continue;
            if (cell.hasBuilding() || state.isBearDenOnCooldown(cell)) continue;
            if (playerUnitsNear(state, cell).isEmpty()) continue;
            list.add(cell);
        }
        return list;
    }

    /**
     * نوبت خرس‌ها: حرکت به سمت هدف، حداکثر یک حمله، و برگشت به لانه وقتی
     * دیگر هدفی در شعاع نمانده است.
     */
    public void runBearTurn(GameState state) {
        for (Bear bear : new ArrayList<>(state.getBears())) {
            if (!bear.isAlive()) continue;

            bear.resetTurnFlags();
            bear.refreshAP();

            Unit target = pickBearTarget(state, bear);
            if (target == null) {
                returnHome(state, bear);
                continue;
            }
            approachAndStrike(state, bear, target);
        }
        state.purgeDeadUnits();
    }

    /** اولویت هدف: اول غیرنظامی، بعد نظامی؛ در هر گروه نزدیک‌ترین. */
    private Unit pickBearTarget(GameState state, Bear bear) {
        HexCell den = bear.getDen() != null ? bear.getDen() : bear.getPosition();
        List<Unit> options = playerUnitsNear(state, den);
        if (options.isEmpty()) return null;

        options.sort(Comparator
                .comparingInt((Unit u) -> u.isMilitary() ? 1 : 0)
                .thenComparingInt(u -> state.getMap().distance(bear.getPosition(), u.getPosition())));
        return options.getFirst();
    }

    private void approachAndStrike(GameState state, Bear bear, Unit target) {
        HexCell goal = target.getPosition();
        while (state.getMap().distance(bear.getPosition(), goal) > GameConfig.BEAR_RANGE
                && bear.getCurrentAP() > 0) {
            if (!stepToward(state, bear, goal)) break;
        }

        if (state.getMap().distance(bear.getPosition(), goal) > GameConfig.BEAR_RANGE) return;
        if (bear.hasAttackedThisTurn() || !bear.hasAP(GameConfig.ATTACK_AP_COST)) return;

        strike(state, bear, target);
    }

    /**
     * حمله‌ی خرس. اگر روی هکس هدف نیروی رزمی باشد نبرد از موتور نبرد روز سه
     * عبور می‌کند (خرس یک تاس دارد) و در نهایت آسیب واقعی خرس روی هدف اعمال
     * می‌شود. خرس هرگز به سازه آسیب نمی‌زند.
     */
    private void strike(GameState state, Bear bear, Unit target) {
        HexCell cell = target.getPosition();
        List<Combatant> defenders = combatService.friendlyCombatants(cell, Owner.PLAYER);

        String detail;
        if (!defenders.isEmpty()) {
            CombatResult result = combatService.resolveDiceCombat(state,
                    new ArrayList<>(List.of(bear)), defenders, 0, "خرس", "نیروهای شما");
            detail = result.getDefenderCasualties().isEmpty()
                    ? "بدون تلفات" : String.join(" , ", result.getDefenderCasualties());
        } else {
            detail = target.getType().getLabel() + " زیر ضربه رفت";
        }

        if (target.isAlive()) target.takeDamage(bear.getAttackDamage());
        bear.spendAP(GameConfig.ATTACK_AP_COST);
        bear.markAttacked();

        state.notify(NotificationKind.DISASTER,
                "خرس در هکس (" + cell.getQ() + "," + cell.getR() + ") حمله کرد — " + detail);
        state.purgeDeadUnits();
    }

    /** بدون هدف، خرس به لانه برمی‌گردد و رویداد تمام می‌شود. */
    private void returnHome(GameState state, Bear bear) {
        HexCell den = bear.getDen();
        if (den == null || bear.getPosition() == null || den.equals(bear.getPosition())) {
            state.removeUnit(bear);
            return;
        }
        while (bear.getCurrentAP() > 0 && !den.equals(bear.getPosition())) {
            if (!stepToward(state, bear, den)) break;
        }
        if (den.equals(bear.getPosition())) state.removeUnit(bear);
    }

    private boolean stepToward(GameState state, Bear bear, HexCell goal) {
        HexCell from = bear.getPosition();
        HexCell best = null;
        int bestDistance = state.getMap().distance(from, goal);

        for (HexCell n : state.getMap().getNeighbors(from)) {
            if (!n.getTerrain().isLandPassable() || n.isBlocked()) continue;
            if (!bear.hasAP(n.getTerrain().getMovementCost())) continue;

            int distance = state.getMap().distance(n, goal);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = n;
            }
        }
        if (best == null) return false;

        bear.spendAP(best.getTerrain().getMovementCost());
        from.removeUnit(bear);
        best.addUnit(bear);
        bear.setPosition(best);
        return true;
    }

    // --------------------------------------------------------------- کمکی‌ها
    private void damageUnits(HexCell cell, int damage, boolean clearAp, DisasterEvent event) {
        for (Unit u : new ArrayList<>(cell.getUnits())) {
            if (!u.isAlive()) continue;
            u.takeDamage(damage);
            if (clearAp) u.clearAP();
            if (!u.isAlive()) {
                event.addEffect(u.getType().getLabel() + " از بین رفت");
            } else {
                event.addEffect(u.getType().getLabel() + " " + damage + " HP آسیب دید ("
                        + u.getHp() + "/" + u.getMaxHp() + ")");
            }
        }
    }

    /** هکس‌های خشکی که می‌توانند مرکز زلزله باشند. */
    public List<HexCell> landCells(GameState state) {
        List<HexCell> list = new ArrayList<>();
        if (state.getMap() == null) return list;
        for (HexCell cell : state.getMap().getAllCells()) {
            if (cell.getTerrain().isLand()) list.add(cell);
        }
        return list;
    }

    private List<Unit> playerUnitsNear(GameState state, HexCell center) {
        List<Unit> list = new ArrayList<>();
        if (center == null) return list;
        for (Unit u : state.getUnits()) {
            if (!u.isAlive() || u.getPosition() == null) continue;
            if (state.getMap().distance(center, u.getPosition()) <= GameConfig.BEAR_TARGET_RADIUS) {
                list.add(u);
            }
        }
        return list;
    }

    private HexCell freeLandNeighbor(GameState state, HexCell center) {
        for (HexCell n : state.getMap().getNeighbors(center)) {
            if (n.getTerrain().isLandPassable() && !n.isBlocked()) return n;
        }
        return null;
    }

    /** رویداد داخل دید بازیکن انیمیشن دارد، بیرون از دید فقط هشدار متنی. */
    private void markVisibility(GameState state, DisasterEvent event) {
        for (HexCell cell : event.getAffected()) {
            if (state.getFog().isVisible(cell.getQ(), cell.getR())) {
                event.setVisible(true);
                return;
            }
        }
    }

    /** برای HUD: توضیح اثرهای فصل جاری. */
    public String seasonSummary(Season season) {
        return season.getLabel() + " — " + season.getEffectText();
    }
}
