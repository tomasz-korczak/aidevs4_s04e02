package pl.tomaszko.s04e02.schedule;

import java.util.ArrayList;
import java.util.List;

public class ConfigurationBatch {

    private final List<ConfigPoint> configs;

    public ConfigurationBatch(List<ConfigPoint> configs) {
        this.configs = new ArrayList<>(configs);
    }

    public List<ConfigPoint> getConfigs() {
        return configs;
    }
}
