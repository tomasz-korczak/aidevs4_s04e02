package pl.tomaszko.s04e02.agent;

public class CaptureRun {

    private int attemptIndex;
    private final int maxAttempts;
    private CaptureStatus status = CaptureStatus.RUNNING;
    private String flag;
    private String failureReason;

    public CaptureRun(int maxAttempts) {
        this.maxAttempts = maxAttempts;
        this.attemptIndex = 0;
    }

    public int getAttemptIndex() {
        return attemptIndex;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public CaptureStatus getStatus() {
        return status;
    }

    public void setStatus(CaptureStatus status) {
        this.status = status;
    }

    public String getFlag() {
        return flag;
    }

    public void setFlag(String flag) {
        this.flag = flag;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public int beginSessionAttempt() {
        attemptIndex++;
        return attemptIndex;
    }

    public boolean hasRemainingAttempts() {
        return attemptIndex < maxAttempts;
    }
}
