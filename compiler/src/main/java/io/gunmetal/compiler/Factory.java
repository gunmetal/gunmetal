package io.gunmetal.compiler;

import javax.lang.model.element.Element;

/**
 * @author rees.byars
 */
interface Factory<R> {

    R create(Element element);

}
