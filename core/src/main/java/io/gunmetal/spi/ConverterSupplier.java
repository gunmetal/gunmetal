package io.gunmetal.spi;

import java.util.List;

/**
 * @author rees.byars
 */
public interface ConverterSupplier {

    List<Converter> convertersForType(TypeKey to);

}
