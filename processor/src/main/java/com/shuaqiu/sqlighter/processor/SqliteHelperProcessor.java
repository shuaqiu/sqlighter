package com.shuaqiu.sqlighter.processor;

import java.util.List;

import javax.annotation.processing.Processor;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import com.google.auto.service.AutoService;
import com.shuaqiu.sqlighter.SqliteColumn;
import com.shuaqiu.sqlighter.SqliteTable;
import com.shuaqiu.sqlighter.processor.util.FieldUtils;
import com.shuaqiu.sqlighter.processor.util.SqliteSchemaUtils;
import com.shuaqiu.sqlighter.processor.util.StringUtils;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;

/**
 * SQLite ContentValue 生成
 */
@AutoService(Processor.class)
public class SqliteHelperProcessor extends SqliteProcessor {

    private static final String SUFFIX = "Helper";

    private static final ClassName CONTENT_VALUES = ClassName.get("android.content", "ContentValues");
    private static final ClassName CURSOR = ClassName.get("android.database", "Cursor");

    @Override
    protected String getSuffix() {
        return SUFFIX;
    }

    /**
     * ContentViews 的方法定义
     *
     * @param classElement 当前的{@link SqliteTable } 标记的Element
     * @return 方法定义
     */
    @Override
    protected MethodSpec[] buildMethodSpecs(final TypeElement classElement) {
        final MethodSpec constructor = buildConstructor();
        final MethodSpec getTableNameMethodSpec = buildGetTableNameMethodSpec(classElement);
        final MethodSpec schemaMethodSpec = buildBuildMethodSpec(classElement);
        final MethodSpec toContentValuesMethodSpec = buildToContentValuesMethodSpec(classElement);
        final MethodSpec fromCursorMethodSpec = buildFromCursorMethodSpec(classElement);

        return new MethodSpec[]{
                constructor,
                getTableNameMethodSpec,
                schemaMethodSpec,
                toContentValuesMethodSpec,
                fromCursorMethodSpec
        };
    }

    /**
     * 构造私有的默认构造函数
     *
     * @return 私有的默认构造函数
     */
    private MethodSpec buildConstructor() {
        final MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder();
        constructorBuilder.addModifiers(Modifier.PRIVATE);
        return constructorBuilder.build();
    }

    /**
     * getTableName 方法定义
     *
     * @param classElement 当前的{@link SqliteTable } 标记的Element
     * @return getTableName 方法定义
     */
    private MethodSpec buildGetTableNameMethodSpec(final TypeElement classElement) {
        final MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("getTableName");
        methodBuilder.addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        methodBuilder.returns(String.class);

        final String tableName = SqliteSchemaUtils.getTableName(classElement);
        methodBuilder.addStatement("return $S", tableName);

        return methodBuilder.build();
    }

    /**
     * schema 方法定义
     *
     * @param classElement 当前的{@link SqliteTable } 标记的Element
     * @return schema 方法定义
     */
    private MethodSpec buildBuildMethodSpec(final TypeElement classElement) {
        final MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("schema");
        methodBuilder.addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        methodBuilder.returns(String.class);

        final String schema = SqliteSchemaUtils.generalSchema(typeUtils, classElement);
        methodBuilder.addStatement("return $S", schema);

        return methodBuilder.build();
    }

    /**
     * toContentValues 的方法定义
     *
     * @param classElement 当前的{@link SqliteTable } 标记的Element
     * @return 方法定义
     */
    private MethodSpec buildToContentValuesMethodSpec(final TypeElement classElement) {
        final MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("toContentValues");
        methodBuilder.addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        methodBuilder.returns(CONTENT_VALUES);

        final TypeName typeName = TypeName.get(classElement.asType());
        methodBuilder.addParameter(typeName, "data", Modifier.FINAL);

        methodBuilder.addStatement("final $T values = new $T()", CONTENT_VALUES, CONTENT_VALUES);

        final List<VariableElement> fields = FieldUtils.getFields(typeUtils, classElement);
        for (final VariableElement field : fields) {
            final SqliteColumn sqliteColumn = field.getAnnotation(SqliteColumn.class);
            if (sqliteColumn != null && sqliteColumn.ignore()) {
                continue;
            }

            final String fieldName = field.getSimpleName().toString();
            final String getFieldValueStatement = buildGetFieldValueStatement(field);

            methodBuilder.addStatement("values.put($S, $L)", fieldName, getFieldValueStatement);
        }

        methodBuilder.addStatement("return values");

        return methodBuilder.build();
    }

