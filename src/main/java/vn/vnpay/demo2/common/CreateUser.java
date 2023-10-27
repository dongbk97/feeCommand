package vn.vnpay.demo2.common;

public enum CreateUser {
    ADMIN("admin"), USER("user");
    final String name;

    CreateUser(String name) {
        this.name = name;
    }
}
