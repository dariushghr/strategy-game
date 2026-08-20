package org.strategygame.controller;

import org.strategygame.common.ActionResult;
import org.strategygame.model.GameState;
import org.strategygame.model.combat.CombatResult;
import org.strategygame.model.event.CombatResolvedEvent;
import org.strategygame.model.map.HexCell;
import org.strategygame.model.tribe.Tribe;
import org.strategygame.model.unit.Owner;

/** حمله‌ی بازیکن و پیامدهای سیاسی آن روی قبیله‌ها. */
public class CombatController {

    private final GameState    state;
    private final GameServices services;

    private CombatResult lastResult;

    public CombatController(GameState state, GameServices services) {
        this.state    = state;
        this.services = services;
    }

    public String attackProblem(HexCell from, HexCell to) {
        return services.combat().attackProblem(state, from, to);
    }

    /**
     * حمله از یک هکس به هکس دیگر. اگر هدف به قبیله‌ای تعلق داشته باشد، پیامد
     * سیاسی (شکستن اتحاد و جنگ) و فتح اردوگاه هم همین‌جا اعمال می‌شود.
     */
    public ActionResult attack(HexCell from, HexCell to) {
        String problem = attackProblem(from, to);
        if (problem != null) return ActionResult.fail(problem);

        Tribe defender = tribeDefending(to);
        CombatResult result = services.combat().attack(state, from, to);
        lastResult = result;
        services.events().publish(new CombatResolvedEvent(result));

        StringBuilder report = new StringBuilder(result.report());

        if (defender != null) {
            services.tribe().onPlayerAttackedTribe(state, defender);
            if (defender.getCamp().isDestroyed()) {
                ActionResult conquest = services.tribe().onCampDestroyed(state, defender);
                report.append('\n').append(conquest.message());
            }
        }
        state.getFog().update(state.getUnits(), state.getBuildings());
        return ActionResult.ok(report.toString());
    }

    /** حمله‌ی مستقیم به دیوار روی یال بین دو هکس. */
    public ActionResult attackWall(HexCell from, HexCell to) {
        if (state.getMap().getWall(from, to) == null)
            return ActionResult.fail("روی این یال دیواری وجود ندارد");

        CombatResult result = services.combat().attackWall(state, from, to);
        lastResult = result;
        services.events().publish(new CombatResolvedEvent(result));
        return ActionResult.ok(result.report());
    }

    public CombatResult lastResult() { return lastResult; }

    public boolean hasWallBetween(HexCell a, HexCell b) {
        return state.getMap().getWall(a, b) != null;
    }

    public boolean hasHostileTarget(HexCell cell) {
        if (cell == null) return false;
        if (!services.combat().hostileCombatants(state, cell, Owner.PLAYER).isEmpty()) return true;
        return services.combat().targetStructure(state, cell) != null;
    }

    /** قبیله‌ای که این هکس (اردوگاه یا یونیت‌هایش) به آن تعلق دارد. */
    private Tribe tribeDefending(HexCell cell) {
        Tribe camp = state.tribeAt(cell);
        if (camp != null) return camp;

        for (Tribe tribe : state.getTribes()) {
            if (tribe.isDefeated()) continue;
            for (var unit : tribe.aliveUnits()) {
                if (cell.equals(unit.getPosition())) return tribe;
            }
        }
        return null;
    }
}
