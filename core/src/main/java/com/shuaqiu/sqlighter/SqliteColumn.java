package com.shuaqiu.sqlighter;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 列 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.FIELD)
public @interface SqliteColumn {

    /** nullable 约束 */
    boolean nullable() default true;

    /** unique 约束 */
    boolean unique() default false;

    /** 默认值 */
    String defaultValue() default "";

    /** 忽略这个字段 */
    boolean ignore() default false;
}
