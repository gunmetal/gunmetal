package io.gunmetal.spi;

import java.util.List;

/**
 * @author rees.byars
 */
public interface ConverterProvider {

    List<Converter> convertersForType(TypeKey to);

}
