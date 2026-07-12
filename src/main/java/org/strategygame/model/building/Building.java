package org.strategygame.model.building;

import org.strategygame.model.map.HexCell;
import org.strategygame.model.resource.ResourceType;
import org.strategygame.model.unit.Worker;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class Building {
    private final String id;
    private HexCell location;
    private final List<Worker> workers = new ArrayList<>();
    private final int maxWorkers;
    private boolean functional = true;
    private int missedUpkeep = 0;

    protected Building(int maxWorkers) {
        this.id         = UUID.randomUUID().toString();
        this.maxWorkers = maxWorkers;
    }

    public String  getId()          { return id; }
    public HexCell getLocation()    { return location; }
    public boolean isFunctional()   { return functional; }
    public int getMaxWorkers()      { return maxWorkers; }
    public List<Worker> getWorkers(){ return workers; }
    public int workerCount()        { return workers.size(); }
    public boolean canTakeWorker()  { return functional && workers.size() < maxWorkers; }

    public void setLocation(HexCell c) { this.location = c; }

    public void addWorker(Worker w)    { if (canTakeWorker()) workers.add(w); }
    public void removeWorker(Worker w) { workers.remove(w); }

    public void payUpkeep()  { missedUpkeep = 0; }
    public void missUpkeep() { if (++missedUpkeep >= 3) functional = false; }
    public int getMissedUpkeep() { return missedUpkeep; }

    public abstract int produce(double multiplier);
    public abstract ResourceType getResourceType();
    public abstract BuildingType getType();
}
