package com.analysis.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LiveDataPayloadDTO {

    @JsonProperty("average_price")
    private BigDecimal averagePrice;

    @JsonProperty("bid_quantity")
    private Long bidQuantity;

    @JsonProperty("bid_price")
    private BigDecimal bidPrice;

    @JsonProperty("day_change")
    private BigDecimal dayChange;

    @JsonProperty("day_change_perc")
    private BigDecimal dayChangePerc;

    @JsonProperty("upper_circuit_limit")
    private BigDecimal upperCircuitLimit;

    @JsonProperty("lower_circuit_limit")
    private BigDecimal lowerCircuitLimit;

    private Object ohlc;

    private LiveDataDepthDTO depth;

    @JsonProperty("high_trade_range")
    private BigDecimal highTradeRange;

    @JsonProperty("implied_volatility")
    private BigDecimal impliedVolatility;

    @JsonProperty("last_trade_quantity")
    private Long lastTradeQuantity;

    @JsonProperty("last_trade_time")
    private Long lastTradeTime;

    @JsonProperty("low_trade_range")
    private BigDecimal lowTradeRange;

    @JsonProperty("last_price")
    private BigDecimal lastPrice;

    @JsonProperty("market_cap")
    private Long marketCap;

    @JsonProperty("offer_price")
    private BigDecimal offerPrice;

    @JsonProperty("offer_quantity")
    private Long offerQuantity;

    @JsonProperty("oi_day_change")
    private BigDecimal oiDayChange;

    @JsonProperty("oi_day_change_percentage")
    private BigDecimal oiDayChangePercentage;

    @JsonProperty("open_interest")
    private Long openInterest;

    @JsonProperty("previous_open_interest")
    private Long previousOpenInterest;

    @JsonProperty("total_buy_quantity")
    private Long totalBuyQuantity;

    @JsonProperty("total_sell_quantity")
    private Long totalSellQuantity;

    private Long volume;

    @JsonProperty("week_52_high")
    private BigDecimal week52High;

    @JsonProperty("week_52_low")
    private BigDecimal week52Low;
}
