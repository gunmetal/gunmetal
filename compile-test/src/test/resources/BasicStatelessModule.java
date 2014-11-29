import io.gunmetal.Module;
import io.gunmetal.Provides;
import io.gunmetal.Singleton;

/**
 * @author rees.byars
 */
@Module(dependsOn = BasicStatefulModule.class)
public class BasicStatelessModule {

    @Provides static BasicStatelessModule module(String name, Object object) {
        return new BasicStatelessModule();
    }

    @Provides @Singleton static String name() {
        return "name";
    }

}
