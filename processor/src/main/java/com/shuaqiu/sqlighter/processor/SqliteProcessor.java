package com.shuaqiu.sqlighter.processor;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import com.shuaqiu.sqlighter.SqliteTable;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

/**
 * SQLite 相关代码生成的基类
 * Created by shuaqiu on 2015-10-10.
 */
public abstract class SqliteProcessor extends AbstractProcessor {

    protected Types typeUtils;
    private Elements elementUtils;
    private Filer filer;
    private Messager messager;

    @Override
    public synchronized void init(final ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        typeUtils = processingEnv.getTypeUtils();
        elementUtils = processingEnv.getElementUtils();
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        final Set<String> types = new HashSet<>(3);
        types.add(SqliteTable.class.getCanonicalName());
        return types;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
        try {
            // Scan classes
            for (final Element annotatedElement : roundEnv.getElementsAnnotatedWith(SqliteTable.class)) {
                // 检查由SqliteTable 标记的对象是否是符合要求的类
                final TypeElement typeElement = checkValidElement(annotatedElement);

                // 如果OK, 则生成对应的代码
                generateCode(typeElement);
            }
            return false;
        } catch (ProcessingException e) {
            error(e.getElement(), e.getMessage());
        } catch (IOException e) {
            error(null, e.getMessage());
        }

        return true;
    }

    /**
     * Checks if the annotated element observes our rules
     */
    private TypeElement checkValidElement(final Element annotatedElement) throws ProcessingException {

        final String annotationName = SqliteTable.class.getSimpleName();

        // 检查由@SqliteTable 标记的是否是一个类
        if (annotatedElement.getKind() != ElementKind.CLASS) {
            final String msg = "Only classes can be annotated with @%s";
            throw new ProcessingException(annotatedElement, msg, annotationName);
        }

        // We can cast it, because we know that it of ElementKind.CLASS
        final TypeElement classElement = (TypeElement) annotatedElement;
        final String classElementName = classElement.getQualifiedName().toString();

        // 检查这个类是不是public 的
        if (!classElement.getModifiers().contains(Modifier.PUBLIC)) {
            final String msg = "The class %s is not public.";
            throw new ProcessingException(classElement, msg, classElementName);
        }

        // 检查这个类是否是抽象类
        if (classElement.getModifiers().contains(Modifier.ABSTRACT)) {
            final String msg = "The class %s is abstract. You can't annotate abstract classes with @%s";
            throw new ProcessingException(classElement, msg, classElementName, annotationName);
        }

        return classElement;
    }

    /**
     * 生成类
     *
     * @param classElement 当前的{@link SqliteTable } 标记的Element, 生成的构造类将位于同一个包下面
     * @throws IOException 写文件出现异常
     */
    private void generateCode(final TypeElement classElement) throws IOException {
        // Schema Builder 的名称, 类名
        final String className = classElement.getSimpleName() + getSuffix();

        // 生成方法定义
        final MethodSpec[] methodSpecs = buildMethodSpecs(classElement);

        // 生成类定义
        final TypeSpec factoryClass = buildTypeSpec(className, methodSpecs);

        // 生成Java 文件
        final JavaFile javaFile = buildJavaFile(elementUtils, classElement, factoryClass);
        javaFile.writeTo(filer);
    }

    /**
     * 获取用于类名的后缀
     *
     * @return 类名的后缀
     */
    protected abstract String getSuffix();

    /**
     * 生成方法定义
     *
     * @param classElement 当前的{@link SqliteTable } 标记的Element
     * @return 方法定义
     */
    protected abstract MethodSpec[] buildMethodSpecs(final TypeElement classElement);

    /**
     * 生成类定义
     *
     * @param className   类名
     * @param methodSpecs 方法定义
     * @return 类定义
     */
    private TypeSpec buildTypeSpec(final String className, final MethodSpec... methodSpecs) {
        final TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className);
        classBuilder.addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        for (final MethodSpec methodSpec : methodSpecs) {
            classBuilder.addMethod(methodSpec);
        }

        final Class<?> superInterface = getSuperInterface();
        if (superInterface != null) {
            classBuilder.addSuperinterface(superInterface);
        }
        return classBuilder.build();
    }

    protected abstract Class<?> getSuperInterface();

    /**
     * 生成Java 文件
     *
     * @param elementUtils Element 的Utility 类
     * @param classElement 当前的{@link SqliteTable } 标记的Element, 生成的构造类将位于同一个包下面
     * @param typeSpec     类定义
     * @return Java 文件
     */
    private JavaFile buildJavaFile(final Elements elementUtils, final TypeElement classElement, final TypeSpec typeSpec) {
        // 包Element
        final PackageElement packageElement = elementUtils.getPackageOf(classElement);
        // 包名
        final String packageName = packageElement.getQualifiedName().toString();
        // 生成Java 文件
        final JavaFile.Builder javaFileBuilder = JavaFile.builder(packageName, typeSpec);
        return javaFileBuilder.build();
    }

    /**
     * 打印错误信息
     *
     * @param element 出现错误的element, 可以为null
     * @param msg     错误信息
     */
    public void error(final Element element, final String msg) {
        messager.printMessage(Diagnostic.Kind.ERROR, msg, element);
    }
}
