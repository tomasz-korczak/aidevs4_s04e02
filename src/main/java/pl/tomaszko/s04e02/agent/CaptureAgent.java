package pl.tomaszko.s04e02.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import pl.tomaszko.s04e02.config.AppProperties;
import pl.tomaszko.s04e02.hub.AsyncResultPoller;
import pl.tomaszko.s04e02.hub.HubFailureClass;
import pl.tomaszko.s04e02.hub.HubFailureClassifier;
import pl.tomaszko.s04e02.hub.HubWindpowerResponse;
import pl.tomaszko.s04e02.schedule.ConfigurationBatch;
import pl.tomaszko.s04e02.schedule.HubReportParser;
import pl.tomaszko.s04e02.schedule.SessionFailedException;
import pl.tomaszko.s04e02.schedule.TurbineReport;
import pl.tomaszko.s04e02.schedule.TurbineScheduleBuilder;
import pl.tomaszko.s04e02.schedule.UnlockCodeService;
import pl.tomaszko.s04e02.schedule.WeatherForecastReport;
import pl.tomaszko.s04e02.tools.PlantTool;

@Component
public class CaptureAgent {

    private static final Logger log = LoggerFactory.getLogger(CaptureAgent.class);

    private final AppProperties appProperties;
    private final PlantTool plantTool;
    private final AsyncResultPoller asyncResultPoller;
    private final HubFailureClassifier failureClassifier;
    private final HubReportParser hubReportParser;
    private final TurbineScheduleBuilder scheduleBuilder;
    private final UnlockCodeService unlockCodeService;
    private final FlagExtractor flagExtractor;
    private final PromptFactory promptFactory;
    private final ChatClient chatClient;

    public CaptureAgent(
            AppProperties appProperties,
            PlantTool plantTool,
            AsyncResultPoller asyncResultPoller,
            HubFailureClassifier failureClassifier,
            HubReportParser hubReportParser,
            TurbineScheduleBuilder scheduleBuilder,
            UnlockCodeService unlockCodeService,
            FlagExtractor flagExtractor,
            PromptFactory promptFactory,
            ChatClient chatClient
    ) {
        this.appProperties = appProperties;
        this.plantTool = plantTool;
        this.asyncResultPoller = asyncResultPoller;
        this.failureClassifier = failureClassifier;
        this.hubReportParser = hubReportParser;
        this.scheduleBuilder = scheduleBuilder;
        this.unlockCodeService = unlockCodeService;
        this.flagExtractor = flagExtractor;
        this.promptFactory = promptFactory;
        this.chatClient = chatClient;
    }

    public CaptureRun run() {
        CaptureRun captureRun = new CaptureRun(appProperties.getPlant().getMaxSessionAttempts());
        while (captureRun.getAttemptIndex() < captureRun.getMaxAttempts()) {
            int attempt = captureRun.beginSessionAttempt();
            log.info("Starting session attempt {}/{}", attempt, captureRun.getMaxAttempts());
            try {
                String flag = runSingleSession(attempt);
                if (flag != null) {
                    captureRun.setFlag(flag);
                    captureRun.setStatus(CaptureStatus.SUCCESS);
                    return captureRun;
                }
                captureRun.setFailureReason("Session attempt " + attempt + " completed without flag");
            } catch (SessionFailedException ex) {
                log.warn("Session attempt {} failed: {} - {}", attempt, ex.getFailureClass(), ex.getMessage());
                captureRun.setFailureReason(ex.getMessage());
            } catch (RuntimeException ex) {
                log.warn("Session attempt {} failed: {}", attempt, ex.getMessage());
                captureRun.setFailureReason(ex.getMessage());
                HubFailureClass classified = classifyText(ex.getMessage());
                if (!failureClassifier.requiresNewStart(classified) && !(ex instanceof IllegalArgumentException)) {
                    captureRun.setFailureReason(ex.getMessage());
                }
            }
        }
        captureRun.setStatus(CaptureStatus.ATTEMPTS_EXHAUSTED);
        if (captureRun.getFailureReason() == null) {
            captureRun.setFailureReason("Exhausted session attempt budget without acquiring flag");
        }
        return captureRun;
    }

    private String runSingleSession(int attemptIndex) {
        ensure(plantTool.start(), "start");

        ensure(plantTool.get("documentation"), "documentation");
        String documentation = lastBody;

        ensure(plantTool.get("weather"), "weather");
        HubWindpowerResponse weatherResult = asyncResultPoller.pollForSource("weather");
        ensure(weatherResult, "weather-result");
        String weatherBody = weatherResult.getRawBody();

        TurbineReport turbineReport;
        WeatherForecastReport weatherReport;
        try {
            turbineReport = hubReportParser.parseTurbine(documentation);
            weatherReport = hubReportParser.parseWeather(weatherBody);
        } catch (RuntimeException parseError) {
            String repaired = gapFillParse(documentation, weatherBody, parseError.getMessage());
            turbineReport = hubReportParser.parseTurbine(documentation, repaired);
            weatherReport = hubReportParser.parseWeather(weatherBody + "\n" + repaired);
        }

        if (!turbineReport.hasRequiredPitches()) {
            throw new SessionFailedException(
                    HubFailureClass.CONFIG_REJECTED,
                    "Idle/production pitch missing from hub documentation/turbine payloads"
            );
        }

        ConfigurationBatch batch = scheduleBuilder.build(turbineReport, weatherReport);
        unlockCodeService.signBatch(batch);
        ensure(plantTool.configBatch(PlantTool.toConfigMaps(batch.getConfigs())), "config");

        ensure(plantTool.get("turbinecheck"), "turbinecheck-final");
        HubWindpowerResponse finalCheck = asyncResultPoller.pollForSource("turbinecheck");
        ensure(finalCheck, "turbinecheck-final-result");

        HubWindpowerResponse done = plantTool.done();
        ensure(done, "done");
        String flag = flagExtractor.extract(done.getRawBody());
        if (flag == null) {
            throw new SessionFailedException(HubFailureClass.CONFIG_REJECTED, done.getRawBody());
        }
        log.info("Flag acquired on attempt {}", attemptIndex);
        return flag;
    }

    private String lastBody;

    private void ensure(HubWindpowerResponse response, String step) {
        lastBody = response == null ? "" : response.getRawBody();
        HubFailureClass failureClass = failureClassifier.classify(response);
        if (failureClassifier.requiresNewStart(failureClass)) {
            throw new SessionFailedException(failureClass, step + ": " + lastBody);
        }
        String flag = flagExtractor.extract(lastBody);
        if (flag != null && "done".equals(step)) {
            return;
        }
    }

    private String gapFillParse(String documentation, String weatherBody, String error) {
        String system = promptFactory.systemPrompt();
        String user = """
                Structured parsing failed: %s

                Extract JSON with fields:
                strengthMs, idlePitchAngle, productionPitchAngle,
                and weather items array of {occurrenceDate, occurrenceHour, windMs}.

                documentation:
                %s

                weather:
                %s
                """.formatted(error, documentation, weatherBody);
        return chatClient.prompt()
                .system(system)
                .user(user)
                .call()
                .content();
    }

    private HubFailureClass classifyText(String text) {
        return failureClassifier.classify(new HubWindpowerResponse(text, null, text));
    }
}
