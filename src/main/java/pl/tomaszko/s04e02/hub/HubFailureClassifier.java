package pl.tomaszko.s04e02.hub;

import org.springframework.stereotype.Component;
import pl.tomaszko.s04e02.config.AppProperties;

@Component
public class HubFailureClassifier {

    private final AppProperties appProperties;

    public HubFailureClassifier(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public HubFailureClass classify(HubWindpowerResponse response) {
        if (response == null) {
            return HubFailureClass.RETRYABLE;
        }
        String haystack = ((response.getMessage() == null ? "" : response.getMessage()) + " "
                + (response.getRawBody() == null ? "" : response.getRawBody())).toLowerCase();

        for (String pattern : appProperties.getPlant().getSessionOverMessagePatterns()) {
            if (pattern != null && !pattern.isBlank() && haystack.contains(pattern.toLowerCase())) {
                return HubFailureClass.SESSION_OVER;
            }
        }
        for (String pattern : appProperties.getPlant().getConfigRejectedMessagePatterns()) {
            if (pattern != null && !pattern.isBlank() && haystack.contains(pattern.toLowerCase())) {
                return HubFailureClass.CONFIG_REJECTED;
            }
        }

        if (haystack.contains("no result") || haystack.contains("empty") || haystack.contains("queue is empty")
                || haystack.contains("nothing") || haystack.contains("not ready")) {
            return HubFailureClass.RETRYABLE;
        }

        Integer code = response.getCode();
        if (code != null && code != 0 && code != 13 && response.getRawBody() != null
                && response.getRawBody().isBlank()) {
            return HubFailureClass.CONFIG_REJECTED;
        }

        return HubFailureClass.SUCCESS;
    }

    public boolean requiresNewStart(HubFailureClass failureClass) {
        return failureClass == HubFailureClass.SESSION_OVER || failureClass == HubFailureClass.CONFIG_REJECTED;
    }
}
