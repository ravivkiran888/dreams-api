package com.analysis.helper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.analysis.documents.ScripMaster;
import com.analysis.dto.CandleDTO;
import com.analysis.dto.CandlePayloadDTO;
import com.analysis.dto.CandleResponseDTO;
import com.analysis.dto.ScripVolumeDataDTO;

@Component
public class ScripVolumeDataHelper {

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
        if (payload == null || payload.getCandles() == null || payload.getCandles().size() < 10) {
            return false;
        }

        List<CandleDTO> candlesWithVolume = payload.getCandles().stream()
                .filter(candle -> candle != null && candle.getVolume() != null)
                .toList();

        if (candlesWithVolume.size() < 10) {
            return false;
        }

        CandleDTO latestCandle = candlesWithVolume.get(candlesWithVolume.size() - 1);
        long latestVolume = latestCandle.getVolume();

        long highestVolumeOfDay = candlesWithVolume.stream()
                .map(CandleDTO::getVolume)
                .max(Long::compareTo)
                .orElse(0L);

        return latestVolume == highestVolumeOfDay;
    }
}
