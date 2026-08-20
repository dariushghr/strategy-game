package org.strategygame.model.tribe;

import org.strategygame.config.GameConfig;
import org.strategygame.model.map.HexCell;
import org.strategygame.model.tribe.mission.TribeMission;
import org.strategygame.model.unit.MilitaryUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** یک قبیله‌ی بی‌طرف روی نقشه. */
public class Tribe {

    private String          id = UUID.randomUUID().toString();
    private final String    name;
    private final TribeType type;
    private final HexCell   campHex;
    private final TribeCamp camp;

    private int           relation      = 0;
    private RelationState relationState = RelationState.NEUTRAL;

    private boolean discovered = false;
    private boolean atWar      = false;
    private boolean allied     = false;
    private boolean defeated   = false;

    private final List<MilitaryUnit> units = new ArrayList<>();

    private TribeMission activeMission;
    private int lastMissionFailTurn  = Integer.MIN_VALUE / 2;
    private int lastMissionOfferTurn = Integer.MIN_VALUE / 2;
    private int lastGuardTurn        = Integer.MIN_VALUE / 2;
    private int lastTradeTurn        = -1;

    /** تعداد دشمنان شکست‌خورده در نزدیکی اردوگاه؛ برای مأموریت قبیله‌ی جنگاور. */
    private int nearbyEnemyKills = 0;

    /** جایزه‌ی مأموریت بازرگان: افزایش نرخ تجارت. */
    private double tradeRateBonus = 0.0;

    /** جایزه‌ی مأموریت ساحلی: تخفیف ساخت اسکله‌ی بعدی. */
    private boolean dockDiscount = false;

    /** آیا هشدار ورود نیروی نظامی به قلمرو قبیله در نوبت قبل داده شده است. */
    private boolean trespassWarned = false;

    public Tribe(String name, TribeType type, HexCell campHex) {
        this.name    = name;
        this.type    = type;
        this.campHex = campHex;
        this.camp    = new TribeCamp(type);
    }

    public String getId()             { return id; }
    public String getName()           { return name; }
    public TribeType getType()        { return type; }
    public HexCell getCampHex()       { return campHex; }
    public TribeCamp getCamp()        { return camp; }
    public int getRelation()          { return relation; }
    public RelationState getRelationState() { return relationState; }
    public boolean isDiscovered()     { return discovered; }
    public boolean isAtWar()          { return atWar; }
    public boolean isAllied()         { return allied; }
    public boolean isDefeated()       { return defeated; }
    public List<MilitaryUnit> getUnits() { return units; }
    public TribeMission getActiveMission() { return activeMission; }
    public int getNearbyEnemyKills()  { return nearbyEnemyKills; }
    public boolean hasDockDiscount()  { return dockDiscount; }
    public boolean isTrespassWarned() { return trespassWarned; }

    public void setDiscovered(boolean v)   { this.discovered = v; }
    public void setTrespassWarned(boolean v){ this.trespassWarned = v; }
    public void setDockDiscount(boolean v) { this.dockDiscount = v; }

    public void restoreId(String savedId) {
        if (savedId != null && !savedId.isBlank()) this.id = savedId;
    }

    public void restoreFlags(boolean discovered, boolean atWar, boolean allied, boolean defeated) {
        this.discovered = discovered;
        this.atWar      = atWar;
        this.allied     = allied;
        this.defeated   = defeated;
    }

    public void restoreTimers(int lastFail, int lastOffer, int lastGuard, int lastTrade,
                              int nearbyKills, double tradeBonus, boolean dockDiscount,
                              boolean trespassWarned) {
        this.lastMissionFailTurn  = lastFail;
        this.lastMissionOfferTurn = lastOffer;
        this.lastGuardTurn        = lastGuard;
        this.lastTradeTurn        = lastTrade;
        this.nearbyEnemyKills     = Math.max(0, nearbyKills);
        this.tradeRateBonus       = tradeBonus;
        this.dockDiscount         = dockDiscount;
        this.trespassWarned       = trespassWarned;
    }

