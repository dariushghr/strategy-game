package org.strategygame.save;

import org.strategygame.model.GameState;
import org.strategygame.model.building.Bazaar;
import org.strategygame.model.building.Building;
import org.strategygame.model.building.BuildingFactory;
import org.strategygame.model.building.BuildingType;
import org.strategygame.model.building.TownHall;
import org.strategygame.model.building.TownHallLevel;
import org.strategygame.model.command.ResearchTechnologyCommand;
import org.strategygame.model.command.ResearchUpgradeCommand;
import org.strategygame.model.command.TownHallCommand;
import org.strategygame.model.command.TrainUnitCommand;
import org.strategygame.model.command.UpgradeTownHallCommand;
import org.strategygame.model.disaster.DisasterEvent;
import org.strategygame.model.disaster.DisasterType;
import org.strategygame.model.event.Notification;
import org.strategygame.model.event.NotificationKind;
import org.strategygame.model.map.FogOfWar;
import org.strategygame.model.map.HexCell;
import org.strategygame.model.map.HexEdge;
import org.strategygame.model.map.HexMap;
import org.strategygame.model.map.ResourceDeposit;
import org.strategygame.model.map.TerrainType;
import org.strategygame.model.map.Wall;
import org.strategygame.model.resource.ResourceType;
import org.strategygame.model.season.Season;
import org.strategygame.model.tech.Technology;
import org.strategygame.model.tribe.RelationState;
import org.strategygame.model.tribe.Tribe;
import org.strategygame.model.tribe.TribeType;
import org.strategygame.model.tribe.mission.MissionFactory;
import org.strategygame.model.tribe.mission.MissionState;
import org.strategygame.model.tribe.mission.TribeMission;
import org.strategygame.model.unit.Bear;
import org.strategygame.model.unit.Explorer;
import org.strategygame.model.unit.MilitaryUnit;
import org.strategygame.model.unit.Owner;
import org.strategygame.model.unit.Unit;
import org.strategygame.model.unit.UnitFactory;
import org.strategygame.model.unit.UnitType;
import org.strategygame.model.unit.Worker;
import org.strategygame.model.upgrade.UpgradeType;
import org.strategygame.save.json.Json;
import org.strategygame.save.json.Json.Arr;
import org.strategygame.save.json.Json.Obj;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * تبدیل {@link GameState} به JSON نسخه‌دار و برعکس. UI ذخیره نمی‌شود.
 * بارگذاری نقشه را دوباره تولید نمی‌کند و RNG را فقط با seed و callCount برمی‌گرداند.
 */
public final class SaveCodec {

    public static final int VERSION = 1;

    private SaveCodec() { }

    public static String encode(GameState state) {
        Obj root = new Obj()
                .put("version", VERSION)
                .put("gameId", state.getGameId())
                .put("savedAt", System.currentTimeMillis())
                .put("turn", state.getTurn())
                .put("season", name(state.getSeason()))
                .put("seed", state.getRandom().getSeed())
                .put("rngCalls", state.getRandom().getCallCount())
                .put("rngState", java.util.Base64.getEncoder().encodeToString(state.getRandom().snapshot()))
                .put("unitCap", state.getUnitCap())
                .put("settlementUnlocked", state.isSettlementUnlocked())
                .put("starvation", state.isStarvation())
                .put("dockDiscount", state.isDockDiscountAvailable())
                .put("storage", encodeStorage(state))
                .put("happiness", encodeHappiness(state))
                .put("technologies", encodeEnumSet(state.getTechnologies()))
                .put("upgrades", encodeEnumSet(state.getDoneUpgrades()))
                .put("map", encodeMap(state))
                .put("buildings", encodeBuildings(state))
                .put("units", encodeUnits(state))
                .put("tribes", encodeTribes(state))
                .put("notifications", encodeNotifications(state))
                .put("lastDisaster", encodeDisaster(state.getLastDisaster()))
                .put("bearDens", encodeBearDens(state));
        return Json.stringify(root);
    }

