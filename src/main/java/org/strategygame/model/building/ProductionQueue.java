package org.strategygame.model.building;

public class ProductionQueue {
    private Object item   = null;
    private int turnsLeft = 0;

    public boolean isEmpty()        { return item == null; }
    public Object  getItem()        { return item; }
    public int     getTurnsLeft()   { return turnsLeft; }

    public void start(Object item, int turns) {
        this.item      = item;
        this.turnsLeft = turns;
    }

    public boolean advance() {
        if (item == null) return false;
        return --turnsLeft <= 0;
    }

    public Object take() {
        Object done = item;
        item      = null;
        turnsLeft = 0;
        return done;
    }
}
