package org.strategygame.model.event;

import org.strategygame.model.combat.CombatResult;

public record CombatResolvedEvent(CombatResult result) { }
