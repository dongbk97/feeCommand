package vn.vnpay.fee.common;

public enum CreateUser {
    ADMIN("admin"), USER("user");
    final String name;

    CreateUser(String name) {
        this.name = name;
    }
}
