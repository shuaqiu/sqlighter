package test;

import java.util.Date;

import com.shuaqiu.sqlighter.SqliteId;
import com.shuaqiu.sqlighter.SqliteTable;

/**
 * Bean for test
 * Created by shuaqiu on 2015-10-11.
 */
@SqliteTable
public class SimpleBean {

    @SqliteId
    private int id;

    private boolean active;

    private String str;

    private Date date;
}
