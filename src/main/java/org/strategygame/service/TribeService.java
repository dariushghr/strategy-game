package org.strategygame.service;

import org.strategygame.common.ActionResult;
import org.strategygame.config.GameConfig;
import org.strategygame.model.GameState;
import org.strategygame.model.building.Building;
import org.strategygame.model.building.BuildingFactory;
import org.strategygame.model.building.BuildingType;
import org.strategygame.model.event.NotificationKind;
import org.strategygame.model.map.HexCell;
import org.strategygame.model.map.TerrainType;
import org.strategygame.model.resource.ResourceType;
import org.strategygame.model.tribe.RelationState;
import org.strategygame.model.tribe.Tribe;
import org.strategygame.model.tribe.TribeType;
import org.strategygame.model.tribe.mission.MissionFactory;
import org.strategygame.model.tribe.mission.TribeMission;
import org.strategygame.model.unit.MilitaryUnit;

import java.util.ArrayList;
import java.util.List;

/**
 * قبیله‌ها به‌عنوان موجودیت مستقل: قرارگیری روی نقشه، کشف شدن، رابطه و همه‌ی
 * تعامل‌های بازیکن (هدیه، مأموریت، جنگ، صلح و اتحاد).
 */
public class TribeService {

    private static final String[] NAMES = {
            "دره‌ی سبز", "سنگ‌کوب", "کاروان زرین", "بلندکوه", "موج آبی"
    };

    private final HappinessService happinessService = new HappinessService();

    // ------------------------------------------------------------- قرارگیری
    /** یک قبیله از هر نوع روی نقشه قرار می‌دهد. */
    public void spawnInitialTribes(GameState state) {
        HexCell home = state.getTownHall() == null ? null : state.getTownHall().getLocation();
        if (home == null) return;

        TribeType[] types = TribeType.values();
        for (int i = 0; i < types.length; i++) {
            HexCell camp = findCampSpot(state, home, types[i]);
            if (camp == null) continue;

            Tribe tribe = new Tribe(NAMES[i % NAMES.length], types[i], camp);
            state.addTribe(tribe);
            reserveTerritory(state, camp);
        }
    }

    private HexCell findCampSpot(GameState state, HexCell home, TribeType type) {
        List<HexCell> candidates = new ArrayList<>();

        for (HexCell c : state.getMap().getAllCells()) {
            if (!isValidCampTerrain(c, type)) continue;
            if (c.hasBuilding() || state.tribeAt(c) != null) continue;
            if (state.getMap().distance(home, c) < GameConfig.TRIBE_MIN_DISTANCE_FROM_TOWN_HALL)
                continue;
            if (tooCloseToOtherCamp(state, c)) continue;
            if (type.requiresCoast() && !state.getMap().isCoastal(c)) continue;
            candidates.add(c);
        }

        if (candidates.isEmpty() && type.requiresCoast()) {
            // اگر ساحل مناسبی نبود، شرط ساحلی بودن را نادیده می‌گیریم تا هر پنج نوع روی نقشه باشند.
            for (HexCell c : state.getMap().getAllCells()) {
                if (!isValidCampTerrain(c, type)) continue;
                if (c.hasBuilding() || state.tribeAt(c) != null) continue;
                if (state.getMap().distance(home, c) < GameConfig.TRIBE_MIN_DISTANCE_FROM_TOWN_HALL)
                    continue;
                if (tooCloseToOtherCamp(state, c)) continue;
                candidates.add(c);
            }
        }
        return state.getRandom().pick(candidates);
    }

    private boolean isValidCampTerrain(HexCell cell, TribeType type) {
        TerrainType terrain = cell.getTerrain();
        if (terrain == TerrainType.SEA || terrain == TerrainType.MOUNTAIN_RANGE) return false;
        return type.getPreferredTerrain().contains(terrain);
    }

    private boolean tooCloseToOtherCamp(GameState state, HexCell cell) {
        for (Tribe t : state.getTribes()) {
            if (t.getCampHex() == null) continue;
            if (state.getMap().distance(cell, t.getCampHex()) < 4) return true;
        }
        return false;
    }

    /** قلمرو کوچک اطراف اردوگاه به بازیکن تعلق ندارد. */
    private void reserveTerritory(GameState state, HexCell camp) {
        camp.setInBorder(false);
        for (HexCell c : state.getMap().getCellsInRadius(camp, GameConfig.TRIBE_TERRITORY_RADIUS)) {
            c.setInBorder(false);
        }
    }

