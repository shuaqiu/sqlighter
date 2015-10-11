package com.shuaqiu.sqlighter.processor;

import com.google.common.truth.Truth;
import com.google.testing.compile.JavaFileObjects;
import org.junit.Test;

import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;

/**
 * Test
 * Created by shuaqiu on 2015-10-11.
 */
public class ProcessorTest {

    @Test
    public void testSchema() {
        Truth.ASSERT.about(javaSource())
                .that(JavaFileObjects.forResource("test/SimpleBean.java"))
                .processedWith(new SqliteSchemaProcessor())
                .compilesWithoutError();
    }

    @Test
    public void testHelper() {
        Truth.ASSERT.about(javaSource())
                .that(JavaFileObjects.forResource("test/SimpleBean.java"))
                .processedWith(new SqliteHelperProcessor())
                .failsToCompile()
                .withErrorContaining("package android.content does not exist");
    }
}