    public static GameState decode(String json) throws SaveException {
        Obj root;
        try {
            root = Json.parseObject(json);
        } catch (RuntimeException e) {
            throw new SaveException(SaveException.Kind.CORRUPT, "فایل ذخیره خراب است", e);
        }

        int version = root.i("version", -1);
        if (version != VERSION) {
            throw new SaveException(SaveException.Kind.VERSION,
                    "نسخه ذخیره " + version + " پشتیبانی نمی‌شود (نسخه فعلی: " + VERSION + ")");
        }

        try {
            return rebuild(root);
        } catch (RuntimeException e) {
            throw new SaveException(SaveException.Kind.CORRUPT, "فایل ذخیره ناقص یا ناسازگار است", e);
        }
    }

    private static GameState rebuild(Obj root) {
        long seed = root.lng("seed");
        GameState state = new GameState(seed);
        String rngState = root.str("rngState");
        if (rngState != null && !rngState.isBlank()) {
            state.getRandom().restoreSnapshot(
                    java.util.Base64.getDecoder().decode(rngState),
                    root.lng("rngCalls", 0));
        } else {
            state.getRandom().restoreTo(root.lng("rngCalls", 0));
        }

        HexMap map = decodeMap(root.obj("map"));
        state.attachWorld(map);
        decodeFog(state.getFog(), root.obj("map"));

        state.restoreMeta(
                root.str("gameId"),
                root.i("turn", 1),
                enumOr(Season.class, root.str("season"), Season.forTurn(root.i("turn", 1))),
                root.i("unitCap", 10),
                root.bool("settlementUnlocked"),
                root.bool("starvation"),
                root.bool("dockDiscount"));

        decodeStorage(state, root.obj("storage"));
        decodeHappiness(state, root.obj("happiness"));
        decodeEnumSet(state.getTechnologies(), Technology.class, root.arr("technologies"));
        decodeEnumSet(state.getDoneUpgrades(), UpgradeType.class, root.arr("upgrades"));

        Map<String, Building> buildingsById = decodeBuildings(state, root.arr("buildings"));
        decodeUnits(state, root.arr("units"), buildingsById);
        decodeTribes(state, root.arr("tribes"));
        decodeNotifications(state, root.arr("notifications"));
        state.setLastDisaster(decodeDisaster(state, root.obj("lastDisaster")));
        decodeBearDens(state, root.arr("bearDens"));

        state.getFog().update(state.getUnits(), state.getBuildings());
        return state;
    }

    // -------------------------------------------------------------- انبار و شادی
    private static Obj encodeStorage(GameState state) {
        Arr amounts = new Arr();
        for (ResourceType t : ResourceType.values()) amounts.add(state.getStorage().get(t));
        return new Obj()
                .put("levelCapacity", state.getStorage().getLevelCapacity())
                .put("bonusCapacity", state.getStorage().getBonusCapacity())
                .put("amounts", amounts);
    }

    private static void decodeStorage(GameState state, Obj obj) {
        if (obj == null) return;
        Arr amounts = obj.arr("amounts");
        int[] values = new int[ResourceType.values().length];
        for (int i = 0; i < values.length && i < amounts.size(); i++) values[i] = amounts.i(i);
        state.getStorage().restore(obj.i("levelCapacity"), obj.i("bonusCapacity"), values);
    }

    private static Obj encodeHappiness(GameState state) {
        Arr fired = new Arr();
        for (String k : state.getHappiness().getFiredEvents()) fired.add(k);
        Arr history = new Arr();
        for (String h : state.getHappiness().getHistory()) history.add(h);
        return new Obj()
                .put("value", state.getHappiness().getValue())
                .put("fired", fired)
                .put("history", history);
    }

