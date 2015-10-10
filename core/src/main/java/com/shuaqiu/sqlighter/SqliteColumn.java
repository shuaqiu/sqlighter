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

    /**
     * nullable 约束
     *
     * @return 是否可以为null
     */
    boolean nullable() default true;

    /**
     * unique 约束
     *
     * @return 是否唯一
     */
    boolean unique() default false;

    /**
     * 默认值
     *
     * @return 默认值
     */
    String defaultValue() default "";

    /**
     * 忽略这个字段
     *
     * @return 是否忽略这个字段
     */
    boolean ignore() default false;
}
