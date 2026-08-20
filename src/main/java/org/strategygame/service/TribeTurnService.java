package org.strategygame.service;

import org.strategygame.config.GameConfig;
import org.strategygame.model.GameState;
import org.strategygame.model.combat.CombatResult;
import org.strategygame.model.combat.Combatant;
import org.strategygame.model.event.NotificationKind;
import org.strategygame.model.map.HexCell;
import org.strategygame.model.map.TerrainType;
import org.strategygame.model.tribe.RelationState;
import org.strategygame.model.tribe.Tribe;
import org.strategygame.model.tribe.TribeType;
import org.strategygame.model.tribe.mission.MissionState;
import org.strategygame.model.tribe.mission.TribeMission;
import org.strategygame.model.unit.MilitaryUnit;
import org.strategygame.model.unit.Owner;
import org.strategygame.model.unit.Unit;
import org.strategygame.model.unit.UnitFactory;
import org.strategygame.model.unit.UnitType;

import java.util.ArrayList;
import java.util.List;

/**
 * رفتار قبیله‌ها در هر نوبت، با ترتیب ثابت:
 * بررسی وضعیت رابطه → تایمر مأموریت → رفتار دفاعی/تهاجمی →
 * ساخت و حرکت یونیت → اعلان. هر قبیله در هر نوبت فقط یک رفتار اصلی دارد.
 */
public class TribeTurnService {

    private final CombatService  combatService;
    private final TribeService   tribeService;

    public TribeTurnService(CombatService combatService, TribeService tribeService) {
        this.combatService = combatService;
        this.tribeService  = tribeService;
    }

    public void executeTurn(GameState state) {
        for (Tribe tribe : new ArrayList<>(state.getTribes())) {
            if (tribe.isDefeated()) continue;

            refreshTribeUnits(tribe);
            checkRelation(state, tribe);
            updateMission(state, tribe);
            performMainBehaviour(state, tribe);
        }
        state.purgeDeadUnits();
    }

    /** AP یونیت‌های قبیله در ابتدای رفتار قبیله تازه می‌شود. */
    private void refreshTribeUnits(Tribe tribe) {
        for (MilitaryUnit u : tribe.aliveUnits()) u.refreshAP();
    }

    // ------------------------------------------------------- بررسی وضعیت رابطه
    private void checkRelation(GameState state, Tribe tribe) {
        if (tribe.isAllied() && tribe.getRelation() < GameConfig.RELATION_ALLIANCE_MIN) {
            tribe.setAllied(false);
            state.notify(NotificationKind.RELATION,
                    "اتحاد با " + tribe.getName() + " غیرفعال شد و به وضعیت دوستی برگشت");
        }
        applyTrespassRules(state, tribe);
    }

    /**
     * ورود نیروی نظامی بازیکن به قلمرو قبیله‌ی بی‌طرف اول هشدار می‌گیرد و اگر
     * در نوبت بعد هم حاضر باشد رابطه کم می‌شود.
     */
    private void applyTrespassRules(GameState state, Tribe tribe) {
        RelationState relation = tribe.getRelationState();
        boolean neutralish = relation == RelationState.NEUTRAL || relation == RelationState.DISPLEASED;
        if (!neutralish || tribe.getCampHex() == null) {
            tribe.setTrespassWarned(false);
            return;
        }

        boolean intruder = hasPlayerMilitaryWithin(state, tribe.getCampHex(),
                GameConfig.TRIBE_WARNING_RADIUS);

        if (!intruder) {
            tribe.setTrespassWarned(false);
            return;
        }

        if (!tribe.isTrespassWarned()) {
            tribe.setTrespassWarned(true);
            state.notify(NotificationKind.RELATION,
                    tribe.getName() + " هشدار داد: نیروی نظامی شما را از قلمرو خود خارج کنید");
            return;
        }

        RelationState changed = tribe.changeRelation(GameConfig.TRIBE_TRESPASS_PENALTY);
        state.notify(NotificationKind.RELATION,
                "ماندن نیروی نظامی در قلمرو " + tribe.getName() + " رابطه را کم کرد ("
                        + tribe.getRelation() + ")");
        tribeService.announceRelation(state, tribe, changed);
    }

