package org.strategygame.controller;

import org.strategygame.common.ActionResult;
import org.strategygame.model.GameState;
import org.strategygame.model.resource.ResourceType;
import org.strategygame.model.tribe.Tribe;
import org.strategygame.model.tribe.mission.MissionFactory;
import org.strategygame.model.tribe.mission.TribeMission;

import java.util.ArrayList;
import java.util.List;

/** دیپلماسی و مأموریت‌های قبیله‌ها. */
public class TribeController {

    private final GameState    state;
    private final GameServices services;

    public TribeController(GameState state, GameServices services) {
        this.state    = state;
        this.services = services;
    }

    /** قبیله‌های کشف‌شده‌ای که هنوز پابرجا هستند و می‌شود با آن‌ها تعامل کرد. */
    public List<Tribe> knownTribes() {
        List<Tribe> list = new ArrayList<>();
        for (Tribe t : state.getTribes()) {
            if (t.isDiscovered() && !t.isDefeated()) list.add(t);
        }
        return list;
    }

    /** قبیله‌های فتح‌شده؛ فقط برای نمایش سابقه، دیگر تعاملی ندارند. */
    public List<Tribe> conqueredTribes() {
        List<Tribe> list = new ArrayList<>();
        for (Tribe t : state.getTribes()) {
            if (t.isDefeated()) list.add(t);
        }
        return list;
    }

    public boolean isCampVisible(Tribe tribe) {
        return services.tribe().isCampVisible(state, tribe);
    }

    // ----------------------------------------------------------------- هدیه
    public String giftProblem(Tribe tribe, ResourceType type, int amount) {
        return services.tribe().giftProblem(state, tribe, type, amount);
    }

    public ActionResult gift(Tribe tribe, ResourceType type, int amount) {
        return services.tribe().gift(state, tribe, type, amount);
    }

    public int giftUnit(ResourceType type)        { return services.tribe().giftUnit(type); }
    public int giftGainPerUnit(ResourceType type) { return services.tribe().giftGainPerUnit(type); }

    // ------------------------------------------------------------ جنگ و صلح
    public String declareWarProblem(Tribe tribe) {
        return services.tribe().declareWarProblem(state, tribe);
    }

    public ActionResult declareWar(Tribe tribe) {
        return services.tribe().declareWar(state, tribe);
    }

    public String peaceProblem(Tribe tribe) {
        return services.tribe().peaceProblem(state, tribe);
    }

    public ActionResult makePeace(Tribe tribe) {
        return services.tribe().makePeace(state, tribe);
    }

    public String allianceProblem(Tribe tribe) {
        return services.tribe().allianceProblem(state, tribe);
    }

    public ActionResult formAlliance(Tribe tribe) {
        return services.tribe().formAlliance(state, tribe);
    }

    // -------------------------------------------------------------- مأموریت
    public String acceptMissionProblem(Tribe tribe) {
        return services.tribe().acceptMissionProblem(state, tribe);
    }

    public ActionResult acceptMission(Tribe tribe) {
        return services.tribe().acceptMission(state, tribe);
    }

    public String turnInProblem(Tribe tribe) {
        return services.tribe().turnInProblem(state, tribe);
    }

    public ActionResult turnInMission(Tribe tribe) {
        return services.tribe().turnInMission(state, tribe);
    }

    public ActionResult cancelMission(Tribe tribe) {
        return services.tribe().cancelMission(state, tribe);
    }

    /** پیش‌نمایش مأموریتی که این قبیله پیشنهاد می‌دهد، قبل از پذیرش. */
    public String missionPreview(Tribe tribe) {
        if (tribe == null) return "—";
        TribeMission mission = MissionFactory.create(tribe);
        return mission.getTitle() + " — " + mission.getObjectiveText()
                + "\nمهلت: " + mission.getDeadlineTurns() + " نوبت"
                + "\nجایزه: " + mission.rewardText();
    }
}
