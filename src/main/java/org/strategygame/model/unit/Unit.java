package org.strategygame.model.unit;

import org.strategygame.config.GameConfig;
import org.strategygame.model.combat.Attackable;
import org.strategygame.model.map.HexCell;

import java.util.UUID;

public abstract class Unit implements Attackable {

    private String id;
    private HexCell position;
    private int currentAP;
    private final int maxAP;
    private final int visionRadius;
    private boolean alive = true;

    private final int maxHp;
    private int hp;

    private Owner owner = Owner.PLAYER;

    /** جریمه‌ی موقت AP (مثلا از شادی پایین یا فصل زمستان). */
    private int apPenalty = 0;

    protected Unit(int maxAP, int visionRadius) {
        this(maxAP, visionRadius, GameConfig.CIVILIAN_HP);
    }

    protected Unit(int maxAP, int visionRadius, int maxHp) {
        this.id           = UUID.randomUUID().toString();
        this.maxAP        = maxAP;
        this.visionRadius = visionRadius;
        this.currentAP    = maxAP;
        this.maxHp        = maxHp;
        this.hp           = maxHp;
    }

    public String getId()          { return id; }
    public HexCell getPosition()   { return position; }
    public int getCurrentAP()      { return currentAP; }
    public int getVisionRadius()   { return visionRadius; }
    public boolean isAlive()       { return alive; }
    public Owner getOwner()        { return owner; }

    /** حداکثر AP بعد از اعمال جریمه‌ها؛ هیچ‌جای دیگری عدد پراکنده کم نمی‌کند. */
    public int getMaxAP() { return Math.max(1, maxAP - apPenalty); }

    /** حداکثر AP پایه بدون جریمه. */
    public int getBaseMaxAP() { return maxAP; }

    public void setPosition(HexCell c) { this.position = c; }
    public void setAlive(boolean v)    { this.alive = v; }
    public void setOwner(Owner o)      { this.owner = o; }

    public void setApPenalty(int penalty) { this.apPenalty = Math.max(0, penalty); }
    public int getApPenalty()             { return apPenalty; }

    public boolean hasAP(int n) { return currentAP >= n; }
    public void spendAP(int n)  { currentAP = Math.max(0, currentAP - n); }
    public void addAP(int n)    { currentAP = Math.min(getMaxAP() + n, currentAP + Math.max(0, n)); }
    public void clearAP()       { currentAP = 0; }
    public void refreshAP()     { currentAP = getMaxAP(); }

    // ------------------------------------------------------------------- HP
    @Override public int getHp()    { return hp; }
    @Override public int getMaxHp() { return maxHp; }
    @Override public String getDisplayName() { return getType().getLabel(); }

    @Override public void takeDamage(int amount) {
        if (amount <= 0) return;
        hp = Math.max(0, hp - amount);
        if (hp == 0) alive = false;
    }

    public void heal(int amount) { hp = Math.min(maxHp, hp + Math.max(0, amount)); }

    /** بازگرداندن هویت و وضعیت ذخیره‌شده؛ فقط از مسیر Load صدا زده می‌شود. */
    public void restoreSaved(String id, int hp, int ap, boolean alive, Owner owner, int apPenalty) {
        if (id != null && !id.isBlank()) this.id = id;
        this.hp = Math.max(0, Math.min(maxHp, hp));
        this.currentAP = Math.max(0, ap);
        this.alive = alive && this.hp > 0;
        if (owner != null) this.owner = owner;
        setApPenalty(apPenalty);
    }

    /** یونیت‌های غیرنظامی؛ در اولویت هدف‌گیری حیوانات وحشی اول هستند. */
    public boolean isMilitary() { return false; }

    public abstract UnitType getType();
}