    // ------------------------------------------------------------------ کشف
    /** قبیله‌هایی که اردوگاهشان کشف شده را علامت می‌زند. */
    public void updateDiscovery(GameState state) {
        for (Tribe tribe : state.getTribes()) {
            if (tribe.isDiscovered() || tribe.getCampHex() == null) continue;
            HexCell camp = tribe.getCampHex();
            if (state.getFog().isExplored(camp.getQ(), camp.getR())) {
                tribe.setDiscovered(true);
                state.notify(NotificationKind.RELATION,
                        "قبیله‌ی " + tribe.getName() + " (" + tribe.getType().getLabel()
                                + ") کشف شد");
            }
        }
    }

    /** آیا اطلاعات زنده‌ی قبیله قابل نمایش است یا فقط اطلاعات ذخیره‌شده. */
    public boolean isCampVisible(GameState state, Tribe tribe) {
        HexCell camp = tribe.getCampHex();
        return camp != null && state.getFog().isVisible(camp.getQ(), camp.getR());
    }

    // ----------------------------------------------------------------- هدیه
    public String giftProblem(GameState state, Tribe tribe, ResourceType type, int amount) {
        if (tribe == null || type == null)   return "انتخاب نامعتبر است";
        if (!tribe.isDiscovered())           return "این قبیله را هنوز کشف نکرده‌اید";
        if (tribe.isDefeated())              return tribe.getName() + " دیگر وجود ندارد";
        if (!tribe.getRelationState().allowsGift())
            return "در وضعیت دشمنی نمی‌توانید هدیه بدهید";
        if (amount < giftUnit(type))
            return "حداقل مقدار هدیه برای " + type.getLabel() + " برابر "
                    + giftUnit(type) + " است";
        if (!state.getStorage().canAfford(type, amount))
            return "منبع کافی نیست (" + type.getLabel() + " موجود: "
                    + state.getStorage().get(type) + ")";
        if (tribe.getRelation() >= GameConfig.RELATION_MAX)
            return "رابطه با این قبیله در بالاترین حد است";
        return null;
    }

    public ActionResult gift(GameState state, Tribe tribe, ResourceType type, int amount) {
        String problem = giftProblem(state, tribe, type, amount);
        if (problem != null) return ActionResult.fail(problem);

        int gain = giftRelationGain(type, amount);
        int spent = (amount / giftUnit(type)) * giftUnit(type);

        state.getStorage().deduct(type, spent);
        int before = tribe.getRelation();
        RelationState changed = tribe.changeRelation(gain);
        announceRelation(state, tribe, changed);

        return ActionResult.ok("هدیه‌ی " + spent + " " + type.getLabel() + " تحویل شد.\n\n"
                + "رابطه: " + before + " → " + tribe.getRelation()
                + " (+" + (tribe.getRelation() - before) + ")\n"
                + "وضعیت: " + tribe.getRelationState().getLabel());
    }

    public int giftUnit(ResourceType type) {
        return switch (type) {
            case FOOD  -> GameConfig.GIFT_FOOD_UNIT;
            case WOOD  -> GameConfig.GIFT_WOOD_UNIT;
            case STONE -> GameConfig.GIFT_STONE_UNIT;
            case IRON  -> GameConfig.GIFT_IRON_UNIT;
        };
    }

    public int giftGainPerUnit(ResourceType type) {
        return switch (type) {
            case FOOD  -> GameConfig.GIFT_FOOD_GAIN;
            case WOOD  -> GameConfig.GIFT_WOOD_GAIN;
            case STONE -> GameConfig.GIFT_STONE_GAIN;
            case IRON  -> GameConfig.GIFT_IRON_GAIN;
        };
    }

    public int giftRelationGain(ResourceType type, int amount) {
        return (amount / giftUnit(type)) * giftGainPerUnit(type);
    }

    // ------------------------------------------------------------------ جنگ
    public String declareWarProblem(GameState state, Tribe tribe) {
        if (tribe == null)          return "قبیله‌ای انتخاب نشده است";
        if (!tribe.isDiscovered())  return "این قبیله را هنوز کشف نکرده‌اید";
        if (tribe.isDefeated())     return tribe.getName() + " دیگر وجود ندارد";
        if (!tribe.getRelationState().allowsWar())
            return "از قبل با این قبیله در وضعیت دشمنی هستید";
        return null;
    }

