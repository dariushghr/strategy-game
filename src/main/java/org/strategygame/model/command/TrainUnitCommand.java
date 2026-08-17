package org.strategygame.model.command;

import org.strategygame.model.GameState;
import org.strategygame.model.unit.UnitType;

/** آموزش یک یونیت در تان هال. */
public class TrainUnitCommand extends TownHallCommand {

    private final UnitType type;

    public TrainUnitCommand(UnitType type) {
        super(type.getTrainTurns());
        this.type = type;
    }

    public UnitType getUnitType() { return type; }

    @Override public String getLabel()  { return "آموزش " + type.getLabel(); }
    @Override public int[]  getCost()   { return type.getCost(); }

    @Override public String getResultText() {
        return "یک «" + type.getLabel() + "» کنار تان هال ساخته می‌شود";
    }

    @Override
    public String validate(GameState state) {
        int level = state.getTownHall().getLevel();
        if (level < type.getRequiredTownHallLevel())
            return type.getLabel() + " به تان هال سطح " + type.getRequiredTownHallLevel()
                    + " نیاز دارد (سطح فعلی: " + level + ")";

        if (type.requiresStable() && !state.hasActiveStable())
            return type.getLabel() + " به یک طویله‌ی فعال نیاز دارد";

        if (type.isMilitary()) {
            if (!state.canSpawnMilitaryUnit())
                return "سقف یونیت نظامی پر است (" + state.militaryUnitCount()
                        + "/" + state.getMilitaryCap() + ")";
        } else if (!state.canSpawnUnit()) {
            return "سقف یونیت پر است (" + state.getUnits().size() + "/" + state.getUnitCap() + ")";
        }
        return null;
    }

    @Override
    protected void applyEffect(GameState state) {
        state.spawnTrainedUnit(type);
    }
}