    private static void decodeHappiness(GameState state, Obj obj) {
        if (obj == null) return;
        Set<String> fired = new HashSet<>();
        Arr firedArr = obj.arr("fired");
        for (int i = 0; i < firedArr.size(); i++) fired.add(firedArr.str(i));
        java.util.List<String> history = new java.util.ArrayList<>();
        Arr histArr = obj.arr("history");
        for (int i = 0; i < histArr.size(); i++) history.add(histArr.str(i));
        state.getHappiness().restore(obj.i("value"), fired, history);
    }

    private static Arr encodeEnumSet(Iterable<? extends Enum<?>> values) {
        Arr arr = new Arr();
        for (Enum<?> v : values) arr.add(v.name());
        return arr;
    }

    private static <E extends Enum<E>> void decodeEnumSet(Set<E> target, Class<E> type, Arr arr) {
        target.clear();
        for (int i = 0; i < arr.size(); i++) {
            E v = enumOr(type, arr.str(i), null);
            if (v != null) target.add(v);
        }
    }

    // ---------------------------------------------------------------- نقشه
    private static Obj encodeMap(GameState state) {
        HexMap map = state.getMap();
        FogOfWar fog = state.getFog();
        Arr cells = new Arr();
        for (HexCell cell : map.getAllCells()) {
            Obj o = new Obj()
                    .put("q", cell.getQ())
                    .put("r", cell.getR())
                    .put("terrain", name(cell.getTerrain()))
                    .put("road", cell.hasRoad())
                    .put("blocked", cell.isBlocked())
                    .put("border", cell.isInBorder())
                    .put("explored", fog.isExplored(cell.getQ(), cell.getR()));
            if (cell.getDeposit() != null) {
                o.put("depositType", name(cell.getDeposit().getType()))
                 .put("depositMax", cell.getDeposit().getMax())
                 .put("depositLeft", cell.getDeposit().getRemaining());
            }
            cells.add(o);
        }

        Arr edges = new Arr();
        for (HexEdge edge : map.getAllEdges()) {
            Obj o = new Obj()
                    .put("q1", edge.getQ1()).put("r1", edge.getR1())
                    .put("q2", edge.getQ2()).put("r2", edge.getR2())
                    .put("river", edge.hasRiver());
            if (edge.getWall() != null) {
                o.put("wallHp", edge.getWall().getHp());
            }
            edges.add(o);
        }

        return new Obj()
                .put("width", map.getWidth())
                .put("height", map.getHeight())
                .put("cells", cells)
                .put("edges", edges);
    }

    private static HexMap decodeMap(Obj obj) {
        if (obj == null) throw new IllegalArgumentException("نقشه در ذخیره نیست");
        int w = obj.i("width", GameState.MAP_W);
        int h = obj.i("height", GameState.MAP_H);
        HexMap map = new HexMap(w, h);
        Arr cells = obj.arr("cells");
        for (int i = 0; i < cells.size(); i++) {
            Obj c = cells.obj(i);
            if (c == null) continue;
            int q = c.i("q"), r = c.i("r");
            TerrainType terrain = enumOr(TerrainType.class, c.str("terrain"), TerrainType.PLAINS);
            HexCell cell = new HexCell(q, r, terrain);
            cell.setRoad(c.bool("road"));
            cell.setBlocked(c.bool("blocked"));
            cell.setInBorder(c.bool("border"));
            if (c.has("depositType")) {
                ResourceType type = enumOr(ResourceType.class, c.str("depositType"), null);
                if (type != null) {
                    ResourceDeposit deposit = new ResourceDeposit(type, c.i("depositMax", 0));
                    deposit.restoreRemaining(c.i("depositLeft", 0));
                    cell.setDeposit(deposit);
                }
            }
            map.setCell(q, r, cell);
        }

        Arr edges = obj.arr("edges");
        for (int i = 0; i < edges.size(); i++) {
            Obj e = edges.obj(i);
            if (e == null) continue;
            HexCell a = map.getCell(e.i("q1"), e.i("r1"));
            HexCell b = map.getCell(e.i("q2"), e.i("r2"));
            HexEdge edge = map.edgeOrCreate(a, b);
            if (edge == null) continue;
            edge.setRiver(e.bool("river"));
            if (e.has("wallHp")) edge.setWall(new Wall(e.i("wallHp")));
        }
        return map;
    }

