package com.analysis.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BullishSignalDTO {

    private String symbol;
    private String sector;
    private Integer bullishScore;

    private BigDecimal dayChangePerc;
    private BigDecimal lastPrice;
    private BigDecimal averagePrice;
    private Long volume;

    private Long totalBuyQuantity;
    private Long totalSellQuantity;
    private Long bidQuantity;
    private Long offerQuantity;
    private Long depthBuyQuantity;
    private Long depthSellQuantity;

    private boolean momentumSignal;
    private boolean priceStrengthSignal;
    private boolean demandSignal;
    private boolean depthSignal;
    private boolean volumeCandleSignal;
}
