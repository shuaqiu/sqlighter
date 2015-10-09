package com.shuaqiu.sqlighter.processor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

import com.shuaqiu.sqlighter.SqliteColumn;
import com.shuaqiu.sqlighter.SqliteId;
import com.shuaqiu.sqlighter.SqliteTable;

/**
 * 生成 Schema
 * Created by shuaqiu on 15-10-9.
 */
public class SqliteSchemaGenerator {

    private static final Map<String, String> SQL_TYPE_MAPPING;
    private static final String DEFAULT_COLUMN = "DEFAULT";

    static {
        SQL_TYPE_MAPPING = new HashMap<>();

        SQL_TYPE_MAPPING.put(DEFAULT_COLUMN, "TEXT");

        SQL_TYPE_MAPPING.put("String", "TEXT");
        SQL_TYPE_MAPPING.put("Integer", "INTEGER");
        SQL_TYPE_MAPPING.put("Long", "INTEGER");
        SQL_TYPE_MAPPING.put("Date", "INTEGER");
        SQL_TYPE_MAPPING.put("Double", "REAL");
        SQL_TYPE_MAPPING.put("Float", "REAL");
    }

    /**
     * 生成 Schema
     *
     * @param typeUtils    Type 的Utility
     * @param classElement 当前的{@link SqliteTable } 标记的Element
     * @return 对应的SQLite 的Schema
     */
    protected static String buildSchema(final Types typeUtils, final TypeElement classElement) {
        final StringBuilder ddl = new StringBuilder("create table if not exists ");

        final String table = getTableName(classElement);
        ddl.append(table);
        ddl.append("(");

        final List<VariableElement> fields = getFields(typeUtils, classElement);

        int i = 0;
        for (final VariableElement column : fields) {
            final SqliteColumn sqliteColumn = column.getAnnotation(SqliteColumn.class);
            if (sqliteColumn != null && sqliteColumn.ignore()) {
                continue;
            }

            if (i > 0) {
                ddl.append(", ");
            }
            i++;

            final String columnName = column.getSimpleName().toString();
            ddl.append(columnName);
            ddl.append(" ");

            final String columnType = getColumnType(typeUtils, column);
            System.err.println("columnType ---> " + columnType + " for " + columnName);
            ddl.append(columnType);

            final SqliteId sqliteId = column.getAnnotation(SqliteId.class);
            if (sqliteId != null) {
                // 如果之前已经有primary key 了, 则替换掉.
                final int prePrimaryKey = ddl.indexOf(" primary key");
                if  (prePrimaryKey > 0) {
                    int prePrimaryKeyComma = ddl.indexOf(",", prePrimaryKey);
                    ddl.delete(prePrimaryKey, prePrimaryKeyComma);
                }

                ddl.append(" primary key");

                if (sqliteId.autoIncrement()) {
                    ddl.append(" autoincrement");
                }
            } else if (sqliteColumn != null) {

                if (!sqliteColumn.nullable()) {
                    ddl.append(" not null");
                }

                if (!sqliteColumn.defaultValue().equals("")) {
                    ddl.append(" default ");
                    ddl.append(sqliteColumn.defaultValue());
                }

                if (sqliteColumn.unique()) {
                    ddl.append(", ");
                    ddl.append(getUniqueClause(table, columnName));
                }
            }
        }

        ddl.append(")");
        return ddl.toString();
    }

    /**
     * 获取表名
     *
     * @param classElement 当前的{@link SqliteTable } 标记的Element
     * @return 表名
     */
    private static String getTableName(final TypeElement classElement) {
        final SqliteTable annotation = classElement.getAnnotation(SqliteTable.class);
        final String value = annotation.value();
        System.err.println("SqliteTable annotation value is : " + value);

        if (value == null || value.trim().equals("")) {
            return classElement.getSimpleName().toString();
        }
        return value;
    }

    /**
     * 获取类的字段列表, 包括父类中的字段
     *
     * @param typeUtils    Type 的Utility
     * @param classElement 当前的{@link SqliteTable } 标记的Element
     * @return 类的字段列表, 包括父类中的字段
     */
    private static List<VariableElement> getFields(final Types typeUtils, final TypeElement classElement) {
        final List<VariableElement> fields = new ArrayList<>();

        final TypeMirror superclass = classElement.getSuperclass();
        // 如果superclass.getKind() == TypeKind.NONE, 则表示已经到了java.lang.Object
        if (superclass.getKind() != TypeKind.NONE) {
            // 还没有到java.lang.Object, 那么先去获取父类里面的字段
            final TypeElement superclassElement = (TypeElement) typeUtils.asElement(superclass);
            List<VariableElement> superclassFields = getFields(typeUtils, superclassElement);
            fields.addAll(superclassFields);
        }

        // 获取这个类的字段
        final List<? extends Element> enclosedElements = classElement.getEnclosedElements();
        for (final Element element : enclosedElements) {
            if (element.getKind() != ElementKind.FIELD) {
                // 不是字段, 忽略
                continue;
            }

            final VariableElement variableElement = (VariableElement) element;
            fields.add(variableElement);
        }

        return fields;
    }

    /**
     * 获取字段对应的数据库字段
     *
     * @param typeUtils    Type 的Utility
     * @param fieldElement 字段
     * @return 数据库字段
     */
    private static String getColumnType(final Types typeUtils, final VariableElement fieldElement) {
        // 获取到这个字段的类型
        final Element element = typeUtils.asElement(fieldElement.asType());
        final TypeElement typeElement = (TypeElement) element;

        // 从映射表中获取
        final String typeName = typeElement.getSimpleName().toString();
        final String columnType = SQL_TYPE_MAPPING.get(typeName);
        if (columnType == null) {
            return SQL_TYPE_MAPPING.get(DEFAULT_COLUMN);
        }
        return columnType;
    }

    /**
     * 获取唯一约束的语句
     *
     * @param table      表名, 用于生成约束名
     * @param columnName 字段名
     * @return 唯一约束的语句
     */
    private static String getUniqueClause(final String table, final String columnName) {
        // CONSTRAINT constraint_name UNIQUE (uc_col1, uc_col2, ... uc_col_n)
        final String constraintName = table + "_" + columnName;
        return "constraint " + constraintName + " unique (" + columnName + ")";
    }
}
