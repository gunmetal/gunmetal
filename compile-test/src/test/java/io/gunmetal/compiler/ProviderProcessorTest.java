package io.gunmetal.compiler;

import com.google.common.io.Resources;
import com.google.testing.compile.JavaFileObjects;
import com.google.testing.compile.JavaSourceSubjectFactory;
import org.junit.Test;
import org.truth0.Truth;

/**
 * @author rees.byars
 */
public class ProviderProcessorTest {

    @Test public void testSandbox() {

        Truth.ASSERT.about(JavaSourceSubjectFactory.javaSource())
                .that(JavaFileObjects.forResource(Resources.getResource("test/BasicModule.java")))
                .processedWith(new ProviderProcessor())
                .compilesWithoutError();

    }

}
