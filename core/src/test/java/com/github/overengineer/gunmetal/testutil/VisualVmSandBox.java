package com.github.overengineer.gunmetal.testutil;

import com.github.overengineer.gunmetal.Container;
import com.github.overengineer.gunmetal.Gunmetal;
import com.github.overengineer.gunmetal.key.Generic;
import com.github.overengineer.gunmetal.module.BaseModule;
import com.github.overengineer.gunmetal.testmocks.A;
import com.google.inject.Guice;
import com.google.inject.Injector;

import javax.inject.Provider;
import java.util.Scanner;

/**
 * @author rees.byars
 */
public class VisualVmSandBox {

    public static void main(String ... args) {

        Container gunmetal = Gunmetal.jsr330().load(new BaseModule() {
            @Override public void configure() { }
        });

        Injector injector = Guice.createInjector();

        Provider<A> guiceProvider = injector.getProvider(A.class);
        Provider<A> gunmetalProvider = gunmetal.get(new Generic<Provider<A>>() {
        });

        Scanner scanner = new Scanner(System.in);

        String command;

        int hashSum = 0;

        boolean running = true;

        while (running) {

            command = scanner.nextLine();

            if ("0".equals(command)) {
                running = false;
            } else if ("1".equals(command)) {
                for (int i = 0; i < 1000000; i++) {
                    hashSum += guiceProvider.get().hashCode();
                }
            } else {
                for (int i = 0; i < 1000000; i++) {
                    hashSum += gunmetalProvider.get().hashCode();
                }
            }

        }

        System.out.println(hashSum);

    }

}
