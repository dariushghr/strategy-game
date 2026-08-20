package org.strategygame.model.event;

import java.util.ArrayList;
import java.util.List;

/** جمع‌کننده‌ی اعلان‌های بازی. View فقط از اینجا می‌خواند. */
public class NotificationCenter {

    private static final int MAX_HISTORY = 200;

    private final List<Notification> history = new ArrayList<>();
    private final List<Notification> pending = new ArrayList<>();

    public void add(int turn, NotificationKind kind, String message) {
        Notification n = new Notification(turn, kind, message);
        history.add(n);
        pending.add(n);
        if (history.size() > MAX_HISTORY) history.removeFirst();
    }

    /** بازگرداندن تاریخچه بدون صف خوانده‌نشده؛ بارگذاری نباید اعلان‌های قدیمی را دوباره نشان دهد. */
    public void restoreHistory(List<Notification> saved) {
        history.clear();
        pending.clear();
        if (saved == null) return;
        history.addAll(saved);
        while (history.size() > MAX_HISTORY) history.removeFirst();
    }

    public List<Notification> getHistory() { return history; }

    public boolean hasPending() { return !pending.isEmpty(); }

    /** تعداد اعلان‌های خوانده‌نشده، برای نشان روی دکمه‌ی گزارش. */
    public int pendingCount() { return pending.size(); }

    /** اعلان‌های نمایش‌داده‌نشده را برمی‌گرداند و صف را خالی می‌کند. */
    public List<Notification> drainPending() {
        List<Notification> copy = new ArrayList<>(pending);
        pending.clear();
        return copy;
    }

    /** آخرین اعلان‌ها برای نمایش در پنل. */
    public List<Notification> latest(int count) {
        int from = Math.max(0, history.size() - count);
        return new ArrayList<>(history.subList(from, history.size()));
    }
}