    /**
     * 构建获取字段值的语句: data.getXxx(). 主要是对于Date 这些类型, 需要转换成long 格式
     *
     * @param field 字段元素
     * @return 获取字段值的语句
     */
    private String buildGetFieldValueStatement(final VariableElement field) {
        final String fieldName = field.getSimpleName().toString();
        final String capitalizeFieldName = StringUtils.capitalize(fieldName);

        final String getFieldValueStatement = "data.get" + capitalizeFieldName + "()";

        final String fieldTypeName = FieldUtils.getFieldTypeQualifiedName(typeUtils, field);

        switch (fieldTypeName) {
            case "java.util.Date":
                return getFieldValueStatement + " == null ? null : " + getFieldValueStatement + ".getTime()";

            case "boolean":
            case "java.lang.Boolean":
                if (fieldName.startsWith("is")) {
                    return "data." + fieldName + "()";
                }
                return "data.is" + capitalizeFieldName + "()";
        }

        return getFieldValueStatement;
    }

    /**
     * fromCursor 的方法定义
     *
     * @param classElement 当前的{@link SqliteTable } 标记的Element
     * @return 方法定义
     */
    private MethodSpec buildFromCursorMethodSpec(final TypeElement classElement) {
        final MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("fromCursor");
        methodBuilder.addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        final TypeName typeName = TypeName.get(classElement.asType());
        methodBuilder.returns(typeName);

        methodBuilder.addParameter(CURSOR, "cursor", Modifier.FINAL);

        methodBuilder.addStatement("final $T bean = new $T()", typeName, typeName);

        final List<VariableElement> fields = FieldUtils.getFields(typeUtils, classElement);

        int columnIndex = 0;
        for (final VariableElement field : fields) {
            final SqliteColumn sqliteColumn = field.getAnnotation(SqliteColumn.class);
            if (sqliteColumn != null && sqliteColumn.ignore()) {
                continue;
            }

            // if (!cursor.isNull(columnIndex)) {
            //     // 如果字段是null 值, 則不作處理
            //     values.setXxx(cursor.getXxx(columnIndex));
            // }
            methodBuilder.beginControlFlow("if (!cursor.isNull($L))", columnIndex);
            methodBuilder.addCode("// if column value is null, ignore this column\n");

            final String fieldName = field.getSimpleName().toString();
            final String capitalizeFieldName = StringUtils.capitalize(fieldName);
            final String getCursorValueStatement = String.format(buildGetCursorValueStatement(field), columnIndex++);

            methodBuilder.addStatement("bean.set$L($L)", capitalizeFieldName, getCursorValueStatement);

            methodBuilder.endControlFlow();
        }

        methodBuilder.addStatement("return bean");

        return methodBuilder.build();
    }

    /**
     * 构建获取字段值的语句: cursor.getXxx(). 主要是根据不同的数据类型, 需要调用Cursor 的不同方法
     *
     * @param field 字段元素
     * @return 获取字段值的语句
     */
    private String buildGetCursorValueStatement(final VariableElement field) {
        final String fieldTypeName = FieldUtils.getFieldTypeQualifiedName(typeUtils, field);
        switch (fieldTypeName) {
            case "int":
            case "java.lang.Integer":
                return "cursor.getInt(%d)";

            case "java.lang.String":
                return "cursor.getString(%d)";

            case "long":
            case "java.lang.Long":
                return "cursor.getLong(%d)";

            case "double":
            case "java.lang.Double":
                return "cursor.getDouble(%d)";

            case "float":
            case "java.lang.Float":
                return "cursor.getFloat(%d)";

            case "boolean":
            case "java.lang.Boolean":
                return "cursor.getInt(%d) == 1";

            case "java.util.Date":
                return "new java.util.Date(cursor.getLong(%d))";
        }

        return "cursor.getString(%d)";
    }
}
