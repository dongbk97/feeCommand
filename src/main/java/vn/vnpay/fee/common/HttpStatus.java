package vn.vnpay.fee.common;

public enum HttpStatus {
    SUCCESS(200, "Successfully", "00"),
    REQUEST_TIMEOUT(408, "Request Timeout", "01"),
    INTERNAL_SERVER_ERROR(500, "Internal server error", "01"),
    EXISTED_REQUEST_ID(200, "Request is existed", "01"),
    EXPIRED_REQUEST(200, "Request is expired", "01");

    private final int code;
    private final String message;
    private final String messageStatus;

    HttpStatus(int code, String message, String messageStatus) {
        this.code = code;
        this.message = message;
        this.messageStatus = messageStatus;
    }

    public String getMessage() {
        return message;
    }

    public int getCode() {
        return code;
    }

    public String getMessageStatus() {
        return messageStatus;
    }
}
