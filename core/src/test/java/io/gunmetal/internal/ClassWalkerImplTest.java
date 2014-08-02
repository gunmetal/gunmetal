/*
 * Copyright (c) 2013.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gunmetal.internal;

import io.gunmetal.spi.ClassWalker;
import io.gunmetal.spi.ComponentErrors;
import io.gunmetal.spi.ComponentMetadata;
import io.gunmetal.spi.InjectionResolver;
import mockit.Expectations;
import mockit.Mocked;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * @author rees.byars
 */
public class ClassWalkerImplTest {

    String field1 = "field1";

    Field field(String name) {
        try {
            return getClass().getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            try {
                return Inner.class.getDeclaredField(name);
            } catch (NoSuchFieldException ee) {
                throw new NullPointerException();
            }
        }
    }

    static class Inner extends ClassWalkerImplTest {
        static String field2 = "field2";
    }

    @Test
    public void testWalk(@Mocked final InjectionResolver injectionResolver,
                         @Mocked final ClassWalker.InjectedMemberVisitor memberVisitor,
                         @Mocked final ComponentErrors errors,
                         @Mocked final ComponentMetadata<?> componentMetadata) {

        ClassWalker walker = new ClassWalkerImpl(injectionResolver, false, false);

        new Expectations() {
            {
                injectionResolver.shouldInject(field(field1)); result = true;
                memberVisitor.visit(field(field1));
                injectionResolver.shouldInject((Method) any); result = false; times = 2;
            }
        };

        walker.walk(getClass(), memberVisitor, memberVisitor, componentMetadata, errors);

        new Expectations() {
            {
                injectionResolver.shouldInject(field(Inner.field2)); result = true;
                memberVisitor.visit(field(Inner.field2));
                injectionResolver.shouldInject(field(field1)); result = true;
                memberVisitor.visit(field(field1));
                injectionResolver.shouldInject((Method) any); result = false; times = 2;
            }
        };

        walker.walk(Inner.class, memberVisitor, memberVisitor, componentMetadata, errors);

    }
}
