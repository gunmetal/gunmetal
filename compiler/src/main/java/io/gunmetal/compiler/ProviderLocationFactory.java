package io.gunmetal.compiler;

import javax.lang.model.element.Element;

/**
 * @author rees.byars
 */
public class ProviderLocationFactory implements Factory<ProviderLocation> {

    private final Factory<MemberMetadata> memberMetadataFactory;

    ProviderLocationFactory(
            Factory<MemberMetadata> memberMetadataFactory) {
        this.memberMetadataFactory = memberMetadataFactory;
    }

    @Override public ProviderLocation create(Element providerElement) {

        // TODO what about the location of the location, e.g. an inner class?

        Element classElement = providerElement.getEnclosingElement();

        return new ProviderLocation(
                memberMetadataFactory.create(classElement));

    }

}
