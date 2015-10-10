package com.shuaqiu.sqlighter;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 主键 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.FIELD)
public @interface SqliteId {

    /**
     * 主键是否自增
     *
     * @return 主键是否自增
     */
    boolean autoIncrement() default false;
}
