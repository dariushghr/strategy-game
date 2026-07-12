package org.strategygame.model.map;

import java.util.ArrayList;
import java.util.List;

public class HexMap {
    private final int width, height;
    private final HexCell[][] grid;

    public static final int[][] DIRS = {
        { 1, 0}, { 1,-1}, { 0,-1},
        {-1, 0}, {-1, 1}, { 0, 1}
    };

    public HexMap(int width, int height) {
        this.width  = width;
        this.height = height;
        this.grid   = new HexCell[width][height];
    }

    public int getWidth()  { return width; }
    public int getHeight() { return height; }

    public HexCell getCell(int q, int r) {
        if (q < 0 || q >= width || r < 0 || r >= height) return null;
        return grid[q][r];
    }

    public void setCell(int q, int r, HexCell c) { grid[q][r] = c; }

    public List<HexCell> getNeighbors(HexCell cell) {
        List<HexCell> list = new ArrayList<>();
        for (int[] d : DIRS) {
            HexCell n = getCell(cell.getQ() + d[0], cell.getR() + d[1]);
            if (n != null) list.add(n);
        }
        return list;
    }

    public List<HexCell> getCellsInRadius(HexCell center, int radius) {
        List<HexCell> list = new ArrayList<>();
        for (int dq = -radius; dq <= radius; dq++) {
            int rMin = Math.max(-radius, -dq - radius);
            int rMax = Math.min( radius, -dq + radius);
            for (int dr = rMin; dr <= rMax; dr++) {
                HexCell c = getCell(center.getQ() + dq, center.getR() + dr);
                if (c != null) list.add(c);
            }
        }
        return list;
    }

    public List<HexCell> getAllCells() {
        List<HexCell> list = new ArrayList<>();
        for (int q = 0; q < width; q++)
            for (int r = 0; r < height; r++)
                if (grid[q][r] != null) list.add(grid[q][r]);
        return list;
    }

    public int distance(HexCell a, HexCell b) {
        int dq = a.getQ() - b.getQ();
        int dr = a.getR() - b.getR();
        return (Math.abs(dq) + Math.abs(dq + dr) + Math.abs(dr)) / 2;
    }
}
