package vn.vnpay.fee.annotation;

import vn.vnpay.fee.common.PropertiesFactory;

import java.lang.reflect.Field;

public class ValueInjector {

    private ValueInjector() {
    }

    public static void injectValues(Object target) throws IllegalAccessException {
        Field[] fields = target.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(CustomValue.class)) {
                CustomValue customValue = field.getAnnotation(CustomValue.class);
                String key = customValue.value();
                field.setAccessible(true);
                Class<?> fieldType = field.getType();
                String value = PropertiesFactory.getFromProperties(key);
                if (value.matches("\\d+") && (fieldType.isAssignableFrom(int.class) || fieldType.isAssignableFrom(Integer.class))) {
                    int number = Integer.parseInt(value);
                    field.set(target, number);
                } else if (value.matches("^(true|false)$") && (fieldType.isAssignableFrom(boolean.class) || fieldType.isAssignableFrom(Boolean.class))) {
                    boolean input = Boolean.parseBoolean(value);
                    field.set(target, input);
                } else {
                    field.set(target, value);
                }
            }
        }
    }

}