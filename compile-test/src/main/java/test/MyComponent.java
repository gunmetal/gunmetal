package test;

import io.gunmetal.Component;

/**
 * @author rees.byars
 */
@Component
public interface MyComponent {

    @test.BasicModule2.Main("module") BasicModule2 get();

}
