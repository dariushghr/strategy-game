package org.strategygame.model.command;

import org.strategygame.model.GameState;
import org.strategygame.model.tech.Technology;

/** تحقیق یک تکنولوژی فاز دو در تان هال. */
public class ResearchTechnologyCommand extends TownHallCommand {

    private final Technology technology;

    public ResearchTechnologyCommand(Technology technology) {
        super(technology.getTurns());
        this.technology = technology;
    }

    public Technology getTechnology() { return technology; }

    @Override public String getLabel() { return "تحقیق " + technology.getLabel(); }
    @Override public int[]  getCost()  { return technology.getCost(); }

    @Override public String getResultText() { return technology.getEffectText(); }

    @Override
    public String validate(GameState state) {
        if (state.hasTechnology(technology))
            return technology.getLabel() + " قبلا تحقیق شده است";

        int level = state.getTownHall().getLevel();
        if (level < technology.getRequiredTownHallLevel())
            return technology.getLabel() + " به تان هال سطح "
                    + technology.getRequiredTownHallLevel() + " نیاز دارد (سطح فعلی: " + level + ")";
        return null;
    }

    @Override
    protected void applyEffect(GameState state) {
        state.applyTechnology(technology);
    }
}
