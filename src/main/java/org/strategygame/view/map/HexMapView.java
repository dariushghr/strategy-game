package org.strategygame.view.map;

import org.strategygame.model.building.Building;
import org.strategygame.model.map.FogOfWar;
import org.strategygame.model.map.HexCell;
import org.strategygame.model.map.HexMap;
import org.strategygame.model.map.TerrainType;
import org.strategygame.model.resource.ResourceType;
import org.strategygame.model.unit.Unit;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.List;

public class HexMapView extends JPanel {

    private static final int HEX_SIZE = 38;
    private static final double SQRT3  = Math.sqrt(3.0);

    private static final double COL_STEP = HEX_SIZE * 1.5;
    private static final double ROW_STEP = HEX_SIZE * SQRT3;
    private static final double ROW_OFF  = HEX_SIZE * SQRT3 / 2.0;

    private static final double[] ZOOM_LEVELS = {0.5, 0.75, 1.0, 1.35, 1.7};
    private int zoomIdx = 2;

    private int camX = 0, camY = 0;
    private int dragOX, dragOY;

    private HexMap   map;
    private FogOfWar fog;

    private final List<HexCell> highlighted = new ArrayList<>();
    private HexCell selected = null;

    private Unit   animUnit;
    private double animX, animY, animDX, animDY;
    private Timer  animTimer;

    public interface CellClick { void on(HexCell cell); }
    private CellClick cellClickListener;

    public HexMapView() {
        setBackground(new Color(15, 15, 20));
        setupInput();
    }

    public void setMapData(HexMap map, FogOfWar fog) {
        this.map = map;
        this.fog = fog;

        int cx = map.getWidth() / 2, cy = map.getHeight() / 2;
        Point2D center = toScreen(cx, cy);
        camX = getWidth()  / 2 - (int) center.x();
        camY = getHeight() / 2 - (int) center.y();
        repaint();
    }

