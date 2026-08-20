package org.strategygame.model.building;

import org.strategygame.config.GameConfig;

public class TownHall extends Building {

    private final ProductionQueue queue = new ProductionQueue();
    private TownHallLevel level = TownHallLevel.LEVEL_1;
    private boolean defensiveWall = false;

    public TownHall() {
        super(BuildingType.TOWN_HALL);
        setDefense(GameConfig.TOWN_HALL_DEFENSE);
    }

    @Override public int produce(double m) { return GameConfig.TOWN_HALL_BASE_FOOD; }

    /** تان هال هرگز کاملا با بلای طبیعی نابود نمی‌شود. */
    @Override public int getMinimumHp() { return 1; }

    public ProductionQueue getQueue()  { return queue; }

    public TownHallLevel getLevelData() { return level; }
    public int getLevel()               { return level.getNumber(); }
    public int getMaxLevel()            { return TownHallLevel.maxLevel().getNumber(); }
    public TownHallLevel getNextLevel() { return level.next(); }
    public boolean canLevelUp()         { return level.next() != null; }

    public int getStorageCapacity() { return level.getStorageCapacity(); }
    public int getMilitaryCap()     { return level.getMilitaryCap(); }

    public boolean hasDefensiveWall() { return defensiveWall; }

    /** اثر تکنولوژی معماری دفاعی: دیوار دفاعی تان هال. */
    public void buildDefensiveWall() {
        if (defensiveWall) return;
        defensiveWall = true;
        setDefense(GameConfig.DEFENSIVE_ARCHITECTURE_DEFENSE);
        setMaxHp(GameConfig.DEFENSIVE_ARCHITECTURE_MAX_HP);
        heal(GameConfig.DEFENSIVE_ARCHITECTURE_MAX_HP - GameConfig.TOWN_HALL_MAX_HP);
    }

    /** یک سطح ارتقا می‌دهد و سطح جدید را برمی‌گرداند. */
    /** بازگرداندن سطح ذخیره‌شده بدون اعمال اثر ارتقا. */
    public void restoreLevel(TownHallLevel saved) {
        if (saved != null) this.level = saved;
    }

    public void restoreDefensiveWall(boolean v) {
        if (!v || defensiveWall) return;
        defensiveWall = true;
        setDefense(GameConfig.DEFENSIVE_ARCHITECTURE_DEFENSE);
        setMaxHp(GameConfig.DEFENSIVE_ARCHITECTURE_MAX_HP);
    }

    public TownHallLevel levelUp() {
        TownHallLevel next = level.next();
        if (next == null) return null;
        level = next;
        if (next == TownHallLevel.LEVEL_2) heal(GameConfig.TOWN_HALL_LEVEL_2_HEAL);
        return next;
    }

    /** غذایی که تان هال هر نوبت به صورت تضمینی می‌دهد. */
    public int foodPerTurn() { return GameConfig.TOWN_HALL_BASE_FOOD; }

    /** چوبی که تان هال هر نوبت به صورت تضمینی می‌دهد. */
    public int woodPerTurn() { return GameConfig.TOWN_HALL_BASE_WOOD; }
}
