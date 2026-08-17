package org.strategygame.model.unit;

import org.strategygame.config.GameConfig;

public class Archer extends MilitaryUnit {

    public Archer() {
        super(GameConfig.ARCHER_AP, GameConfig.ARCHER_HP,
              GameConfig.ARCHER_BATTLE_HP, GameConfig.ARCHER_RANGE,
              GameConfig.STRUCTURE_DAMAGE_ARCHER);
    }

    @Override public UnitType getType() { return UnitType.ARCHER; }
}