    // --------------------------------------------------------- تایمر مأموریت
    private void updateMission(GameState state, Tribe tribe) {
        TribeMission mission = tribe.getActiveMission();
        if (mission == null) {
            offerMissionIfFriendly(state, tribe);
            return;
        }
        if (!mission.isOpen()) return;

        if (mission.isObjectiveMet(state, tribe)) mission.markReadyToTurnIn();
        else mission.markNotReady();

        boolean failed = mission.tickDeadline();
        if (failed) {
            tribe.recordMissionFailure(state.getTurn());
            tribe.changeRelation(GameConfig.MISSION_FAIL_RELATION);
            state.notify(NotificationKind.MISSION,
                    "مأموریت «" + mission.getTitle() + "» از " + tribe.getName()
                            + " شکست خورد (رابطه " + GameConfig.MISSION_FAIL_RELATION + ")");
        } else if (mission.getState() == MissionState.READY_TO_TURN_IN) {
            state.notify(NotificationKind.MISSION,
                    "مأموریت «" + mission.getTitle() + "» آماده‌ی تحویل است");
        }
    }

    /** قبیله‌ی دوست هر چند نوبت یک بار مأموریت جدید پیشنهاد می‌دهد. */
    private void offerMissionIfFriendly(GameState state, Tribe tribe) {
        if (!tribe.isDiscovered() || tribe.isAtWar()) return;
        if (tribe.getRelation() < GameConfig.RELATION_TRADE_MIN) return;
        if (tribe.isMissionOnCooldown(state.getTurn())) return;

        int since = state.getTurn() - tribe.getLastMissionOfferTurn();
        if (since < GameConfig.FRIENDLY_MISSION_INTERVAL) return;

        tribe.setLastMissionOfferTurn(state.getTurn());
        state.notify(NotificationKind.MISSION,
                tribe.getName() + " مأموریت جدیدی برای شما دارد");
    }

    // ------------------------------------------------------------ رفتار اصلی
    /** اولویت: دفاع از اردوگاه، ساخت نگهبان در دشمنی، پیشنهاد در دوستی، بیکاری. */
    private void performMainBehaviour(GameState state, Tribe tribe) {
        if (defendCamp(state, tribe)) return;
        if (tribe.getRelationState() == RelationState.ENEMY || tribe.isAtWar()) {
            if (engagePlayer(state, tribe)) return;
            spawnGuard(state, tribe);
        }
    }

    /** اگر نیروی بازیکن نزدیک اردوگاه باشد، نزدیک‌ترین نگهبان واکنش نشان می‌دهد. */
    private boolean defendCamp(GameState state, Tribe tribe) {
        HexCell camp = tribe.getCampHex();
        if (camp == null) return false;
        if (!tribe.isAtWar() && tribe.getRelationState() != RelationState.ENEMY) return false;

        Unit threat = nearestPlayerMilitary(state, camp, GameConfig.TRIBE_AGGRESSION_RADIUS);
        if (threat == null) return false;

        MilitaryUnit defender = nearestTribeUnit(state, tribe, threat.getPosition());
        if (defender == null) return false;

        actWith(state, tribe, defender, threat.getPosition());
        return true;
    }

    /** رفتار تهاجمی قبیله‌ی دشمن روی نزدیک‌ترین هدف بازیکن. */
    private boolean engagePlayer(GameState state, Tribe tribe) {
        if (tribe.aliveUnits().isEmpty()) return false;

        HexCell camp = tribe.getCampHex();
        Unit target = nearestPlayerMilitary(state, camp, GameConfig.TRIBE_AGGRESSION_RADIUS);
        if (target == null) return false;

        MilitaryUnit attacker = nearestTribeUnit(state, tribe, target.getPosition());
        if (attacker == null) return false;

        actWith(state, tribe, attacker, target.getPosition());
        return true;
    }

    /** هر یونیت قبیله در هر نوبت حداکثر یک حرکت و یک حمله دارد. */
    private void actWith(GameState state, Tribe tribe, MilitaryUnit unit, HexCell targetCell) {
        if (unit.getPosition() == null || targetCell == null) return;

        int distance = state.getMap().distance(unit.getPosition(), targetCell);
        if (distance > unit.getAttackRange()) {
            stepToward(state, unit, targetCell);
            distance = state.getMap().distance(unit.getPosition(), targetCell);
        }

        if (distance <= unit.getAttackRange() && unit.hasAP(GameConfig.ATTACK_AP_COST)) {
            tribeAttack(state, tribe, unit, targetCell);
        }
    }

