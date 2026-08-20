package org.strategygame.model;

import org.strategygame.config.GameConfig;
import org.strategygame.model.building.*;
import org.strategygame.model.disaster.DisasterEvent;
import org.strategygame.model.event.NotificationCenter;
import org.strategygame.model.event.NotificationKind;
import org.strategygame.model.happiness.HappinessState;
import org.strategygame.model.map.*;
import org.strategygame.model.resource.*;
import org.strategygame.model.season.Season;
import org.strategygame.model.tech.Technology;
import org.strategygame.model.tribe.Tribe;
import org.strategygame.model.unit.*;
import org.strategygame.model.upgrade.UpgradeType;
import org.strategygame.service.RandomService;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * ریشه‌ی دامنه‌ی بازی. تمام وضعیت بازی اینجا نگه داشته می‌شود و هیچ ارجاعی
 * به UI ندارد؛ سرویس‌ها منطق نوبت، نبرد، تجارت و رویدادها را روی همین شیء
 * اجرا می‌کنند.
 */
public class GameState {

    public static final int MAP_W = 20;
    public static final int MAP_H = 20;
    public static final int FOOD_PER_UNIT = 1;

    private final RandomService random;
    private String gameId = UUID.randomUUID().toString();

    private int turn = 1;
    private final ResourceStorage storage = new ResourceStorage();
    private HexMap   map;
    private FogOfWar fog;
    private TownHall townHall;

    private final List<Unit>     units     = new ArrayList<>();
    private final List<Building> buildings = new ArrayList<>();
    private final List<Tribe>    tribes    = new ArrayList<>();
    private final List<Bear>     bears     = new ArrayList<>();

    private final Set<UpgradeType> doneUpgrades = EnumSet.noneOf(UpgradeType.class);
    private final Set<Technology>  technologies = EnumSet.noneOf(Technology.class);

    private final HappinessState     happiness     = new HappinessState();
    private final NotificationCenter notifications = new NotificationCenter();

    private int     unitCap            = 10;
    private boolean settlementUnlocked = false;
    private boolean starvation         = false;
    private boolean dockDiscount       = false;

    private Season        season       = Season.SPRING;
    private DisasterEvent lastDisaster = null;

    /** آخرین نوبتی که در هر لانه‌ی جنگلی حمله‌ی خرس رخ داده است. */
    private final Map<String, Integer> bearDenCooldown = new HashMap<>();

    public GameState() { this(System.currentTimeMillis()); }

    public GameState(long seed) { this.random = new RandomService(seed); }

    // ------------------------------------------------------------ راه‌اندازی
    public void initialize() {
        int cx = MAP_W / 2, cy = MAP_H / 2;

        map = new MapGenerator(MAP_W, MAP_H).generate(random, cx, cy);
        fog = new FogOfWar(map);

        HexCell center = map.getCell(cx, cy);
        center.setInBorder(true);
        for (HexCell n : map.getNeighbors(center)) {
            if (n.getTerrain().isLandPassable()) n.setInBorder(true);
        }

        townHall = new TownHall();
        townHall.setLocation(center);
        center.setBuilding(townHall);
        buildings.add(townHall);
        storage.setLevelCapacity(townHall.getStorageCapacity());

        List<HexCell> spots = walkableNeighbors(center);
        spawn(new Explorer(), center);
        spawn(new Builder(),  spotAt(spots, 0, center));
        spawn(new Builder(),  spotAt(spots, 1, center));
        spawn(new Worker(),   spotAt(spots, 2, center));
        spawn(new Worker(),   spotAt(spots, 3, center));

        placeNeutralTradingPost(center);
        updateSeason();
        fog.update(units, buildings);
    }

    private List<HexCell> walkableNeighbors(HexCell cell) {
        List<HexCell> list = new ArrayList<>();
        for (HexCell n : map.getNeighbors(cell)) {
            if (n.getTerrain().isLandPassable() && !n.hasBuilding()) list.add(n);
        }
        return list;
    }

    private HexCell spotAt(List<HexCell> list, int index, HexCell fallback) {
        return index < list.size() ? list.get(index) : fallback;
    }

    /** پاسگاه تجاری بی‌طرف از ابتدای بازی روی نقشه است. */
    private void placeNeutralTradingPost(HexCell townHallCell) {
        List<HexCell> candidates = new ArrayList<>();
        for (HexCell c : map.getAllCells()) {
            int d = map.distance(townHallCell, c);
            if (d < 3 || d > 6) continue;
            if (!c.getTerrain().isLandPassable() || c.hasBuilding() || c.hasResource()) continue;
            candidates.add(c);
        }
        HexCell chosen = random.pick(candidates);
        if (chosen == null) return;

        Building post = BuildingFactory.create(BuildingType.TRADING_POST);
        post.setLocation(chosen);
        chosen.setBuilding(post);
        buildings.add(post);
    }

