package org.strategygame.model.unit;

public enum UnitType {
    EXPLORER("اکسپلورر"),
    BUILDER("بیلدر"),
    WORKER("کارگر"),
    BORDER_EXPANDER("مرزگشا");

    private final String label;
    UnitType(String label) { this.label = label; }
    public String getLabel() { return label; }
}
