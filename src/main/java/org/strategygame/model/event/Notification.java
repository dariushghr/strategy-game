package org.strategygame.model.event;

/** یک اعلان بازی. مستقل از UI ساخته می‌شود و نمایش آن وظیفه‌ی View است. */
public record Notification(int turn, NotificationKind kind, String message) {

    public String display() { return "[نوبت " + turn + "] " + kind.getLabel() + ": " + message; }
}
