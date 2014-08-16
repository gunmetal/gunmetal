package io.gunmetal.testmocks.dongle.bl;

import com.google.common.eventbus.Subscribe;

import java.util.List;

/**
 * @author rees.byars
 */
public class Dongler {

    public Dongler() { }

    public Dongler(String name, List<Dongler> donglers) { }

    @Subscribe
    public void observe(Dongler event) {
        System.out.println("dongler got event:  " + event);
    }

}
