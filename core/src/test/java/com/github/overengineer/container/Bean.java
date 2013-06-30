package com.github.overengineer.container;


import com.github.overengineer.container.metadata.Prototype;
import com.google.inject.Inject;

@Prototype
public class Bean implements IBean {

    @Inject
    public Bean(IBean2 bean2){
        bean2.doStuff();
    }

    @Override
    public void stuff() {
    }
}