    private static void decodeFog(FogOfWar fog, Obj mapObj) {
        if (fog == null || mapObj == null) return;
        Arr cells = mapObj.arr("cells");
        for (int i = 0; i < cells.size(); i++) {
            Obj c = cells.obj(i);
            if (c == null) continue;
            fog.restoreExplored(c.i("q"), c.i("r"), c.bool("explored"));
        }
    }

    // ------------------------------------------------------------- ساختمان‌ها
    private static Arr encodeBuildings(GameState state) {
        Arr arr = new Arr();
        for (Building b : state.getBuildings()) {
            Obj o = new Obj()
                    .put("id", b.getId())
                    .put("type", name(b.getType()))
                    .put("q", b.getLocation() == null ? -1 : b.getLocation().getQ())
                    .put("r", b.getLocation() == null ? -1 : b.getLocation().getR())
                    .put("hp", b.getHp())
                    .put("functional", !b.isBroken())
                    .put("missedUpkeep", b.getMissedUpkeep())
                    .put("disabledTurns", b.getDisabledTurnsLeft())
                    .put("lastTradeTurn", b.getLastTradeTurn())
                    .put("defense", b.getDefense());
            if (b instanceof Bazaar bazaar) o.put("bazaarLevel", bazaar.getTradeLevel());
            if (b instanceof TownHall hall) {
                o.put("townHallLevel", hall.getLevelData().name());
                o.put("defensiveWall", hall.hasDefensiveWall());
                o.put("queue", encodeQueue(hall.getQueue().getActive()));
            }
            arr.add(o);
        }
        return arr;
    }

    private static Map<String, Building> decodeBuildings(GameState state, Arr arr) {
        Map<String, Building> byId = new HashMap<>();
        for (int i = 0; i < arr.size(); i++) {
            Obj o = arr.obj(i);
            if (o == null) continue;
            BuildingType type = enumOr(BuildingType.class, o.str("type"), null);
            if (type == null) continue;
            Building building = BuildingFactory.create(type);
            HexCell cell = state.getMap().getCell(o.i("q"), o.i("r"));
            if (building instanceof TownHall hall) {
                hall.restoreLevel(enumOr(TownHallLevel.class, o.str("townHallLevel"), TownHallLevel.LEVEL_1));
                hall.restoreDefensiveWall(o.bool("defensiveWall"));
                Obj queue = o.obj("queue");
                if (queue != null) hall.getQueue().restoreActive(decodeCommand(queue));
                state.setTownHall(hall);
            }
            if (building instanceof Bazaar bazaar) {
                bazaar.setTradeLevel(o.i("bazaarLevel", 1));
            }
            building.restoreSaved(
                    o.str("id"),
                    o.i("hp"),
                    o.bool("functional", true),
                    o.i("missedUpkeep"),
                    o.i("disabledTurns"),
                    o.i("lastTradeTurn", -1),
                    o.i("defense"));
            if (cell != null) {
                building.setLocation(cell);
                cell.setBuilding(building);
            }
            state.addBuilding(building);
            byId.put(building.getId(), building);
        }
        return byId;
    }

