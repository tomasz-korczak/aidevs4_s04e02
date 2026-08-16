package pl.tomaszko.s04e02.schedule;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class TurbineScheduleBuilder {

    public ConfigurationBatch build(TurbineReport turbine, WeatherForecastReport forecast) {
        if (turbine == null || !turbine.hasRequiredPitches()) {
            throw new IllegalArgumentException("Turbine idle/production pitch angles are required");
        }
        if (forecast == null || forecast.getItems().isEmpty()) {
            throw new IllegalArgumentException("Weather forecast is required");
        }

        List<WeatherPoint> points = forecast.getItems().stream()
                .sorted(Comparator.comparingLong(WeatherPoint::sortKey))
                .toList();

        double strength = turbine.getStrengthMs();
        List<WeatherPoint> storms = points.stream()
                .filter(p -> p.getWindMs() >= strength)
                .toList();
        List<WeatherPoint> safe = points.stream()
                .filter(p -> p.getWindMs() < strength)
                .toList();

        if (safe.isEmpty()) {
            throw new IllegalStateException("No safe production window in forecast");
        }

        WeatherPoint bestProduction = safe.stream()
                .max(Comparator.comparingDouble(WeatherPoint::getWindMs)
                        .thenComparing(Comparator.comparingLong(WeatherPoint::sortKey).reversed()))
                .orElseThrow();

        List<ConfigPoint> configs = new ArrayList<>();
        for (WeatherPoint storm : storms) {
            configs.add(idlePoint(storm, turbine.getIdlePitchAngle()));
        }
        configs.add(productionPoint(bestProduction, turbine.getProductionPitchAngle()));

        configs.sort(Comparator
                .comparing(ConfigPoint::getStartDate)
                .thenComparingInt(ConfigPoint::getStartHour));

        List<ConfigPoint> deduped = new ArrayList<>();
        for (ConfigPoint point : configs) {
            if (deduped.stream().noneMatch(existing ->
                    existing.getStartDate().equals(point.getStartDate())
                            && existing.getStartHour() == point.getStartHour()
                            && existing.getTurbineMode().equals(point.getTurbineMode()))) {
                deduped.add(point);
            }
        }

        return new ConfigurationBatch(deduped);
    }

    private static ConfigPoint idlePoint(WeatherPoint point, double idlePitch) {
        return new ConfigPoint(
                point.getOccurrenceDate().toString(),
                point.getOccurrenceHour(),
                idlePitch,
                "idle",
                point.getWindMs()
        );
    }

    private static ConfigPoint productionPoint(WeatherPoint point, double productionPitch) {
        return new ConfigPoint(
                point.getOccurrenceDate().toString(),
                point.getOccurrenceHour(),
                productionPitch,
                "production",
                point.getWindMs()
        );
    }
}
