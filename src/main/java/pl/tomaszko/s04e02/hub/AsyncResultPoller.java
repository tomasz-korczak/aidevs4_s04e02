package pl.tomaszko.s04e02.hub;

import org.springframework.stereotype.Component;
import pl.tomaszko.s04e02.config.AppProperties;
import pl.tomaszko.s04e02.tools.PlantTool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AsyncResultPoller {

    public static final String FAILURE_KEY = "__failure__";

    private static final Pattern SOURCE_FUNCTION = Pattern.compile(
            "\"sourceFunction\"\\s*:\\s*\"([^\"]+)\"",
            Pattern.CASE_INSENSITIVE
    );

    private final PlantTool plantTool;
    private final HubFailureClassifier failureClassifier;
    private final AppProperties appProperties;

    public AsyncResultPoller(PlantTool plantTool, HubFailureClassifier failureClassifier, AppProperties appProperties) {
        this.plantTool = plantTool;
        this.failureClassifier = failureClassifier;
        this.appProperties = appProperties;
    }

    public HubWindpowerResponse pollForSource(String sourceFunction) {
        List<HubWindpowerResponse> results = pollForSourceCount(sourceFunction, 1);
        if (results.isEmpty()) {
            throw new IllegalStateException("No hub result for sourceFunction=" + sourceFunction);
        }
        return results.getFirst();
    }

    public List<HubWindpowerResponse> pollForSourceCount(String sourceFunction, int count) {
        if (count <= 0) {
            return List.of();
        }
        String expected = sourceFunction.toLowerCase(Locale.ROOT);
        List<HubWindpowerResponse> found = new ArrayList<>(count);
        long interval = Math.max(50L, appProperties.getPlant().getGetResultPollIntervalMs());
        long maxInterval = Math.max(interval, appProperties.getPlant().getGetResultPollMaxIntervalMs());
        long deadline = System.currentTimeMillis() + 40_000L;
        long sleep = interval;

        while (found.size() < count && System.currentTimeMillis() < deadline) {
            HubWindpowerResponse response = plantTool.getResult();
            HubFailureClass failureClass = failureClassifier.classify(response);
            if (failureClassifier.requiresNewStart(failureClass)) {
                found.add(response);
                return found;
            }

            if (hasPayload(response) && expected.equals(extractSourceFunction(response))) {
                found.add(response);
                sleep = interval;
                continue;
            }

            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while polling getResult", e);
            }
            sleep = Math.min(maxInterval, sleep + interval / 2);
        }

        if (found.size() < count) {
            throw new IllegalStateException(
                    "Timed out waiting for " + count + " async hub results for sourceFunction=" + sourceFunction
                            + " (got " + found.size() + ")"
            );
        }
        return found;
    }

    public Map<String, HubWindpowerResponse> pollForSources(Collection<String> sourceFunctions) {
        Set<String> pending = new LinkedHashSet<>();
        for (String source : sourceFunctions) {
            if (source != null && !source.isBlank()) {
                pending.add(source.toLowerCase(Locale.ROOT));
            }
        }
        if (pending.isEmpty()) {
            throw new IllegalArgumentException("At least one sourceFunction is required");
        }

        Map<String, HubWindpowerResponse> found = new LinkedHashMap<>();
        long interval = Math.max(50L, appProperties.getPlant().getGetResultPollIntervalMs());
        long maxInterval = Math.max(interval, appProperties.getPlant().getGetResultPollMaxIntervalMs());
        long deadline = System.currentTimeMillis() + 40_000L;
        long sleep = interval;

        while (!pending.isEmpty() && System.currentTimeMillis() < deadline) {
            HubWindpowerResponse response = plantTool.getResult();
            HubFailureClass failureClass = failureClassifier.classify(response);
            if (failureClassifier.requiresNewStart(failureClass)) {
                found.put(FAILURE_KEY, response);
                return found;
            }

            if (hasPayload(response)) {
                String source = extractSourceFunction(response);
                if (source != null && pending.remove(source)) {
                    found.put(source, response);
                    sleep = interval;
                    continue;
                }
            }

            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while polling getResult", e);
            }
            sleep = Math.min(maxInterval, sleep + interval / 2);
        }

        if (!pending.isEmpty()) {
            throw new IllegalStateException("Timed out waiting for async hub results: " + pending);
        }
        return found;
    }

    private String extractSourceFunction(HubWindpowerResponse response) {
        String body = response.getRawBody();
        if (body == null || body.isBlank()) {
            return null;
        }
        Matcher matcher = SOURCE_FUNCTION.matcher(body);
        if (matcher.find()) {
            return matcher.group(1).toLowerCase(Locale.ROOT);
        }
        return null;
    }

    private boolean hasPayload(HubWindpowerResponse response) {
        String body = response.getRawBody();
        if (body == null || body.isBlank()) {
            return false;
        }
        HubFailureClass failureClass = failureClassifier.classify(response);
        return failureClass != HubFailureClass.RETRYABLE;
    }
}
