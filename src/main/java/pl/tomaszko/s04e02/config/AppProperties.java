package pl.tomaszko.s04e02.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Hub hub = new Hub();
    private final Llm llm = new Llm();
    private final Plant plant = new Plant();
    private final Prompt prompt = new Prompt();

    public Hub getHub() {
        return hub;
    }

    public Llm getLlm() {
        return llm;
    }

    public Plant getPlant() {
        return plant;
    }

    public Prompt getPrompt() {
        return prompt;
    }

    public static class Hub {
        private String verifyUrl = "https://hub.ag3nts.org/verify";
        private String task = "windpower";

        public String getVerifyUrl() {
            return verifyUrl;
        }

        public void setVerifyUrl(String verifyUrl) {
            this.verifyUrl = verifyUrl;
        }

        public String getTask() {
            return task;
        }

        public void setTask(String task) {
            this.task = task;
        }
    }

    public static class Llm {
        private String model = "inclusionai/ling-3.0-flash";
        private String openrouterBaseUrl = "https://openrouter.ai/api/v1";

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getOpenrouterBaseUrl() {
            return openrouterBaseUrl;
        }

        public void setOpenrouterBaseUrl(String openrouterBaseUrl) {
            this.openrouterBaseUrl = openrouterBaseUrl;
        }
    }

    public static class Plant {
        private int maxSessionAttempts = 5;
        private long getResultPollIntervalMs = 500;
        private long getResultPollMaxIntervalMs = 2000;
        private List<String> sessionOverMessagePatterns = new ArrayList<>(List.of(
                "session is over",
                "configuration session is over",
                "service window is over",
                "service window expired",
                "window expired"
        ));
        private List<String> configRejectedMessagePatterns = new ArrayList<>(List.of(
                "incorrect",
                "insufficient power",
                "unsafe",
                "destroyed"
        ));

        public int getMaxSessionAttempts() {
            return maxSessionAttempts;
        }

        public void setMaxSessionAttempts(int maxSessionAttempts) {
            this.maxSessionAttempts = maxSessionAttempts;
        }

        public long getGetResultPollIntervalMs() {
            return getResultPollIntervalMs;
        }

        public void setGetResultPollIntervalMs(long getResultPollIntervalMs) {
            this.getResultPollIntervalMs = getResultPollIntervalMs;
        }

        public long getGetResultPollMaxIntervalMs() {
            return getResultPollMaxIntervalMs;
        }

        public void setGetResultPollMaxIntervalMs(long getResultPollMaxIntervalMs) {
            this.getResultPollMaxIntervalMs = getResultPollMaxIntervalMs;
        }

        public List<String> getSessionOverMessagePatterns() {
            return sessionOverMessagePatterns;
        }

        public void setSessionOverMessagePatterns(List<String> sessionOverMessagePatterns) {
            this.sessionOverMessagePatterns = sessionOverMessagePatterns;
        }

        public List<String> getConfigRejectedMessagePatterns() {
            return configRejectedMessagePatterns;
        }

        public void setConfigRejectedMessagePatterns(List<String> configRejectedMessagePatterns) {
            this.configRejectedMessagePatterns = configRejectedMessagePatterns;
        }
    }

    public static class Prompt {
        private String systemTemplateLocation = "classpath:prompts/system-prompt.txt";

        public String getSystemTemplateLocation() {
            return systemTemplateLocation;
        }

        public void setSystemTemplateLocation(String systemTemplateLocation) {
            this.systemTemplateLocation = systemTemplateLocation;
        }
    }
}
