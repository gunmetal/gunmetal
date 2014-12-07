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
@test.BasicModule.Main("module")
public class BasicModule {

    @Provides @Main("notModule") BasicModule() { }

    @Provides @Main("notModule") List<BasicModule>
    module(@Main("module") String name, @Main("module") Object object) {
        return Arrays.asList(this);
    }

    @Provides @Main("module") @Singleton static String name() {
        return "name";
    }

    @Provides @Main("module") static Object o(@Main("module") String name) {
        return name;
    }

    @Provides @Main("module") static Provider<? extends BasicModule> provider() {
        return null;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @io.gunmetal.Qualifier
    public @interface Main {
        String value();
    }

}
