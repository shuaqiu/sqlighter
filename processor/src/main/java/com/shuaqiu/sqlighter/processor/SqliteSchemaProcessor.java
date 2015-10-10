package com.shuaqiu.sqlighter.processor;

import javax.annotation.processing.Processor;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import com.google.auto.service.AutoService;
import com.shuaqiu.sqlighter.SqliteSchemaBuilder;
import com.shuaqiu.sqlighter.SqliteTable;
import com.shuaqiu.sqlighter.processor.util.SqliteSchemaUtils;
import com.squareup.javapoet.MethodSpec;

/**
 * SQLite Scheme 生成
 */
@AutoService(Processor.class)
public class SqliteSchemaProcessor extends SqliteProcessor {

    private static final String SUFFIX = "SchemaBuilder";


    @Override
    protected String getSuffix() {
        return SUFFIX;
    }

    /**
     * Schema 的build 方法定义
     *
     * @param classElement 当前的{@link SqliteTable } 标记的Element
     * @return Schema 的build 方法定义
     */
    @Override
    protected MethodSpec[] buildMethodSpecs(final TypeElement classElement) {
        final MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("build");
        methodBuilder.addModifiers(Modifier.PUBLIC);
        methodBuilder.addAnnotation(Override.class);
        methodBuilder.returns(String.class);

        final String schema = SqliteSchemaUtils.generalSchema(typeUtils, classElement);
        methodBuilder.addStatement("return $S", schema);

        return new MethodSpec[]{methodBuilder.build()};
    }

    @Override
    protected Class<?>[] getSuperInterfaces() {
        return new Class<?>[]{SqliteSchemaBuilder.class};
    }
}
