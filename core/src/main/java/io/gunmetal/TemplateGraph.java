package io.gunmetal;

/**
 * @author rees.byars
 */
public interface TemplateGraph {

    ObjectGraph newInstance(Object... statefulModules);

}
