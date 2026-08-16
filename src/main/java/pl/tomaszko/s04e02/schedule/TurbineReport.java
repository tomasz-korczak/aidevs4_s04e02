package pl.tomaszko.s04e02.schedule;

public class TurbineReport {

    private final double strengthMs;
    private final Double idlePitchAngle;
    private final Double productionPitchAngle;
    private final String raw;

    public TurbineReport(double strengthMs, Double idlePitchAngle, Double productionPitchAngle, String raw) {
        this.strengthMs = strengthMs;
        this.idlePitchAngle = idlePitchAngle;
        this.productionPitchAngle = productionPitchAngle;
        this.raw = raw;
    }

    public double getStrengthMs() {
        return strengthMs;
    }

    public Double getIdlePitchAngle() {
        return idlePitchAngle;
    }

    public Double getProductionPitchAngle() {
        return productionPitchAngle;
    }

    public String getRaw() {
        return raw;
    }

    public boolean hasRequiredPitches() {
        return idlePitchAngle != null && productionPitchAngle != null;
    }
}
