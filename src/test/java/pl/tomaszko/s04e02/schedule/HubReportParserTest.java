package pl.tomaszko.s04e02.schedule;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HubReportParserTest {

    private final HubReportParser parser = new HubReportParser(new ObjectMapper().findAndRegisterModules());

    @Test
    void parsesDocumentationCutoffAndPitchYieldTable() {
        String docs = """
                {
                  "pitchAngleYieldPercent": [
                    {"pitchAngleDeg": 0, "yieldPercent": "100"},
                    {"pitchAngleDeg": 45, "yieldPercent": "65"},
                    {"pitchAngleDeg": 90, "yieldPercent": "0"}
                  ],
                  "safety": {
                    "cutoffWindMs": 14,
                    "minOperationalWindMs": 4
                  }
                }
                """;
        String turbinecheck = """
                {
                  "sourceFunction": "turbinecheck",
                  "bladePitchAngleDeg": 0,
                  "battery": "low"
                }
                """;

        TurbineReport report = parser.parseTurbine(docs, turbinecheck);
        assertEquals(14.0, report.getStrengthMs());
        assertEquals(90.0, report.getIdlePitchAngle());
        assertEquals(0.0, report.getProductionPitchAngle());
        assertTrue(report.hasRequiredPitches());
    }

    @Test
    void parsesWeatherForecastTimestamps() {
        String weather = """
                {
                  "sourceFunction": "weather",
                  "forecast": [
                    {"timestamp": "2026-08-17 00:00:00", "windMs": 3.0},
                    {"timestamp": "2026-08-17 14:00:00", "windMs": 12.5},
                    {"timestamp": "2026-08-17 16:00:00", "windMs": 15.0}
                  ]
                }
                """;
        WeatherForecastReport report = parser.parseWeather(weather);
        assertEquals(3, report.getItems().size());
        assertEquals(14, report.getItems().get(1).getOccurrenceHour());
        assertEquals(12.5, report.getItems().get(1).getWindMs());
    }
}
