package org.strategygame.model.event;

import org.strategygame.model.season.Season;

public record TurnEndedEvent(int turn, Season season) { }