    public ActionResult declareWar(GameState state, Tribe tribe) {
        String problem = declareWarProblem(state, tribe);
        if (problem != null) return ActionResult.fail(problem);

        TribeMission mission = tribe.getActiveMission();
        boolean hadMission = mission != null && mission.isOpen();
        if (hadMission) mission.cancel();
        tribe.clearActiveMission();

        tribe.declareWar();
        happinessService.onWarDeclared(state, tribe);

        state.notify(NotificationKind.WAR, "جنگ با " + tribe.getName() + " آغاز شد");
        return ActionResult.ok("جنگ با " + tribe.getName() + " اعلام شد.\n\n"
                + "رابطه به " + GameConfig.RELATION_WAR_VALUE + " رسید و تجارت بسته شد."
                + (hadMission ? "\nمأموریت فعال لغو شد." : ""));
    }

    // ------------------------------------------------------------------ صلح
    public String peaceProblem(GameState state, Tribe tribe) {
        if (tribe == null)       return "قبیله‌ای انتخاب نشده است";
        if (tribe.isDefeated())  return tribe.getName() + " دیگر وجود ندارد";
        if (!tribe.getRelationState().allowsPeace())
            return "صلح فقط در وضعیت دشمنی ممکن است";
        if (!state.getStorage().canAffordAll(GameConfig.PEACE_COST))
            return "منابع کافی نیست (نیاز: "
                    + BuildingType.formatCost(GameConfig.PEACE_COST) + ")";
        return null;
    }

    public ActionResult makePeace(GameState state, Tribe tribe) {
        String problem = peaceProblem(state, tribe);
        if (problem != null) return ActionResult.fail(problem);

        state.getStorage().deductAll(GameConfig.PEACE_COST);
        tribe.makePeace();
        announceRelation(state, tribe, tribe.getRelationState());

        return ActionResult.ok("صلح با " + tribe.getName() + " برقرار شد.\n\n"
                + "هزینه: " + BuildingType.formatCost(GameConfig.PEACE_COST) + "\n"
                + "رابطه به " + GameConfig.RELATION_AFTER_PEACE + " رسید (وضعیت: "
                + tribe.getRelationState().getLabel() + ")\n"
                + "تجارت و مأموریت هنوز قفل هستند.");
    }

    // ----------------------------------------------------------------- اتحاد
    public String allianceProblem(GameState state, Tribe tribe) {
        if (tribe == null)          return "قبیله‌ای انتخاب نشده است";
        if (tribe.isDefeated())     return tribe.getName() + " دیگر وجود ندارد";
        if (tribe.isAllied())       return "از قبل با این قبیله متحد هستید";
        if (tribe.isAtWar())        return "در حالت جنگ اتحاد ممکن نیست";
        if (tribe.getRelation() < GameConfig.RELATION_ALLIANCE_MIN)
            return "اتحاد به رابطه‌ی حداقل " + GameConfig.RELATION_ALLIANCE_MIN
                    + " نیاز دارد (فعلی: " + tribe.getRelation() + ")";
        if (tribe.hadRecentMissionFailure(state.getTurn()))
            return "در " + GameConfig.MISSION_FAIL_COOLDOWN
                    + " نوبت اخیر مأموریت این قبیله شکست خورده است";

        for (Tribe other : state.getTribes()) {
            if (other == tribe || !other.isAllied()) continue;
            if (other.getType() == TribeType.WARRIOR)
                return "اتحاد با قبیله‌ی جنگاور مانع اتحاد با بقیه‌ی قبیله‌ها است";
            if (tribe.getType() == TribeType.WARRIOR)
                return "برای اتحاد با قبیله‌ی جنگاور نباید اتحاد دیگری داشته باشید";
            if (isFarmerMountainConflict(tribe.getType(), other.getType()))
                return "قبیله‌ی کشاورز و کوه‌نشین هم‌زمان متحد نمی‌شوند";
        }
        return null;
    }

    private boolean isFarmerMountainConflict(TribeType a, TribeType b) {
        return (a == TribeType.FARMER && b == TribeType.MOUNTAIN)
            || (a == TribeType.MOUNTAIN && b == TribeType.FARMER);
    }

