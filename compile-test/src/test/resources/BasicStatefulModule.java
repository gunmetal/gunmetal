import io.gunmetal.Provides;

/**
 * @author rees.byars
 */
public class BasicStatefulModule {

    @Provides Object object() {
        return this;
    }

}
