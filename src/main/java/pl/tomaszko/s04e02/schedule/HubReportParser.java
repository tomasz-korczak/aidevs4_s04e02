package pl.tomaszko.s04e02.schedule;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class HubReportParser {

    private static final DateTimeFormatter[] DATE_TIMES = new DateTimeFormatter[]{
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
    };

    private final ObjectMapper objectMapper;

    public HubReportParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public TurbineReport parseTurbine(String... payloads) {
        String joined = String.join("\n", payloads);
        Double strength = null;
        Double idle = null;
        Double production = null;

        for (String payload : payloads) {
            try {
                JsonNode root = objectMapper.readTree(payload);
                if (root == null || root.isNull()) {
                    continue;
                }
                if (strength == null) {
                    strength = readStrength(root);
                }
                Double[] pitches = readPitches(root);
                if (idle == null) {
                    idle = pitches[0];
                }
                if (production == null) {
                    production = pitches[1];
                }
            } catch (Exception ignored) {
            }
        }

        if (strength == null) {
            strength = firstDouble(joined,
                    "cutoffWindMs", "strengthMs", "strength_ms", "turbineStrength", "maxSafeWind");
        }
        if (idle == null) {
            idle = firstDouble(joined, "idlePitchAngle", "idle_pitch", "idlePitch", "pitchIdle");
        }
        if (production == null) {
            production = firstDouble(joined,
                    "productionPitchAngle", "production_pitch", "maxProductionPitch", "maxPitch");
        }

        if (strength == null) {
            throw new IllegalArgumentException("Unable to parse turbine strength from hub payloads");
        }
        return new TurbineReport(strength, idle, production, joined);
    }

    public WeatherForecastReport parseWeather(String payload) {
        List<WeatherPoint> points = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode forecast = root.get("forecast");
            if (forecast != null && forecast.isArray()) {
                collectWeatherNodes(forecast, points);
            } else {
                collectWeatherNodes(root, points);
            }
        } catch (Exception ignored) {
        }
        if (points.isEmpty()) {
            points.addAll(parseWeatherFromText(payload));
        }
        if (points.isEmpty()) {
            throw new IllegalArgumentException("Unable to parse weather forecast");
        }
        return new WeatherForecastReport(points);
    }

    private Double readStrength(JsonNode root) {
        JsonNode safety = root.get("safety");
        if (safety != null) {
            Double cutoff = asDouble(safety.get("cutoffWindMs"));
            if (cutoff != null) {
                return cutoff;
            }
        }
        Double direct = firstPresent(root, "cutoffWindMs", "strengthMs", "turbineStrength", "maxSafeWind");
        if (direct != null) {
            return direct;
        }
        return findNumericField(root, "cutoffWindMs", "strengthMs");
    }

    private Double[] readPitches(JsonNode root) {
        Double idle = null;
        Double production = null;
        JsonNode table = root.get("pitchAngleYieldPercent");
        if (table != null && table.isArray()) {
            for (JsonNode row : table) {
                Double pitch = asDouble(row.get("pitchAngleDeg"));
                if (pitch == null) {
                    continue;
                }
                String yield = text(row, "yieldPercent");
                if (yield == null) {
                    continue;
                }
                String normalized = yield.trim().toLowerCase(Locale.ROOT);
                if (isZeroYield(normalized)) {
                    idle = pitch;
                }
                if (isFullYield(normalized)) {
                    production = pitch;
                }
            }
        }
        if (idle == null) {
            idle = firstPresent(root, "idlePitchAngle", "idlePitch");
        }
        if (production == null) {
            production = firstPresent(root, "productionPitchAngle", "maxProductionPitch");
        }
        return new Double[]{idle, production};
    }

    private static boolean isZeroYield(String yield) {
        return yield.equals("0") || yield.startsWith("0%") || yield.equals("0-0");
    }

    private static boolean isFullYield(String yield) {
        return yield.equals("100") || yield.startsWith("100") || yield.contains("90-100");
    }

    private void collectWeatherNodes(JsonNode node, List<WeatherPoint> points) {
        if (node == null) {
            return;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                WeatherPoint point = tryPoint(child);
                if (point != null) {
                    points.add(point);
                } else {
                    collectWeatherNodes(child, points);
                }
            }
            return;
        }
        if (node.isObject()) {
            if (node.has("windMsRange") || node.has("pitchAngleDeg") || node.has("yieldPercent")) {
                return;
            }
            WeatherPoint point = tryPoint(node);
            if (point != null) {
                points.add(point);
                return;
            }
            Iterator<String> names = node.fieldNames();
            while (names.hasNext()) {
                String name = names.next();
                if ("unit".equals(name) || "safety".equals(name) || "pitchAngleYieldPercent".equals(name)
                        || "windPowerYieldPercent".equals(name)) {
                    continue;
                }
                collectWeatherNodes(node.get(name), points);
            }
        }
    }

    private WeatherPoint tryPoint(JsonNode node) {
        if (node == null || !node.isObject()) {
            return null;
        }
        if (node.has("windMsRange") || node.has("pitchAngleDeg")) {
            return null;
        }
        Double wind = firstPresent(node, "windMs", "wind_ms", "wind", "speed", "windSpeed");
        if (wind == null) {
            return null;
        }
        LocalDate date = null;
        Integer hour = null;
        if (node.has("occurrenceDate") || node.has("date") || node.has("startDate")) {
            String rawDate = text(node, "occurrenceDate", "date", "startDate");
            if (rawDate != null && rawDate.length() >= 10) {
                date = LocalDate.parse(rawDate.substring(0, 10));
            }
        }
        if (node.has("occurrenceHour") || node.has("hour") || node.has("startHour")) {
            hour = node.has("occurrenceHour") ? node.get("occurrenceHour").asInt()
                    : node.has("hour") ? node.get("hour").asInt() : node.get("startHour").asInt();
        }
        String dateTime = text(node, "timestamp", "occurrence", "time", "datetime");
        if ((date == null || hour == null) && dateTime != null) {
            LocalDateTime dt = parseDateTime(dateTime);
            if (dt != null) {
                date = dt.toLocalDate();
                hour = dt.getHour();
            }
        }
        if (date == null || hour == null) {
            return null;
        }
        return new WeatherPoint(date, hour, wind);
    }

    private List<WeatherPoint> parseWeatherFromText(String payload) {
        List<WeatherPoint> points = new ArrayList<>();
        String[] lines = payload.split("\\R");
        for (String line : lines) {
            LocalDateTime dt = null;
            Matcher matcher = Pattern.compile("(\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}(?::\\d{2})?)").matcher(line);
            if (matcher.find()) {
                dt = parseDateTime(matcher.group(1));
            }
            Matcher windMatcher = Pattern.compile("(?i)\"windMs\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)").matcher(line);
            if (dt != null && windMatcher.find()) {
                points.add(new WeatherPoint(dt.toLocalDate(), dt.getHour(), Double.parseDouble(windMatcher.group(1))));
            }
        }
        return points;
    }

    private Double firstDouble(String text, String... keys) {
        for (String key : keys) {
            Pattern pattern = Pattern.compile("(?i)\"" + Pattern.quote(key) + "\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return Double.parseDouble(matcher.group(1));
            }
        }
        return null;
    }

    private Double firstPresent(JsonNode node, String... names) {
        for (String name : names) {
            if (node.has(name)) {
                Double value = asDouble(node.get(name));
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

    private Double findNumericField(JsonNode node, String... names) {
        if (node == null) {
            return null;
        }
        if (node.isObject()) {
            Double direct = firstPresent(node, names);
            if (direct != null) {
                return direct;
            }
            Iterator<JsonNode> elements = node.elements();
            while (elements.hasNext()) {
                Double found = findNumericField(elements.next(), names);
                if (found != null) {
                    return found;
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                Double found = findNumericField(child, names);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private Double asDouble(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.asDouble();
        }
        if (node.isTextual()) {
            try {
                return Double.parseDouble(node.asText().trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String text(JsonNode node, String... names) {
        for (String name : names) {
            if (node.has(name) && !node.get(name).isNull()) {
                return node.get(name).asText();
            }
        }
        return null;
    }

    private LocalDateTime parseDateTime(String value) {
        String normalized = value.trim().replace('T', ' ');
        for (DateTimeFormatter formatter : DATE_TIMES) {
            try {
                return LocalDateTime.parse(normalized, formatter);
            } catch (Exception ignored) {
            }
        }
        try {
            if (normalized.length() >= 16) {
                return LocalDateTime.parse(normalized.substring(0, 16), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public String extractUnlockCode(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(payload);
            JsonNode code = findField(node, "unlockCode", "unlock_code");
            if (code != null && code.isTextual()) {
                String value = code.asText();
                if (!value.isBlank()) {
                    return value;
                }
            }
        } catch (Exception ignored) {
        }
        Matcher matcher = Pattern.compile("(?i)\"unlockCode\"\\s*:\\s*\"([^\"]+)\"").matcher(payload);
        if (matcher.find()) {
            return matcher.group(1);
        }
        matcher = Pattern.compile("(?i)unlockCode\\s*[:=]\\s*([A-Za-z0-9_\\-]+)").matcher(payload);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public String extractSignedParam(String payload, String fieldName) {
        if (payload == null || payload.isBlank() || fieldName == null || fieldName.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode signed = root.get("signedParams");
            if (signed != null && signed.isObject() && signed.has(fieldName)) {
                JsonNode value = signed.get(fieldName);
                if (value != null && !value.isNull()) {
                    return value.asText();
                }
            }
        } catch (Exception ignored) {
        }
        Matcher matcher = Pattern.compile(
                "(?i)\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*\"([^\"]+)\""
        ).matcher(payload);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private JsonNode findField(JsonNode node, String... names) {
        if (node == null) {
            return null;
        }
        if (node.isObject()) {
            for (String name : names) {
                if (node.has(name)) {
                    return node.get(name);
                }
            }
            Iterator<JsonNode> elements = node.elements();
            while (elements.hasNext()) {
                JsonNode found = findField(elements.next(), names);
                if (found != null) {
                    return found;
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                JsonNode found = findField(child, names);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
