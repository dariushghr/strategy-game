package org.strategygame.controller;

import org.strategygame.model.event.EventBus;
import org.strategygame.save.SaveService;
import org.strategygame.service.BuildService;
import org.strategygame.service.CombatService;
import org.strategygame.service.DisasterService;
import org.strategygame.service.HappinessService;
import org.strategygame.service.MovementService;
import org.strategygame.service.ProductionService;
import org.strategygame.service.TradeService;
import org.strategygame.service.TribeService;
import org.strategygame.service.TribeTurnService;

/**
 * یک نمونه از هر سرویس برای کل بازی. کنترلرها سرویس نمی‌سازند تا وضعیت
 * مشترک (مثل سرویس تصادفی و قبیله‌ها) در همه‌ی لایه‌ها یکی بماند.
 */
public class GameServices {

    private final ProductionService production = new ProductionService();
    private final MovementService   movement   = new MovementService();
    private final BuildService      build      = new BuildService();
    private final CombatService     combat     = new CombatService();
    private final TradeService      trade      = new TradeService();
    private final HappinessService  happiness  = new HappinessService();
    private final TribeService      tribe      = new TribeService();
    private final TribeTurnService  tribeTurn  = new TribeTurnService(combat, tribe);
    private final DisasterService   disaster   = new DisasterService(combat);
    private final EventBus          events     = new EventBus();
    private final SaveService       saves      = new SaveService();

    public ProductionService production() { return production; }
    public MovementService movement()     { return movement; }
    public BuildService build()           { return build; }
    public CombatService combat()         { return combat; }
    public TradeService trade()           { return trade; }
    public HappinessService happiness()   { return happiness; }
    public TribeService tribe()           { return tribe; }
    public TribeTurnService tribeTurn()   { return tribeTurn; }
    public DisasterService disaster()     { return disaster; }
    public EventBus events()              { return events; }
    public SaveService saves()            { return saves; }
}