    public ActionResult formAlliance(GameState state, Tribe tribe) {
        String problem = allianceProblem(state, tribe);
        if (problem != null) return ActionResult.fail(problem);

        tribe.setAllied(true);
        state.notify(NotificationKind.RELATION, "اتحاد با " + tribe.getName() + " برقرار شد");
        return ActionResult.ok("اتحاد با " + tribe.getName() + " برقرار شد.\n\n"
                + "تا وقتی رابطه بالای " + GameConfig.RELATION_ALLIANCE_MIN
                + " بماند، بونوس اتحاد فعال است.");
    }

    // -------------------------------------------------------------- مأموریت
    public String acceptMissionProblem(GameState state, Tribe tribe) {
        if (tribe == null)          return "قبیله‌ای انتخاب نشده است";
        if (!tribe.isDiscovered())  return "این قبیله را هنوز کشف نکرده‌اید";
        if (tribe.isDefeated())     return tribe.getName() + " دیگر وجود ندارد";
        if (tribe.isAtWar())        return "در حالت جنگ مأموریت داده نمی‌شود";
        if (tribe.hasOpenMission()) return "یک مأموریت فعال از این قبیله دارید";
        if (tribe.getRelation() < GameConfig.RELATION_TRADE_MIN)
            return "گرفتن مأموریت به رابطه‌ی حداقل " + GameConfig.RELATION_TRADE_MIN
                    + " نیاز دارد (فعلی: " + tribe.getRelation() + ")";
        if (tribe.isMissionOnCooldown(state.getTurn()))
            return "بعد از شکست مأموریت باید " + GameConfig.MISSION_FAIL_COOLDOWN
                    + " نوبت صبر کنید";
        return null;
    }

    public ActionResult acceptMission(GameState state, Tribe tribe) {
        String problem = acceptMissionProblem(state, tribe);
        if (problem != null) return ActionResult.fail(problem);

        TribeMission mission = MissionFactory.create(tribe);
        mission.accept();
        tribe.setActiveMission(mission);
        tribe.setLastMissionOfferTurn(state.getTurn());

        state.notify(NotificationKind.MISSION,
                "مأموریت «" + mission.getTitle() + "» از " + tribe.getName() + " گرفته شد");
        return ActionResult.ok("مأموریت «" + mission.getTitle() + "» شروع شد.\n\n"
                + "هدف: " + mission.getObjectiveText() + "\n"
                + "مهلت: " + mission.getDeadlineTurns() + " نوبت\n"
                + "جایزه: " + mission.rewardText());
    }

    public String turnInProblem(GameState state, Tribe tribe) {
        if (tribe == null || tribe.getActiveMission() == null)
            return "مأموریت فعالی وجود ندارد";

        TribeMission mission = tribe.getActiveMission();
        if (mission.isRewardClaimed()) return "جایزه‌ی این مأموریت قبلا گرفته شده است";
        if (!mission.isObjectiveMet(state, tribe))
            return "شرط مأموریت هنوز کامل نشده: " + mission.getObjectiveText();
        if (!state.getStorage().canAffordAll(mission.getPaymentCost()))
            return "منابع لازم برای تحویل را ندارید ("
                    + BuildingType.formatCost(mission.getPaymentCost()) + ")";

        int[] reward = mission.getRewardResources();
        ResourceType[] types = ResourceType.values();
        for (int i = 0; i < types.length && i < reward.length; i++) {
            if (reward[i] > 0 && state.getStorage().freeSpace(types[i]) < reward[i])
                return "ظرفیت انبار برای جایزه‌ی " + reward[i] + " "
                        + types[i].getLabel() + " کافی نیست";
        }
        return null;
    }

    public ActionResult turnInMission(GameState state, Tribe tribe) {
        String problem = turnInProblem(state, tribe);
        if (problem != null) return ActionResult.fail(problem);

        TribeMission mission = tribe.getActiveMission();
        mission.markReadyToTurnIn();
        if (!mission.turnIn()) return ActionResult.fail("این مأموریت قابل تحویل نیست");

        state.getStorage().deductAll(mission.getPaymentCost());
        int[] reward = mission.getRewardResources();
        ResourceType[] types = ResourceType.values();
        for (int i = 0; i < types.length && i < reward.length; i++) {
            if (reward[i] > 0) state.getStorage().add(types[i], reward[i]);
        }

        RelationState changed = tribe.changeRelation(mission.getRewardRelation());
        mission.grantSpecialReward(state, tribe);
        tribe.clearActiveMission();
        announceRelation(state, tribe, changed);

        state.notify(NotificationKind.MISSION,
                "مأموریت «" + mission.getTitle() + "» کامل شد");
        return ActionResult.ok("مأموریت «" + mission.getTitle() + "» تحویل شد.\n\n"
                + "جایزه: " + mission.rewardText() + "\n"
                + "رابطه‌ی فعلی: " + tribe.getRelation()
                + " (" + tribe.getRelationState().getLabel() + ")");
    }

