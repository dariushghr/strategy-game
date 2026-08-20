package org.strategygame.model.event;

import org.strategygame.model.disaster.DisasterEvent;

public record DisasterStartedEvent(DisasterEvent event) { }
