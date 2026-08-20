package org.strategygame.model.happiness;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * شادی امپراتوری. مقدار آن انباشته است و هر نوبت از صفر محاسبه نمی‌شود؛
 * رویدادهای «یک‌بار مصرف» با کلید ثبت می‌شوند تا دوباره اعمال نشوند.
 */
public class HappinessState {

    private int            value = 0;
    private HappinessLevel level = HappinessLevel.NORMAL;

    private final Set<String>  firedEvents = new HashSet<>();
    private final List<String>  history    = new ArrayList<>();

    public Set<String> getFiredEvents() { return firedEvents; }

    public void restore(int value, Set<String> fired, List<String> history) {
        this.value = value;
        this.level = HappinessLevel.of(value);
        this.firedEvents.clear();
        if (fired != null) this.firedEvents.addAll(fired);
        this.history.clear();
        if (history != null) this.history.addAll(history);
    }

    public int getValue()              { return value; }
    public HappinessLevel getLevel()   { return level; }
    public List<String> getHistory()   { return history; }

    /**
     * تغییر مقدار شادی.
     * @return سطح جدید اگر سطح عوض شده باشد، در غیر این صورت {@code null}.
     */
    public HappinessLevel change(int delta, String reason) {
        if (delta == 0) return null;
        value += delta;
        history.add((delta > 0 ? "+" : "") + delta + " : " + reason);

        HappinessLevel next = HappinessLevel.of(value);
        if (next == level) return null;
        level = next;
        return level;
    }

    /**
     * رویدادی که فقط یک بار در کل بازی اثر می‌گذارد (مثلا اولین بار رسیدن
     * به سقف یونیت).
     * @return سطح جدید اگر عوض شد، در غیر این صورت {@code null}.
     */
    public HappinessLevel changeOnce(String key, int delta, String reason) {
        if (firedEvents.contains(key)) return null;
        firedEvents.add(key);
        return change(delta, reason);
    }

    public boolean hasFired(String key) { return firedEvents.contains(key); }

    /** برای رویدادهایی که با تغییر شرط باید بتوانند دوباره فعال شوند. */
    public void resetEvent(String key) { firedEvents.remove(key); }

    public double productionMultiplier() { return level.productionMultiplier(); }
    public int workerPenalty()           { return level.workerPenalty(); }
    public int apPenalty()               { return level.apPenalty(); }

    public String statusText() {
        return level.getLabel() + " (" + (value > 0 ? "+" : "") + value + ") — "
                + level.getEffectText();
    }
}
