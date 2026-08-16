package pl.tomaszko.s04e02.hub;

public class HubWindpowerResponse {

    private final String rawBody;
    private final Integer code;
    private final String message;

    public HubWindpowerResponse(String rawBody, Integer code, String message) {
        this.rawBody = rawBody;
        this.code = code;
        this.message = message;
    }

    public String getRawBody() {
        return rawBody;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
