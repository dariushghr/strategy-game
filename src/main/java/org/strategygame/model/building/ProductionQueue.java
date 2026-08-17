package org.strategygame.model.building;

import org.strategygame.model.command.TownHallCommand;

/**
 * صف تولید تان هال. طبق قواعد بازی در هر لحظه فقط یک دستور فعال وجود دارد؛
 * تا وقتی دستور فعال تمام نشده، دستور دوم پذیرفته نمی‌شود.
 */
public class ProductionQueue {

    private TownHallCommand active;

    public boolean isEmpty()               { return active == null; }
    public TownHallCommand getActive()     { return active; }
    public int getTurnsLeft()              { return active == null ? 0 : active.getTurnsLeft(); }
    public String getLabel()               { return active == null ? "—" : active.getLabel(); }

    /** شروع دستور جدید؛ اگر دستور فعالی وجود دارد رد می‌شود. */
    public boolean start(TownHallCommand command) {
        if (active != null || command == null) return false;
        active = command;
        return true;
    }

    /** لغو دستور فعال. منابع پرداخت‌شده برنمی‌گردند. */
    public boolean cancel() {
        if (active == null) return false;
        active.cancel();
        active = null;
        return true;
    }

    /**
     * یک نوبت جلو می‌برد. اگر تایمر صفر شده باشد دستور آماده را برمی‌گرداند
     * و صف را خالی می‌کند؛ در غیر این صورت {@code null}.
     */
    public TownHallCommand advanceTurn() {
        if (active == null) return null;
        if (!active.tick()) return null;
        TownHallCommand done = active;
        active = null;
        return done;
    }
}
