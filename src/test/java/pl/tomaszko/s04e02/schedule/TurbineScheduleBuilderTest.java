package pl.tomaszko.s04e02.schedule;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TurbineScheduleBuilderTest {

    private final TurbineScheduleBuilder builder = new TurbineScheduleBuilder();
    private final LocalDate day = LocalDate.of(2026, 8, 16);

    @Test
    void idlesStormsIncludingEqualStrengthAndProducesAtBestSafeHour() {
        TurbineReport turbine = new TurbineReport(20.0, 90.0, 10.0, "raw");
        WeatherForecastReport forecast = new WeatherForecastReport(List.of(
                new WeatherPoint(day, 8, 5.0),
                new WeatherPoint(day, 9, 20.0),
                new WeatherPoint(day, 10, 12.0),
                new WeatherPoint(day, 11, 18.0),
                new WeatherPoint(day, 12, 3.0),
                new WeatherPoint(day, 13, 22.0)
        ));

        ConfigurationBatch batch = builder.build(turbine, forecast);
        List<ConfigPoint> configs = batch.getConfigs();

        assertTrue(configs.stream().anyMatch(c -> c.getStartHour() == 9 && "idle".equals(c.getTurbineMode())));
        assertTrue(configs.stream().anyMatch(c -> c.getStartHour() == 13 && "idle".equals(c.getTurbineMode())));
        assertTrue(configs.stream().noneMatch(c -> c.getStartHour() == 8));
        assertTrue(configs.stream().noneMatch(c -> c.getStartHour() == 12));
        ConfigPoint production = configs.stream()
                .filter(c -> "production".equals(c.getTurbineMode()))
                .findFirst()
                .orElseThrow();
        assertEquals(11, production.getStartHour());
        assertEquals(10.0, production.getPitchAngle());

        for (int i = 1; i < configs.size(); i++) {
            assertTrue(configs.get(i - 1).getStartHour() <= configs.get(i).getStartHour()
                    || configs.get(i - 1).getStartDate().compareTo(configs.get(i).getStartDate()) < 0);
        }
    }

    @Test
    void prefersEarliestHourWhenSeveralShareMaxSafeWind() {
        TurbineReport turbine = new TurbineReport(14.0, 90.0, 0.0, "raw");
        LocalDate day2 = day.plusDays(2);
        LocalDate day3 = day.plusDays(3);
        WeatherForecastReport forecast = new WeatherForecastReport(List.of(
                new WeatherPoint(day, 18, 25.0),
                new WeatherPoint(day, 20, 5.9),
                new WeatherPoint(day2, 20, 5.9),
                new WeatherPoint(day3, 18, 22.0)
        ));

        ConfigurationBatch batch = builder.build(turbine, forecast);
        ConfigPoint production = batch.getConfigs().stream()
                .filter(c -> "production".equals(c.getTurbineMode()))
                .findFirst()
                .orElseThrow();

        assertEquals(day.toString(), production.getStartDate());
        assertEquals(20, production.getStartHour());
        assertEquals(0.0, production.getPitchAngle());
        assertTrue(batch.getConfigs().stream().anyMatch(c ->
                c.getStartDate().equals(day.toString()) && c.getStartHour() == 18 && "idle".equals(c.getTurbineMode())));
        assertTrue(batch.getConfigs().stream().anyMatch(c ->
                c.getStartDate().equals(day3.toString()) && c.getStartHour() == 18 && "idle".equals(c.getTurbineMode())));
    }

    @Test
    void idlesEveryStormEvenAfterProductionHour() {
        TurbineReport turbine = new TurbineReport(14.0, 90.0, 0.0, "raw");
        WeatherForecastReport forecast = new WeatherForecastReport(List.of(
                new WeatherPoint(day, 10, 6.0),
                new WeatherPoint(day, 18, 25.0),
                new WeatherPoint(day.plusDays(1), 18, 22.0)
        ));

        ConfigurationBatch batch = builder.build(turbine, forecast);
        assertEquals(1, batch.getConfigs().stream().filter(c -> "production".equals(c.getTurbineMode())).count());
        assertEquals(2, batch.getConfigs().stream().filter(c -> "idle".equals(c.getTurbineMode())).count());
        assertTrue(batch.getConfigs().stream().anyMatch(c -> c.getStartHour() == 10 && "production".equals(c.getTurbineMode())));
    }

    @Test
    void reappliesIdleForLaterStormAfterReset() {
        TurbineReport turbine = new TurbineReport(15.0, 80.0, 5.0, "raw");
        WeatherForecastReport forecast = new WeatherForecastReport(List.of(
                new WeatherPoint(day, 1, 16.0),
                new WeatherPoint(day, 2, 4.0),
                new WeatherPoint(day, 3, 17.0),
                new WeatherPoint(day, 4, 10.0)
        ));

        ConfigurationBatch batch = builder.build(turbine, forecast);
        assertTrue(batch.getConfigs().stream().anyMatch(c -> c.getStartHour() == 1 && "idle".equals(c.getTurbineMode())));
        assertTrue(batch.getConfigs().stream().anyMatch(c -> c.getStartHour() == 3 && "idle".equals(c.getTurbineMode())));
        assertTrue(batch.getConfigs().stream().anyMatch(c -> c.getStartHour() == 4 && "production".equals(c.getTurbineMode())));
    }

    @Test
    void missingPitchFails() {
        TurbineReport turbine = new TurbineReport(15.0, null, 5.0, "raw");
        WeatherForecastReport forecast = new WeatherForecastReport(List.of(
                new WeatherPoint(day, 4, 10.0)
        ));
        assertThrows(IllegalArgumentException.class, () -> builder.build(turbine, forecast));
    }
}