    private static Obj encodeQueue(TownHallCommand cmd) {
        if (cmd == null) return null;
        Obj o = new Obj().put("turnsLeft", cmd.getTurnsLeft());
        if (cmd instanceof TrainUnitCommand t) {
            o.put("kind", "TRAIN").put("payload", t.getUnitType().name());
        } else if (cmd instanceof ResearchTechnologyCommand t) {
            o.put("kind", "TECH").put("payload", t.getTechnology().name());
        } else if (cmd instanceof ResearchUpgradeCommand t) {
            o.put("kind", "UPGRADE").put("payload", t.getUpgrade().name());
        } else if (cmd instanceof UpgradeTownHallCommand t) {
            o.put("kind", "TOWN_HALL").put("payload", t.getTarget().name());
        } else {
            return null;
        }
        return o;
    }

    private static TownHallCommand decodeCommand(Obj o) {
        String kind = o.str("kind", "");
        String payload = o.str("payload");
        TownHallCommand cmd = switch (kind) {
            case "TRAIN" -> new TrainUnitCommand(UnitType.valueOf(payload));
            case "TECH" -> new ResearchTechnologyCommand(Technology.valueOf(payload));
            case "UPGRADE" -> new ResearchUpgradeCommand(UpgradeType.valueOf(payload));
            case "TOWN_HALL" -> new UpgradeTownHallCommand(TownHallLevel.valueOf(payload));
            default -> null;
        };
        if (cmd != null) cmd.restoreTurnsLeft(o.i("turnsLeft", cmd.getTurnsLeft()));
        return cmd;
    }

    // --------------------------------------------------------------- یونیت‌ها
    private static Arr encodeUnits(GameState state) {
        Arr arr = new Arr();
        for (Unit u : state.getUnits()) arr.add(encodeUnit(u, null));
        for (Bear b : state.getBears()) arr.add(encodeUnit(b, null));
        return arr;
    }

    private static Obj encodeUnit(Unit u, String tribeId) {
        Obj o = new Obj()
                .put("id", u.getId())
                .put("type", name(u.getType()))
                .put("owner", name(u.getOwner()))
                .put("q", u.getPosition() == null ? -1 : u.getPosition().getQ())
                .put("r", u.getPosition() == null ? -1 : u.getPosition().getR())
                .put("hp", u.getHp())
                .put("ap", u.getCurrentAP())
                .put("alive", u.isAlive())
                .put("apPenalty", u.getApPenalty());
        if (tribeId != null) o.put("tribeId", tribeId);
        if (u instanceof Worker w && w.getStationedAt() != null) {
            o.put("stationId", w.getStationedAt().getId());
        }
        if (u instanceof Explorer ex) o.put("autoExplore", ex.isAutoExplore());
        if (u instanceof Bear bear) {
            o.put("denQ", bear.getDen() == null ? -1 : bear.getDen().getQ());
            o.put("denR", bear.getDen() == null ? -1 : bear.getDen().getR());
            o.put("attacked", bear.hasAttackedThisTurn());
        }
        return o;
    }

    private static void decodeUnits(GameState state, Arr arr, Map<String, Building> buildings) {
        for (int i = 0; i < arr.size(); i++) {
            Obj o = arr.obj(i);
            if (o == null) continue;
            Unit unit = createUnit(state, o);
            if (unit == null) continue;
            unit.restoreSaved(
                    o.str("id"),
                    o.i("hp"),
                    o.i("ap"),
                    o.bool("alive", true),
                    enumOr(Owner.class, o.str("owner"), Owner.PLAYER),
                    o.i("apPenalty"));
            if (unit instanceof Explorer ex) ex.setAutoExplore(o.bool("autoExplore"));
            if (unit instanceof Bear bear) bear.restoreAttackedThisTurn(o.bool("attacked"));

            HexCell cell = state.getMap().getCell(o.i("q"), o.i("r"));
            if (cell != null) state.restorePlaceUnit(unit, cell);

            if (unit instanceof Worker w && o.has("stationId")) {
                Building b = buildings.get(o.str("stationId"));
                if (b != null) w.restoreStation(b);
            }
        }
    }