    // ------------------------------------------------------------ دسترسی پایه
    public RandomService getRandom()             { return random; }
    public String getGameId()                    { return gameId; }
    public int getTurn()                         { return turn; }

    /** اتصال نقشهٔ ذخیره‌شده بدون اجرای مجدد تولیدکنندهٔ نقشه. */
    public void attachWorld(HexMap map) {
        this.map = map;
        this.fog = new FogOfWar(map);
    }

    public void setTownHall(TownHall hall) { this.townHall = hall; }

    public void restoreMeta(String gameId, int turn, Season season, int unitCap,
                            boolean settlementUnlocked, boolean starvation, boolean dockDiscount) {
        if (gameId != null && !gameId.isBlank()) this.gameId = gameId;
        this.turn = Math.max(1, turn);
        this.season = season != null ? season : Season.forTurn(this.turn);
        this.unitCap = Math.max(1, unitCap);
        this.settlementUnlocked = settlementUnlocked;
        this.starvation = starvation;
        this.dockDiscount = dockDiscount;
    }

    public Map<String, Integer> getBearDenCooldown() { return bearDenCooldown; }

    public void restoreBearDenCooldown(Map<String, Integer> saved) {
        bearDenCooldown.clear();
        if (saved != null) bearDenCooldown.putAll(saved);
    }

    /** قرار دادن یونیت ذخیره‌شده روی هکس بدون منطق اسپاون جدید. */
    public void restorePlaceUnit(Unit unit, HexCell cell) {
        if (unit == null || cell == null) return;
        unit.setPosition(cell);
        cell.addUnit(unit);
        if (unit.getOwner() == Owner.PLAYER) units.add(unit);
        else if (unit instanceof Bear bear)  bears.add(bear);
    }
    public void nextTurn()                       { turn++; updateSeason(); }
    public ResourceStorage getStorage()          { return storage; }
    public HexMap getMap()                       { return map; }
    public FogOfWar getFog()                     { return fog; }
    public TownHall getTownHall()                { return townHall; }
    public List<Unit> getUnits()                 { return units; }
    public List<Building> getBuildings()         { return buildings; }
    public List<Tribe> getTribes()               { return tribes; }
    public List<Bear> getBears()                 { return bears; }
    public Set<UpgradeType> getDoneUpgrades()    { return doneUpgrades; }
    public Set<Technology> getTechnologies()     { return technologies; }
    public HappinessState getHappiness()         { return happiness; }
    public NotificationCenter getNotifications() { return notifications; }
    public Season getSeason()                    { return season; }
    public DisasterEvent getLastDisaster()       { return lastDisaster; }
    public boolean isSettlementUnlocked()        { return settlementUnlocked; }
    public boolean isStarvation()                { return starvation; }
    public int getUnitCap()                      { return unitCap; }

    public void setStarvation(boolean v)          { this.starvation = v; }
    public void setSettlementUnlocked(boolean v)  { this.settlementUnlocked = v; }
    public void setLastDisaster(DisasterEvent e)  { this.lastDisaster = e; }
    public void addUnitCap(int bonus)             { this.unitCap += bonus; }

    public void notify(NotificationKind kind, String message) {
        notifications.add(turn, kind, message);
    }

    private void updateSeason() { this.season = Season.forTurn(turn); }

    /** برای تست و دیباگ: پریدن به یک فصل مشخص. */
    public void forceSeason(Season target) {
        int offset = target.ordinal() * GameConfig.SEASON_LENGTH;
        this.turn   = offset + 1;
        this.season = target;
    }

    // ----------------------------------------------------------- یونیت‌ها
    public void spawn(Unit u, HexCell cell) {
        if (u == null || cell == null) return;
        u.setPosition(cell);
        cell.addUnit(u);
        if (u.getOwner() == Owner.PLAYER)   units.add(u);
        else if (u instanceof Bear bear)    bears.add(bear);
    }

    public void removeUnit(Unit u) {
        if (u == null) return;
        units.remove(u);
        if (u instanceof Bear bear) bears.remove(bear);
        for (Tribe t : tribes) {
            if (u instanceof MilitaryUnit m) t.removeUnit(m);
        }
        if (u.getPosition() != null) u.getPosition().removeUnit(u);
        u.setAlive(false);
    }

    /** حذف همه‌ی یونیت‌های مرده از نقشه و لیست‌ها. */
    public void purgeDeadUnits() {
        for (Unit u : new ArrayList<>(units))  if (!u.isAlive()) removeUnit(u);
        for (Bear b : new ArrayList<>(bears))  if (!b.isAlive()) removeUnit(b);
        for (Tribe t : tribes) {
            for (MilitaryUnit m : new ArrayList<>(t.getUnits())) {
                if (!m.isAlive()) removeUnit(m);
            }
        }
    }

