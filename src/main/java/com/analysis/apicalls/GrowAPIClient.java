package com.analysis.apicalls;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.analysis.constants.Constants;
import com.analysis.dto.CandleResponseDTO;
import com.analysis.dto.LiveDataResponseDTO;
import com.analysis.service.GrowAccessTokenService;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class GrowAPIClient {

    private static final ZoneId MARKET_ZONE = ZoneId.of("Asia/Kolkata");
    private static final LocalTime CANDLE_START_TIME = LocalTime.of(9, 15);
    private static final LocalTime CANDLE_END_TIME = LocalTime.of(17, 30);
    private static final DateTimeFormatter GROWW_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RestTemplate restTemplate;
    private final GrowAccessTokenService accessTokenService;

    public GrowAPIClient(RestTemplate restTemplate,
                         GrowAccessTokenService accessTokenService) {
        this.restTemplate = restTemplate;
        this.accessTokenService = accessTokenService;
    }

    
    
    @CircuitBreaker(name = "growwApi", fallbackMethod = "sectorFallback")
    public String getSectorData(String tradingSymbol) {

        Constants.RATE_LIMITER.acquire();

        String url = UriComponentsBuilder
                .fromUriString("https://api.groww.in/v1/live-data/quote")
                .queryParam("exchange", "NSE")
                .queryParam("segment", "CASH")
                .queryParam("trading_symbol", tradingSymbol)
                .build()
                .encode()
                .toUriString();

        return executeGetRequest(url, String.class);
    }

    @CircuitBreaker(name = "growwApi", fallbackMethod = "candleFallback")
    public CandleResponseDTO getCandleData(String growwSymbol, String candleInterval) {

        Constants.RATE_LIMITER.acquire();
        CandleWindow candleWindow = resolveCandleWindow();

        String url = String.format(
                "https://api.groww.in/v1/historical/candles?exchange=NSE&segment=CASH&groww_symbol=%s&start_time=%s&end_time=%s&candle_interval=%s",
                growwSymbol,
                formatGrowwDateTimeQueryValue(formatDateTime(candleWindow.startTime())),
                formatGrowwDateTimeQueryValue(formatDateTime(candleWindow.endTime())),
                candleInterval
        );

        return executeGetRequest(url, CandleResponseDTO.class);
    }

    @CircuitBreaker(name = "growwApi", fallbackMethod = "liveDataFallback")
    public LiveDataResponseDTO getLiveData(String tradingSymbol) {

        Constants.RATE_LIMITER.acquire();

        String url = UriComponentsBuilder
                .fromUriString("https://api.groww.in/v1/live-data/quote")
            .queryParam("exchange", "NSE")
            .queryParam("segment", "CASH")
                .queryParam("trading_symbol", tradingSymbol)
                .build()
                .encode()
                .toUriString();

        return executeGetRequest(url, LiveDataResponseDTO.class);
    }
    
    
    public String sectorFallback(String tradingSymbol, Throwable t) {
        log.error("Circuit breaker triggered for {} : {}", tradingSymbol, t.getMessage());
        return null;
    }

    public CandleResponseDTO candleFallback(String growwSymbol,
                                            String candleInterval,
                                            Throwable t) {
        log.error("Circuit breaker triggered for candle request {} [today 09:15:00 - 17:30:00 @ {}]: {}",
                growwSymbol, candleInterval, t.getMessage());
        return null;
    }

    public LiveDataResponseDTO liveDataFallback(String tradingSymbol, Throwable t) {
        log.error("Circuit breaker triggered for live data request {}: {}",
            tradingSymbol, t.getMessage());
        return null;
    }

    private <T> T executeGetRequest(String url, Class<T> responseType) {
        HttpEntity<String> entity = new HttpEntity<>(buildHeaders());

        ResponseEntity<T> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, responseType);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Groww API error: " + response.getStatusCode());
        }
        return response.getBody();
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + accessTokenService.getGrowAccessToken());
        headers.set("X-API-VERSION", "1.0");
        return headers;
    }

    private CandleWindow resolveCandleWindow() {
        LocalDateTime now = LocalDateTime.now(MARKET_ZONE).truncatedTo(ChronoUnit.SECONDS);
        LocalDate currentDate = now.toLocalDate();
        LocalTime currentTime = now.toLocalTime();

        if (isWeekend(currentDate)) {
            LocalDate lastTradingDate = getMostRecentTradingDate(currentDate);
            return buildFullSessionWindow(lastTradingDate);
        }

        if (currentTime.isBefore(CANDLE_START_TIME)) {
            return buildFullSessionWindow(getPreviousTradingDate(currentDate));
        }

        LocalDateTime sessionStart = currentDate.atTime(CANDLE_START_TIME);
        LocalDateTime sessionEnd = currentTime.isAfter(CANDLE_END_TIME)
                ? currentDate.atTime(CANDLE_END_TIME)
                : now;

        return new CandleWindow(sessionStart, sessionEnd);
    }

    private CandleWindow buildFullSessionWindow(LocalDate tradingDate) {
        return new CandleWindow(
                tradingDate.atTime(CANDLE_START_TIME),
                tradingDate.atTime(CANDLE_END_TIME)
        );
    }

    private LocalDate getPreviousTradingDate(LocalDate currentDate) {
        return getMostRecentTradingDate(currentDate.minusDays(1));
    }

    private LocalDate getMostRecentTradingDate(LocalDate date) {
        LocalDate tradingDate = date;
        while (isWeekend(tradingDate)) {
            tradingDate = tradingDate.minusDays(1);
        }
        return tradingDate;
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(GROWW_DATE_TIME_FORMATTER);
    }

    private String formatGrowwDateTimeQueryValue(String dateTime) {
        return dateTime.replace(" ", "+");
    }

    private record CandleWindow(LocalDateTime startTime, LocalDateTime endTime) {
    }
    
}
