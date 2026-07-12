package org.strategygame.model.resource;

public enum ResourceType {
    FOOD("غذا"),
    WOOD("چوب"),
    STONE("سنگ"),
    IRON("آهن");

    private final String label;

    ResourceType(String label) { this.label = label; }

    public String getLabel() { return label; }
}
