package com.github.overengineer.gunmetal.compiler;

import com.google.common.io.Resources;
import com.google.testing.compile.JavaFileObjects;
import com.google.testing.compile.JavaSourceSubjectFactory;
import org.junit.Test;
import org.truth0.Truth;

/**
 * @author rees.byars
 */
public class DeconstructedApiProcessorTest {

    @Test public void testRequiresInterface() {

        Truth.ASSERT.about(JavaSourceSubjectFactory.javaSource())
                .that(JavaFileObjects.forResource(Resources.getResource("DeconstructedApiOnConcrete.java")))
                .processedWith(new DeconstructedApiProcessor())
                .failsToCompile()
                .withErrorContaining("not an interface");

    }

}
