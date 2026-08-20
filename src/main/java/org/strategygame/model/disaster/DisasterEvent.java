package org.strategygame.model.disaster;

import org.strategygame.model.map.HexCell;

import java.util.ArrayList;
import java.util.List;

/**
 * نتیجه‌ی یک بلای طبیعی. کاملا مستقل از View است؛ View با خواندن همین شیء
 * تصمیم می‌گیرد انیمیشن اجرا کند یا فقط هشدار متنی بدهد.
 */
public class DisasterEvent {

    private final DisasterType  type;
    private final int           turn;
    private final HexCell       center;
    private final List<HexCell> affected = new ArrayList<>();
    private final List<String>  effects  = new ArrayList<>();
    private boolean visible = false;

    public DisasterEvent(DisasterType type, int turn, HexCell center) {
        this.type   = type;
        this.turn   = turn;
        this.center = center;
    }

    public DisasterType getType()      { return type; }
    public int getTurn()               { return turn; }
    public HexCell getCenter()         { return center; }
    public List<HexCell> getAffected() { return affected; }
    public List<String> getEffects()   { return effects; }

    /** اگر رویداد داخل دید بازیکن باشد انیمیشن اجرا می‌شود، وگرنه فقط هشدار. */
    public boolean isVisible()          { return visible; }
    public void setVisible(boolean v)   { this.visible = v; }

    public void addAffected(HexCell cell) { if (cell != null) affected.add(cell); }
    public void addEffect(String text)    { if (text != null) effects.add(text); }

    public String headline() {
        String where = center == null ? "" : " در هکس (" + center.getQ() + "," + center.getR() + ")";
        return type.getLabel() + where;
    }

    public String report() {
        StringBuilder sb = new StringBuilder(headline()).append('\n');
        if (!visible) sb.append("این رویداد خارج از دید شما رخ داد.\n");
        sb.append("هکس‌های تحت تأثیر: ").append(affected.size()).append('\n');
        if (effects.isEmpty()) {
            sb.append("خسارتی ثبت نشد.\n");
        } else {
            for (String e : effects) sb.append("• ").append(e).append('\n');
        }
        return sb.toString();
    }
}