    private void tribeAttack(GameState state, Tribe tribe, MilitaryUnit unit, HexCell targetCell) {
        List<Combatant> attackers = new ArrayList<>(List.of(unit));
        List<Combatant> defenders = combatService.friendlyCombatants(targetCell, Owner.PLAYER);
        if (defenders.isEmpty()) return;

        int wallBonus = combatService.wallBonusBetween(state, unit.getPosition(), targetCell);
        CombatResult result = combatService.resolveDiceCombat(state, attackers, defenders,
                wallBonus, tribe.getName(), "نیروهای شما");
        unit.spendAP(GameConfig.ATTACK_AP_COST);

        state.notify(NotificationKind.TRIBE_ATTACK,
                tribe.getName() + " به نیروهای شما در هکس ("
                        + targetCell.getQ() + "," + targetCell.getR() + ") حمله کرد. "
                        + "تلفات شما: " + (result.getDefenderCasualties().isEmpty()
                        ? "بدون تلفات" : String.join(" , ", result.getDefenderCasualties())));
    }

    /** یک قدم به سمت هدف؛ فقط روی زمین قابل عبور و با رعایت ظرفیت هکس. */
    private void stepToward(GameState state, MilitaryUnit unit, HexCell target) {
        HexCell from = unit.getPosition();
        HexCell best = null;
        int bestDistance = state.getMap().distance(from, target);

        for (HexCell n : state.getMap().getNeighbors(from)) {
            if (!n.getTerrain().isLandPassable() || n.isBlocked()) continue;
            if (n.getTerrain() == TerrainType.MOUNTAIN_RANGE) continue;
            if (!state.hexHasRoomFor(n, unit.getType())) continue;

            int cost = n.getTerrain().getMovementCost();
            if (!unit.hasAP(cost)) continue;

            int distance = state.getMap().distance(n, target);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = n;
            }
        }
        if (best == null) return;

        unit.spendAP(best.getTerrain().getMovementCost());
        from.removeUnit(unit);
        best.addUnit(unit);
        unit.setPosition(best);
    }

    /** قبیله‌ی دشمن بدون یونیت، هر چند نوبت یک نگهبان می‌سازد. */
    private void spawnGuard(GameState state, Tribe tribe) {
        if (!tribe.canSpawnGuard(state.getTurn())) return;

        HexCell camp = tribe.getCampHex();
        if (camp == null) return;

        UnitType type = guardTypeFor(tribe);
        HexCell spot = state.hexHasRoomFor(camp, type) ? camp : freeNeighbor(state, camp, type);
        if (spot == null) return;

        MilitaryUnit guard = UnitFactory.createMilitary(type, Owner.TRIBE);
        if (guard == null) return;

        guard.setPosition(spot);
        spot.addUnit(guard);
        tribe.addUnit(guard);
        tribe.setLastGuardTurn(state.getTurn());

        state.notify(NotificationKind.GUARD_SPAWNED,
                tribe.getName() + " یک نگهبان جدید ساخت ("
                        + tribe.aliveUnits().size() + "/" + tribe.getType().getGuardCap() + ")");
    }

    /** قبیله‌ی جنگاور ترکیبی از شمشیرزن و کماندار می‌سازد، بقیه فقط شمشیرزن. */
    private UnitType guardTypeFor(Tribe tribe) {
        if (tribe.getType() != TribeType.WARRIOR) return UnitType.SWORDSMAN;
        return tribe.aliveUnits().size() % 2 == 0 ? UnitType.SWORDSMAN : UnitType.ARCHER;
    }

    private HexCell freeNeighbor(GameState state, HexCell camp, UnitType type) {
        for (HexCell n : state.getMap().getNeighbors(camp)) {
            if (!n.getTerrain().isLandPassable() || n.isBlocked()) continue;
            if (state.hexHasRoomFor(n, type)) return n;
        }
        return null;
    }

    // ---------------------------------------------------------------- کمکی‌ها
    private boolean hasPlayerMilitaryWithin(GameState state, HexCell center, int radius) {
        return nearestPlayerMilitary(state, center, radius) != null;
    }

    private Unit nearestPlayerMilitary(GameState state, HexCell center, int radius) {
        if (center == null) return null;
        Unit best = null;
        int bestDistance = Integer.MAX_VALUE;

        for (Unit u : state.getUnits()) {
            if (!u.isAlive() || !u.isMilitary() || u.getPosition() == null) continue;
            int distance = state.getMap().distance(center, u.getPosition());
            if (distance <= radius && distance < bestDistance) {
                bestDistance = distance;
                best = u;
            }
        }
        return best;
    }

    private MilitaryUnit nearestTribeUnit(GameState state, Tribe tribe, HexCell target) {
        MilitaryUnit best = null;
        int bestDistance = Integer.MAX_VALUE;

        for (MilitaryUnit u : tribe.aliveUnits()) {
            if (u.getPosition() == null) continue;
            int distance = state.getMap().distance(u.getPosition(), target);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = u;
            }
        }
        return best;
    }
}
