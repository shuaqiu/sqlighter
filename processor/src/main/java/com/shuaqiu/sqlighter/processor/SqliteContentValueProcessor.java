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
        methodBuilder.addModifiers(Modifier.PUBLIC);
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
            methodBuilder.addStatement("values.put($S, $L)", fieldName, "data.get" + capitalize(fieldName) + "()");
        }

        methodBuilder.addStatement("return values");

        return methodBuilder.build();
    }

    private String capitalize(final String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
