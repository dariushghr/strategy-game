package org.strategygame.model.event;

import org.strategygame.save.SaveSlot;

public record SaveCompletedEvent(SaveSlot slot, boolean autosave, boolean success, String message) { }
