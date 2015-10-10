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
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;

/**
 * SQLite ContentValue 生成
 */
@AutoService(Processor.class)
public class SqliteContentValueProcessor extends SqliteProcessor {

    private static final String SUFFIX = "ContentValueBuilder";

    private static final ClassName CONTENT_VALUES = ClassName.get("android.content", "ContentValues");

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
        final MethodSpec toContentValuesMethodSpec = buildToContentValuesMethodSpec(classElement);
        return new MethodSpec[]{toContentValuesMethodSpec};
    }

    /**
     * toContentValues 的方法定义
     *
     * @param classElement 当前的{@link SqliteTable } 标记的Element
     * @return 方法定义
     */
    private MethodSpec buildToContentValuesMethodSpec(final TypeElement classElement) {
        final MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("toContentValues");
        methodBuilder.addModifiers(Modifier.PUBLIC, Modifier.FINAL);
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
            final  String getFieldValueStatement = buildGetFieldValueStatement(field);

            methodBuilder.addStatement("values.put($S, $L)", fieldName, getFieldValueStatement);
        }

        methodBuilder.addStatement("return values");

        return methodBuilder.build();
    }

    /**
     * 构建获取字段值的语句: data.getXxx(). 主要是对于Date 这些类型, 需要转换成long 格式
     * @param field 字段元素
     * @return 获取字段值的语句
     */
    private String buildGetFieldValueStatement(final VariableElement field) {
        final String fieldName = field.getSimpleName().toString();
        final String getFieldValueStatement = "data.get" + capitalize(fieldName) + "()";

        final String fieldTypeName = FieldUtils.getFieldTypeQualifiedName(typeUtils, field);
        System.err.println("fieldTypeName ---> " + fieldTypeName + " for " + fieldName);

        switch (fieldTypeName) {
            case "java.util.Date":
                return getFieldValueStatement + " == null ? null : " + getFieldValueStatement + ".getTime()";
        }

        return getFieldValueStatement;
    }

    /**
     * 将字符串的首字母大写
     * @param str 字符串
     * @return 首字母大写后的字符串
     */
    private String capitalize(final String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