    private void setupInput() {

        addMouseWheelListener(e -> {
            zoomIdx = (e.getWheelRotation() < 0)
                ? Math.min(zoomIdx + 1, ZOOM_LEVELS.length - 1)
                : Math.max(zoomIdx - 1, 0);
            repaint();
        });

        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                dragOX = e.getX() - camX;
                dragOY = e.getY() - camY;
            }
            @Override public void mouseClicked(MouseEvent e) {
                HexCell cell = pickCell(e.getX(), e.getY());
                if (cell != null && cellClickListener != null)
                    cellClickListener.on(cell);
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                camX = e.getX() - dragOX;
                camY = e.getY() - dragOY;
                repaint();
            }
        });
    }

    public void setCellClickListener(CellClick l) { this.cellClickListener = l; }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (map == null) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double zoom = ZOOM_LEVELS[zoomIdx];

        g2.translate(camX, camY);
        g2.scale(zoom, zoom);

        for (HexCell cell : map.getAllCells()) paintCell(g2, cell);
        paintUnits(g2);
        if (animUnit != null) paintAnimUnit(g2);

        g2.dispose();
    }

    private void paintCell(Graphics2D g, HexCell cell) {
        Point2D c = toScreen(cell.getQ(), cell.getR());
        Shape hex = makeHex(c);

        boolean explored = fog != null && fog.isExplored(cell.getQ(), cell.getR());
        boolean visible  = fog != null && fog.isVisible(cell.getQ(), cell.getR());

        if (!explored) {
            g.setColor(new Color(12, 12, 16));
            g.fill(hex);
            g.setColor(new Color(40, 40, 50));
            g.draw(hex);
            return;
        }

        Color base = terrainColor(cell.getTerrain());
        if (!visible) base = darken(base, 0.45f);
        g.setColor(base);
        g.fill(hex);

        if (explored) paintResourceDot(g, c, cell);

        if (cell.hasBuilding()) paintBuildingIcon(g, c, cell.getBuilding());

        if (highlighted.contains(cell)) {
            g.setColor(new Color(255, 255, 80, 60));
            g.fill(hex);
            g.setColor(new Color(255, 220, 50, 180));
            g.setStroke(new BasicStroke(2f));
            g.draw(hex);
            g.setStroke(new BasicStroke(1f));
        }

        if (cell.equals(selected)) {
            g.setColor(new Color(255, 200, 0, 130));
            g.fill(hex);
            g.setColor(Color.YELLOW);
            g.setStroke(new BasicStroke(2.5f));
            g.draw(hex);
            g.setStroke(new BasicStroke(1f));
            return;
        }

        g.setColor(cell.isInBorder() ? new Color(80, 160, 255, 100) : new Color(50, 50, 60));
        g.draw(hex);
    }

    private void paintResourceDot(Graphics2D g, Point2D c, HexCell cell) {
        if (cell.getDeposit() == null) return;
        int ox = (int) c.x() - 5, oy = (int) c.y() - 5;
        if (cell.getDeposit().isDepleted()) {
            g.setColor(new Color(80, 80, 80, 160));
        } else {
            g.setColor(resourceColor(cell.getDeposit().getType()));
        }
        g.fillOval(ox, oy, 11, 11);
        g.setColor(Color.BLACK);
        g.drawOval(ox, oy, 11, 11);
    }

    private void paintBuildingIcon(Graphics2D g, Point2D c, Building b) {
        String icon = buildingIcon(b);
        g.setFont(new Font("Dialog", Font.BOLD, 15));
        FontMetrics fm = g.getFontMetrics();
        int tx = (int) c.x() - fm.stringWidth(icon) / 2;
        int ty = (int) c.y() + fm.getAscent() / 2;
        g.setColor(new Color(0, 0, 0, 120));
        g.drawString(icon, tx + 1, ty + 1);
        g.setColor(Color.WHITE);
        g.drawString(icon, tx, ty);
    }

    private void paintUnits(Graphics2D g) {
        if (map == null) return;
        for (HexCell cell : map.getAllCells()) {
            if (cell.getUnits().isEmpty()) continue;
            if (fog != null && !fog.isVisible(cell.getQ(), cell.getR())) continue;
            Point2D c = toScreen(cell.getQ(), cell.getR());
            List<Unit> here = cell.getUnits();
            for (int i = 0; i < here.size(); i++) {
                Unit u = here.get(i);
                if (u == animUnit) continue;
                double ox = c.x() + (i - (here.size() - 1) / 2.0) * 12;
                paintUnitDot(g, u, (int) ox, (int) c.y());
            }
        }
    }

    private void paintAnimUnit(Graphics2D g) {
        if (animUnit == null) return;
        paintUnitDot(g, animUnit, (int) animX, (int) animY);
    }

    private void paintUnitDot(Graphics2D g, Unit u, int x, int y) {
        Color col = unitColor(u.getType());
        g.setColor(col);
        g.fillOval(x - 11, y - 11, 22, 22);
        g.setColor(col.darker());
        g.setStroke(new BasicStroke(2f));
        g.drawOval(x - 11, y - 11, 22, 22);
        g.setStroke(new BasicStroke(1f));

        String lbl = unitLabel(u.getType());
        g.setFont(new Font("Dialog", Font.BOLD, 11));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(Color.WHITE);
        g.drawString(lbl, x - fm.stringWidth(lbl) / 2, y + fm.getAscent() / 2 - 1);

        int maxW = 20;
        int apW = (int)((double) u.getCurrentAP() / u.getMaxAP() * maxW);
        g.setColor(new Color(40, 40, 40, 180));
        g.fillRect(x - maxW / 2, y + 12, maxW, 3);
        g.setColor(apColor(u));
        g.fillRect(x - maxW / 2, y + 12, apW, 3);
    }

    public void animMove(final Unit unit, HexCell from, HexCell to, final Runnable done) {
        Point2D p1 = toScreen(from.getQ(), from.getR());
        Point2D p2 = toScreen(to.getQ(),   to.getR());
        animUnit = unit;
        animX    = p1.x();
        animY    = p1.y();
        double dx = p2.x() - p1.x(), dy = p2.y() - p1.y();
        double dist = Math.sqrt(dx * dx + dy * dy);
        double speed = 8.0;
        animDX = dx / dist * speed;
        animDY = dy / dist * speed;
        final double targetX = p2.x(), targetY = p2.y();

        if (animTimer != null) animTimer.stop();
        animTimer = new Timer(14, e -> {
            animX += animDX; animY += animDY;
            if (Math.abs(animX - targetX) < Math.abs(animDX)
             && Math.abs(animY - targetY) < Math.abs(animDY)) {
                animUnit = null;
                animTimer.stop();
                done.run();
            }
            repaint();
        });
        animTimer.start();
    }

    public Point2D toScreen(int q, int r) {
        double px = HEX_SIZE * 1.5  * q;
        double py = HEX_SIZE * SQRT3 * (r + q / 2.0);
        return new Point2D(px, py);
    }

    public HexCell pickCell(int screenX, int screenY) {
        if (map == null) return null;
        double zoom = ZOOM_LEVELS[zoomIdx];

        double wx = (screenX - camX) / zoom;
        double wy = (screenY - camY) / zoom;

        double fq = wx * 2.0 / 3.0 / HEX_SIZE;
        double fr = (-wx / 3.0 + SQRT3 / 3.0 * wy) / HEX_SIZE;

        double fs = -fq - fr;
        int rq = (int) Math.round(fq);
        int rr = (int) Math.round(fr);
        int rs = (int) Math.round(fs);
        if (rq + rr + rs != 0) {
            double dq = Math.abs(rq - fq);
            double dr = Math.abs(rr - fr);
            double ds = Math.abs(rs - fs);
            if (dq > dr && dq > ds) rq = -rr - rs;
            else if (dr > ds)       rr = -rq - rs;
        }
        return map.getCell(rq, rr);
    }

    private Shape makeHex(Point2D c) {
        GeneralPath path = new GeneralPath();
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI / 3.0 * i;
            double vx = c.x() + HEX_SIZE * Math.cos(angle);
            double vy = c.y() + HEX_SIZE * Math.sin(angle);
            if (i == 0) path.moveTo(vx, vy);
            else        path.lineTo(vx, vy);
        }
        path.closePath();
        return path;
    }

    public void setHighlight(List<HexCell> cells) {
        highlighted.clear();
        highlighted.addAll(cells);
        repaint();
    }

    public void clearHighlight() { highlighted.clear(); repaint(); }
    public void setSelected(HexCell c) { selected = c; repaint(); }

    private Color terrainColor(TerrainType t) {
        return switch (t) {
            case PLAINS    -> new Color(195, 172, 85);
            case FOREST    -> new Color(34, 105, 36);
            case MOUNTAIN  -> new Color(115, 100, 90);
            case GRASSLAND -> new Color(75, 162, 60);
        };
    }

    private Color resourceColor(ResourceType t) {
        return switch (t) {
            case FOOD  -> new Color(255, 215, 50);
            case WOOD  -> new Color(139, 80, 20);
            case STONE -> new Color(180, 180, 190);
            case IRON  -> new Color(90, 100, 220);
        };
    }

    private Color unitColor(org.strategygame.model.unit.UnitType t) {
        return switch (t) {
            case EXPLORER        -> new Color(30, 144, 255);
            case BUILDER         -> new Color(255, 150, 30);
            case WORKER          -> new Color(50, 210, 80);
            case BORDER_EXPANDER -> new Color(200, 60, 210);
        };
    }

    private Color apColor(Unit u) {
        float ratio = (float) u.getCurrentAP() / u.getMaxAP();
        if (ratio > 0.6f) return new Color(50, 200, 80);
        if (ratio > 0.3f) return new Color(220, 170, 30);
        return new Color(210, 60, 60);
    }

    private Color darken(Color c, float factor) {
        return new Color((int)(c.getRed() * factor),
                         (int)(c.getGreen() * factor),
                         (int)(c.getBlue() * factor));
    }

    private String unitLabel(org.strategygame.model.unit.UnitType t) {
        return switch (t) {
            case EXPLORER        -> "E";
            case BUILDER         -> "B";
            case WORKER          -> "W";
            case BORDER_EXPANDER -> "X";
        };
    }

    private String buildingIcon(Building b) {
        return switch (b.getType()) {
            case TOWN_HALL   -> "⌂";
            case LUMBER_MILL -> "L";
            case STONE_MINE  -> "S";
            case IRON_MINE   -> "I";
            case FARM        -> "F";
            case STABLE      -> "T";
            case SETTLEMENT  -> "V";
        };
    }

    public record Point2D(double x, double y) { }
}
