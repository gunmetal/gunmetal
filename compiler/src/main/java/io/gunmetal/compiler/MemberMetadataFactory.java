package io.gunmetal.compiler;

import io.gunmetal.Provider;

import javax.lang.model.element.Element;

/**
 * @author rees.byars
 */
class MemberMetadataFactory implements Factory<MemberMetadata> {

    private final Provider<Builder<Qualifier>> qualifierBuilderProvider;
    private final Provider<Builder<Scope>> scopeBuilderProvider;

    MemberMetadataFactory(
            Provider<Builder<Qualifier>> qualifierBuilderProvider,
            Provider<Builder<Scope>> scopeBuilderProvider) {
        this.qualifierBuilderProvider = qualifierBuilderProvider;
        this.scopeBuilderProvider = scopeBuilderProvider;
    }

    @Override public MemberMetadata create(Element element) {

        Builder<Qualifier> qualifierBuilder = qualifierBuilderProvider.get();
        Builder<Scope> scopeBuilder = scopeBuilderProvider.get();

        // TODO Overrides, MultiBind, Lazy?  would require some refactor since those don't apply to location

        new AnnotatedElement(element).accept(qualifierBuilder, scopeBuilder);

        return new MemberMetadata(qualifierBuilder.build(), scopeBuilder.build(), element);

    }

}
