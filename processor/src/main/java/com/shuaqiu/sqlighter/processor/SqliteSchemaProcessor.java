package com.shuaqiu.sqlighter.processor;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
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

import com.google.auto.service.AutoService;
import com.shuaqiu.sqlighter.SqliteColumn;
import com.shuaqiu.sqlighter.SqliteId;
import com.shuaqiu.sqlighter.SqliteSchemaBuilder;
import com.shuaqiu.sqlighter.SqliteTable;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

/**
 * SQLite Scheme 生成
 */
@AutoService(Processor.class)
public class SqliteSchemaProcessor extends AbstractProcessor {

    private static final String SUFFIX = "SchemaBuilder";

    private Types typeUtils;
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
        types.add(SqliteId.class.getCanonicalName());
        types.add(SqliteColumn.class.getCanonicalName());
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
     * 生成Schema Builder 类
     *
     * @param classElement 当前的{@link SqliteTable } 标记的Element, 生成的构造类将位于同一个包下面
     * @throws IOException 写文件出现异常
     */
    private void generateCode(final TypeElement classElement) throws IOException {
        // Schema Builder 的名称, 类名
        final String schemaBuilderName = classElement.getSimpleName() + SUFFIX;

        // 生成方法定义
        final MethodSpec createMethod = buildMethodSpec(classElement);

        // 生成类定义
        final TypeSpec factoryClass = buildTypeSpec(schemaBuilderName, createMethod);

        // 生成Java 文件
        final JavaFile javaFile = buildJavaFile(elementUtils, classElement, factoryClass);
        javaFile.writeTo(filer);
    }

    /**
     * Schema 的build 方法定义
     *
     * @param classElement 当前的{@link SqliteTable } 标记的Element
     * @return Schema 的build 方法定义
     */
    private MethodSpec buildMethodSpec(final TypeElement classElement) {
        final MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("build");
        methodBuilder.addModifiers(Modifier.PUBLIC);
        methodBuilder.addAnnotation(Override.class);
        methodBuilder.returns(String.class);

        final String schema = SqliteSchemaGenerator.buildSchema(typeUtils, classElement);
        methodBuilder.addStatement("return $S", schema);

        return methodBuilder.build();
    }

    /**
     * Schema Builder 对应的类定义
     *
     * @param schemaBuilderName Schema Builder 的名称, 类名
     * @param methodSpec        Schema 的build 方法定义
     * @return Schema Builder 对应的类定义
     */
    private TypeSpec buildTypeSpec(final String schemaBuilderName, final MethodSpec methodSpec) {
        final TypeSpec.Builder classBuilder = TypeSpec.classBuilder(schemaBuilderName);
        classBuilder.addModifiers(Modifier.PUBLIC, Modifier.FINAL);
        classBuilder.addMethod(methodSpec);
        classBuilder.addSuperinterface(SqliteSchemaBuilder.class);
        return classBuilder.build();
    }

    /**
     * 生成Java 文件
     *
     * @param elementUtils Element 的Utility 类
     * @param classElement 当前的{@link SqliteTable } 标记的Element, 生成的构造类将位于同一个包下面
     * @param typeSpec     Schema Builder 对应的类定义
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
