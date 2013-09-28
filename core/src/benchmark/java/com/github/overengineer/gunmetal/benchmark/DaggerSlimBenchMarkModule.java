package com.github.overengineer.gunmetal.benchmark;

import com.github.overengineer.gunmetal.testmocks.*;
import dagger.Module;

@Module(library = true, injects = { AA.class, R.class, V.class })
public class DaggerSlimBenchMarkModule {

}