    public List<Unit> idleUnitsWithAP() {
        List<Unit> list = new ArrayList<>();
        for (Unit u : units) {
            boolean stationed = (u instanceof Worker w) && w.isStationed();
            if (u.getCurrentAP() > 0 && !stationed) list.add(u);
        }
        return list;
    }

    public boolean canSpawnUnit() { return units.size() < unitCap; }

    public int getMilitaryCap() {
        return townHall == null ? 0 : townHall.getMilitaryCap();
    }

    public int militaryUnitCount() {
        int count = 0;
        for (Unit u : units) if (u.isMilitary()) count++;
        return count;
    }

    public boolean canSpawnMilitaryUnit() {
        return canSpawnUnit() && militaryUnitCount() < getMilitaryCap();
    }

    public List<MilitaryUnit> playerMilitaryUnits() {
        List<MilitaryUnit> list = new ArrayList<>();
        for (Unit u : units) if (u instanceof MilitaryUnit m) list.add(m);
        return list;
    }

    /** تعداد یونیت‌های یک نوع خاص روی یک هکس؛ برای ظرفیت نظامی هر هکس. */
    public int countTypeAt(HexCell cell, UnitType type) {
        int count = 0;
        for (Unit u : cell.getUnits()) {
            if (u.isAlive() && u.getType() == type) count++;
        }
        return count;
    }

    /** آیا با ظرفیت نظامی هکس، جای این نوع یونیت هست. */
    public boolean hexHasRoomFor(HexCell cell, UnitType type) {
        if (cell == null) return false;
        return countTypeAt(cell, type) < MilitaryUnit.hexCapacity(type);
    }

    // ----------------------------------------------------------- ساختمان‌ها
    public void addBuilding(Building b) { buildings.add(b); }

    /** نابودی کامل یک سازه: کارگرها آزاد و هکس خالی می‌شود. */
    public void destroyBuilding(Building b) {
        if (b == null) return;
        b.releaseAllWorkers();
        if (b.getLocation() != null && b.getLocation().getBuilding() == b) {
            b.getLocation().setBuilding(null);
        }
        buildings.remove(b);
        if (b instanceof TownHall) townHall = null;
    }

    public boolean hasActiveStable() {
        for (Building b : buildings) {
            if (b.getType() == BuildingType.STABLE && b.isFunctional()) return true;
        }
        return false;
    }

    public Bazaar findBazaar() {
        for (Building b : buildings) if (b instanceof Bazaar bazaar) return bazaar;
        return null;
    }

    /** پاسگاه تجاری بی‌طرفی که هکس آن داخل قلمرو بازیکن است. */
    public Building findUsableTradingPost() {
        for (Building b : buildings) {
            if (b.getType() != BuildingType.TRADING_POST) continue;
            if (b.getLocation() != null && b.getLocation().isInBorder()) return b;
        }
        return null;
    }

    public Building findTradingPost() {
        for (Building b : buildings) {
            if (b.getType() == BuildingType.TRADING_POST) return b;
        }
        return null;
    }

    public List<Building> buildingsOfType(BuildingType type) {
        List<Building> list = new ArrayList<>();
        for (Building b : buildings) if (b.getType() == type) list.add(b);
        return list;
    }

    // -------------------------------------------------- اثر دستورهای تان هال
    /** ضریب تولید معادن سنگ و آهن. */
    public double getMiningMultiplier() {
        double m = 1.0;
        if (technologies.contains(Technology.STEEL_TOOLS))
            m = Math.max(m, GameConfig.STEEL_TOOLS_MINING_MULTIPLIER);
        if (doneUpgrades.contains(UpgradeType.PROFESSIONAL_TOOLS))
            m = Math.max(m, GameConfig.STEEL_TOOLS_MINING_MULTIPLIER);
        return m;
    }

    public boolean hasTechnology(Technology t) { return technologies.contains(t); }

    public boolean isSailingUnlocked() { return hasTechnology(Technology.SAILING); }

    public void applyTechnology(Technology t) {
        if (!technologies.add(t)) return;
        if (t == Technology.DEFENSIVE_ARCHITECTURE && townHall != null) {
            townHall.buildDefensiveWall();
        }
        notify(NotificationKind.PRODUCTION, t.getLabel() + " تحقیق شد — " + t.getEffectText());
    }

