package org.strategygame.model.map;

import org.strategygame.model.resource.ResourceType;

import java.util.Random;

public class MapGenerator {
    private final int width, height;

    private static final int[][] DIRS = HexMap.DIRS;

    public MapGenerator(int width, int height) {
        this.width  = width;
        this.height = height;
    }

    public HexMap generate(long seed) {
        Random rng = new Random(seed);
        HexMap map = new HexMap(width, height);

        for (int q = 0; q < width; q++)
            for (int r = 0; r < height; r++)
                map.setCell(q, r, new HexCell(q, r, TerrainType.PLAINS));

        placeMountains(map, rng);
        placeForests(map, rng);
        placeGrasslands(map, rng);
        distributeResources(map, rng);
        return map;
    }

    private void placeMountains(HexMap map, Random rng) {
        int chains = 2 + rng.nextInt(2);
        for (int c = 0; c < chains; c++) {
            int q = rng.nextInt(width), r = rng.nextInt(height);
            int len = 4 + rng.nextInt(6);
            for (int i = 0; i < len; i++) {
                if (map.getCell(q, r) != null)
                    map.setCell(q, r, new HexCell(q, r, TerrainType.MOUNTAIN));
                int[] d = DIRS[rng.nextInt(6)];
                q = clamp(q + d[0], 0, width  - 1);
                r = clamp(r + d[1], 0, height - 1);
            }
        }
    }

    private void placeForests(HexMap map, Random rng) {

        int cx = width / 2, cy = height / 2;
        placeCluster(map, rng, cx + 1, cy, TerrainType.FOREST, 4);

        int extra = 3 + rng.nextInt(3);
        for (int i = 0; i < extra; i++) {
            int q = 2 + rng.nextInt(Math.max(1, width  - 4));
            int r = 2 + rng.nextInt(Math.max(1, height - 4));
            placeCluster(map, rng, q, r, TerrainType.FOREST, 3 + rng.nextInt(4));
        }
    }

    private void placeGrasslands(HexMap map, Random rng) {
        for (HexCell cell : map.getAllCells()) {
            if (cell.getTerrain() == TerrainType.PLAINS && rng.nextInt(10) < 3)
                map.setCell(cell.getQ(), cell.getR(),
                        new HexCell(cell.getQ(), cell.getR(), TerrainType.GRASSLAND));
        }
    }

    private void placeCluster(HexMap map, Random rng, int cq, int cr,
                               TerrainType type, int size) {
        for (int i = 0; i < size; i++) {
            int q = clamp(cq + rng.nextInt(3) - 1, 0, width  - 1);
            int r = clamp(cr + rng.nextInt(3) - 1, 0, height - 1);
            HexCell cell = map.getCell(q, r);
            if (cell != null && cell.getTerrain() == TerrainType.PLAINS)
                map.setCell(q, r, new HexCell(q, r, type));
        }
    }

    private void distributeResources(HexMap map, Random rng) {
        for (HexCell cell : map.getAllCells()) {
            switch (cell.getTerrain()) {
                case FOREST -> cell.setDeposit(new ResourceDeposit(ResourceType.WOOD, 200 + rng.nextInt(100)));
                case MOUNTAIN -> {
                    if (rng.nextInt(10) < 3) {

                        var ironCell = new HexCell(cell.getQ(), cell.getR(), TerrainType.MOUNTAIN);
                        ironCell.setDeposit(new ResourceDeposit(ResourceType.IRON, 60 + rng.nextInt(60)));
                        map.setCell(cell.getQ(), cell.getR(), ironCell);
                    } else {
                        cell.setDeposit(new ResourceDeposit(ResourceType.STONE, 150 + rng.nextInt(100)));
                    }
                }
                case GRASSLAND -> {
                    if (rng.nextInt(10) < 6)
                        cell.setDeposit(new ResourceDeposit(ResourceType.FOOD, 300 + rng.nextInt(100)));
                }
                case PLAINS -> {
                    if (rng.nextInt(10) < 3)
                        cell.setDeposit(new ResourceDeposit(ResourceType.FOOD, 180 + rng.nextInt(80)));
                }
            }
        }
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
