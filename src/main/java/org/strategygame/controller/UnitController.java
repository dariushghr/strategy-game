package org.strategygame.controller;

import org.strategygame.common.ActionResult;
import org.strategygame.model.GameState;
import org.strategygame.model.building.Building;
import org.strategygame.model.building.BuildingType;
import org.strategygame.model.map.HexCell;
import org.strategygame.model.unit.BorderExpander;
import org.strategygame.model.unit.Builder;
import org.strategygame.model.unit.Unit;
import org.strategygame.model.unit.Worker;

import java.util.List;

/** اکشن‌های یونیت: حرکت، استقرار کارگر، توسعه مرز و ساخت‌وساز. */
public class UnitController {

    private static final int STATION_AP = 1;

    private final GameState    state;
    private final GameServices services;

    public UnitController(GameState state, GameServices services) {
        this.state    = state;
        this.services = services;
    }

    // ----------------------------------------------------------------- حرکت
    public List<HexCell> reachable(Unit unit) {
        return services.movement().reachable(state, unit);
    }

    public List<HexCell> path(Unit unit, HexCell dest) {
        return services.movement().path(state, unit, dest);
    }

    public String stepProblem(Unit unit, HexCell to) {
        return unit == null
                ? "یونیتی انتخاب نشده است"
                : services.movement().stepProblem(state, unit, unit.getPosition(), to);
    }

    public boolean stepOnce(Unit unit, HexCell next) {
        return services.movement().step(state, unit, next) == null;
    }

    public int stepCost(Unit unit, HexCell to) {
        return unit == null ? -1 : services.movement().stepCost(state, unit, unit.getPosition(), to);
    }

    // ---------------------------------------------------------------- کارگر
    public boolean station(Worker worker, Building building) {
        if (worker == null || building == null || worker.isStationed())    return false;
        if (worker.getPosition() == null
                || !worker.getPosition().equals(building.getLocation()))   return false;
        if (!building.canTakeWorker() || !worker.hasAP(STATION_AP))        return false;

        worker.station(building, STATION_AP);
        return true;
    }

    public boolean unstation(Worker worker) {
        if (worker == null || !worker.isStationed()) return false;
        worker.unstation();
        return true;
    }

    // -------------------------------------------------------------- مرزگشا
    public boolean expand(BorderExpander expander, HexCell cell) {
        if (expander == null || cell == null) return false;
        if (!state.getFog().isExplored(cell.getQ(), cell.getR())) return false;

        cell.setInBorder(true);
        for (HexCell n : state.getMap().getNeighbors(cell)) {
            if (!state.getFog().isExplored(n.getQ(), n.getR())) continue;
            if (state.tribeAt(n) != null) continue;
            n.setInBorder(true);
        }

        expander.consume();
        state.removeUnit(expander);
        return true;
    }

    // ------------------------------------------------------------ ساخت‌وساز
    public String buildProblem(Builder builder, BuildingType type, HexCell cell) {
        return services.build().buildProblem(state, builder, type, cell);
    }

    public ActionResult build(Builder builder, BuildingType type, HexCell cell) {
        ActionResult result = services.build().build(state, builder, type, cell);
        if (result.success() && type == BuildingType.SETTLEMENT && cell.hasBuilding()) {
            services.happiness().onSettlementBuilt(state, cell.getBuilding());
        }
        return result;
    }

    public String roadProblem(Builder builder, HexCell cell) {
        return services.build().roadProblem(state, builder, cell);
    }

    public ActionResult buildRoad(Builder builder, HexCell cell) {
        return services.build().buildRoad(state, builder, cell);
    }

    public String wallProblem(Builder builder, HexCell a, HexCell b) {
        return services.build().wallProblem(state, builder, a, b);
    }

    public ActionResult buildWall(Builder builder, HexCell a, HexCell b) {
        return services.build().buildWall(state, builder, a, b);
    }

    public String demolishProblem(Builder builder, HexCell cell) {
        return services.build().demolishProblem(state, builder, cell);
    }

    public ActionResult demolish(Builder builder, HexCell cell) {
        return services.build().demolish(state, builder, cell);
    }

    public BuildingType buildableTypeAt(HexCell cell) {
        return services.build().buildableTypeAt(state, cell);
    }

    public boolean isTechUnlocked(BuildingType type) {
        return services.build().isTechUnlocked(state, type);
    }

    public int[] effectiveCost(BuildingType type) {
        return services.build().effectiveCost(state, type);
    }
}
