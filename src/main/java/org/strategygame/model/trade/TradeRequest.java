package org.strategygame.model.trade;

import org.strategygame.model.resource.ResourceType;

/** درخواست یک معامله: چه منبعی، چه مقدار، در ازای چه منبعی. */
public record TradeRequest(ResourceType source, ResourceType target, int amount) { }
