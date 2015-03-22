package io.gunmetal.spi;

import java.util.List;

/**
 * @author rees.byars
 */
public interface Converter {

    List<Class<?>> supportedFromTypes();

    Object convert(Object from);

}
