package pl.tomaszko.s04e02.schedule;

public class ConfigPoint {

    private final String startDate;
    private final int startHour;
    private final double pitchAngle;
    private final String turbineMode;
    private final double windMs;
    private String unlockCode;

    public ConfigPoint(String startDate, int startHour, double pitchAngle, String turbineMode, double windMs) {
        this.startDate = startDate;
        this.startHour = startHour;
        this.pitchAngle = pitchAngle;
        this.turbineMode = turbineMode;
        this.windMs = windMs;
    }

    public String getStartDate() {
        return startDate;
    }

    public int getStartHour() {
        return startHour;
    }

    public double getPitchAngle() {
        return pitchAngle;
    }

    public String getTurbineMode() {
        return turbineMode;
    }

    public double getWindMs() {
        return windMs;
    }

    public String getUnlockCode() {
        return unlockCode;
    }

    public void setUnlockCode(String unlockCode) {
        this.unlockCode = unlockCode;
    }
}
