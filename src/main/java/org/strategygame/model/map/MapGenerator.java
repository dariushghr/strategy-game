package org.strategygame.model.map;

import org.strategygame.model.resource.ResourceType;
import org.strategygame.service.RandomService;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * تولید نقشه‌ی فاز دو: دریا، رشته‌کوه، کوه، جنگل، سبزه‌زار، رودخانه و منابع.
 * در پایان تضمین می‌کند که محل تان هال داخل منطقه‌ی بسته گیر نکرده باشد.
 */
public class MapGenerator {

    /** حداقل تعداد هکس زمینی که از محل شروع قابل دسترسی باشد. */
    private static final int MIN_REACHABLE_LAND = 40;

    private final int width, height;

    public MapGenerator(int width, int height) {
        this.width  = width;
        this.height = height;
    }

    public HexMap generate(RandomService rng, int startQ, int startR) {
        HexMap map = new HexMap(width, height);

        for (int q = 0; q < width; q++)
            for (int r = 0; r < height; r++)
                map.setCell(q, r, new HexCell(q, r, TerrainType.PLAINS));

        placeSea(map, rng);
        placeMountains(map, rng);
        placeForests(map, rng, startQ, startR);
        placeGrasslands(map, rng);
        placeRivers(map, rng);
        distributeResources(map, rng);
        ensurePlayableStart(map, startQ, startR);
        return map;
    }

    // -------------------------------------------------------------------- دریا
    /** یک دریای ساحلی در یکی از لبه‌های نقشه به‌علاوه‌ی چند دریاچه. */
    private void placeSea(HexMap map, RandomService rng) {
        boolean seaOnLeft = rng.nextInt(2) == 0;
        int bandDepth = 2 + rng.nextInt(2);

        for (int r = 0; r < height; r++) {
            int depth = bandDepth + (rng.nextInt(3) - 1);
            for (int i = 0; i < depth; i++) {
                int q = seaOnLeft ? i : width - 1 - i;
                HexCell cell = map.getCell(q, r);
                if (cell != null) cell.setTerrain(TerrainType.SEA);
            }
        }

        int lakes = 1 + rng.nextInt(2);
        for (int i = 0; i < lakes; i++) {
            int q = 4 + rng.nextInt(Math.max(1, width - 8));
            int r = 4 + rng.nextInt(Math.max(1, height - 8));
            HexCell center = map.getCell(q, r);
            if (center == null) continue;
            center.setTerrain(TerrainType.SEA);
            for (HexCell n : map.getNeighbors(center)) {
                if (rng.nextInt(10) < 5) n.setTerrain(TerrainType.SEA);
            }
        }
    }

    // -------------------------------------------------------------------- کوه
    /**
     * زنجیره‌های کوه. قلب هر زنجیره رشته‌کوهِ غیرقابل عبور است و اطرافش
     * کوهستانِ قابل عبور با هزینه‌ی بیشتر.
     */
    private void placeMountains(HexMap map, RandomService rng) {
        int chains = 2 + rng.nextInt(2);
        for (int c = 0; c < chains; c++) {
            int q = rng.nextInt(width), r = rng.nextInt(height);
            int len = 4 + rng.nextInt(6);
            for (int i = 0; i < len; i++) {
                HexCell cell = map.getCell(q, r);
                if (cell != null && cell.isLand()) {
                    boolean ridge = rng.nextInt(10) < 4;
                    cell.setTerrain(ridge ? TerrainType.MOUNTAIN_RANGE : TerrainType.MOUNTAIN);
                    for (HexCell n : map.getNeighbors(cell)) {
                        if (n.isLand() && n.getTerrain() == TerrainType.PLAINS
                                && rng.nextInt(10) < 4) {
                            n.setTerrain(TerrainType.MOUNTAIN);
                        }
                    }
                }
                int[] d = HexMap.DIRS[rng.nextInt(6)];
                q = clamp(q + d[0], 0, width  - 1);
                r = clamp(r + d[1], 0, height - 1);
            }
        }
    }

    private void placeForests(HexMap map, RandomService rng, int startQ, int startR) {
        placeCluster(map, rng, startQ + 1, startR, TerrainType.FOREST, 4);

        int extra = 3 + rng.nextInt(3);
        for (int i = 0; i < extra; i++) {
            int q = 2 + rng.nextInt(Math.max(1, width  - 4));
            int r = 2 + rng.nextInt(Math.max(1, height - 4));
            placeCluster(map, rng, q, r, TerrainType.FOREST, 3 + rng.nextInt(4));
        }
    }

    private void placeGrasslands(HexMap map, RandomService rng) {
        for (HexCell cell : map.getAllCells()) {
            if (cell.getTerrain() == TerrainType.PLAINS && rng.nextInt(10) < 3)
                cell.setTerrain(TerrainType.GRASSLAND);
        }
    }

