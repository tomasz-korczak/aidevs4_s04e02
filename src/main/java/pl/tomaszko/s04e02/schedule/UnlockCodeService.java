package pl.tomaszko.s04e02.schedule;

import org.springframework.stereotype.Component;
import pl.tomaszko.s04e02.hub.AsyncResultPoller;
import pl.tomaszko.s04e02.hub.HubFailureClass;
import pl.tomaszko.s04e02.hub.HubFailureClassifier;
import pl.tomaszko.s04e02.hub.HubWindpowerResponse;
import pl.tomaszko.s04e02.tools.PlantTool;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class UnlockCodeService {

    private final PlantTool plantTool;
    private final AsyncResultPoller asyncResultPoller;
    private final HubReportParser hubReportParser;
    private final HubFailureClassifier failureClassifier;

    public UnlockCodeService(
            PlantTool plantTool,
            AsyncResultPoller asyncResultPoller,
            HubReportParser hubReportParser,
            HubFailureClassifier failureClassifier
    ) {
        this.plantTool = plantTool;
        this.asyncResultPoller = asyncResultPoller;
        this.hubReportParser = hubReportParser;
        this.failureClassifier = failureClassifier;
    }

    public void signBatch(ConfigurationBatch batch) {
        List<ConfigPoint> points = batch.getConfigs();
        if (points.isEmpty()) {
            return;
        }

        for (ConfigPoint point : points) {
            HubWindpowerResponse order = plantTool.unlockCodeGenerator(
                    point.getStartDate(),
                    point.getStartHour(),
                    point.getWindMs(),
                    point.getPitchAngle()
            );
            HubFailureClass orderClass = failureClassifier.classify(order);
            if (failureClassifier.requiresNewStart(orderClass)) {
                throw new SessionFailedException(orderClass, order.getRawBody());
            }
        }

        List<HubWindpowerResponse> results =
                asyncResultPoller.pollForSourceCount("unlockCodeGenerator", points.size());

        Set<ConfigPoint> assigned = new HashSet<>();
        for (HubWindpowerResponse result : results) {
            HubFailureClass resultClass = failureClassifier.classify(result);
            if (failureClassifier.requiresNewStart(resultClass)) {
                throw new SessionFailedException(resultClass, result.getRawBody());
            }

            String code = hubReportParser.extractUnlockCode(result.getRawBody());
            if (code == null || code.isBlank()) {
                throw new IllegalStateException("Unlock code missing in hub response: " + result.getRawBody());
            }

            ConfigPoint point = matchPoint(points, assigned, result.getRawBody());
            if (point == null) {
                throw new IllegalStateException(
                        "Unable to match unlock result to a config point: " + result.getRawBody()
                );
            }
            point.setUnlockCode(code);
            assigned.add(point);
        }

        for (ConfigPoint point : points) {
            if (point.getUnlockCode() == null || point.getUnlockCode().isBlank()) {
                throw new IllegalStateException(
                        "Missing unlock code for config point " + point.getStartDate()
                                + " hour=" + point.getStartHour()
                );
            }
        }
    }

    private ConfigPoint matchPoint(List<ConfigPoint> points, Set<ConfigPoint> assigned, String body) {
        String startDate = hubReportParser.extractSignedParam(body, "startDate");
        String startHour = hubReportParser.extractSignedParam(body, "startHour");
        String windMs = hubReportParser.extractSignedParam(body, "windMs");
        String pitchAngle = hubReportParser.extractSignedParam(body, "pitchAngle");

        for (ConfigPoint point : points) {
            if (assigned.contains(point)) {
                continue;
            }
            if (matches(point, startDate, startHour, windMs, pitchAngle)) {
                return point;
            }
        }
        return null;
    }

    private boolean matches(
            ConfigPoint point,
            String startDate,
            String startHour,
            String windMs,
            String pitchAngle
    ) {
        if (startDate != null && !startDate.equals(point.getStartDate())) {
            return false;
        }
        if (startHour != null && !startHour.equals(PlantTool.formatStartHour(point.getStartHour()))) {
            return false;
        }
        if (windMs != null && !nearlyEqual(parseDouble(windMs), point.getWindMs())) {
            return false;
        }
        if (pitchAngle != null && !nearlyEqual(parseDouble(pitchAngle), point.getPitchAngle())) {
            return false;
        }
        return startDate != null || startHour != null || windMs != null || pitchAngle != null;
    }

    private static Double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception ex) {
            return null;
        }
    }

    private static boolean nearlyEqual(Double left, double right) {
        if (left == null) {
            return false;
        }
        return Math.abs(left - right) < 0.0001;
    }
}