    public ActionResult cancelMission(GameState state, Tribe tribe) {
        if (tribe == null || !tribe.hasOpenMission())
            return ActionResult.fail("مأموریت فعالی برای لغو وجود ندارد");

        TribeMission mission = tribe.getActiveMission();
        mission.cancel();
        tribe.clearActiveMission();
        RelationState changed = tribe.changeRelation(GameConfig.MISSION_CANCEL_RELATION);
        announceRelation(state, tribe, changed);

        return ActionResult.ok("مأموریت «" + mission.getTitle() + "» لغو شد.\n\n"
                + "رابطه " + GameConfig.MISSION_CANCEL_RELATION + " تغییر کرد (فعلی: "
                + tribe.getRelation() + ")");
    }

    // ------------------------------------------------------- حمله و فتح اردوگاه
    /** بازیکن به قبیله حمله کرد: اتحاد می‌شکند و رابطه به کف می‌رسد. */
    public void onPlayerAttackedTribe(GameState state, Tribe tribe) {
        if (tribe == null || tribe.isDefeated()) return;

        happinessService.onTribeAttacked(state, tribe);

        if (tribe.isAllied()) {
            tribe.setAllied(false);
            state.notify(NotificationKind.RELATION,
                    "اتحاد با " + tribe.getName() + " به‌خاطر حمله شکست");
        }
        if (tribe.hasOpenMission()) {
            tribe.getActiveMission().cancel();
            tribe.clearActiveMission();
        }
        tribe.declareWar();
        state.notify(NotificationKind.WAR, "قبیله‌ی " + tribe.getName() + " با شما وارد جنگ شد");
    }

    /** اردوگاه نابود شد: قبیله شکست می‌خورد و اردوگاه به پاسگاه بازیکن تبدیل می‌شود. */
    public ActionResult onCampDestroyed(GameState state, Tribe tribe) {
        if (tribe == null || tribe.isDefeated()) return ActionResult.fail("قبیله‌ای شکست نخورد");

        HexCell camp = tribe.getCampHex();
        tribe.markDefeated();

        for (MilitaryUnit u : new ArrayList<>(tribe.getUnits())) state.removeUnit(u);

        if (camp != null) {
            camp.setInBorder(true);
            for (HexCell n : state.getMap().getNeighbors(camp)) {
                if (n.getTerrain().isLandPassable()) n.setInBorder(true);
            }
            if (!camp.hasBuilding()) {
                Building outpost = BuildingFactory.create(BuildingType.OUTPOST);
                outpost.setLocation(camp);
                camp.setBuilding(outpost);
                state.addBuilding(outpost);
            }
        }

        int[] loot = tribe.getType().getLoot();
        ResourceType[] types = ResourceType.values();
        for (int i = 0; i < types.length && i < loot.length; i++) {
            if (loot[i] > 0) state.getStorage().add(types[i], loot[i]);
        }

        state.getFog().update(state.getUnits(), state.getBuildings());
        state.notify(NotificationKind.WAR,
                "قبیله‌ی " + tribe.getName() + " شکست خورد و اردوگاهش به پاسگاه تبدیل شد");

        return ActionResult.ok("قبیله‌ی " + tribe.getName() + " شکست خورد.\n\n"
                + "اردوگاه به پاسگاه شما تبدیل شد و هکس‌های مجاز اطراف به قلمرو اضافه شدند.\n"
                + "غنیمت: " + tribe.getType().lootText() + "\n"
                + "تجارت، مأموریت و اتحاد این قبیله خاتمه یافت.");
    }

    void announceRelation(GameState state, Tribe tribe, RelationState changed) {
        if (changed == null) return;
        state.notify(NotificationKind.RELATION,
                "رابطه با " + tribe.getName() + " به «" + changed.getLabel() + "» تغییر کرد");
    }
}