    private void placeCluster(HexMap map, RandomService rng, int cq, int cr,
                              TerrainType type, int size) {
        for (int i = 0; i < size; i++) {
            int q = clamp(cq + rng.nextInt(3) - 1, 0, width  - 1);
            int r = clamp(cr + rng.nextInt(3) - 1, 0, height - 1);
            HexCell cell = map.getCell(q, r);
            if (cell != null && cell.getTerrain() == TerrainType.PLAINS) cell.setTerrain(type);
        }
    }

    // --------------------------------------------------------------- رودخانه
    /**
     * رودخانه‌ها روی یال بین هکس‌ها ثبت می‌شوند: یک مسیر تصادفی روی زمین انتخاب
     * می‌شود و یال بین هر دو هکس متوالی مسیر رودخانه‌دار می‌شود.
     */
    private void placeRivers(HexMap map, RandomService rng) {
        int rivers = 2 + rng.nextInt(2);
        for (int i = 0; i < rivers; i++) {
            HexCell current = randomLandCell(map, rng);
            if (current == null) continue;

            int length = 6 + rng.nextInt(8);
            for (int step = 0; step < length; step++) {
                List<HexCell> options = new ArrayList<>();
                for (HexCell n : map.getNeighbors(current)) {
                    if (n.isLand() && !map.hasRiver(current, n)) options.add(n);
                }
                HexCell next = rng.pick(options);
                if (next == null) break;

                HexEdge edge = map.edgeOrCreate(current, next);
                if (edge != null) edge.setRiver(true);
                current = next;
            }
        }
    }

    private HexCell randomLandCell(HexMap map, RandomService rng) {
        List<HexCell> land = new ArrayList<>();
        for (HexCell c : map.getAllCells()) {
            if (c.isLand() && c.getTerrain() != TerrainType.MOUNTAIN_RANGE) land.add(c);
        }
        return rng.pick(land);
    }

    // ----------------------------------------------------------------- منابع
    private void distributeResources(HexMap map, RandomService rng) {
        for (HexCell cell : map.getAllCells()) {
            switch (cell.getTerrain()) {
                case FOREST -> cell.setDeposit(
                        new ResourceDeposit(ResourceType.WOOD, 200 + rng.nextInt(100)));
                case MOUNTAIN -> {
                    if (rng.nextInt(10) < 3)
                        cell.setDeposit(new ResourceDeposit(ResourceType.IRON, 60 + rng.nextInt(60)));
                    else
                        cell.setDeposit(new ResourceDeposit(ResourceType.STONE, 150 + rng.nextInt(100)));
                }
                case GRASSLAND -> {
                    if (rng.nextInt(10) < 6)
                        cell.setDeposit(new ResourceDeposit(ResourceType.FOOD, 300 + rng.nextInt(100)));
                }
                case PLAINS -> {
                    if (rng.nextInt(10) < 3)
                        cell.setDeposit(new ResourceDeposit(ResourceType.FOOD, 180 + rng.nextInt(80)));
                }
                case SEA -> {
                    if (rng.nextInt(10) < 4)
                        cell.setDeposit(new ResourceDeposit(ResourceType.FOOD, 120 + rng.nextInt(80)));
                }
                default -> { }
            }
        }
    }

    // ------------------------------------------------------- ایمنی نقطه‌ی شروع
    /**
     * تان هال نباید داخل منطقه‌ی بسته‌ی دریا/رشته‌کوه گیر کند. اگر تعداد هکس‌های
     * زمینیِ قابل دسترسی کم بود، موانع اطراف نقطه‌ی شروع باز می‌شوند.
     */
    private void ensurePlayableStart(HexMap map, int startQ, int startR) {
        HexCell start = map.getCell(startQ, startR);
        if (start == null) return;

        if (!start.isLand() || start.getTerrain() == TerrainType.MOUNTAIN_RANGE)
            start.setTerrain(TerrainType.PLAINS);

        for (int radius = 1; countReachableLand(map, start) < MIN_REACHABLE_LAND && radius <= 4; radius++) {
            for (HexCell c : map.getCellsInRadius(start, radius)) {
                if (c.getTerrain() == TerrainType.MOUNTAIN_RANGE || c.isWater())
                    c.setTerrain(TerrainType.PLAINS);
            }
        }
    }

    private int countReachableLand(HexMap map, HexCell start) {
        Set<HexCell> seen = new HashSet<>();
        Deque<HexCell> queue = new ArrayDeque<>();
        seen.add(start);
        queue.add(start);

        while (!queue.isEmpty()) {
            HexCell cur = queue.poll();
            for (HexCell n : map.getNeighbors(cur)) {
                if (seen.contains(n)) continue;
                if (!n.getTerrain().isLandPassable()) continue;
                seen.add(n);
                queue.add(n);
            }
        }
        return seen.size();
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
