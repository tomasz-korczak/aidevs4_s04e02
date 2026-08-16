package pl.tomaszko.s04e02.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;
import pl.tomaszko.s04e02.agent.CaptureAgent;
import pl.tomaszko.s04e02.agent.CaptureRun;
import pl.tomaszko.s04e02.agent.CaptureStatus;
import pl.tomaszko.s04e02.agent.SetupFailedException;
import pl.tomaszko.s04e02.agent.SetupValidator;

@Component
public class CaptureApplicationRunner implements ApplicationRunner, ExitCodeGenerator {

    private static final Logger log = LoggerFactory.getLogger(CaptureApplicationRunner.class);

    private final SetupValidator setupValidator;
    private final CaptureAgent captureAgent;
    private int exitCode = 1;

    public CaptureApplicationRunner(SetupValidator setupValidator, CaptureAgent captureAgent) {
        this.setupValidator = setupValidator;
        this.captureAgent = captureAgent;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            setupValidator.validateOrThrow();
            CaptureRun captureRun = captureAgent.run();
            if (captureRun.getStatus() == CaptureStatus.SUCCESS && captureRun.getFlag() != null) {
                System.out.println(captureRun.getFlag());
                log.info("Capture succeeded: {}", captureRun.getFlag());
                exitCode = 0;
            } else {
                String reason = captureRun.getFailureReason() == null
                        ? "Capture failed"
                        : captureRun.getFailureReason();
                System.out.println("FAILURE: " + reason);
                log.error("Capture failed: {}", reason);
                exitCode = 2;
            }
        } catch (SetupFailedException ex) {
            System.out.println("SETUP_FAILED: " + ex.getMessage());
            log.error("Setup failed: {}", ex.getMessage());
            exitCode = 3;
        } catch (Exception ex) {
            System.out.println("FAILURE: " + ex.getMessage());
            log.error("Unexpected failure", ex);
            exitCode = 1;
        }
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}
