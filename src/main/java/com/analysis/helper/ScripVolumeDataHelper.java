package com.analysis.helper;

import java.util.List;
import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.analysis.documents.ScripMaster;
import com.analysis.dto.CandleDTO;
import com.analysis.dto.CandlePayloadDTO;
import com.analysis.dto.CandleResponseDTO;
import com.analysis.dto.ScripVolumeDataDTO;


/*

API response is valid and successful

candleResponse is not null
status is SUCCESS (case-insensitive)
See ScripVolumeDataHelper.java:39
Enough candle data exists

payload and candle list must be present
at least 6 candles are required (MINIMUM_CANDLE_COUNT = 6)
See ScripVolumeDataHelper.java:17 and ScripVolumeDataHelper.java:44
Last 6 candles must have required fields

no null candle in that 6-candle window
each must have volume, openPrice, and closePrice
See ScripVolumeDataHelper.java:51
Latest candle must be bullish

bullish means close price > open price
See ScripVolumeDataHelper.java:59 and ScripVolumeDataHelper.java:90
Latest volume spike condition

compute average volume of previous 5 candles
latest candle volume must be at least 1.5 times that average (MINIMUM_VOLUME_MULTIPLIER = 1.5)
Formula: latestVolume >= avgPrevious5Volume × 1.5
See ScripVolumeDataHelper.java:63 and ScripVolumeDataHelper.java:69
So overall: bullish latest candle + significant volume spike over prior 5 candles, with strict data-validity checks.

 */

@Component
public class ScripVolumeDataHelper {

    private static final int MINIMUM_CANDLE_COUNT = 6;
    private static final int PREVIOUS_CANDLE_COUNT = 5;
    private static final double MINIMUM_VOLUME_MULTIPLIER = 1.5d;

    public ScripVolumeDataDTO buildVolumeResponse(ScripMaster scripMaster,
                                                  CandleResponseDTO candleResponse,
                                                  String errorMessage) {
        return new ScripVolumeDataDTO(
                scripMaster.getId(),
                scripMaster.getScripCode(),
                scripMaster.getSymbol(),
                scripMaster.getSector(),
                candleResponse,
                errorMessage
        );
    }

    public String buildGrowwSymbol(String symbol) {
        return symbol.startsWith("NSE-") ? symbol : "NSE-" + symbol;
    }

    public boolean hasSignificantCurrentVolume(CandleResponseDTO candleResponse) {
        if (candleResponse == null || !"SUCCESS".equalsIgnoreCase(candleResponse.getStatus())) {
            return false;
        }

        CandlePayloadDTO payload = candleResponse.getPayload();
        if (payload == null || payload.getCandles() == null || payload.getCandles().size() < MINIMUM_CANDLE_COUNT) {
            return false;
        }

        List<CandleDTO> candles = payload.getCandles();
        List<CandleDTO> recentCandles = candles.subList(candles.size() - MINIMUM_CANDLE_COUNT, candles.size());

        if (recentCandles.stream().anyMatch(candle -> candle == null
                || candle.getVolume() == null
                || candle.getOpenPrice() == null
                || candle.getClosePrice() == null)) {
            return false;
        }

        CandleDTO latestCandle = recentCandles.get(recentCandles.size() - 1);
        if (!isBullish(latestCandle)) {
            return false;
        }

        double averagePreviousVolume = recentCandles.subList(0, PREVIOUS_CANDLE_COUNT).stream()
                .mapToLong(CandleDTO::getVolume)
                .average()
                .orElse(0.0d);

        double latestVolume = latestCandle.getVolume();
        return latestVolume >= (averagePreviousVolume * MINIMUM_VOLUME_MULTIPLIER);
    }

    public double getAveragePreviousFiveVolume(CandleResponseDTO candleResponse) {
        if (candleResponse == null || candleResponse.getPayload() == null || candleResponse.getPayload().getCandles() == null) {
            return 0.0d;
        }

        List<CandleDTO> candles = candleResponse.getPayload().getCandles();
        if (candles.size() < MINIMUM_CANDLE_COUNT) {
            return 0.0d;
        }

        List<CandleDTO> recentCandles = candles.subList(candles.size() - MINIMUM_CANDLE_COUNT, candles.size());
        return recentCandles.subList(0, PREVIOUS_CANDLE_COUNT).stream()
                .filter(candle -> candle != null && candle.getVolume() != null)
                .mapToLong(CandleDTO::getVolume)
                .average()
                .orElse(0.0d);
    }

    private boolean isBullish(CandleDTO candle) {
        BigDecimal openPrice = candle.getOpenPrice();
        BigDecimal closePrice = candle.getClosePrice();
        return closePrice.compareTo(openPrice) > 0;
    }
}