    public void applyUpgrade(UpgradeType type) {
        if (!doneUpgrades.add(type)) return;
        switch (type) {
            case STORAGE_UPGRADE_1, STORAGE_UPGRADE_2 -> storage.upgradeCapacity();
            case SETTLEMENT_TECH -> setSettlementUnlocked(true);
            default -> { }
        }
        notify(NotificationKind.PRODUCTION, type.getLabel() + " انجام شد");
    }

    public void applyTownHallUpgrade() {
        if (townHall == null) return;
        TownHallLevel next = townHall.levelUp();
        if (next == null) return;

        storage.setLevelCapacity(townHall.getStorageCapacity());
        Bazaar bazaar = findBazaar();
        if (bazaar != null) bazaar.setTradeLevel(next.getNumber());

        notify(NotificationKind.PRODUCTION,
                "تان هال به " + next.getLabel() + " ارتقا یافت — " + next.bonusText());
    }

    /** ساخت یونیت تولیدشده در تان هال روی نزدیک‌ترین هکس مجاز. */
    public void spawnTrainedUnit(UnitType type) {
        Unit unit = UnitFactory.create(type);
        if (unit == null || townHall == null) return;

        HexCell spot = findSpawnSpot(townHall.getLocation(), type);
        if (spot == null) {
            notify(NotificationKind.PRODUCTION,
                    "جایی برای استقرار " + type.getLabel() + " کنار تان هال نبود");
            return;
        }
        spawn(unit, spot);
        notify(NotificationKind.PRODUCTION, type.getLabel() + " آماده شد");
    }

    private HexCell findSpawnSpot(HexCell home, UnitType type) {
        if (home == null) return null;
        if (hexHasRoomFor(home, type)) return home;
        for (HexCell n : map.getNeighbors(home)) {
            if (!n.getTerrain().isLandPassable()) continue;
            if (hexHasRoomFor(n, type)) return n;
        }
        return null;
    }

    /** جایزه‌ی مأموریت قبیله‌ی جنگاور: چند شمشیرزن کمکی. */
    public void spawnAlliedSwordsmen(int count, HexCell near) {
        HexCell base = near != null ? near : (townHall != null ? townHall.getLocation() : null);
        if (base == null) return;

        int spawned = 0;
        List<HexCell> options = new ArrayList<>();
        options.add(base);
        options.addAll(map.getNeighbors(base));

        for (HexCell cell : options) {
            if (spawned >= count) break;
            if (!cell.getTerrain().isLandPassable()) continue;
            while (spawned < count && hexHasRoomFor(cell, UnitType.SWORDSMAN)) {
                Swordsman s = new Swordsman();
                spawn(s, cell);
                spawned++;
            }
        }
        if (spawned > 0) {
            notify(NotificationKind.MISSION, spawned + " شمشیرزن کمکی به شما پیوستند");
        }
    }

    public boolean isDockDiscountAvailable()          { return dockDiscount; }
    public void setDockDiscountAvailable(boolean v)    { this.dockDiscount = v; }

    // ------------------------------------------------------------ قبیله‌ها
    public void addTribe(Tribe tribe) { tribes.add(tribe); }

    /** قبیله‌ای که اردوگاهش روی این هکس است. */
    public Tribe tribeAt(HexCell cell) {
        if (cell == null) return null;
        for (Tribe t : tribes) {
            if (!t.isDefeated() && cell.equals(t.getCampHex())) return t;
        }
        return null;
    }

    public Tribe tribeOwning(MilitaryUnit unit) {
        for (Tribe t : tribes) {
            if (t.getUnits().contains(unit)) return t;
        }
        return null;
    }

    /**
     * ثبت شکست یک دشمن؛ مأموریت شکار قبیله‌های نزدیک را جلو می‌برد.
     */
    public void notifyEnemyDefeated(HexCell where) {
        if (where == null) return;
        for (Tribe t : tribes) {
            if (t.isDefeated() || t.getCampHex() == null) continue;
            if (map.distance(where, t.getCampHex()) <= GameConfig.TRIBE_AGGRESSION_RADIUS) {
                t.addNearbyEnemyKill();
            }
        }
    }

    // -------------------------------------------------------- خرس و لانه‌ها
    public boolean isBearDenOnCooldown(HexCell den) {
        Integer last = bearDenCooldown.get(denKey(den));
        return last != null && turn - last < GameConfig.BEAR_COOLDOWN;
    }

    public void markBearDenUsed(HexCell den) {
        bearDenCooldown.put(denKey(den), turn);
    }

    private String denKey(HexCell den) { return den.getQ() + ":" + den.getR(); }

    // ------------------------------------------------------ نرخ برای نمایش
    /** نرخ خالص منابع برای HUD؛ محاسبه در سرویس تولید انجام می‌شود. */
    public ResourceRate calcRate() {
        return new org.strategygame.service.ProductionService().calculate(this);
    }
}
