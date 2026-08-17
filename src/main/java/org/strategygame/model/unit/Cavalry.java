package org.strategygame.model.unit;

import org.strategygame.config.GameConfig;

public class Cavalry extends MilitaryUnit {

    public Cavalry() {
        super(GameConfig.CAVALRY_AP, GameConfig.CAVALRY_HP,
              GameConfig.CAVALRY_BATTLE_HP, GameConfig.CAVALRY_RANGE,
              GameConfig.STRUCTURE_DAMAGE_CAVALRY);
    }

    @Override public UnitType getType() { return UnitType.CAVALRY; }
}
