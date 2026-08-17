package org.strategygame.service;

import org.strategygame.config.GameConfig;
import org.strategygame.model.GameState;
import org.strategygame.model.map.HexCell;
import org.strategygame.model.map.HexMap;
import org.strategygame.model.map.TerrainType;
import org.strategygame.model.unit.Unit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * قواعد حرکت. هزینه‌ی هر قدم اینجا و فقط اینجا محاسبه می‌شود: زمین، جاده،
 * رودخانه‌ی روی یال، فصل و دریانوردی همه در همین یک تابع جمع شده‌اند.
 */
public class MovementService {

    /** هزینه‌ای که یعنی «این قدم ممکن نیست». */
    public static final int IMPOSSIBLE = -1;

    /** دلیل غیرمجاز بودن یک قدم؛ {@code null} یعنی مجاز است. */
    public String stepProblem(GameState state, Unit unit, HexCell from, HexCell to) {
        if (unit == null || from == null || to == null) return "انتخاب نامعتبر است";
        if (!state.getMap().areNeighbors(from, to))     return "این هکس مجاور نیست";
        if (to.getTerrain() == TerrainType.MOUNTAIN_RANGE)
            return "رشته‌کوه قابل عبور نیست";
        if (to.isBlocked())
            return "این هکس مسدود شده است";
        if (to.isWater() && !state.isSailingUnlocked())
            return "ورود به دریا به تکنولوژی دریانوردی نیاز دارد";

        int cost = stepCost(state, unit, from, to);
        if (cost == IMPOSSIBLE) return "این مسیر قابل عبور نیست";
        if (!unit.hasAP(cost))
            return "AP کافی نیست (نیاز: " + cost + "، موجود: " + unit.getCurrentAP() + ")";
        return null;
    }

    /** هزینه‌ی حرکت از یک هکس به هکس مجاور. */
    public int stepCost(GameState state, Unit unit, HexCell from, HexCell to) {
        if (from == null || to == null) return IMPOSSIBLE;
        if (to.getTerrain() == TerrainType.MOUNTAIN_RANGE) return IMPOSSIBLE;
        if (to.isBlocked()) return IMPOSSIBLE;
        if (to.isWater() && !state.isSailingUnlocked()) return IMPOSSIBLE;

        HexMap map = state.getMap();
        int cost;

        boolean roadToRoad = from.hasRoad() && to.hasRoad();
        if (roadToRoad) {
            cost = GameConfig.ROAD_MOVEMENT_COST;
        } else {
            cost = to.getTerrain().getMovementCost();
        }

        if (map.hasRiver(from, to)) {
            cost += roadToRoad
                    ? GameConfig.RIVER_CROSSING_COST_WITH_ROAD
                    : GameConfig.RIVER_CROSSING_COST;
        }

        if (to.isLand() && !roadToRoad) {
            cost += state.getSeason().landMovementExtraCost();
        }

        return Math.max(1, cost);
    }

    /**
     * انجام یک قدم. ورود به دریا AP همان نوبت را صفر می‌کند.
     * @return {@code null} در صورت موفقیت، وگرنه دلیل خطا.
     */
    public String step(GameState state, Unit unit, HexCell to) {
        HexCell from = unit.getPosition();
        String problem = stepProblem(state, unit, from, to);
        if (problem != null) return problem;

        int cost = stepCost(state, unit, from, to);
        unit.spendAP(cost);
        from.removeUnit(unit);
        to.addUnit(unit);
        unit.setPosition(to);

        if (to.isWater()) unit.clearAP();

        state.getFog().update(state.getUnits(), state.getBuildings());
        return null;
    }

    /** هکس‌هایی که با AP فعلی قابل رسیدن هستند. */
    public List<HexCell> reachable(GameState state, Unit unit) {
        if (unit == null || unit.getPosition() == null || unit.getCurrentAP() <= 0)
            return List.of();

        HexMap map = state.getMap();
        HexCell start = unit.getPosition();

        Map<HexCell, Integer> bestCost = new HashMap<>();
        bestCost.put(start, 0);

        PriorityQueue<HexCell> frontier =
                new PriorityQueue<>(Comparator.comparingInt(c -> bestCost.getOrDefault(c, Integer.MAX_VALUE)));
        frontier.add(start);

        Set<HexCell> result = new LinkedHashSet<>();

        while (!frontier.isEmpty()) {
            HexCell current = frontier.poll();
            int currentCost = bestCost.getOrDefault(current, Integer.MAX_VALUE);

            for (HexCell next : map.getNeighbors(current)) {
                int stepCost = stepCost(state, unit, current, next);
                if (stepCost == IMPOSSIBLE) continue;

                int newCost = currentCost + stepCost;
                if (newCost > unit.getCurrentAP()) continue;

                Integer known = bestCost.get(next);
                if (known == null || newCost < known) {
                    bestCost.put(next, newCost);
                    frontier.add(next);
                    result.add(next);
                }
            }
        }
        return new ArrayList<>(result);
    }

    /** کوتاه‌ترین مسیر تا مقصد که با AP فعلی قابل طی شدن باشد. */
    public List<HexCell> path(GameState state, Unit unit, HexCell dest) {
        if (unit == null || dest == null || unit.getPosition() == null) return List.of();
        HexCell start = unit.getPosition();
        if (start.equals(dest)) return List.of();

        HexMap map = state.getMap();
        Map<HexCell, Integer> bestCost = new HashMap<>();
        Map<HexCell, HexCell> parent   = new HashMap<>();
        bestCost.put(start, 0);

        PriorityQueue<HexCell> frontier =
                new PriorityQueue<>(Comparator.comparingInt(c -> bestCost.getOrDefault(c, Integer.MAX_VALUE)));
        frontier.add(start);

        while (!frontier.isEmpty()) {
            HexCell current = frontier.poll();
            if (current.equals(dest)) break;
            int currentCost = bestCost.getOrDefault(current, Integer.MAX_VALUE);

            for (HexCell next : map.getNeighbors(current)) {
                int stepCost = stepCost(state, unit, current, next);
                if (stepCost == IMPOSSIBLE) continue;

                int newCost = currentCost + stepCost;
                Integer known = bestCost.get(next);
                if (known == null || newCost < known) {
                    bestCost.put(next, newCost);
                    parent.put(next, current);
                    frontier.add(next);
                }
            }
        }

        Integer total = bestCost.get(dest);
        if (total == null || total > unit.getCurrentAP()) return List.of();

        LinkedList<HexCell> result = new LinkedList<>();
        HexCell cur = dest;
        while (!cur.equals(start)) {
            result.addFirst(cur);
            cur = parent.get(cur);
            if (cur == null) return List.of();
        }
        return result;
    }
}
