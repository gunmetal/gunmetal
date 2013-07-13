package com.github.overengineer.gunmetal;

import com.github.overengineer.gunmetal.metadata.DeconstructedApi;
import com.github.overengineer.gunmetal.metadata.ImplementedBy;
import com.github.overengineer.gunmetal.proxy.aop.Aspect;
import com.github.overengineer.gunmetal.proxy.aop.JoinPoint;
import com.github.overengineer.gunmetal.proxy.aop.Pointcut;
import org.junit.Test;
import java.io.Serializable;

/**
 *
 */
public class SandboxTest implements Serializable {


    @DeconstructedApi
    public interface Sandbox {
        @ImplementedBy(SandboxImpl.class)
        void fill(String message);
    }

    public static final class SandboxImpl {

        public void fill(String message, Sandbox sandbox) {
            System.out.println(message + sandbox);
        }

    }

    @Pointcut(classes = Sandbox.class)
    public static class SandySpect implements Aspect  {
        @Override
        public Object advise(JoinPoint invocation) throws Throwable {
            return invocation.join();
        }
    }

    @Test
    public void testLoadModule() {

        Gunmetal.raw().gimmeThatAopTainer()
                .addAspect(SandySpect.class)
                .registerDeconstructedApi(Sandbox.class)
                .get(Sandbox.class).fill("blah blah blah");

    }

}
