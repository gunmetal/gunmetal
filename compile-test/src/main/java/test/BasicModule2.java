package test;

import io.gunmetal.Provider;
import io.gunmetal.Provides;
import io.gunmetal.Singleton;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.List;

/**
 * @author rees.byars
 */
@test.BasicModule2.Main("module")
public class BasicModule2 {

    @Provides
    @Main("notModule")
    BasicModule2() { }

    @Provides
    @Main("notModule")
    List<BasicModule2> module(
            @Main("module") String name,
            @Main("module") Object object) {
        return Arrays.asList(this);
    }

    @Provides
    @Main("module")
    @Singleton
    static String name() {
        return "name";
    }

    @Provides
    @Main("module")
    static Object o(@Main("module") String name) {
        return name;
    }

    @Provides
    @Main("module")
    static Provider<? extends BasicModule2> provider() {
        return null;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @io.gunmetal.Qualifier
    public @interface Main {
        String value();
    }

}
