package org.strategygame.model.command;

import org.strategygame.model.GameState;
import org.strategygame.model.building.TownHallLevel;

/** ارتقای سطح تان هال؛ بعد از گذشتن نوبت‌های لازم اعمال می‌شود. */
public class UpgradeTownHallCommand extends TownHallCommand {

    private final TownHallLevel target;

    public UpgradeTownHallCommand(TownHallLevel target) {
        super(target.getUpgradeTurns());
        this.target = target;
    }

    public TownHallLevel getTarget() { return target; }

    @Override public String getLabel() { return "ارتقا به " + target.getLabel(); }
    @Override public int[]  getCost()  { return target.getUpgradeCost(); }

    @Override public String getResultText() { return target.bonusText(); }

    @Override
    public String validate(GameState state) {
        if (!state.getTownHall().canLevelUp())
            return "تان هال در بالاترین سطح است";
        if (state.getTownHall().getNextLevel() != target)
            return "این ارتقا سطح بعدی تان هال نیست";
        return null;
    }

    @Override
    protected void applyEffect(GameState state) {
        state.applyTownHallUpgrade();
    }
}
