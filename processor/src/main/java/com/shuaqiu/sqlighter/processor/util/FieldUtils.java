package com.shuaqiu.sqlighter.processor.util;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

import com.shuaqiu.sqlighter.SqliteTable;

/**
 * 类的字段 Utility
 * Created by shuaqiu on 2015-10-10.
 */
public final class FieldUtils {

    /**
     * 获取类的字段列表, 包括父类中的字段
     *
     * @param typeUtils    Type 的Utility
     * @param classElement 当前的{@link SqliteTable } 标记的Element
     * @return 类的字段列表, 包括父类中的字段
     */
    public static List<VariableElement> getFields(final Types typeUtils, final TypeElement classElement) {
        final List<VariableElement> fields = new ArrayList<>();

        final TypeMirror superclass = classElement.getSuperclass();
        // 如果superclass.getKind() == TypeKind.NONE, 则表示已经到了java.lang.Object
        if (superclass.getKind() != TypeKind.NONE) {
            // 还没有到java.lang.Object, 那么先去获取父类里面的字段
            final TypeElement superclassElement = (TypeElement) typeUtils.asElement(superclass);
            final List<VariableElement> superclassFields = getFields(typeUtils, superclassElement);
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
     * 获取到这个字段的类型
     *
     * @param typeUtils    Type 的Utility
     * @param fieldElement 字段
     * @return 这个字段的类型
     */
    public static TypeElement getFieldTypeElement(final Types typeUtils, final VariableElement fieldElement) {
        final Element element = typeUtils.asElement(fieldElement.asType());
        return (TypeElement) element;
    }

    /**
     * 获取到这个字段的类型名称
     *
     * @param typeUtils    Type 的Utility
     * @param fieldElement 字段
     * @return 这个字段的类型名称
     */
    public static String getFieldTypeSimpleName(final Types typeUtils, final VariableElement fieldElement) {
        final TypeElement fieldTypeElement = getFieldTypeElement(typeUtils, fieldElement);
        return fieldTypeElement.getSimpleName().toString();
    }

    /**
     * 获取到这个字段的类型的全名
     *
     * @param typeUtils    Type 的Utility
     * @param fieldElement 字段
     * @return 这个字段的类型的全名
     */
    public static String getFieldTypeQualifiedName(final Types typeUtils, final VariableElement fieldElement) {
        final TypeElement fieldTypeElement = getFieldTypeElement(typeUtils, fieldElement);
        return fieldTypeElement.getQualifiedName().toString();
    }
}
