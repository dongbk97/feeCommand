package vn.vnpay.fee.common;

public enum Performer {
    ADMIN("admin"), USER("user");
    final String name;

    Performer(String name) {
        this.name = name;
    }
}