    private static Unit createUnit(GameState state, Obj o) {
        UnitType type = enumOr(UnitType.class, o.str("type"), null);
        if (type == UnitType.BEAR) {
            HexCell den = state.getMap().getCell(o.i("denQ", -1), o.i("denR", -1));
            return new Bear(den);
        }
        return type == null ? null : UnitFactory.create(type);
    }

    // ---------------------------------------------------------------- قبیله‌ها
    private static Arr encodeTribes(GameState state) {
        Arr arr = new Arr();
        for (Tribe t : state.getTribes()) {
            Obj o = new Obj()
                    .put("id", t.getId())
                    .put("name", t.getName())
                    .put("type", name(t.getType()))
                    .put("q", t.getCampHex() == null ? -1 : t.getCampHex().getQ())
                    .put("r", t.getCampHex() == null ? -1 : t.getCampHex().getR())
                    .put("campHp", t.getCamp().getHp())
                    .put("relation", t.getRelation())
                    .put("relationState", name(t.getRelationState()))
                    .put("discovered", t.isDiscovered())
                    .put("atWar", t.isAtWar())
                    .put("allied", t.isAllied())
                    .put("defeated", t.isDefeated())
                    .put("lastMissionFailTurn", t.getLastMissionFailTurn())
                    .put("lastMissionOfferTurn", t.getLastMissionOfferTurn())
                    .put("lastGuardTurn", t.getLastGuardTurn())
                    .put("lastTradeTurn", t.getLastTradeTurn())
                    .put("nearbyKills", t.getNearbyEnemyKills())
                    .put("tradeBonus", t.getTradeRateBonus())
                    .put("dockDiscount", t.hasDockDiscount())
                    .put("trespassWarned", t.isTrespassWarned())
                    .put("mission", encodeMission(t.getActiveMission(), t));
            Arr units = new Arr();
            for (MilitaryUnit u : t.getUnits()) units.add(encodeUnit(u, t.getId()));
            o.put("units", units);
            arr.add(o);
        }
        return arr;
    }

    private static void decodeTribes(GameState state, Arr arr) {
        for (int i = 0; i < arr.size(); i++) {
            Obj o = arr.obj(i);
            if (o == null) continue;
            HexCell campHex = state.getMap().getCell(o.i("q"), o.i("r"));
            TribeType type = enumOr(TribeType.class, o.str("type"), TribeType.FARMER);
            Tribe tribe = new Tribe(o.str("name", type.getLabel()), type, campHex);
            tribe.restoreId(o.str("id"));
            tribe.getCamp().restoreHp(o.i("campHp", type.getCampHp()));
            tribe.setRelation(o.i("relation"));
            tribe.restoreFlags(o.bool("discovered"), o.bool("atWar"), o.bool("allied"), o.bool("defeated"));
            tribe.restoreTimers(
                    o.i("lastMissionFailTurn", Integer.MIN_VALUE / 2),
                    o.i("lastMissionOfferTurn", Integer.MIN_VALUE / 2),
                    o.i("lastGuardTurn", Integer.MIN_VALUE / 2),
                    o.i("lastTradeTurn", -1),
                    o.i("nearbyKills"),
                    o.dbl("tradeBonus", 0),
                    o.bool("dockDiscount"),
                    o.bool("trespassWarned"));

            Obj mission = o.obj("mission");
            if (mission != null) {
                TribeMission restored = MissionFactory.restore(type, mission.i("huntKills", 0));
                restored.restoreRuntime(
                        enumOr(MissionState.class, mission.str("state"), MissionState.AVAILABLE),
                        mission.i("turnsLeft"),
                        mission.bool("rewardClaimed"));
                tribe.setActiveMission(restored);
            }

            Arr units = o.arr("units");
            for (int u = 0; u < units.size(); u++) {
                Obj uo = units.obj(u);
                if (uo == null) continue;
                Unit created = createUnit(state, uo);
                if (!(created instanceof MilitaryUnit military)) continue;
                military.restoreSaved(
                        uo.str("id"),
                        uo.i("hp"),
                        uo.i("ap"),
                        uo.bool("alive", true),
                        Owner.TRIBE,
                        uo.i("apPenalty"));
                HexCell cell = state.getMap().getCell(uo.i("q"), uo.i("r"));
                if (cell != null) {
                    military.setPosition(cell);
                    cell.addUnit(military);
                }
                tribe.addUnit(military);
            }
            state.addTribe(tribe);
        }
    }

