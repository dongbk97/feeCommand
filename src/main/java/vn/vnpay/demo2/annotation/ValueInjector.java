package vn.vnpay.demo2.annotation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.vnpay.demo2.common.PropertiesFactory;

import java.lang.reflect.Field;

public class ValueInjector {
    private static final Logger logger = LoggerFactory.getLogger(ValueInjector.class);

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