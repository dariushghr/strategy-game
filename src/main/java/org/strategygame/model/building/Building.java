package org.strategygame.model.building;

import org.strategygame.model.combat.Structure;
import org.strategygame.model.map.HexCell;
import org.strategygame.model.resource.ResourceType;
import org.strategygame.model.unit.Worker;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class Building implements Structure {

    private final String       id;
    private final BuildingType type;
    private HexCell            location;
    private final List<Worker> workers = new ArrayList<>();
    private final int          maxWorkers;

    private boolean functional   = true;
    private int     missedUpkeep = 0;

    private int maxHp;
    private int hp;
    private int defense = 0;

    /** نوبت‌های باقی‌مانده‌ی از کار افتادن موقت (مثلا بعد از سیل). */
    private int disabledTurnsLeft = 0;

    protected Building(BuildingType type) {
        this.id         = UUID.randomUUID().toString();
        this.type       = type;
        this.maxWorkers = type.getMaxWorkers();
        this.maxHp      = type.getMaxHp();
        this.hp         = this.maxHp;
    }

    public String  getId()          { return id; }
    public HexCell getLocation()    { return location; }
    public int getMaxWorkers()      { return maxWorkers; }
    public List<Worker> getWorkers(){ return workers; }
    public int workerCount()        { return workers.size(); }

    public BuildingType getType()          { return type; }
    public ResourceType getResourceType()  { return type.getYieldType(); }

    public void setLocation(HexCell c) { this.location = c; }

    // ----------------------------------------------------------- کار و نگهداری
    /** سازه وقتی کار می‌کند که خراب نشده و موقتا از کار نیفتاده باشد. */
    public boolean isFunctional()   { return functional && disabledTurnsLeft <= 0; }
    public boolean isBroken()       { return !functional; }
    public boolean isTemporarilyDisabled() { return functional && disabledTurnsLeft > 0; }
    public int getDisabledTurnsLeft(){ return disabledTurnsLeft; }

    public void disableFor(int turns) {
        this.disabledTurnsLeft = Math.max(this.disabledTurnsLeft, turns);
    }

    /** در پایان هر نوبت صدا زده می‌شود تا اثرهای موقت کم شوند. */
    public void tickTemporaryEffects() {
        if (disabledTurnsLeft > 0) disabledTurnsLeft--;
    }

    public boolean canTakeWorker()  { return isFunctional() && workers.size() < maxWorkers; }

    public void addWorker(Worker w)    { if (canTakeWorker()) workers.add(w); }
    public void removeWorker(Worker w) { workers.remove(w); }

    /** آزاد کردن همه‌ی کارگرها؛ در تخریب و نابودی سازه لازم است. */
    public void releaseAllWorkers() {
        for (Worker w : new ArrayList<>(workers)) w.unstation();
        workers.clear();
    }

    // ------------------------------------------------- محدودیت معامله در هر نوبت
    private int lastTradeTurn = -1;

    /** آیا در این نوبت با این سازه معامله انجام شده است. */
    public boolean hasTradedOn(int turn) { return lastTradeTurn == turn; }
    public void markTraded(int turn)     { this.lastTradeTurn = turn; }

    public void payUpkeep()  { missedUpkeep = 0; }
    public void missUpkeep() { if (++missedUpkeep >= 3) functional = false; }
    public int getMissedUpkeep() { return missedUpkeep; }

    // -------------------------------------------------------------- HP و حمله
    @Override public int getHp()    { return hp; }
    @Override public int getMaxHp() { return maxHp; }
    @Override public int getDefense(){ return defense; }
    @Override public boolean isDemolishable() { return type.isDemolishable(); }
    @Override public String getDisplayName()  { return type.getLabel(); }

    @Override public void takeDamage(int amount) {
        if (amount <= 0) return;
        hp = Math.max(0, hp - amount);
    }

    public void heal(int amount) { hp = Math.min(maxHp, hp + Math.max(0, amount)); }

    public void setDefense(int v) { this.defense = v; }

    protected void setMaxHp(int v) {
        this.maxHp = Math.max(1, v);
        this.hp    = Math.min(hp, this.maxHp);
    }

    /**
     * حداقل HP این سازه؛ زلزله نمی‌تواند سازه‌هایی را که کف HP دارند
     * (مثل تان هال) کاملا نابود کند.
     */
    public int getMinimumHp() { return 0; }

    public abstract int produce(double multiplier);
}
