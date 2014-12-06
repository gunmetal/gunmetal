package test;

import io.gunmetal.Module;
import io.gunmetal.Provides;
import io.gunmetal.Singleton;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author rees.byars
 */
@Module
@test.BasicModule.Main("module")
public class BasicModule {

    @Provides BasicModule() { }

    @Provides @Main("notModule") test.BasicModule module(@Main("module") String name, @Main("module") Object object) {
        return this;
    }

    @Provides @Main("module") @Singleton static String name() {
        return "name";
    }

    @Provides @Main("module") static Object o(@Main("module") String name) {
        return name;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @io.gunmetal.Qualifier
    public @interface Main {
        String value();
    }

}
