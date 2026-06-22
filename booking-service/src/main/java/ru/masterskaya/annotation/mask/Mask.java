package ru.masterskaya.annotation.mask;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Аннотация с возможностью указать стратегию маскирования
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Mask {
    MaskType value() default MaskType.FULL;

    enum MaskType {
        FULL,           // ******
        PARTIAL,        // abc***xyz
        EMAIL,          // a***@domain.com
        JWT             // eyJ... (header).***.***
    }
}
