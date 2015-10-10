package com.shuaqiu.sqlighter;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Bean to Table */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface SqliteTable {

    /**
     * table 名
     *
     * @return table 名
     */
    String value() default "";
}
