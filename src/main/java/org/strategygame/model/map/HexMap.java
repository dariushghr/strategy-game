package org.strategygame.model.map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HexMap {

    private final int width, height;
    private final HexCell[][] grid;

    /** یال‌های نقشه (رودخانه و دیوار) با کلید نرمال‌شده‌ی دو سر یال. */
    private final Map<String, HexEdge> edges = new HashMap<>();

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

    public boolean areNeighbors(HexCell a, HexCell b) {
        return a != null && b != null && !a.equals(b) && distance(a, b) == 1;
    }

    // -------------------------------------------------------------- یال‌ها
    private static String key(int q1, int r1, int q2, int r2) {
        boolean firstIsSmaller = (q1 < q2) || (q1 == q2 && r1 <= r2);
        return firstIsSmaller
                ? q1 + ":" + r1 + "|" + q2 + ":" + r2
                : q2 + ":" + r2 + "|" + q1 + ":" + r1;
    }

    /** یال بین دو هکس مجاور؛ اگر ثبت نشده باشد {@code null}. */
    public HexEdge getEdge(HexCell a, HexCell b) {
        if (!areNeighbors(a, b)) return null;
        return edges.get(key(a.getQ(), a.getR(), b.getQ(), b.getR()));
    }

    /** یال بین دو هکس مجاور؛ اگر وجود نداشته باشد ساخته می‌شود. */
    public HexEdge edgeOrCreate(HexCell a, HexCell b) {
        if (!areNeighbors(a, b)) return null;
        String k = key(a.getQ(), a.getR(), b.getQ(), b.getR());
        HexEdge edge = edges.get(k);
        if (edge == null) {
            edge = new HexEdge(a.getQ(), a.getR(), b.getQ(), b.getR());
            edges.put(k, edge);
        }
        return edge;
    }

    public void removeEdgeIfEmpty(HexCell a, HexCell b) {
        String k = key(a.getQ(), a.getR(), b.getQ(), b.getR());
        HexEdge edge = edges.get(k);
        if (edge != null && edge.isEmpty()) edges.remove(k);
    }

    public boolean hasRiver(HexCell a, HexCell b) {
        HexEdge edge = getEdge(a, b);
        return edge != null && edge.hasRiver();
    }

    public Wall getWall(HexCell a, HexCell b) {
        HexEdge edge = getEdge(a, b);
        return edge != null && edge.hasWall() ? edge.getWall() : null;
    }

    /** آیا این هکس حداقل یک یال رودخانه دارد. */
    public boolean touchesRiver(HexCell cell) {
        for (HexCell n : getNeighbors(cell)) {
            if (hasRiver(cell, n)) return true;
        }
        return false;
    }

    /** آیا این هکس ساحلی است؛ یعنی حداقل یک همسایه‌ی دریایی دارد. */
    public boolean isCoastal(HexCell cell) {
        for (HexCell n : getNeighbors(cell)) {
            if (n.isWater()) return true;
        }
        return false;
    }

    public Collection<HexEdge> getAllEdges() { return edges.values(); }

    /** همه‌ی هکس‌های دارای دیوار در اطراف یک هکس. */
    public List<HexEdge> getEdgesAround(HexCell cell) {
        List<HexEdge> list = new ArrayList<>();
        for (HexCell n : getNeighbors(cell)) {
            HexEdge e = getEdge(cell, n);
            if (e != null) list.add(e);
        }
        return list;
    }
}
