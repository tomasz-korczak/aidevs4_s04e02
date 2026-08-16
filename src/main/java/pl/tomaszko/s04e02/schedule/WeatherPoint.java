package pl.tomaszko.s04e02.schedule;

import java.time.LocalDate;
import java.util.Objects;

public class WeatherPoint {

    private final LocalDate occurrenceDate;
    private final int occurrenceHour;
    private final double windMs;

    public WeatherPoint(LocalDate occurrenceDate, int occurrenceHour, double windMs) {
        this.occurrenceDate = occurrenceDate;
        this.occurrenceHour = occurrenceHour;
        this.windMs = windMs;
    }

    public LocalDate getOccurrenceDate() {
        return occurrenceDate;
    }

    public int getOccurrenceHour() {
        return occurrenceHour;
    }

    public double getWindMs() {
        return windMs;
    }

    public long sortKey() {
        return occurrenceDate.toEpochDay() * 24L + occurrenceHour;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WeatherPoint that)) {
            return false;
        }
        return occurrenceHour == that.occurrenceHour
                && Double.compare(that.windMs, windMs) == 0
                && Objects.equals(occurrenceDate, that.occurrenceDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(occurrenceDate, occurrenceHour, windMs);
    }
}
