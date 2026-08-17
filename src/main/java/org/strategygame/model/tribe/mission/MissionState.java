package org.strategygame.model.tribe.mission;

/** ماشین حالت مأموریت قبیله. */
public enum MissionState {

    AVAILABLE("قابل دریافت"),
    ACTIVE("در جریان"),
    READY_TO_TURN_IN("آماده تحویل"),
    COMPLETED("انجام‌شده"),
    FAILED("شکست‌خورده"),
    CANCELLED("لغوشده");

    private final String label;
    MissionState(String label) { this.label = label; }
    public String getLabel() { return label; }

    public boolean isFinished() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }
}
