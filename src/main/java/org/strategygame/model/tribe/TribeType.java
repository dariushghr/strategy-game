package org.strategygame.model.tribe;

import org.strategygame.config.GameConfig;
import org.strategygame.model.building.BuildingType;
import org.strategygame.model.map.TerrainType;
import org.strategygame.model.resource.ResourceType;

import java.util.Set;

/** پنج نوع قبیله‌ی بی‌طرف با نرخ تجارت، منابع خروجی، HP اردوگاه و غنیمت خودشان. */
public enum TribeType {

    FARMER("کشاورز", GameConfig.TRIBE_RATE_FARMER,
            Set.of(ResourceType.FOOD), true, 150,
            new int[]{60, 20, 0, 0},
            Set.of(TerrainType.GRASSLAND, TerrainType.PLAINS)),

    WARRIOR("جنگاور", GameConfig.TRIBE_RATE_WARRIOR,
            Set.of(), false, 250,
            new int[]{20, 20, 20, 20},
            Set.of(TerrainType.PLAINS, TerrainType.FOREST)),

    MERCHANT("بازرگان", GameConfig.TRIBE_RATE_MERCHANT,
            Set.of(), true, 150,
            new int[]{30, 30, 30, 10},
            Set.of(TerrainType.PLAINS, TerrainType.GRASSLAND)),

    MOUNTAIN("کوه‌نشین", GameConfig.TRIBE_RATE_MOUNTAIN,
            Set.of(ResourceType.STONE, ResourceType.IRON), true, 200,
            new int[]{0, 0, 60, 30},
            Set.of(TerrainType.MOUNTAIN)),

    COASTAL("ساحلی", GameConfig.TRIBE_RATE_COASTAL,
            Set.of(ResourceType.FOOD), true, 150,
            new int[]{50, 20, 0, 0},
            Set.of(TerrainType.PLAINS, TerrainType.GRASSLAND, TerrainType.FOREST));

    private final String                label;
    private final double                tradeRate;
    private final Set<ResourceType>     tradeOutputs;
    private final boolean               trader;
    private final int                   campHp;
    private final int[]                 loot;
    private final Set<TerrainType>      preferredTerrain;

    TribeType(String label, double tradeRate, Set<ResourceType> tradeOutputs, boolean trader,
              int campHp, int[] loot, Set<TerrainType> preferredTerrain) {
        this.label            = label;
        this.tradeRate        = tradeRate;
        this.tradeOutputs     = tradeOutputs;
        this.trader           = trader;
        this.campHp           = campHp;
        this.loot             = loot;
        this.preferredTerrain = preferredTerrain;
    }

    public String getLabel()                  { return label; }
    public double getTradeRate()              { return tradeRate; }
    public Set<ResourceType> getTradeOutputs(){ return tradeOutputs; }
    public boolean isTrader()                 { return trader; }
    public int getCampHp()                    { return campHp; }
    public int[] getLoot()                    { return loot; }
    public Set<TerrainType> getPreferredTerrain() { return preferredTerrain; }

    /** سقف تعداد نگهبان این قبیله. */
    public int getGuardCap() {
        return this == WARRIOR ? GameConfig.TRIBE_GUARD_CAP_WARRIOR : GameConfig.TRIBE_GUARD_CAP;
    }

    /** قبیله‌ی ساحلی باید نزدیک دریا باشد. */
    public boolean requiresCoast() { return this == COASTAL; }

    public String lootText() { return BuildingType.formatCost(loot); }

    public String tradeText() {
        if (!trader) return "تجارت ندارد";
        String out = tradeOutputs.isEmpty() ? "هر منبعی" : labels();
        return "نرخ " + (int) (tradeRate * 100) + "٪، خروجی: " + out;
    }

    private String labels() {
        StringBuilder sb = new StringBuilder();
        for (ResourceType t : tradeOutputs) {
            if (sb.length() > 0) sb.append(" یا ");
            sb.append(t.getLabel());
        }
        return sb.toString();
    }
}
