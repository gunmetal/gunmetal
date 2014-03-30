package io.gunmetal.benchmarks;

import io.gunmetal.testmocks.*;
import dagger.Module;

@Module(library = true, injects = { AA.class, R.class, V.class })
public class DaggerSlimBenchMarkModule {

}
