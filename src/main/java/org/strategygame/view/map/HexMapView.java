package org.strategygame.view.map;

import org.strategygame.model.GameState;
import org.strategygame.model.building.Building;
import org.strategygame.model.building.BuildingType;
import org.strategygame.model.building.TownHall;
import org.strategygame.model.disaster.DisasterEvent;
import org.strategygame.model.disaster.DisasterType;
import org.strategygame.model.map.FogOfWar;
import org.strategygame.model.map.HexCell;
import org.strategygame.model.map.HexMap;
import org.strategygame.model.map.TerrainType;
import org.strategygame.model.map.Wall;
import org.strategygame.model.season.Season;
import org.strategygame.model.tribe.Tribe;
import org.strategygame.model.unit.Owner;
import org.strategygame.model.unit.Unit;
import org.strategygame.model.unit.UnitType;
import org.strategygame.view.BuildingPalette;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
    private boolean cameraCentered = false;

    private HexMap   map;
    private FogOfWar fog;

    private final List<HexCell> highlighted = new ArrayList<>();
    private HexCell selected = null;

    private Unit   animUnit;
    private double animX, animY, animDX, animDY;
    private Timer  animTimer;

    public interface CellClick { void on(HexCell cell); }
    private CellClick cellClickListener;

    /** اطلاعات سازه‌ای که روی یک هکس قابل ساخت است، برای رنگ‌آمیزی نقشه. */
    public record BuildHint(BuildingType type, boolean unlocked, boolean affordable) { }

    /** برای هر هکس می‌گوید چه سازه‌ای آنجا قابل ساخت است؛ {@code null} یعنی هیچ‌چیز. */
    public interface BuildOptions { BuildHint at(HexCell cell); }

    private BuildOptions buildOptions;
    private boolean showBuildOverlay = true;

    private HexCell builtFlashCell;
    private int     builtFlashLeft;
    private Timer   builtFlashTimer;

    /** وضعیت بازی برای کشیدن رودخانه، دیوار، اردوگاه، فصل و بلایا. */
    private GameState state;

    private DisasterEvent disaster;
    private int   disasterFramesLeft;
    private Timer disasterTimer;
    private int   shakeX, shakeY;

    /** ذره‌های برف زمستان و باران پاییز؛ فقط تصویری هستند. */
    private final List<double[]> weather = new ArrayList<>();
    private Timer  weatherTimer;
    private Season weatherSeason;
    private final Random weatherRandom = new Random(7);

    public HexMapView() {
        setBackground(new Color(15, 15, 20));
        setupInput();
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                if (!cameraCentered) centerCamera();
            }
        });
    }

    public void setMapData(HexMap map, FogOfWar fog) {
        this.map = map;
        this.fog = fog;
        repaint();
    }

    /** نقشه برای کشیدن یال‌ها، اردوگاه‌ها و افکت فصل به وضعیت بازی نیاز دارد. */
    public void setState(GameState state) {
        this.state = state;
        setMapData(state.getMap(), state.getFog());
        syncWeather(state.getSeason());
    }

    public void centerCamera() {
        if (map == null || getWidth() == 0 || getHeight() == 0) return;
        int cx = map.getWidth() / 2, cy = map.getHeight() / 2;
        Point2D center = toScreen(cx, cy);
        camX = getWidth()  / 2 - (int) center.x();
        camY = getHeight() / 2 - (int) center.y();
        cameraCentered = true;
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

        g2.translate(camX + shakeX, camY + shakeY);
        g2.scale(zoom, zoom);

        for (HexCell cell : map.getAllCells()) paintCell(g2, cell);
        paintRoads(g2);
        paintEdgeFeatures(g2);
        paintTribeCamps(g2);
        if (disaster != null) paintDisaster(g2);
        paintUnits(g2);
        if (animUnit != null) paintAnimUnit(g2);
        if (builtFlashCell != null) paintBuiltFlash(g2);

        g2.dispose();

        Graphics2D overlay = (Graphics2D) g.create();
        paintWeather(overlay);
        if (showBuildOverlay) paintLegend((Graphics2D) g.create());
        overlay.dispose();
    }

    // ------------------------------------------------------- جاده و یال‌ها
    private void paintRoads(Graphics2D g) {
        for (HexCell cell : map.getAllCells()) {
            if (!cell.hasRoad() || !isExplored(cell)) continue;

            Point2D c = toScreen(cell.getQ(), cell.getR());
            g.setColor(new Color(150, 120, 80, 220));
            g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            boolean connected = false;
            for (HexCell n : map.getNeighbors(cell)) {
                if (!n.hasRoad()) continue;
                connected = true;
                Point2D nc = toScreen(n.getQ(), n.getR());
                g.drawLine((int) c.x(), (int) c.y(),
                        (int) ((c.x() + nc.x()) / 2), (int) ((c.y() + nc.y()) / 2));
            }
            if (!connected) {
                g.fillOval((int) c.x() - 5, (int) c.y() - 5, 10, 10);
            }
            g.setStroke(new BasicStroke(1f));
        }
    }

    /** رودخانه و دیوار روی یال بین دو هکس کشیده می‌شوند، نه داخل هکس. */
    private void paintEdgeFeatures(Graphics2D g) {
        for (HexCell cell : map.getAllCells()) {
            if (!isExplored(cell)) continue;
            for (HexCell n : map.getNeighbors(cell)) {
                if (cell.getQ() > n.getQ() || (cell.getQ() == n.getQ() && cell.getR() > n.getR()))
                    continue;

                if (map.hasRiver(cell, n)) {
                    paintEdgeLine(g, cell, n, new Color(70, 150, 240), 5f);
                }
                Wall wall = map.getWall(cell, n);
                if (wall != null) {
                    float ratio = (float) wall.getHp() / wall.getMaxHp();
                    Color color = ratio > 0.6f ? new Color(210, 210, 215)
                                : ratio > 0.3f ? new Color(200, 170, 110)
                                               : new Color(200, 110, 100);
                    paintEdgeLine(g, cell, n, color, 7f);
                }
            }
        }
    }

    private void paintEdgeLine(Graphics2D g, HexCell a, HexCell b, Color color, float width) {
        Point2D pa = toScreen(a.getQ(), a.getR());
        Point2D pb = toScreen(b.getQ(), b.getR());

        double mx = (pa.x() + pb.x()) / 2.0, my = (pa.y() + pb.y()) / 2.0;
        double dx = pb.x() - pa.x(),        dy = pb.y() - pa.y();
        double len = Math.hypot(dx, dy);
        if (len < 1e-6) return;

        // یال مشترک عمود بر خط مرکز دو هکس است و طول آن یک ضلع شش‌ضلعی است
        double px = -dy / len * (HEX_SIZE / 2.0);
        double py =  dx / len * (HEX_SIZE / 2.0);

        g.setColor(color);
        g.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine((int) (mx - px), (int) (my - py), (int) (mx + px), (int) (my + py));
        g.setStroke(new BasicStroke(1f));
    }

    // ----------------------------------------------------------- اردوگاه‌ها
    private void paintTribeCamps(Graphics2D g) {
        if (state == null) return;

        for (Tribe tribe : state.getTribes()) {
            HexCell camp = tribe.getCampHex();
            if (camp == null || tribe.isDefeated() || !isExplored(camp)) continue;

            Point2D c = toScreen(camp.getQ(), camp.getR());
            Color color = relationColor(tribe);

            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 70));
            g.fill(makeHex(c));
            g.setColor(color);
            g.setStroke(new BasicStroke(3f));
            g.draw(makeHex(c));
            g.setStroke(new BasicStroke(1f));

            String title = "▲ " + tribe.getName();
            String info  = tribe.getRelationState().getLabel() + " " + tribe.getRelation()
                    + " | HP " + tribe.getCamp().getHp() + "/" + tribe.getCamp().getMaxHp();
            if (!isVisible(camp)) info += " (اطلاعات قدیمی)";

            drawTag(g, (int) c.x(), (int) c.y() - 6, title, color, Font.BOLD, 13);
            drawTag(g, (int) c.x(), (int) c.y() + 12, info, new Color(230, 235, 245), Font.PLAIN, 10);
        }
    }

    private Color relationColor(Tribe tribe) {
        if (tribe.isAtWar())  return new Color(255, 90, 90);
        if (tribe.isAllied()) return new Color(120, 255, 190);
        return switch (tribe.getRelationState()) {
            case ENEMY      -> new Color(255, 90, 90);
            case DISPLEASED -> new Color(255, 175, 90);
            case NEUTRAL    -> new Color(220, 220, 230);
            case FRIENDLY   -> new Color(150, 235, 160);
            case ALLIED     -> new Color(120, 255, 190);
        };
    }

    private void drawTag(Graphics2D g, int cx, int cy, String text,
                         Color color, int style, int size) {
        g.setFont(new Font("Dialog", style, size));
        FontMetrics fm = g.getFontMetrics();
        int x = cx - fm.stringWidth(text) / 2;
        g.setColor(new Color(0, 0, 0, 165));
        g.fillRoundRect(x - 5, cy - fm.getAscent(), fm.stringWidth(text) + 10, fm.getHeight(), 7, 7);
        g.setColor(color);
        g.drawString(text, x, cy);
    }

    // -------------------------------------------------------------- بلایا
    /** انیمیشن بلای طبیعی؛ فقط برای رویدادهای داخل دید بازیکن صدا زده می‌شود. */
    public void playDisaster(DisasterEvent event) {
        this.disaster = event;
        this.disasterFramesLeft = 34;

        if (disasterTimer != null) disasterTimer.stop();
        disasterTimer = new Timer(60, e -> {
            disasterFramesLeft--;

            if (disaster != null && disaster.getType() == DisasterType.EARTHQUAKE
                    && disasterFramesLeft > 18) {
                shakeX = weatherRandom.nextInt(11) - 5;
                shakeY = weatherRandom.nextInt(11) - 5;
            } else {
                shakeX = 0; shakeY = 0;
            }

            if (disasterFramesLeft <= 0) {
                disaster = null;
                shakeX = 0; shakeY = 0;
                disasterTimer.stop();
            }
            repaint();
        });
        disasterTimer.start();
        repaint();
    }

    private void paintDisaster(Graphics2D g) {
        for (HexCell cell : disaster.getAffected()) {
            Point2D c = toScreen(cell.getQ(), cell.getR());
            Shape hex = makeHex(c);

            switch (disaster.getType()) {
                case EARTHQUAKE -> {
                    g.setColor(new Color(120, 80, 50, 110));
                    g.fill(hex);
                    g.setColor(new Color(40, 25, 20, 220));
                    g.setStroke(new BasicStroke(2f));
                    int x = (int) c.x(), y = (int) c.y();
                    g.drawLine(x - 14, y - 10, x - 2, y + 2);
                    g.drawLine(x - 2,  y + 2,  x + 6, y - 6);
                    g.drawLine(x + 6,  y - 6,  x + 15, y + 9);
                    g.setStroke(new BasicStroke(1f));
                }
                case FLOOD -> {
                    int alpha = 90 + (disasterFramesLeft % 6) * 12;
                    g.setColor(new Color(60, 130, 235, Math.min(190, alpha)));
                    g.fill(hex);
                    g.setColor(new Color(190, 225, 255, 200));
                    for (int i = -1; i <= 1; i++) {
                        int y = (int) c.y() + i * 9 + (disasterFramesLeft % 4);
                        g.drawArc((int) c.x() - 15, y, 14, 6, 0, 180);
                        g.drawArc((int) c.x() + 1,  y, 14, 6, 180, 180);
                    }
                }
                case BEAR_ATTACK -> {
                    g.setColor(new Color(120, 70, 30, 110));
                    g.fill(hex);
                    drawTag(g, (int) c.x(), (int) c.y() - HEX_SIZE + 14,
                            "حمله‌ی خرس", new Color(255, 200, 150), Font.BOLD, 12);
                }
            }
        }
    }

    // ---------------------------------------------------------- افکت فصل
    /** برف زمستان و باران پاییز؛ با عوض شدن فصل افکت قبلی متوقف می‌شود. */
    public void syncWeather(Season season) {
        if (season == weatherSeason) return;
        weatherSeason = season;
        weather.clear();

        if (weatherTimer != null) { weatherTimer.stop(); weatherTimer = null; }
        if (season != Season.WINTER && season != Season.AUTUMN) { repaint(); return; }

        int count = season == Season.WINTER ? 110 : 150;
        for (int i = 0; i < count; i++) weather.add(newParticle(true));

        weatherTimer = new Timer(55, e -> {
            double speed = weatherSeason == Season.WINTER ? 1.6 : 6.0;
            double drift = weatherSeason == Season.WINTER ? 0.6 : 2.2;
            for (double[] p : weather) {
                p[1] += speed * p[2];
                p[0] += drift;
                if (p[1] > Math.max(1, getHeight())) {
                    double[] fresh = newParticle(false);
                    p[0] = fresh[0]; p[1] = fresh[1]; p[2] = fresh[2];
                }
            }
            repaint();
        });
        weatherTimer.start();
    }

    private double[] newParticle(boolean anywhere) {
        int w = Math.max(1, getWidth());
        int h = Math.max(1, getHeight());
        double x = weatherRandom.nextInt(w + 200) - 100;
        double y = anywhere ? weatherRandom.nextInt(h) : -weatherRandom.nextInt(40) - 5;
        double scale = 0.6 + weatherRandom.nextDouble();
        return new double[]{x, y, scale};
    }

    private void paintWeather(Graphics2D g) {
        if (weather.isEmpty() || weatherSeason == null) return;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (weatherSeason == Season.WINTER) {
            g.setColor(new Color(235, 245, 255, 190));
            for (double[] p : weather) {
                int size = (int) (2 + p[2] * 2);
                g.fillOval((int) p[0], (int) p[1], size, size);
            }
        } else {
            g.setColor(new Color(150, 190, 235, 150));
            g.setStroke(new BasicStroke(1.4f));
            for (double[] p : weather) {
                int len = (int) (7 + p[2] * 6);
                g.drawLine((int) p[0], (int) p[1], (int) p[0] - 3, (int) p[1] + len);
            }
            g.setStroke(new BasicStroke(1f));
        }
    }

    private boolean isExplored(HexCell cell) {
        return fog == null || fog.isExplored(cell.getQ(), cell.getR());
    }

    private boolean isVisible(HexCell cell) {
        return fog == null || fog.isVisible(cell.getQ(), cell.getR());
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

        if (showBuildOverlay && !cell.hasBuilding()) paintBuildOverlay(g, hex, c, cell);

        if (explored) paintResourceIcon(g, c, cell);

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

    private void paintResourceIcon(Graphics2D g, Point2D c, HexCell cell) {
        if (cell.getDeposit() == null) return;
        int cx = (int) c.x();
        int cy = (int) c.y() + (cell.hasBuilding() ? -14 : 0);

        if (cell.getDeposit().isDepleted()) {
            paintDepletedIcon(g, cx, cy);
            return;
        }
        switch (cell.getDeposit().getType()) {
            case STONE -> paintStoneIcon(g, cx, cy);
            case WOOD  -> paintWoodIcon(g, cx, cy);
            case FOOD  -> paintWheatIcon(g, cx, cy);
            case IRON  -> paintIronIcon(g, cx, cy);
        }
    }

    private void paintStoneIcon(Graphics2D g, int cx, int cy) {
        GeneralPath rock = new GeneralPath();
        rock.moveTo(cx - 8, cy + 4);
        rock.lineTo(cx - 6, cy - 3);
        rock.lineTo(cx,     cy - 6);
        rock.lineTo(cx + 6, cy - 3);
        rock.lineTo(cx + 8, cy + 4);
        rock.lineTo(cx + 3, cy + 6);
        rock.lineTo(cx - 3, cy + 6);
        rock.closePath();
        g.setColor(new Color(165, 165, 172));
        g.fill(rock);
        g.setColor(new Color(90, 90, 98));
        g.setStroke(new BasicStroke(1.5f));
        g.draw(rock);
        g.setColor(new Color(210, 210, 216));
        g.drawLine(cx - 3, cy - 2, cx + 2, cy - 4);
        g.setStroke(new BasicStroke(1f));
    }

    private void paintWoodIcon(Graphics2D g, int cx, int cy) {
        Color bark  = new Color(120, 72, 30);
        Color inner = new Color(190, 130, 70);
        for (int i = 0; i < 2; i++) {
            int ly = cy - 4 + i * 8;
            g.setColor(bark);
            g.fillRoundRect(cx - 9, ly, 18, 7, 6, 6);
            g.setColor(inner);
            g.fillOval(cx - 9, ly, 7, 7);
            g.setColor(bark.darker());
            g.drawOval(cx - 9, ly, 7, 7);
        }
    }

    private void paintWheatIcon(Graphics2D g, int cx, int cy) {
        Color grain = new Color(226, 178, 40);
        g.setColor(grain);
        g.setStroke(new BasicStroke(1.6f));
        int[] dx = {-6, 0, 6};
        for (int sx : dx) {
            int topX = cx + sx / 2;
            g.drawLine(cx + sx, cy + 7, topX, cy - 7);
            for (int k = 0; k < 4; k++) {
                int gy = cy - 6 + k * 3;
                int gx = topX + (sx == 0 ? 0 : (sx > 0 ? 1 : -1)) * (k);
                g.drawLine(gx, gy, gx - 3, gy - 2);
                g.drawLine(gx, gy, gx + 3, gy - 2);
            }
        }
        g.setStroke(new BasicStroke(1f));
    }

    private void paintIronIcon(Graphics2D g, int cx, int cy) {
        Color ore   = new Color(95, 110, 200);
        Color light = new Color(160, 175, 245);
        int[][] gems = {{cx - 4, cy}, {cx + 4, cy + 2}};
        for (int[] p : gems) {
            int gx = p[0], gy = p[1];
            GeneralPath d = new GeneralPath();
            d.moveTo(gx, gy - 6);
            d.lineTo(gx + 5, gy);
            d.lineTo(gx, gy + 6);
            d.lineTo(gx - 5, gy);
            d.closePath();
            g.setColor(ore);
            g.fill(d);
            g.setColor(new Color(40, 50, 110));
            g.draw(d);
            g.setColor(light);
            g.drawLine(gx, gy - 5, gx - 2, gy);
        }
    }

    private void paintDepletedIcon(Graphics2D g, int cx, int cy) {
        g.setColor(new Color(90, 90, 90, 150));
        g.fillOval(cx - 6, cy - 6, 12, 12);
        g.setColor(new Color(60, 60, 60, 180));
        g.drawOval(cx - 6, cy - 6, 12, 12);
        g.drawLine(cx - 4, cy + 4, cx + 4, cy - 4);
    }

    /** رنگ‌آمیزی هکس بر اساس سازه‌ای که روی آن قابل ساخت است. */
    private void paintBuildOverlay(Graphics2D g, Shape hex, Point2D c, HexCell cell) {
        if (buildOptions == null) return;
        BuildHint hint = buildOptions.at(cell);
        if (hint == null) return;

        Color col = BuildingPalette.of(hint.type());
        int fillAlpha   = hint.unlocked() ? (hint.affordable() ? 95 : 55) : 35;
        int strokeAlpha = hint.unlocked() ? (hint.affordable() ? 240 : 150) : 110;

        g.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), fillAlpha));
        g.fill(hex);
        g.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), strokeAlpha));
        g.setStroke(new BasicStroke(hint.affordable() && hint.unlocked() ? 3f : 2f));
        g.draw(hex);
        g.setStroke(new BasicStroke(1f));

        String mark = BuildingPalette.marker(hint.type())
                + (!hint.unlocked() ? "🔒" : (hint.affordable() ? "" : "!"));
        g.setFont(new Font("Dialog", Font.BOLD, 12));
        FontMetrics fm = g.getFontMetrics();
        int bx = (int) c.x() - fm.stringWidth(mark) / 2;
        int by = (int) c.y() + HEX_SIZE - 8;
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRoundRect(bx - 4, by - fm.getAscent(), fm.stringWidth(mark) + 8, fm.getHeight(), 6, 6);
        g.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 255));
        g.drawString(mark, bx, by);
    }

    private void paintBuildingIcon(Graphics2D g, Point2D c, Building b) {
        String icon = buildingIcon(b);
        if (b instanceof TownHall th) icon += " L" + th.getLevel();

        g.setFont(new Font("Dialog", Font.BOLD, 15));
        FontMetrics fm = g.getFontMetrics();
        int tx = (int) c.x() - fm.stringWidth(icon) / 2;
        int ty = (int) c.y() + fm.getAscent() / 2;

        Color col = BuildingPalette.of(b.getType());
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRoundRect(tx - 6, ty - fm.getAscent(), fm.stringWidth(icon) + 12, fm.getHeight(), 8, 8);
        g.setColor(col);
        g.drawString(icon, tx, ty);

        String name = b.getType().getLabel() + (b.isFunctional() ? "" : " (خراب)");
        g.setFont(new Font("Dialog", Font.PLAIN, 11));
        FontMetrics nfm = g.getFontMetrics();
        int nx = (int) c.x() - nfm.stringWidth(name) / 2;
        int ny = (int) c.y() + fm.getHeight() + 6;
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRoundRect(nx - 4, ny - nfm.getAscent(), nfm.stringWidth(name) + 8, nfm.getHeight(), 6, 6);
        g.setColor(b.isFunctional() ? Color.WHITE : new Color(255, 130, 130));
        g.drawString(name, nx, ny);
    }

    /** نشانه‌ی موقتی «ساخته شد» روی هکسی که همین الان سازه‌اش ساخته شده. */
    private void paintBuiltFlash(Graphics2D g) {
        Point2D c = toScreen(builtFlashCell.getQ(), builtFlashCell.getR());
        boolean on = (builtFlashLeft / 4) % 2 == 0;

        g.setColor(new Color(90, 255, 120, on ? 235 : 120));
        g.setStroke(new BasicStroke(4f));
        g.draw(makeHex(c));
        g.setStroke(new BasicStroke(1f));

        String txt = "✔ ساخته شد";
        g.setFont(new Font("Dialog", Font.BOLD, 13));
        FontMetrics fm = g.getFontMetrics();
        int tx = (int) c.x() - fm.stringWidth(txt) / 2;
        int ty = (int) c.y() - HEX_SIZE + 2;
        g.setColor(new Color(20, 60, 25, 220));
        g.fillRoundRect(tx - 6, ty - fm.getAscent(), fm.stringWidth(txt) + 12, fm.getHeight(), 8, 8);
        g.setColor(new Color(140, 255, 160));
        g.drawString(txt, tx, ty);
    }

    /** راهنمای رنگ‌ها؛ در مختصات صفحه کشیده می‌شود تا با زوم بزرگ/کوچک نشود. */
    private void paintLegend(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setFont(new Font("Dialog", Font.PLAIN, 11));
        FontMetrics fm = g.getFontMetrics();

        List<BuildingType> types = new ArrayList<>();
        for (BuildingType t : BuildingType.values()) {
            if (t.isPlayerBuildable()) types.add(t);
        }
        int rows = types.size();
        int lineH = fm.getHeight() + 3;

        String footer = "! = منابع کم    🔒 = تکنولوژی لازم";
        int textW = fm.stringWidth(footer);
        for (BuildingType t : types) {
            textW = Math.max(textW, fm.stringWidth(legendRow(t)));
        }
        int w = textW + 50, h = 24 + rows * lineH + fm.getHeight() + 8;
        int x = 12, y = getHeight() - h - 12;

        g.setColor(new Color(12, 16, 26, 215));
        g.fillRoundRect(x, y, w, h, 10, 10);
        g.setColor(new Color(70, 90, 130));
        g.drawRoundRect(x, y, w, h, 10, 10);

        g.setColor(new Color(255, 200, 60));
        g.setFont(new Font("Dialog", Font.BOLD, 12));
        g.drawString("رنگ سازه‌های قابل ساخت", x + 10, y + 16);
        g.setFont(new Font("Dialog", Font.PLAIN, 11));

        int ly = y + 20;
        for (BuildingType t : types) {
            ly += lineH;
            Color col = BuildingPalette.of(t);
            g.setColor(col);
            g.fillRoundRect(x + 10, ly - 9, 14, 11, 4, 4);
            g.setColor(new Color(0, 0, 0, 160));
            g.drawRoundRect(x + 10, ly - 9, 14, 11, 4, 4);
            g.setColor(new Color(220, 230, 245));
            g.drawString(legendRow(t), x + 30, ly);
        }

        g.setColor(new Color(150, 165, 190));
        g.drawString(footer, x + 10, y + h - 8);
        g.dispose();
    }

    private String legendRow(BuildingType t) {
        return BuildingPalette.marker(t) + " — " + t.getLabel() + "  (" + t.costText() + ")";
    }

    public void setBuildOptions(BuildOptions o)  { this.buildOptions = o; repaint(); }
    public boolean isBuildOverlayVisible()       { return showBuildOverlay; }

    public void setShowBuildOverlay(boolean v) {
        this.showBuildOverlay = v;
        repaint();
    }

    /** هکس تازه‌ساخته‌شده را چند ثانیه چشمک‌زن نشان می‌دهد. */
    public void flashBuilt(HexCell cell) {
        builtFlashCell = cell;
        builtFlashLeft = 40;
        if (builtFlashTimer != null) builtFlashTimer.stop();
        builtFlashTimer = new Timer(90, e -> {
            if (--builtFlashLeft <= 0) {
                builtFlashCell = null;
                builtFlashTimer.stop();
            }
            repaint();
        });
        builtFlashTimer.start();
        repaint();
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

        // حلقه‌ی دور یونیت مالک آن را نشان می‌دهد: خودی، قبیله یا حیوان وحشی
        g.setColor(ownerColor(u.getOwner()));
        g.setStroke(new BasicStroke(u.getOwner() == Owner.PLAYER ? 2f : 3f));
        g.drawOval(x - 11, y - 11, 22, 22);
        g.setStroke(new BasicStroke(1f));

        String lbl = unitLabel(u.getType());
        g.setFont(new Font("Dialog", Font.BOLD, 11));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(u.getType() == UnitType.SWORDSMAN ? new Color(30, 30, 40) : Color.WHITE);
        g.drawString(lbl, x - fm.stringWidth(lbl) / 2, y + fm.getAscent() / 2 - 1);

        int maxW = 20;
        g.setColor(new Color(40, 40, 40, 180));
        g.fillRect(x - maxW / 2, y + 12, maxW, 3);
        g.setColor(apColor(u));
        g.fillRect(x - maxW / 2, y + 12,
                (int) ((double) u.getCurrentAP() / u.getMaxAP() * maxW), 3);

        int hpW = (int) ((double) u.getHp() / u.getMaxHp() * maxW);
        g.setColor(new Color(40, 40, 40, 180));
        g.fillRect(x - maxW / 2, y + 16, maxW, 3);
        g.setColor(hpColor(u));
        g.fillRect(x - maxW / 2, y + 16, hpW, 3);
    }

    private Color ownerColor(Owner owner) {
        return switch (owner) {
            case PLAYER -> new Color(20, 30, 45);
            case TRIBE  -> new Color(255, 80, 80);
            case WILD   -> new Color(255, 170, 60);
        };
    }

    private Color hpColor(Unit u) {
        double ratio = (double) u.getHp() / u.getMaxHp();
        if (ratio > 0.6) return new Color(90, 200, 255);
        if (ratio > 0.3) return new Color(230, 190, 60);
        return new Color(230, 80, 80);
    }

    public void animMove(final Unit unit, HexCell from, HexCell to, final Runnable done) {
        Point2D p1 = toScreen(from.getQ(), from.getR());
        Point2D p2 = toScreen(to.getQ(),   to.getR());
        animUnit = unit;
        animX    = p1.x();
        animY    = p1.y();
        double dx = p2.x() - p1.x(), dy = p2.y() - p1.y();
        double dist = Math.sqrt(dx * dx + dy * dy);
        final double speed = 8.0;

        if (dist < 1e-6) { animUnit = null; done.run(); repaint(); return; }
        animDX = dx / dist * speed;
        animDY = dy / dist * speed;
        final double targetX = p2.x(), targetY = p2.y();

        if (animTimer != null) animTimer.stop();
        animTimer = new Timer(14, e -> {
            animX += animDX; animY += animDY;

            double remaining = Math.hypot(targetX - animX, targetY - animY);
            if (remaining <= speed) {
                animX = targetX; animY = targetY;
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
            case PLAINS         -> new Color(195, 172, 85);
            case FOREST         -> new Color(34, 105, 36);
            case MOUNTAIN       -> new Color(115, 100, 90);
            case MOUNTAIN_RANGE -> new Color(62, 56, 54);
            case GRASSLAND      -> new Color(75, 162, 60);
            case SEA            -> new Color(28, 78, 150);
        };
    }

    private Color unitColor(UnitType t) {
        return switch (t) {
            case EXPLORER        -> new Color(30, 144, 255);
            case BUILDER         -> new Color(255, 150, 30);
            case WORKER          -> new Color(50, 210, 80);
            case BORDER_EXPANDER -> new Color(200, 60, 210);
            case SWORDSMAN       -> new Color(225, 225, 235);
            case ARCHER          -> new Color(190, 235, 130);
            case CAVALRY         -> new Color(255, 205, 90);
            case BEAR            -> new Color(140, 85, 45);
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

    private String unitLabel(UnitType t) {
        return switch (t) {
            case EXPLORER        -> "E";
            case BUILDER         -> "B";
            case WORKER          -> "W";
            case BORDER_EXPANDER -> "X";
            case SWORDSMAN       -> "S";
            case ARCHER          -> "A";
            case CAVALRY         -> "C";
            case BEAR            -> "🐾";
        };
    }

    private String buildingIcon(Building b) {
        return BuildingPalette.marker(b.getType());
    }

    public record Point2D(double x, double y) { }
}
