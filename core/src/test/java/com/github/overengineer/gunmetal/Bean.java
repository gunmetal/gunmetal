package com.github.overengineer.gunmetal;


import com.github.overengineer.gunmetal.metadata.Prototype;
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
