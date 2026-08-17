package org.strategygame.model.map;

import org.strategygame.model.building.Building;
import org.strategygame.model.unit.Unit;

import java.util.Arrays;
import java.util.List;

public class FogOfWar {
    private final HexMap map;
    private final boolean[][] explored;
    private final boolean[][] visible;

    public FogOfWar(HexMap map) {
        this.map      = map;
        this.explored = new boolean[map.getWidth()][map.getHeight()];
        this.visible  = new boolean[map.getWidth()][map.getHeight()];
    }

    public void update(List<Unit> units, List<Building> buildings) {

        for (boolean[] row : visible) Arrays.fill(row, false);

        for (Unit u : units) {
            if (u.getPosition() != null)
                reveal(u.getPosition(), u.getVisionRadius());
        }
        for (Building b : buildings) {
            if (b.getLocation() != null)
                reveal(b.getLocation(), 2);
        }
    }

    /** آشکار کردن کل نقشه؛ برای ابزار تست سناریوها. */
    public void revealAll() {
        for (int q = 0; q < map.getWidth(); q++) {
            for (int r = 0; r < map.getHeight(); r++) {
                HexCell cell = map.getCell(q, r);
                if (cell == null) continue;
                explored[q][r] = true;
                visible[q][r]  = true;
                cell.setExplored(true);
            }
        }
    }

    private void reveal(HexCell center, int radius) {
        for (HexCell c : map.getCellsInRadius(center, radius)) {
            int q = c.getQ(), r = c.getR();
            visible[q][r]  = true;
            explored[q][r] = true;
            c.setExplored(true);
        }
    }

    public boolean isExplored(int q, int r) {
        return inBounds(q, r) && explored[q][r];
    }

    public boolean isVisible(int q, int r) {
        return inBounds(q, r) && visible[q][r];
    }

    private boolean inBounds(int q, int r) {
        return q >= 0 && q < map.getWidth() && r >= 0 && r < map.getHeight();
    }
}
