package org.strategygame.controller;

import org.strategygame.common.ActionResult;
import org.strategygame.model.GameState;
import org.strategygame.model.building.ProductionQueue;
import org.strategygame.model.building.TownHall;
import org.strategygame.model.building.TownHallLevel;
import org.strategygame.model.command.ResearchTechnologyCommand;
import org.strategygame.model.command.ResearchUpgradeCommand;
import org.strategygame.model.command.TownHallCommand;
import org.strategygame.model.command.TrainUnitCommand;
import org.strategygame.model.command.UpgradeTownHallCommand;
import org.strategygame.model.tech.Technology;
import org.strategygame.model.unit.UnitType;
import org.strategygame.model.upgrade.UpgradeType;

/**
 * صف تان هال. آموزش یونیت، تحقیق و ارتقای سطح همه از یک مسیر عبور می‌کنند
 * چون همه‌شان یک {@link TownHallCommand} هستند.
 */
public class BuildingController {

    private final GameState state;

    public BuildingController(GameState state) {
        this.state = state;
    }

    public ActionResult queueTrain(UnitType type)        { return enqueue(new TrainUnitCommand(type)); }
    public ActionResult queueUpgrade(UpgradeType type)   { return enqueue(new ResearchUpgradeCommand(type)); }
    public ActionResult queueTechnology(Technology tech) { return enqueue(new ResearchTechnologyCommand(tech)); }

    public ActionResult queueTownHallUpgrade() {
        TownHall townHall = state.getTownHall();
        if (townHall == null)      return ActionResult.fail("تان هال وجود ندارد");
        if (!townHall.canLevelUp()) return ActionResult.fail(
                "تان هال در بالاترین سطح (" + townHall.getMaxLevel() + ") است");

        TownHallLevel next = townHall.getNextLevel();
        return enqueue(new UpgradeTownHallCommand(next));
    }

    public String trainProblem(UnitType type)        { return problem(new TrainUnitCommand(type)); }
    public String upgradeProblem(UpgradeType type)   { return problem(new ResearchUpgradeCommand(type)); }
    public String technologyProblem(Technology tech) { return problem(new ResearchTechnologyCommand(tech)); }

    public String townHallUpgradeProblem() {
        TownHall townHall = state.getTownHall();
        if (townHall == null)       return "تان هال وجود ندارد";
        if (!townHall.canLevelUp()) return "تان هال در بالاترین سطح است";
        return problem(new UpgradeTownHallCommand(townHall.getNextLevel()));
    }

    /** لغو دستور فعال؛ طبق قواعد بازی منابع پرداخت‌شده برنمی‌گردند. */
    public ActionResult cancelQueue() {
        TownHall townHall = state.getTownHall();
        if (townHall == null) return ActionResult.fail("تان هال وجود ندارد");

        ProductionQueue queue = townHall.getQueue();
        if (queue.isEmpty()) return ActionResult.fail("صف تان هال خالی است");

        String label = queue.getLabel();
        queue.cancel();
        return ActionResult.ok("دستور «" + label + "» لغو شد.\n\nمنابع پرداخت‌شده برنمی‌گردد.");
    }

    // ---------------------------------------------------------------- مشترک
    /** دلیل غیرمجاز بودن شروع یک دستور: صف، شرط اختصاصی و منابع. */
    private String problem(TownHallCommand command) {
        TownHall townHall = state.getTownHall();
        if (townHall == null) return "تان هال وجود ندارد";

        ProductionQueue queue = townHall.getQueue();
        if (!queue.isEmpty())
            return "صف تان هال پر است: " + queue.getLabel()
                    + " (" + queue.getTurnsLeft() + " نوبت باقی مانده)";

        String specific = command.validate(state);
        if (specific != null) return specific;

        if (!state.getStorage().canAffordAll(command.getCost()))
            return "منابع کافی نیست (نیاز: " + command.costText() + ")";
        return null;
    }

    private ActionResult enqueue(TownHallCommand command) {
        String problem = problem(command);
        if (problem != null) return ActionResult.fail(problem);

        state.getStorage().deductAll(command.getCost());
        state.getTownHall().getQueue().start(command);

        return ActionResult.ok("«" + command.getLabel() + "» به صف تان هال اضافه شد.\n\n"
                + "هزینه‌ی پرداخت‌شده: " + command.costText() + "\n"
                + "مدت: " + command.getTotalTurns() + " نوبت\n"
                + "نتیجه: " + command.getResultText());
    }
}
