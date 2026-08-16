package pl.tomaszko.s04e02.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.tomaszko.s04e02.hub.HubWindpowerResponse;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ToolExecutionLogger {

    private static final Logger log = LoggerFactory.getLogger("pl.tomaszko.s04e02.tools");

    private final ObjectMapper objectMapper;

    public ToolExecutionLogger(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void logRequest(Map<String, Object> answer) {
        try {
            Map<String, Object> redacted = new LinkedHashMap<>(answer);
            log.info("plantTool request action={} params={}", redacted.get("action"), objectMapper.writeValueAsString(redacted));
        } catch (Exception ex) {
            log.info("plantTool request action={}", answer.get("action"));
        }
    }

    public void logResponse(HubWindpowerResponse response) {
        log.info("plantTool response code={} message={} body={}",
                response.getCode(),
                response.getMessage(),
                response.getRawBody());
    }

    public void logError(Map<String, Object> answer, Exception ex) {
        log.error("plantTool error action={} message={}", answer.get("action"), ex.getMessage());
    }
}
