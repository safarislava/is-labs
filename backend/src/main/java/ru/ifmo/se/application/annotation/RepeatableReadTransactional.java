package ru.ifmo.se.application.annotation;

import jakarta.enterprise.util.Nonbinding;
import jakarta.interceptor.InterceptorBinding;
import jakarta.transaction.Transactional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Inherited
@InterceptorBinding
@Transactional
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RepeatableReadTransactional {

    @Nonbinding
    Transactional.TxType value() default Transactional.TxType.REQUIRED;

    @Nonbinding
    Class<?>[] rollbackOn() default {};

    @Nonbinding
    Class<?>[] dontRollbackOn() default {};
}
