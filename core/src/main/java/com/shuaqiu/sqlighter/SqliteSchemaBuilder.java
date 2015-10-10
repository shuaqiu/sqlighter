package com.shuaqiu.sqlighter;

/**
 * SQLite 的建表语句
 * Created by shuaqiu on 10/8/15.
 */
public interface SqliteSchemaBuilder {

    /**
     * 构造建表语句
     *
     * @return 建表语句
     */
    String build();
}
