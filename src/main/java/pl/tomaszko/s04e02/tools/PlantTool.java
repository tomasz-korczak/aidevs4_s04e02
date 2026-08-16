package pl.tomaszko.s04e02.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import pl.tomaszko.s04e02.hub.HubClient;
import pl.tomaszko.s04e02.hub.HubWindpowerRequest;
import pl.tomaszko.s04e02.hub.HubWindpowerResponse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class PlantTool {

    private final HubClient hubClient;

    public PlantTool(HubClient hubClient) {
        this.hubClient = hubClient;
    }

    @Tool(description = "Execute a windpower plant hub action. Actions: start, get, getResult, config, unlockCodeGenerator, done.")
    public String plantTool(
            @ToolParam(description = "Hub action name") String action,
            @ToolParam(description = "get param: weather|turbinecheck|powerplantcheck|documentation") String param,
            @ToolParam(description = "config/unlock startDate") String startDate,
            @ToolParam(description = "config/unlock startHour as 0-23 hour; sent to hub as HH:MM:SS") Integer startHour,
            @ToolParam(description = "config/unlock pitchAngle") Double pitchAngle,
            @ToolParam(description = "config turbineMode idle|production") String turbineMode,
            @ToolParam(description = "config unlockCode") String unlockCode,
            @ToolParam(description = "unlock windMs") Double windMs,
            @ToolParam(description = "JSON array string of config points for batch config") String configsJson
    ) {
        return execute(action, param, startDate, startHour, pitchAngle, turbineMode, unlockCode, windMs, null).getRawBody();
    }

    public HubWindpowerResponse start() {
        return execute("start", null, null, null, null, null, null, null, null);
    }

    public HubWindpowerResponse get(String param) {
        return execute("get", param, null, null, null, null, null, null, null);
    }

    public HubWindpowerResponse getResult() {
        return execute("getResult", null, null, null, null, null, null, null, null);
    }

    public HubWindpowerResponse unlockCodeGenerator(String startDate, int startHour, double windMs, double pitchAngle) {
        return execute("unlockCodeGenerator", null, startDate, startHour, pitchAngle, null, null, windMs, null);
    }

    public HubWindpowerResponse configBatch(List<Map<String, Object>> configs) {
        return execute("config", null, null, null, null, null, null, null, configs);
    }

    public HubWindpowerResponse done() {
        return execute("done", null, null, null, null, null, null, null, null);
    }

    private HubWindpowerResponse execute(
            String action,
            String param,
            String startDate,
            Integer startHour,
            Double pitchAngle,
            String turbineMode,
            String unlockCode,
            Double windMs,
            List<Map<String, Object>> configs
    ) {
        Map<String, Object> answer = HubWindpowerRequest.action(action);
        if (param != null && !param.isBlank()) {
            answer.put("param", param);
        }
        if (configs != null) {
            answer.put("configs", configs);
        } else {
            if (startDate != null) {
                answer.put("startDate", startDate);
            }
            if (startHour != null) {
                answer.put("startHour", formatStartHour(startHour));
            }
            if (pitchAngle != null) {
                answer.put("pitchAngle", pitchAngle);
            }
            if (turbineMode != null) {
                answer.put("turbineMode", turbineMode);
            }
            if (unlockCode != null) {
                answer.put("unlockCode", unlockCode);
            }
            if (windMs != null) {
                answer.put("windMs", windMs);
            }
        }
        return hubClient.post(answer);
    }

    public static List<Map<String, Object>> toConfigMaps(List<pl.tomaszko.s04e02.schedule.ConfigPoint> points) {
        List<Map<String, Object>> configs = new ArrayList<>();
        for (pl.tomaszko.s04e02.schedule.ConfigPoint point : points) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("startDate", point.getStartDate());
            map.put("startHour", formatStartHour(point.getStartHour()));
            map.put("pitchAngle", point.getPitchAngle());
            map.put("turbineMode", point.getTurbineMode());
            map.put("unlockCode", point.getUnlockCode());
            configs.add(map);
        }
        return configs;
    }

    public static String formatStartHour(int hour) {
        if (hour < 0 || hour > 23) {
            throw new IllegalArgumentException("startHour must be 0-23, got " + hour);
        }
        return String.format("%02d:00:00", hour);
    }
}
