package com.analysis.service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.analysis.apicalls.GrowAPIClient;
import com.analysis.documents.BullishSignalSnapshot;
import com.analysis.documents.ScripMaster;
import com.analysis.dto.BullishSignalDTO;
import com.analysis.dto.CandleDTO;
import com.analysis.dto.CandlePayloadDTO;
import com.analysis.dto.CandleResponseDTO;
import com.analysis.dto.LiveDataDepthDTO;
import com.analysis.dto.LiveDataDepthEntryDTO;
import com.analysis.dto.LiveDataPayloadDTO;
import com.analysis.dto.LiveDataResponseDTO;
import com.analysis.helper.ScripVolumeDataHelper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BullishSignalScannerService {

    private static final int MAX_BULLISH_SCORE = 5;
    private static final String DEFAULT_CANDLE_INTERVAL = "5minute";
    private static final int LAST_CANDLE_COUNT = 7;

    private final GrowAPIClient growAPIClient;
    private final ScripMasterService scripMasterService;
    private final ScripVolumeDataHelper scripVolumeDataHelper;
    private final MongoTemplate mongoTemplate;

    @Value("${analysis.bullish.min-score:4}")
    private int minBullishScore;

    @Value("${analysis.bullish.min-day-change-perc:0.8}")
    private BigDecimal minDayChangePerc;

    @Value("${analysis.bullish.scheduler-enabled:true}")
    private boolean schedulerEnabled;

    @Value("${analysis.bullish.market-start-time:09:15}")
    private String marketStartTime;

    @Value("${analysis.bullish.market-end-time:16:00}")
    private String marketEndTime;

    @Value("${analysis.bullish.market-time-zone:Asia/Kolkata}")
    private String marketTimeZone;

    @Scheduled(fixedDelayString = "${analysis.bullish.refresh-ms:300000}",
            initialDelayString = "${analysis.bullish.initial-delay-ms:10000}")
    public void refreshBullishSignalsSnapshot() {
        if (!schedulerEnabled) {
            log.info("Bullish signal scheduler is disabled. Skipping scheduled refresh.");
            return;
        }

        if (!isWithinMarketHours()) {
            log.debug("Outside market hours ({} to {}, zone {}). Skipping scheduled refresh.",
                    marketStartTime,
                    marketEndTime,
                    marketTimeZone);
            return;
        }

        runFullScanAndUpsert();
    }

    private boolean isWithinMarketHours() {
        ZoneId zoneId = ZoneId.of(marketTimeZone);
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        DayOfWeek dayOfWeek = now.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return false;
        }

        LocalTime start = LocalTime.parse(marketStartTime);
        LocalTime end = LocalTime.parse(marketEndTime);
        LocalTime current = now.toLocalTime();

        return !current.isBefore(start) && !current.isAfter(end);
    }

    public void runFullScanAndUpsert() {
        List<ScripMaster> allScripMasters = scripMasterService.getAllScripMasters();
        int skippedEmptySymbols = 0;
        int nullPayloadUpserts = 0;
        int successUpserts = 0;
        int apiFailures = 0;
        int failureUpserts = 0;

        log.info("Starting bullish symbol scan and Mongo upsert. totalScrips: {}, minScore: {}, minDayChangePerc: {}",
                allScripMasters.size(),
                minBullishScore,
                minDayChangePerc);

        for (ScripMaster scripMaster : allScripMasters) {
            String symbol = resolveTradingSymbol(scripMaster);
            if (!StringUtils.hasText(symbol)) {
                skippedEmptySymbols++;
                log.debug("Skipping scrip with empty symbol: {}", scripMaster);
                continue;
            }

            try {
                LiveDataResponseDTO liveDataResponse = growAPIClient.getLiveData(symbol);
                if (liveDataResponse == null || liveDataResponse.getPayload() == null) {
                    nullPayloadUpserts++;
                    log.debug("Skipping symbol {} due to null live data payload", symbol);
                    upsertBullishSignalSnapshot(
                            symbol,
                            scripMaster.getSector(),
                            liveDataResponse,
                            null,
                            null,
                            "NO_LIVE_DATA",
                            "Live data response or payload was null"
                    );
                    continue;
                }

                LiveDataPayloadDTO payload = liveDataResponse.getPayload();
                CandleResponseDTO candleResponse = growAPIClient.getCandleData(
                        scripVolumeDataHelper.buildGrowwSymbol(symbol),
                        DEFAULT_CANDLE_INTERVAL
                );
                candleResponse = trimToLastCandles(candleResponse, LAST_CANDLE_COUNT);

                BullishSignalDTO bullishSignal = buildBullishSignal(symbol, scripMaster.getSector(), payload, candleResponse);
        upsertBullishSignalSnapshot(
            symbol,
            scripMaster.getSector(),
            liveDataResponse,
            candleResponse,
            bullishSignal,
            "SUCCESS",
            null
        );
        successUpserts++;
            } catch (Exception e) {
                apiFailures++;
                log.warn("Unable to compute bullish signal for symbol {}", symbol, e);
        upsertBullishSignalSnapshot(
            symbol,
            scripMaster.getSector(),
            null,
            null,
            null,
            "API_FAILURE",
            e.getMessage()
        );
        failureUpserts++;
            }
        }

    log.info("Completed bullish symbol scan and Mongo upsert. totalScrips: {}, successUpserts: {}, nullPayloadUpserts: {}, failureUpserts: {}, skippedEmptySymbols: {}, apiFailures: {}",
                allScripMasters.size(),
        successUpserts,
        nullPayloadUpserts,
        failureUpserts,
                skippedEmptySymbols,
                apiFailures);
    }

    private void upsertBullishSignalSnapshot(String symbol,
                                             String sector,
                                             LiveDataResponseDTO liveDataResponse,
                                             CandleResponseDTO candleResponse,
                         BullishSignalDTO bullishSignal,
                         String scanStatus,
                         String scanError) {
        Instant refreshedAt = Instant.now();
        Query query = Query.query(Criteria.where("_id").is(symbol));
        Update update = new Update()
                .set("symbol", symbol)
                .set("sector", sector)
                .set("liveDataResponse", liveDataResponse)
                .set("candleResponse", candleResponse)
                .set("bullishSignal", bullishSignal)
        .set("scanStatus", scanStatus)
        .set("scanError", scanError)
                .set("refreshedAt", refreshedAt);

        mongoTemplate.upsert(query, update, BullishSignalSnapshot.class);
    }

    private CandleResponseDTO trimToLastCandles(CandleResponseDTO candleResponse, int maxCandles) {
        if (candleResponse == null || candleResponse.getPayload() == null) {
            return candleResponse;
        }

        List<CandleDTO> candles = candleResponse.getPayload().getCandles();
        if (candles == null || candles.size() <= maxCandles) {
            return candleResponse;
        }

        int fromIndex = candles.size() - maxCandles;
        List<CandleDTO> trimmedCandles = new ArrayList<>(candles.subList(fromIndex, candles.size()));

        CandlePayloadDTO trimmedPayload = new CandlePayloadDTO();
        trimmedPayload.setCandles(trimmedCandles);
        trimmedPayload.setClosingPrice(candleResponse.getPayload().getClosingPrice());
        trimmedPayload.setStartTime(candleResponse.getPayload().getStartTime());
        trimmedPayload.setEndTime(candleResponse.getPayload().getEndTime());
        trimmedPayload.setIntervalInMinutes(candleResponse.getPayload().getIntervalInMinutes());

        CandleResponseDTO trimmedResponse = new CandleResponseDTO();
        trimmedResponse.setStatus(candleResponse.getStatus());
        trimmedResponse.setPayload(trimmedPayload);
        return trimmedResponse;
    }

    /**
     * Builds a bullish signal by evaluating five independent filters and converting them into a score.
     *
     * Filter logic:
     * 1) Momentum: dayChangePerc > configured minDayChangePerc.
     * 2) Price strength: lastPrice > averagePrice.
     * 3) Demand pressure: totalBuyQuantity > totalSellQuantity and bidQuantity > offerQuantity.
     * 4) Order book depth: summed buy depth quantity > summed sell depth quantity.
     * 5) Candle-volume confirmation: current candle volume is significant (helper-based check).
     *
     * Scoring and selection:
     * - Each true filter contributes +1 to bullishScore.
     * - Maximum score is MAX_BULLISH_SCORE.
     */
    private BullishSignalDTO buildBullishSignal(String symbol,
                                                String sector,
                                                LiveDataPayloadDTO payload,
                                                CandleResponseDTO candleResponse) {
        BigDecimal dayChangePerc = payload.getDayChangePerc();
        BigDecimal lastPrice = payload.getLastPrice();
        BigDecimal averagePrice = payload.getAveragePrice();
        Long totalBuyQuantity = payload.getTotalBuyQuantity();
        Long totalSellQuantity = payload.getTotalSellQuantity();
        Long bidQuantity = payload.getBidQuantity();
        Long offerQuantity = payload.getOfferQuantity();

        long depthBuyQuantity = sumDepthQuantity(payload.getDepth(), true);
        long depthSellQuantity = sumDepthQuantity(payload.getDepth(), false);

        boolean momentumSignal = isGreaterThan(dayChangePerc, minDayChangePerc);
        boolean priceStrengthSignal = isGreaterThan(lastPrice, averagePrice);
        boolean demandSignal = isGreaterThan(totalBuyQuantity, totalSellQuantity)
                && isGreaterThan(bidQuantity, offerQuantity);
        boolean depthSignal = depthBuyQuantity > depthSellQuantity;
        boolean volumeCandleSignal = scripVolumeDataHelper.hasSignificantCurrentVolume(candleResponse);

        int bullishScore = 0;
        bullishScore += momentumSignal ? 1 : 0;
        bullishScore += priceStrengthSignal ? 1 : 0;
        bullishScore += demandSignal ? 1 : 0;
        bullishScore += depthSignal ? 1 : 0;
        bullishScore += volumeCandleSignal ? 1 : 0;
        bullishScore = Math.min(bullishScore, MAX_BULLISH_SCORE);

        return new BullishSignalDTO(
                symbol,
                sector,
                bullishScore,
                dayChangePerc,
                lastPrice,
                averagePrice,
                payload.getVolume(),
                totalBuyQuantity,
                totalSellQuantity,
                bidQuantity,
                offerQuantity,
                depthBuyQuantity,
                depthSellQuantity,
                momentumSignal,
                priceStrengthSignal,
                demandSignal,
                depthSignal,
                volumeCandleSignal
        );
    }

    private long sumDepthQuantity(LiveDataDepthDTO depth, boolean isBuySide) {
        if (depth == null) {
            return 0L;
        }

        List<LiveDataDepthEntryDTO> entries = isBuySide ? depth.getBuy() : depth.getSell();
        if (entries == null || entries.isEmpty()) {
            return 0L;
        }

        return entries.stream()
                .filter(Objects::nonNull)
                .map(LiveDataDepthEntryDTO::getQuantity)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .sum();
    }

    private boolean isGreaterThan(BigDecimal left, BigDecimal right) {
        return left != null && right != null && left.compareTo(right) > 0;
    }

    private boolean isGreaterThan(Long left, Long right) {
        return left != null && right != null && left > right;
    }

    private String resolveTradingSymbol(ScripMaster scripMaster) {
        if (scripMaster == null) {
            return null;
        }

        if (StringUtils.hasText(scripMaster.getSymbol())) {
            return scripMaster.getSymbol().trim();
        }

        return null;
    }
}