    // -------------------------------------------------------------- رابطه
    /**
     * تعیین مقدار رابطه با Clamp بین -۱۰۰ و +۱۰۰.
     * @return وضعیت جدید اگر وضعیت عوض شده باشد، در غیر این صورت {@code null}.
     */
    public RelationState setRelation(int value) {
        relation = Math.max(GameConfig.RELATION_MIN, Math.min(GameConfig.RELATION_MAX, value));
        RelationState next = RelationState.of(relation);
        if (next == relationState) {
            syncAllianceFlag();
            return null;
        }
        relationState = next;
        syncAllianceFlag();
        return relationState;
    }

    public RelationState changeRelation(int delta) { return setRelation(relation + delta); }

    /** اتحاد با افتادن رابطه زیر حد اتحاد خودبه‌خود غیرفعال می‌شود. */
    private void syncAllianceFlag() {
        if (allied && relation < GameConfig.RELATION_ALLIANCE_MIN) allied = false;
    }

    public void setAllied(boolean v) { this.allied = v; }

    public void declareWar() {
        atWar  = true;
        allied = false;
        setRelation(GameConfig.RELATION_WAR_VALUE);
    }

    public void makePeace() {
        atWar = false;
        setRelation(GameConfig.RELATION_AFTER_PEACE);
    }

    public void markDefeated() {
        defeated = true;
        atWar    = false;
        allied   = false;
        units.clear();
        if (activeMission != null) {
            activeMission.cancel();
            activeMission = null;
        }
    }

    // ---------------------------------------------------------- مأموریت‌ها
    public void setActiveMission(TribeMission mission) { this.activeMission = mission; }
    public void clearActiveMission()                   { this.activeMission = null; }

    public boolean hasOpenMission() {
        return activeMission != null && activeMission.isOpen();
    }

    public void recordMissionFailure(int turn) {
        this.lastMissionFailTurn = turn;
        this.activeMission = null;
    }

    /** بعد از شکست مأموریت، تا چند نوبت مأموریت جدیدی داده نمی‌شود. */
    public boolean isMissionOnCooldown(int turn) {
        return turn - lastMissionFailTurn < GameConfig.MISSION_FAIL_COOLDOWN;
    }

    public boolean hadRecentMissionFailure(int turn) { return isMissionOnCooldown(turn); }

    public int getLastMissionOfferTurn()            { return lastMissionOfferTurn; }
    public void setLastMissionOfferTurn(int turn)   { this.lastMissionOfferTurn = turn; }
    public int getLastMissionFailTurn()             { return lastMissionFailTurn; }

    public void addNearbyEnemyKill() { nearbyEnemyKills++; }

    // -------------------------------------------------------------- تجارت
    public double getTradeRate() { return type.getTradeRate() + tradeRateBonus; }
    public double getTradeRateBonus() { return tradeRateBonus; }

    public void addTradeRateBonus(double bonus) { this.tradeRateBonus += bonus; }

    public boolean hasTradedOn(int turn) { return lastTradeTurn == turn; }
    public void markTraded(int turn)     { this.lastTradeTurn = turn; }
    public int getLastTradeTurn()        { return lastTradeTurn; }

    // ------------------------------------------------------------ نگهبان‌ها
    public int getLastGuardTurn()          { return lastGuardTurn; }
    public void setLastGuardTurn(int turn) { this.lastGuardTurn = turn; }

    public boolean canSpawnGuard(int turn) {
        return units.size() < type.getGuardCap()
                && turn - lastGuardTurn >= GameConfig.TRIBE_GUARD_INTERVAL;
    }

    public void addUnit(MilitaryUnit u)    { units.add(u); }
    public void removeUnit(MilitaryUnit u) { units.remove(u); }

    /** یونیت‌های زنده‌ی قبیله. */
    public List<MilitaryUnit> aliveUnits() {
        List<MilitaryUnit> alive = new ArrayList<>();
        for (MilitaryUnit u : units) if (u.isAlive()) alive.add(u);
        return alive;
    }

    public String describe() {
        return name + " (" + type.getLabel() + ") — " + relationState.getLabel()
                + " " + (relation > 0 ? "+" : "") + relation;
    }
}
