package com.touroperator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.touroperator.domain.WeatherForecast;
import com.touroperator.repository.WeatherRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;

@Service
public class WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);
    private static final String WEATHER_API_URL = "https://wttr.in/{city}?format=j1";
    private static final ObjectMapper MAPPER = new ObjectMapper();


    private static final Map<String, String> TRANSLATIONS = Map.ofEntries(
          Map.entry("sunny",                     "Сонячно"),
          Map.entry("clear",                     "Ясно"),
          Map.entry("partly cloudy",             "Мінливо хмарно"),
          Map.entry("partly sunny",              "Мінливо сонячно"),
          Map.entry("cloudy",                    "Хмарно"),
          Map.entry("overcast",                  "Похмуро"),
          Map.entry("mist",                      "Туман"),
          Map.entry("fog",                       "Туман"),
          Map.entry("foggy",                     "Туманно"),
          Map.entry("light rain",                "Невеликий дощ"),
          Map.entry("moderate rain",             "Помірний дощ"),
          Map.entry("heavy rain",                "Сильний дощ"),
          Map.entry("drizzle",                   "Мряка"),
          Map.entry("light drizzle",             "Невелика мряка"),
          Map.entry("freezing drizzle",          "Крижана мряка"),
          Map.entry("patchy rain possible",      "Місцями можливий дощ"),
          Map.entry("patchy rain nearby",        "Місцями дощ поблизу"),
          Map.entry("light rain shower",         "Невеликий дощовий шквал"),
          Map.entry("moderate or heavy rain shower", "Помірний або сильний дощ"),
          Map.entry("torrential rain shower",    "Злива"),
          Map.entry("thundery outbreaks possible", "Можлива гроза"),
          Map.entry("patchy light rain with thunder", "Місцями дощ із грозою"),
          Map.entry("moderate or heavy rain with thunder", "Сильний дощ із грозою"),
          Map.entry("thunderstorm",              "Гроза"),
          Map.entry("blizzard",                  "Хуртовина"),
          Map.entry("snow",                      "Сніг"),
          Map.entry("light snow",                "Невеликий сніг"),
          Map.entry("moderate snow",             "Помірний сніг"),
          Map.entry("heavy snow",                "Сильний сніг"),
          Map.entry("patchy snow possible",      "Місцями можливий сніг"),
          Map.entry("light sleet",               "Невеликий мокрий сніг"),
          Map.entry("moderate or heavy sleet",   "Сильний мокрий сніг"),
          Map.entry("ice pellets",               "Крупа"),
          Map.entry("light showers of ice pellets", "Невелика крупа"),
          Map.entry("blowing snow",              "Поземок"),
          Map.entry("freezing fog",              "Крижаний туман"),
          Map.entry("haze",                      "Серпанок"),
          Map.entry("windy",                     "Вітряно")
    );

    private final WeatherRepository weatherRepo;
    private final RestTemplate restTemplate;

    public WeatherService(WeatherRepository weatherRepo) {
        this.weatherRepo = weatherRepo;

        RestTemplate rt = new RestTemplate();
        rt.getMessageConverters().stream()
              .filter(c -> c instanceof StringHttpMessageConverter)
              .map(c -> (StringHttpMessageConverter) c)
              .forEach(c -> c.setDefaultCharset(StandardCharsets.UTF_8));
        this.restTemplate = rt;
    }

    public WeatherForecast getWeather(String city) {
        if (city == null || city.isBlank()) {
            return WeatherForecast.unknown("невідоме місто");
        }

        var cached = weatherRepo.findFreshCache(city);
        if (cached.isPresent()) {
            log.debug("Погода з кешу для {}", city);
            return cached.get();
        }

        try {
            WeatherForecast fresh = fetchFromApi(city);
            if (fresh != null) {
                weatherRepo.save(fresh);
                log.info("Погода для {}: {} {}", city, fresh.getFormattedTemp(), fresh.getDescription());
                return fresh;
            }
        } catch (Exception e) {
            log.warn("API погоди недоступний для {}: {}", city, e.getMessage());
        }

        var stale = weatherRepo.findLatestForCity(city);
        if (stale.isPresent()) {
            WeatherForecast wf = stale.get();
            wf.setDescription(wf.getDescription() + " (з кешу)");
            return wf;
        }

        return WeatherForecast.unknown(city);
    }


    private static final Map<String, String> CITY_ALIASES = Map.ofEntries(
          Map.entry("париж",     "Paris"),
          Map.entry("берлін",    "Berlin"),
          Map.entry("берлин",    "Berlin"),
          Map.entry("лондон",    "London"),
          Map.entry("рим",       "Rome"),
          Map.entry("мадрид",    "Madrid"),
          Map.entry("барселона", "Barcelona"),
          Map.entry("відень",    "Vienna"),
          Map.entry("прага",     "Prague"),
          Map.entry("амстердам", "Amsterdam"),
          Map.entry("варшава",   "Warsaw"),
          Map.entry("будапешт",  "Budapest"),
          Map.entry("стамбул",   "Istanbul"),
          Map.entry("анталья",   "Antalya"),
          Map.entry("токіо",     "Tokyo"),
          Map.entry("цюрих",     "Zurich"),
          Map.entry("марракеш",  "Marrakech"),
          Map.entry("балі",      "Bali"),
          Map.entry("дубай",     "Dubai"),
          Map.entry("нью-йорк",  "New York"),
          Map.entry("нью йорк",  "New York"),
          Map.entry("бангкок",   "Bangkok"),
          Map.entry("сінгапур",  "Singapore"),
          Map.entry("барі",      "Bari"),
          Map.entry("афіни",     "Athens"),
          Map.entry("лісабон",   "Lisbon"),
          Map.entry("брюссель",  "Brussels"),
          Map.entry("копенгаген","Copenhagen"),
          Map.entry("стокгольм", "Stockholm"),
          Map.entry("осло",      "Oslo"),
          Map.entry("гельсінкі", "Helsinki"),
          Map.entry("київ",      "Kyiv"),
          Map.entry("львів",     "Lviv"),
          Map.entry("одеса",     "Odesa")
    );


    private String normalizeCity(String city) {
        if (city == null || city.isBlank()) return city;
        String key = city.toLowerCase().trim();
        String mapped = CITY_ALIASES.get(key);
        if (mapped != null) {
            log.debug("Місто '{}' замінено на '{}'", city, mapped);
            return mapped;
        }

        return city.trim();
    }

    private WeatherForecast fetchFromApi(String city) throws Exception {
        String normalizedCity = normalizeCity(city);
        String encodedCity = java.net.URLEncoder.encode(normalizedCity, StandardCharsets.UTF_8);
        String url = WEATHER_API_URL.replace("{city}", encodedCity);

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0 (compatible; TourOperator/1.0)");
        headers.set("Accept", "application/json, text/plain, */*");

        ResponseEntity<String> response = restTemplate.exchange(
              url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

        String body = response.getBody();
        if (body == null || body.isBlank()) return null;

        JsonNode root = MAPPER.readTree(body);
        JsonNode conditions = root.path("current_condition");
        if (!conditions.isArray() || conditions.isEmpty()) return null;

        JsonNode current = conditions.get(0);

        String tempC     = current.path("temp_C").asText(null);
        String feelsC    = current.path("FeelsLikeC").asText(null);
        String humidityS = current.path("humidity").asText(null);
        String windKmph  = current.path("windspeedKmph").asText(null);

        String desc = "Ясно";
        JsonNode descArr = current.path("weatherDesc");
        if (descArr.isArray() && !descArr.isEmpty()) {
            String raw = descArr.get(0).path("value").asText("").trim();
            if (!raw.isBlank()) desc = translate(raw);
        }

        desc = desc.replace("\u0000", "").trim();

        WeatherForecast wf = new WeatherForecast();
        wf.setCity(city);
        wf.setForecastDate(LocalDate.now());
        wf.setTemperature(new BigDecimal(tempC != null ? tempC.trim() : "0"));
        wf.setDescription(desc);
        if (feelsC    != null) wf.setFeelsLike(new BigDecimal(feelsC.trim()));
        if (humidityS != null) wf.setHumidity(Integer.parseInt(humidityS.trim()));
        if (windKmph  != null) wf.setWindSpeed(Integer.parseInt(windKmph.trim()));

        return wf;
    }

    private static String translate(String english) {
        String key = english.toLowerCase().trim();
        return TRANSLATIONS.getOrDefault(key, english);
    }
}