package pl.tomaszko.s04e02.hub;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import pl.tomaszko.s04e02.config.AppProperties;
import pl.tomaszko.s04e02.logging.ToolExecutionLogger;

import java.util.Map;

@Component
public class HubClient {

    private final RestClient restClient;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final ToolExecutionLogger toolExecutionLogger;
    private final String hubApiKey;

    public HubClient(
            RestClient hubRestClient,
            AppProperties appProperties,
            ObjectMapper objectMapper,
            ToolExecutionLogger toolExecutionLogger,
            @Value("${HUB_API_KEY:}") String hubApiKey
    ) {
        this.restClient = hubRestClient;
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
        this.toolExecutionLogger = toolExecutionLogger;
        this.hubApiKey = hubApiKey == null ? "" : hubApiKey;
    }

    public boolean hasApiKey() {
        return !hubApiKey.isBlank();
    }

    public HubWindpowerResponse post(Map<String, Object> answer) {
        HubWindpowerRequest request = new HubWindpowerRequest(
                hubApiKey,
                appProperties.getHub().getTask(),
                answer
        );
        toolExecutionLogger.logRequest(answer);
        try {
            String body = restClient.post()
                    .uri(java.net.URI.create(appProperties.getHub().getVerifyUrl()))
                    .body(request)
                    .retrieve()
                    .body(String.class);
            if (body == null) {
                body = "";
            }
            Integer code = null;
            String message = null;
            try {
                JsonNode node = objectMapper.readTree(body);
                if (node.has("code") && node.get("code").canConvertToInt()) {
                    code = node.get("code").asInt();
                }
                if (node.has("message")) {
                    message = node.get("message").asText();
                }
            } catch (Exception ignored) {
            }
            HubWindpowerResponse response = new HubWindpowerResponse(body, code, message);
            toolExecutionLogger.logResponse(response);
            return response;
        } catch (RestClientException ex) {
            toolExecutionLogger.logError(answer, ex);
            throw ex;
        }
    }

    public boolean isReachable() {
        try {
            java.net.URI uri = java.net.URI.create(appProperties.getHub().getVerifyUrl());
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return false;
            }
            int port = uri.getPort();
            if (port < 0) {
                port = "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
            }
            try (java.net.Socket socket = new java.net.Socket()) {
                socket.connect(new java.net.InetSocketAddress(host, port), 3_000);
                return true;
            }
        } catch (Exception ex) {
            return false;
        }
    }
}
