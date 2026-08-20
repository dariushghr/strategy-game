package org.strategygame.model.command;

import org.strategygame.model.GameState;
import org.strategygame.model.building.BuildingType;

/**
 * یک دستور تان هال (Command Pattern). تان هال در هر لحظه فقط یک دستور فعال دارد.
 * هر دستور می‌داند هزینه‌اش چیست، چند نوبت طول می‌کشد، چه زمانی کامل می‌شود،
 * نتیجه‌ی کامل شدنش چیست و لغو شدنش چه رفتاری دارد.
 */
public abstract class TownHallCommand {

    private final int totalTurns;
    private int       turnsLeft;
    private boolean   effectApplied = false;
    private boolean   cancelled     = false;

    protected TownHallCommand(int totalTurns) {
        this.totalTurns = Math.max(1, totalTurns);
        this.turnsLeft  = this.totalTurns;
    }

    /** نام دستور برای نمایش در UI. */
    public abstract String getLabel();

    /** هزینه‌ی دستور که در لحظه‌ی شروع کم می‌شود. */
    public abstract int[] getCost();

    /** توضیح نتیجه‌ی کامل شدن دستور. */
    public abstract String getResultText();

    /** دلیل غیرمجاز بودن شروع این دستور؛ {@code null} یعنی مجاز است. */
    public abstract String validate(GameState state);

    /** اثر واقعی دستور روی وضعیت بازی. فقط از {@link #complete} صدا زده می‌شود. */
    protected abstract void applyEffect(GameState state);

    public int     getTotalTurns()   { return totalTurns; }
    public int     getTurnsLeft()    { return turnsLeft; }
    public boolean isCancelled()     { return cancelled; }
    public boolean isEffectApplied() { return effectApplied; }
    public boolean isReady()         { return turnsLeft <= 0; }

    public String costText() { return BuildingType.formatCost(getCost()); }

    /** یک نوبت از زمان باقی‌مانده کم می‌کند و می‌گوید آماده‌ی اجرا شد یا نه. */
    public boolean tick() {
        if (cancelled || effectApplied) return false;
        if (turnsLeft > 0) turnsLeft--;
        return turnsLeft <= 0;
    }

    /** اثر دستور را دقیقا یک بار اجرا می‌کند. */
    public final boolean complete(GameState state) {
        if (cancelled || effectApplied) return false;
        effectApplied = true;
        applyEffect(state);
        return true;
    }

    /** لغو دستور. طبق قواعد بازی منابع پرداخت‌شده برنمی‌گردند. */
    public void cancel() {
        if (!effectApplied) cancelled = true;
    }

    /** بازگرداندن تایمر صف از فایل ذخیره. */
    public void restoreTurnsLeft(int left) {
        this.turnsLeft = Math.max(0, Math.min(totalTurns, left));
    }

    @Override public String toString() {
        return getLabel() + " (" + turnsLeft + "/" + totalTurns + " نوبت)";
    }
}
