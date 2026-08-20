package org.strategygame.model.map;

import org.strategygame.model.resource.ResourceType;

public class ResourceDeposit {
    private final ResourceType type;
    private int remaining;
    private final int max;

    public ResourceDeposit(ResourceType type, int amount) {
        this.type      = type;
        this.max       = amount;
        this.remaining = amount;
    }

    public ResourceType getType()  { return type; }
    public int getRemaining()      { return remaining; }
    public int getMax()            { return max; }
    public boolean isDepleted()    { return remaining <= 0; }

    public void restoreRemaining(int remaining) {
        this.remaining = Math.max(0, Math.min(max, remaining));
    }

    public int extract(int requested) {
        int actual = Math.min(requested, remaining);
        remaining -= actual;
        return actual;
    }
}
