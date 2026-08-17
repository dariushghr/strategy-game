package org.strategygame.service;

import org.strategygame.config.GameConfig;
import org.strategygame.model.GameState;
import org.strategygame.model.building.Building;
import org.strategygame.model.combat.CombatCategory;
import org.strategygame.model.combat.CombatResult;
import org.strategygame.model.combat.Combatant;
import org.strategygame.model.combat.DamageResolver;
import org.strategygame.model.combat.DicePair;
import org.strategygame.model.combat.Structure;
import org.strategygame.model.event.NotificationKind;
import org.strategygame.model.map.HexCell;
import org.strategygame.model.map.Wall;
import org.strategygame.model.unit.Owner;
import org.strategygame.model.unit.Unit;
import org.strategygame.model.unit.UnitType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * موتور نبرد. تمام حمله‌ها — بازیکن، قبیله و حیوان وحشی — از همین سرویس
 * استفاده می‌کنند تا قواعد تاس، اولویت آسیب و حمله به سازه یک‌جا بمانند.
 */
public class CombatService {

    // ------------------------------------------------------- اعتبارسنجی حمله
    /** دلیل غیرمجاز بودن حمله از یک هکس به هکس دیگر؛ {@code null} یعنی مجاز. */
    public String attackProblem(GameState state, HexCell from, HexCell to) {
        if (from == null || to == null) return "انتخاب نامعتبر است";
        if (from.equals(to))            return "هکس مهاجم و مدافع نمی‌توانند یکی باشند";

        int distance = state.getMap().distance(from, to);
        if (distance > 2) return "هدف خارج از برد است (حداکثر ۲ هکس)";

        List<Combatant> attackers = eligibleAttackers(state, from, distance, Owner.PLAYER);
        if (attackers.isEmpty()) {
            if (distance == 2)
                return "از فاصله ۲ هکس فقط کماندار می‌تواند حمله کند";
            return "یونیت نظامی آماده‌ای با AP کافی روی هکس مهاجم نیست";
        }

        List<Combatant> defenders = hostileCombatants(state, to, Owner.PLAYER);
        if (defenders.isEmpty() && targetStructure(state, to) == null)
            return "روی هکس هدف چیزی برای حمله وجود ندارد";
        return null;
    }

    /**
     * حمله‌ی بازیکن. اگر هکس هدف مدافع زنده داشته باشد نبرد تاسی انجام می‌شود؛
     * در غیر این صورت اگر سازه‌ای وجود داشته باشد آسیب سازه‌ای اعمال می‌شود.
     */
    public CombatResult attack(GameState state, HexCell from, HexCell to) {
        int distance = state.getMap().distance(from, to);
        List<Combatant> attackers = eligibleAttackers(state, from, distance, Owner.PLAYER);
        List<Combatant> defenders = hostileCombatants(state, to, Owner.PLAYER);

        if (!defenders.isEmpty()) {
            int wallBonus = wallBonusBetween(state, from, to);
            CombatResult result = resolveDiceCombat(state, attackers, defenders,
                    wallBonus, describe(attackers), describe(defenders));
            spendAttackAp(attackers);
            return result;
        }
        return attackStructureAt(state, from, to, attackers);
    }

    // ----------------------------------------------------------- نبرد تاسی
    /**
     * هسته‌ی نبرد تاسی: برای هر نوع حاضر در سمت مهاجم یک تاس، تاس‌ها نزولی
     * مرتب و جفت‌به‌جفت مقایسه می‌شوند، تساوی به نفع مدافع است و تاس بدون جفت
     * نادیده گرفته می‌شود.
     */
    public CombatResult resolveDiceCombat(GameState state,
                                          List<Combatant> attackers,
                                          List<Combatant> defenders,
                                          int wallBonus,
                                          String attackerLabel,
                                          String defenderLabel) {
        CombatResult result = new CombatResult(attackerLabel, defenderLabel);
        RandomService rng = state.getRandom();

        List<Integer> attackDice = rollDice(rng, attackDiceCount(attackers), 0);
        List<Integer> defenseDice = rollDice(rng, defenseDiceCount(defenders), wallBonus);

        attackDice.sort(Comparator.reverseOrder());
        defenseDice.sort(Comparator.reverseOrder());
        attackDice.forEach(result::addAttackDie);
        defenseDice.forEach(result::addDefenseDie);
        result.setWallBonus(wallBonus);

        int pairs = Math.min(attackDice.size(), defenseDice.size());
        int defenderHits = 0, attackerHits = 0;
        for (int i = 0; i < pairs; i++) {
            DicePair pair = DicePair.compare(attackDice.get(i), defenseDice.get(i));
            result.addPair(pair);
            if (pair.attackerWins()) defenderHits++; else attackerHits++;
        }

        applyHits(state, defenders, defenderHits, result, true);
        applyHits(state, attackers, attackerHits, result, false);

        state.purgeDeadUnits();
        return result;
    }

