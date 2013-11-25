package io.gunmetal.internal;

/**
 * @author rees.byars
 */
class CircularReferenceException extends RuntimeException {

    private ComponentMetadata metadata;
    private ProvisionStrategy<?> reverseStrategy;

    protected CircularReferenceException(String message) {
        super(message);
    }

    protected CircularReferenceException(ComponentMetadata metadata) {
        this.metadata = metadata;
    }

    public ComponentMetadata metadata() {
        return metadata;
    }

    public void setReverseStrategy(ProvisionStrategy<?> reverseStrategy) {
        this.reverseStrategy = reverseStrategy;
    }

    public ProvisionStrategy<?> getReverseStrategy() {
        return reverseStrategy;
    }

    @Override
    public String getMessage() {
        if (reverseStrategy != null) {
            return super.getMessage() + " of with metadata [" + metadata() + "]";
        }
        return super.getMessage();
    }

}