    private static Obj encodeMission(TribeMission mission, Tribe tribe) {
        if (mission == null) return null;
        return new Obj()
                .put("state", name(mission.getState()))
                .put("turnsLeft", mission.getTurnsLeft())
                .put("rewardClaimed", mission.isRewardClaimed())
                .put("huntKills", MissionFactory.huntKillsAtStart(mission));
    }

    // --------------------------------------------------------- اعلان و بلایا
    private static Arr encodeNotifications(GameState state) {
        Arr arr = new Arr();
        for (Notification n : state.getNotifications().getHistory()) {
            arr.add(new Obj()
                    .put("turn", n.turn())
                    .put("kind", name(n.kind()))
                    .put("message", n.message()));
        }
        return arr;
    }

    private static void decodeNotifications(GameState state, Arr arr) {
        java.util.List<Notification> list = new java.util.ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            Obj o = arr.obj(i);
            if (o == null) continue;
            NotificationKind kind = enumOr(NotificationKind.class, o.str("kind"), NotificationKind.GENERAL);
            list.add(new Notification(o.i("turn"), kind, o.str("message", "")));
        }
        state.getNotifications().restoreHistory(list);
    }

    private static Obj encodeDisaster(DisasterEvent event) {
        if (event == null) return null;
        Arr affected = new Arr();
        for (HexCell c : event.getAffected()) {
            affected.add(new Obj().put("q", c.getQ()).put("r", c.getR()));
        }
        Arr effects = new Arr();
        for (String e : event.getEffects()) effects.add(e);
        return new Obj()
                .put("type", name(event.getType()))
                .put("turn", event.getTurn())
                .put("q", event.getCenter() == null ? -1 : event.getCenter().getQ())
                .put("r", event.getCenter() == null ? -1 : event.getCenter().getR())
                .put("visible", event.isVisible())
                .put("affected", affected)
                .put("effects", effects);
    }

    private static DisasterEvent decodeDisaster(GameState state, Obj o) {
        if (o == null) return null;
        DisasterType type = enumOr(DisasterType.class, o.str("type"), null);
        if (type == null) return null;
        HexCell center = state.getMap().getCell(o.i("q", -1), o.i("r", -1));
        DisasterEvent event = new DisasterEvent(type, o.i("turn"), center);
        event.setVisible(o.bool("visible"));
        Arr affected = o.arr("affected");
        for (int i = 0; i < affected.size(); i++) {
            Obj c = affected.obj(i);
            if (c != null) event.addAffected(state.getMap().getCell(c.i("q"), c.i("r")));
        }
        Arr effects = o.arr("effects");
        for (int i = 0; i < effects.size(); i++) event.addEffect(effects.str(i));
        return event;
    }

    private static Arr encodeBearDens(GameState state) {
        Arr arr = new Arr();
        for (var e : state.getBearDenCooldown().entrySet()) {
            arr.add(new Obj().put("key", e.getKey()).put("turn", e.getValue()));
        }
        return arr;
    }

    private static void decodeBearDens(GameState state, Arr arr) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < arr.size(); i++) {
            Obj o = arr.obj(i);
            if (o != null) map.put(o.str("key"), o.i("turn"));
        }
        state.restoreBearDenCooldown(map);
    }

    private static String name(Enum<?> value) { return value == null ? null : value.name(); }

    private static <E extends Enum<E>> E enumOr(Class<E> type, String name, E fallback) {
        if (name == null || name.isBlank()) return fallback;
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
