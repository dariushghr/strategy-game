package org.strategygame.model.event;

/** دسته‌بندی اعلان‌ها. */
public enum NotificationKind {

    WAR("جنگ"),
    TRIBE_ATTACK("حمله قبیله"),
    GUARD_SPAWNED("نگهبان جدید"),
    MISSION("مأموریت"),
    TRADE("تجارت"),
    RELATION("رابطه"),
    HAPPINESS("شادی"),
    DISASTER("بلای طبیعی"),
    SEASON("فصل"),
    PRODUCTION("تولید"),
    COMBAT("نبرد"),
    GENERAL("عمومی");

    private final String label;
    NotificationKind(String label) { this.label = label; }
    public String getLabel() { return label; }
}
