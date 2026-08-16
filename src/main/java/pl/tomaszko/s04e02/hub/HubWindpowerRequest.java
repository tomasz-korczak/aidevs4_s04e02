package pl.tomaszko.s04e02.hub;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.Map;

public class HubWindpowerRequest {

    private String apikey;
    private String task;
    private Map<String, Object> answer = new LinkedHashMap<>();

    public HubWindpowerRequest() {
    }

    public HubWindpowerRequest(String apikey, String task, Map<String, Object> answer) {
        this.apikey = apikey;
        this.task = task;
        this.answer = answer;
    }

    public String getApikey() {
        return apikey;
    }

    public void setApikey(String apikey) {
        this.apikey = apikey;
    }

    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public Map<String, Object> getAnswer() {
        return answer;
    }

    public void setAnswer(Map<String, Object> answer) {
        this.answer = answer;
    }

    public static Map<String, Object> action(String actionName) {
        Map<String, Object> answer = new LinkedHashMap<>();
        answer.put("action", actionName);
        return answer;
    }
}
