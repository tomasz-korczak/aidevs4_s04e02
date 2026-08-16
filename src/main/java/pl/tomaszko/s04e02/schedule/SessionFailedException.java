package pl.tomaszko.s04e02.schedule;

import pl.tomaszko.s04e02.hub.HubFailureClass;

public class SessionFailedException extends RuntimeException {

    private final HubFailureClass failureClass;

    public SessionFailedException(HubFailureClass failureClass, String message) {
        super(message);
        this.failureClass = failureClass;
    }

    public HubFailureClass getFailureClass() {
        return failureClass;
    }
}