    private List<Integer> rollDice(RandomService rng, int count, int bonus) {
        List<Integer> dice = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int value = rng.rollDice() + bonus;
            dice.add(Math.min(GameConfig.DICE_MAX_VALUE, value));
        }
        return dice;
    }

    /** یک تاس حمله برای هر نوع یونیت حاضر در سمت مهاجم. */
    public int attackDiceCount(List<Combatant> attackers) {
        return distinctTypes(attackers).size();
    }

    /** تعداد تاس دفاع بر اساس دسته‌ی رزمی مدافع. */
    public int defenseDiceCount(List<Combatant> defenders) {
        if (defenders.isEmpty()) return 0;

        boolean hasRegular = defenders.stream()
                .anyMatch(c -> c.getCombatCategory() == CombatCategory.REGULAR);
        if (hasRegular) {
            return Math.max(CombatCategory.REGULAR.getDefenseDice(), distinctTypes(defenders).size());
        }
        boolean hasBarbarian = defenders.stream()
                .anyMatch(c -> c.getCombatCategory() == CombatCategory.BARBARIAN);
        return hasBarbarian
                ? CombatCategory.BARBARIAN.getDefenseDice()
                : CombatCategory.WILD_ANIMAL.getDefenseDice();
    }

    private Set<UnitType> distinctTypes(List<Combatant> list) {
        Set<UnitType> types = new LinkedHashSet<>();
        for (Combatant c : list) if (c.isAlive()) types.add(c.getType());
        return types;
    }

    private void applyHits(GameState state, List<Combatant> side, int hits,
                           CombatResult result, boolean defenderSide) {
        if (hits <= 0) return;
        List<Combatant> killed = new ArrayList<>();
        DamageResolver.standardChain().apply(side, hits, killed);

        for (Combatant dead : killed) {
            String name = dead.getDisplayName();
            if (defenderSide) result.addDefenderCasualty(name); else result.addAttackerCasualty(name);
            if (dead.getOwner() != Owner.PLAYER) state.notifyEnemyDefeated(dead.getPosition());
        }
    }

    private void spendAttackAp(List<Combatant> attackers) {
        for (Combatant c : attackers) c.spendAP(GameConfig.ATTACK_AP_COST);
    }

    // -------------------------------------------------------- حمله به سازه
    /** حمله به سازه بدون تاس؛ فقط وقتی مدافع زنده‌ای روی هکس نمانده باشد. */
    public CombatResult attackStructureAt(GameState state, HexCell from, HexCell to,
                                          List<Combatant> attackers) {
        CombatResult result = new CombatResult(describe(attackers), "سازه");

        Structure target = targetStructure(state, to);
        if (target == null) {
            result.setNote("روی هکس هدف سازه‌ای برای حمله نیست");
            return result;
        }
        if (!hostileCombatants(state, to, Owner.PLAYER).isEmpty()) {
            result.setNote("تا وقتی مدافع روی هکس هست، سازه آسیب نمی‌بیند");
            return result;
        }

        int damage = 0;
        for (Combatant c : attackers) damage += c.getStructureDamage();
        if (damage <= 0) {
            result.setNote("هیچ‌کدام از یونیت‌های شما به سازه آسیب نمی‌زنند");
            return result;
        }

        target.takeDamage(damage);
        boolean destroyed = target.isDestroyed();
        result.setStructureOutcome(target.getDisplayName(), damage, destroyed);
        result.setNote("HP باقی‌مانده: " + target.getHp() + "/" + target.getMaxHp());

        if (destroyed) onStructureDestroyed(state, to, target);
        spendAttackAp(attackers);
        return result;
    }

    /** حمله‌ی مستقیم به دیوار روی یال بین دو هکس. */
    public CombatResult attackWall(GameState state, HexCell from, HexCell to) {
        int distance = state.getMap().distance(from, to);
        List<Combatant> attackers = eligibleAttackers(state, from, distance, Owner.PLAYER);
        CombatResult result = new CombatResult(describe(attackers), "دیوار");

        Wall wall = state.getMap().getWall(from, to);
        if (wall == null) {
            result.setNote("روی این یال دیواری وجود ندارد");
            return result;
        }
        if (attackers.isEmpty()) {
            result.setNote("یونیت آماده‌ای برای حمله ندارید");
            return result;
        }

        int damage = 0;
        for (Combatant c : attackers) damage += c.getStructureDamage();
        wall.takeDamage(damage);
        boolean destroyed = wall.isDestroyed();
        result.setStructureOutcome("دیوار", damage, destroyed);
        result.setNote("HP دیوار: " + wall.getHp() + "/" + wall.getMaxHp());

        if (destroyed) {
            var edge = state.getMap().getEdge(from, to);
            if (edge != null) edge.removeWall();
            state.getMap().removeEdgeIfEmpty(from, to);
        }
        spendAttackAp(attackers);
        return result;
    }

    private void onStructureDestroyed(GameState state, HexCell cell, Structure target) {
        if (target instanceof Building building) {
            state.destroyBuilding(building);
            state.notify(NotificationKind.COMBAT,
                    building.getType().getLabel() + " نابود شد");
        }
    }

    /** سازه‌ی قابل حمله روی یک هکس (ساختمان یا اردوگاه قبیله). */
    public Structure targetStructure(GameState state, HexCell cell) {
        if (cell == null) return null;
        var tribe = state.tribeAt(cell);
        if (tribe != null && !tribe.isDefeated()) return tribe.getCamp();
        return cell.getBuilding();
    }

    // ---------------------------------------------------------------- کمکی‌ها
    /**
     * یونیت‌های مهاجم مجاز. در فاصله ۱ همه‌ی انواع و در فاصله ۲ فقط کماندار
     * و فقط یک تاس حمله.
     */
    public List<Combatant> eligibleAttackers(GameState state, HexCell from,
                                             int distance, Owner owner) {
        List<Combatant> list = new ArrayList<>();
        if (from == null) return list;

        for (Unit u : from.getUnits()) {
            if (!u.isAlive() || u.getOwner() != owner) continue;
            if (!(u instanceof Combatant c)) continue;
            if (!c.hasAP(GameConfig.ATTACK_AP_COST)) continue;
            if (c.getAttackRange() < distance) continue;
            list.add(c);
        }

        if (distance == 2) {
            list.removeIf(c -> c.getType() != UnitType.ARCHER);
            if (list.size() > 1) return new ArrayList<>(List.of(list.getFirst()));
        }
        return list;
    }

    /** جنگنده‌های دشمن روی یک هکس. */
    public List<Combatant> hostileCombatants(GameState state, HexCell cell, Owner viewer) {
        List<Combatant> list = new ArrayList<>();
        if (cell == null) return list;
        for (Unit u : cell.getUnits()) {
            if (!u.isAlive() || u.getOwner() == viewer) continue;
            if (u instanceof Combatant c) list.add(c);
        }
        return list;
    }

    /** جنگنده‌های خودی روی یک هکس؛ برای دفاع. */
    public List<Combatant> friendlyCombatants(HexCell cell, Owner owner) {
        List<Combatant> list = new ArrayList<>();
        if (cell == null) return list;
        for (Unit u : cell.getUnits()) {
            if (!u.isAlive() || u.getOwner() != owner) continue;
            if (u instanceof Combatant c) list.add(c);
        }
        return list;
    }

    /** امتیاز دیوار برای مدافع؛ فقط وقتی حمله از یال دارای دیوار انجام شود. */
    public int wallBonusBetween(GameState state, HexCell from, HexCell to) {
        if (!state.getMap().areNeighbors(from, to)) return 0;
        Wall wall = state.getMap().getWall(from, to);
        return wall == null ? 0 : GameConfig.WALL_DEFENSE_BONUS;
    }

    private String describe(List<Combatant> list) {
        if (list.isEmpty()) return "—";
        StringBuilder sb = new StringBuilder();
        for (UnitType t : distinctTypes(list)) {
            long count = list.stream().filter(c -> c.getType() == t && c.isAlive()).count();
            if (sb.length() > 0) sb.append(" + ");
            sb.append(count).append(" ").append(t.getLabel());
        }
        return sb.toString();
    }
}
