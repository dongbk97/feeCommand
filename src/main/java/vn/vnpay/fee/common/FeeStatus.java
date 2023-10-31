package vn.vnpay.fee.common;

public enum FeeStatus {
    CREATE("01"),
    FEE_CHARGING("02"),
    FEE_STOP("03");
    final String code;

    FeeStatus(String code) {
        this.code = code;
    }
}
