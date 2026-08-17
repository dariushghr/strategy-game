package org.strategygame.model.unit;

import org.strategygame.config.GameConfig;

public class Swordsman extends MilitaryUnit {

    public Swordsman() {
        super(GameConfig.SWORDSMAN_AP, GameConfig.SWORDSMAN_HP,
              GameConfig.SWORDSMAN_BATTLE_HP, GameConfig.SWORDSMAN_RANGE,
              GameConfig.STRUCTURE_DAMAGE_SWORDSMAN);
    }

    @Override public UnitType getType() { return UnitType.SWORDSMAN; }
}
