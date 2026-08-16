package pl.tomaszko.s04e02.agent;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pl.tomaszko.s04e02.hub.HubClient;

@Component
public class SetupValidator {

    private final HubClient hubClient;
    private final String openRouterApiKey;

    public SetupValidator(HubClient hubClient, @Value("${OPENROUTER_API_KEY:}") String openRouterApiKey) {
        this.hubClient = hubClient;
        this.openRouterApiKey = openRouterApiKey == null ? "" : openRouterApiKey;
    }

    public void validateOrThrow() {
        if (!hubClient.hasApiKey()) {
            throw new SetupFailedException("Missing required environment variable HUB_API_KEY");
        }
        if (openRouterApiKey.isBlank()) {
            throw new SetupFailedException("Missing required environment variable OPENROUTER_API_KEY");
        }
        if (!hubClient.isReachable()) {
            throw new SetupFailedException("Plant hub is unreachable at configured verify URL");
        }
    }
}
