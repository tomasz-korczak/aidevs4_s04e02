package pl.tomaszko.s04e02.schedule;

import java.util.ArrayList;
import java.util.List;

public class WeatherForecastReport {

    private final List<WeatherPoint> items;
    private boolean consumed;

    public WeatherForecastReport(List<WeatherPoint> items) {
        this.items = new ArrayList<>(items);
    }

    public List<WeatherPoint> getItems() {
        return items;
    }

    public boolean isConsumed() {
        return consumed;
    }

    public void markConsumed() {
        this.consumed = true;
    }
}
