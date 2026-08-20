package org.strategygame.model.tribe.mission;

import org.strategygame.model.GameState;
import org.strategygame.model.building.BuildingType;
import org.strategygame.model.tribe.Tribe;

/**
 * مأموریت قبیله‌ای. هر قبیله در هر لحظه حداکثر یک مأموریت فعال دارد و
 * چرخه‌ی حالت آن در همین کلاس نگه داشته می‌شود تا جایزه دوبار Claim نشود.
 */
public abstract class TribeMission {

    private final int  deadlineTurns;
    private MissionState state = MissionState.AVAILABLE;
    private int  turnsLeft;
    private boolean rewardClaimed = false;

    protected TribeMission(int deadlineTurns) {
        this.deadlineTurns = deadlineTurns;
        this.turnsLeft     = deadlineTurns;
    }

    public abstract String getTitle();

    /** توضیح کاری که بازیکن باید انجام دهد. */
    public abstract String getObjectiveText();

    /** آیا شرط تکمیل مأموریت برقرار است. */
    public abstract boolean isObjectiveMet(GameState state, Tribe tribe);

    /** منابعی که در لحظه‌ی تحویل از بازیکن کم می‌شود. */
    public int[] getPaymentCost() { return new int[]{0, 0, 0, 0}; }

    /** منابع جایزه. */
    public int[] getRewardResources() { return new int[]{0, 0, 0, 0}; }

    public abstract int getRewardRelation();

    /** جایزه‌ی غیرمنبعی (یونیت کمکی، تخفیف و ...). */
    public String getSpecialRewardText() { return null; }

    /** اعمال جایزه‌ی غیرمنبعی. */
    public void grantSpecialReward(GameState state, Tribe tribe) { }

    // ------------------------------------------------------------ چرخه‌ی حالت
    public void restoreRuntime(MissionState saved, int turnsLeft, boolean rewardClaimed) {
        if (saved != null) this.state = saved;
        this.turnsLeft = Math.max(0, turnsLeft);
        this.rewardClaimed = rewardClaimed;
    }

    public MissionState getState() { return state; }
    public int getDeadlineTurns()  { return deadlineTurns; }
    public int getTurnsLeft()      { return turnsLeft; }
    public boolean isRewardClaimed() { return rewardClaimed; }

    public boolean isOpen() {
        return state == MissionState.ACTIVE || state == MissionState.READY_TO_TURN_IN;
    }

    public void accept() {
        if (state == MissionState.AVAILABLE) state = MissionState.ACTIVE;
    }

    /** یک نوبت از مهلت کم می‌کند؛ اگر مهلت تمام شود مأموریت شکست می‌خورد. */
    public boolean tickDeadline() {
        if (!isOpen()) return false;
        if (turnsLeft > 0) turnsLeft--;
        if (turnsLeft <= 0 && state != MissionState.READY_TO_TURN_IN) {
            state = MissionState.FAILED;
            return true;
        }
        return false;
    }

    /** با برقرار شدن شرط، مأموریت آماده‌ی تحویل می‌شود. */
    public void markReadyToTurnIn() {
        if (state == MissionState.ACTIVE) state = MissionState.READY_TO_TURN_IN;
    }

    /** برگشت به حالت فعال اگر شرط دیگر برقرار نباشد. */
    public void markNotReady() {
        if (state == MissionState.READY_TO_TURN_IN) state = MissionState.ACTIVE;
    }

    /** تحویل مأموریت؛ فقط یک بار ممکن است. */
    public boolean turnIn() {
        if (state != MissionState.READY_TO_TURN_IN || rewardClaimed) return false;
        rewardClaimed = true;
        state = MissionState.COMPLETED;
        return true;
    }

    public void fail()   { if (!state.isFinished()) state = MissionState.FAILED; }
    public void cancel() { if (!state.isFinished()) state = MissionState.CANCELLED; }

    public String rewardText() {
        StringBuilder sb = new StringBuilder();
        String res = BuildingType.formatCost(getRewardResources());
        if (!res.equals("رایگان")) sb.append(res);
        if (sb.length() > 0) sb.append(" + ");
        sb.append("رابطه +").append(getRewardRelation());
        if (getSpecialRewardText() != null) sb.append(" + ").append(getSpecialRewardText());
        return sb.toString();
    }

    public String summary() {
        return getTitle() + " — " + state.getLabel()
                + " (" + turnsLeft + "/" + deadlineTurns + " نوبت)";
    }
}
