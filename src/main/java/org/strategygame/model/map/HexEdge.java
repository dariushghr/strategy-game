package org.strategygame.model.map;

/**
 * یال بین دو هکس. رودخانه بین دو هکس است نه روی یک هکس، و دیوار هم روی
 * همین یال ساخته می‌شود؛ بنابراین یک یال می‌تواند هم‌زمان رودخانه و دیوار داشته باشد.
 */
public class HexEdge {

    private final int q1, r1, q2, r2;
    private boolean river;
    private Wall    wall;

    HexEdge(int q1, int r1, int q2, int r2) {
        this.q1 = q1; this.r1 = r1;
        this.q2 = q2; this.r2 = r2;
    }

    public int getQ1() { return q1; }
    public int getR1() { return r1; }
    public int getQ2() { return q2; }
    public int getR2() { return r2; }

    public boolean hasRiver() { return river; }
    public void setRiver(boolean v) { this.river = v; }

    public boolean hasWall()   { return wall != null && !wall.isDestroyed(); }
    public Wall getWall()      { return wall; }
    public void setWall(Wall w){ this.wall = w; }
    public void removeWall()   { this.wall = null; }

    /** آیا این یال دیگر هیچ اطلاعاتی ندارد و می‌تواند از نقشه پاک شود. */
    public boolean isEmpty() { return !river && wall == null; }

    public boolean connects(HexCell a, HexCell b) {
        return (a.getQ() == q1 && a.getR() == r1 && b.getQ() == q2 && b.getR() == r2)
            || (a.getQ() == q2 && a.getR() == r2 && b.getQ() == q1 && b.getR() == r1);
    }
}
