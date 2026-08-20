package org.strategygame.model.disaster;

public enum DisasterType {

    EARTHQUAKE("زلزله"),
    FLOOD("سیل"),
    BEAR_ATTACK("حمله خرس");

    private final String label;
    DisasterType(String label) { this.label = label; }
    public String getLabel() { return label; }
}
