package pl.tomaszko.s04e02.hub;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import pl.tomaszko.s04e02.config.AppProperties;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class HubClientTest {

    @Test
    void requestEnvelopeContainsActionTaskAndDoesNotRequireLoggingApiKeyInAnswer() throws Exception {
        Map<String, Object> answer = HubWindpowerRequest.action("start");
        HubWindpowerRequest request = new HubWindpowerRequest("secret-key", "windpower", answer);
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(request);
        assertEquals(true, json.contains("\"task\":\"windpower\""));
        assertEquals(true, json.contains("\"action\":\"start\""));
        assertEquals(true, json.contains("secret-key"));
        Map<String, Object> loggedAnswer = new LinkedHashMap<>(answer);
        assertFalse(loggedAnswer.containsKey("apikey"));
    }

    @Test
    void classifierDetectsSessionOver() {
        AppProperties properties = new AppProperties();
        HubFailureClassifier classifier = new HubFailureClassifier(properties);
        HubWindpowerResponse response = new HubWindpowerResponse(
                "{\"message\":\"configuration session is over\"}",
                1,
                "configuration session is over"
        );
        assertEquals(HubFailureClass.SESSION_OVER, classifier.classify(response));
    }
}
