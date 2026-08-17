package org.strategygame.model.tribe.mission;

import org.strategygame.model.GameState;
import org.strategygame.model.building.Building;
import org.strategygame.model.building.BuildingType;
import org.strategygame.model.map.HexCell;
import org.strategygame.model.map.HexMap;
import org.strategygame.model.tribe.Tribe;
import org.strategygame.model.tribe.TribeType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** مأموریت تحویل منابع (قبیله‌ی کشاورز و کوه‌نشین). */
class DeliveryMission extends TribeMission {

    private final String title;
    private final int[]  payment;
    private final int[]  reward;
    private final int    relationReward;

    DeliveryMission(String title, int[] payment, int deadline, int[] reward, int relationReward) {
        super(deadline);
        this.title          = title;
        this.payment        = payment;
        this.reward         = reward;
        this.relationReward = relationReward;
    }

    @Override public String getTitle() { return title; }

    @Override public String getObjectiveText() {
        return "تحویل " + BuildingType.formatCost(payment) + " به قبیله";
    }

    @Override public int[] getPaymentCost()     { return payment; }
    @Override public int[] getRewardResources() { return reward; }
    @Override public int getRewardRelation()    { return relationReward; }

    @Override public boolean isObjectiveMet(GameState state, Tribe tribe) {
        return state.getStorage().canAffordAll(payment);
    }
}

/** مأموریت بازرگان: ساخت مسیر جاده‌ی پیوسته تا کنار اردوگاه. */
class RoadMission extends TribeMission {

    private static final double TRADE_RATE_BONUS = 0.10;

    RoadMission() { super(10); }

    @Override public String getTitle() { return "مسیر تجاری"; }

    @Override public String getObjectiveText() {
        return "یک مسیر جاده‌ی پیوسته از تان هال تا هکس مجاور اردوگاه بساز";
    }

    @Override public int getRewardRelation() { return 20; }

    @Override public String getSpecialRewardText() {
        return "نرخ تجارت این قبیله +" + (int) (TRADE_RATE_BONUS * 100) + "٪";
    }

    @Override public void grantSpecialReward(GameState state, Tribe tribe) {
        tribe.addTradeRateBonus(TRADE_RATE_BONUS);
    }

    @Override
    public boolean isObjectiveMet(GameState state, Tribe tribe) {
        HexMap map = state.getMap();
        HexCell start = state.getTownHall() == null ? null : state.getTownHall().getLocation();
        if (start == null || tribe.getCampHex() == null) return false;

        Set<HexCell> seen  = new HashSet<>();
        Deque<HexCell> queue = new ArrayDeque<>();
        for (HexCell n : map.getNeighbors(start)) {
            if (n.hasRoad()) { seen.add(n); queue.add(n); }
        }

        while (!queue.isEmpty()) {
            HexCell cur = queue.poll();
            if (map.areNeighbors(cur, tribe.getCampHex())) return true;
            for (HexCell n : map.getNeighbors(cur)) {
                if (n.hasRoad() && seen.add(n)) queue.add(n);
            }
        }
        return false;
    }
}

/** مأموریت جنگاور: شکست دادن دو دشمن در شعاع پنج هکسی اردوگاه. */
class HuntMission extends TribeMission {

    private static final int REQUIRED_KILLS   = 2;
    private static final int REWARD_SWORDSMEN = 3;

    private final int killsAtStart;

    HuntMission(int killsAtStart) {
        super(8);
        this.killsAtStart = killsAtStart;
    }

    @Override public String getTitle() { return "شکار دشمنان" ; }

    @Override public String getObjectiveText() {
        return REQUIRED_KILLS + " دشمن یا بربر را در شعاع ۵ هکسی اردوگاه شکست بده";
    }

    @Override public int getRewardRelation() { return 20; }

    @Override public String getSpecialRewardText() { return REWARD_SWORDSMEN + " شمشیرزن کمکی"; }

    @Override public void grantSpecialReward(GameState state, Tribe tribe) {
        state.spawnAlliedSwordsmen(REWARD_SWORDSMEN, tribe.getCampHex());
    }

    @Override public boolean isObjectiveMet(GameState state, Tribe tribe) {
        return tribe.getNearbyEnemyKills() - killsAtStart >= REQUIRED_KILLS;
    }
}

/** مأموریت ساحلی: ساخت اسکله در فاصله‌ی حداکثر چهار هکس از اردوگاه. */
class DockMission extends TribeMission {

    private static final int MAX_DISTANCE = 4;

    DockMission() { super(10); }

    @Override public String getTitle() { return "ساخت اسکله" ; }

    @Override public String getObjectiveText() {
        return "یک اسکله در فاصله‌ی حداکثر " + MAX_DISTANCE + " هکس از اردوگاه بساز";
    }

    @Override public int[] getRewardResources() { return new int[]{30, 0, 0, 0}; }
    @Override public int getRewardRelation()    { return 15; }

    @Override public String getSpecialRewardText() { return "تخفیف ساخت اسکله‌ی بعدی"; }

    @Override public void grantSpecialReward(GameState state, Tribe tribe) {
        tribe.setDockDiscount(true);
        state.setDockDiscountAvailable(true);
    }

    @Override
    public boolean isObjectiveMet(GameState state, Tribe tribe) {
        if (tribe.getCampHex() == null) return false;
        for (Building b : state.getBuildings()) {
            if (b.getType() != BuildingType.DOCK || b.getLocation() == null) continue;
            if (state.getMap().distance(b.getLocation(), tribe.getCampHex()) <= MAX_DISTANCE)
                return true;
        }
        return false;
    }
}

/** ساخت مأموریت مناسب هر نوع قبیله. */
public final class MissionFactory {

    private MissionFactory() { }

    public static TribeMission create(Tribe tribe) {
        TribeType type = tribe.getType();
        return switch (type) {
            case FARMER   -> new DeliveryMission("تامین آذوقه",
                    new int[]{0, 20, 10, 0}, 5, new int[]{30, 0, 0, 0}, 15);
            case MOUNTAIN -> new DeliveryMission("تامین ابزار",
                    new int[]{0, 15, 0, 10}, 6, new int[]{0, 0, 20, 0}, 15);
            case MERCHANT -> new RoadMission();
            case WARRIOR  -> new HuntMission(tribe.getNearbyEnemyKills());
            case COASTAL  -> new DockMission();
        };
    }

    /** فهرست خلاصه‌ی مأموریت هر نوع قبیله برای نمایش در پنل. */
    public static List<String> describeAll() {
        List<String> list = new ArrayList<>();
        for (TribeType t : TribeType.values()) list.add(t.getLabel());
        return list;
    }
}
