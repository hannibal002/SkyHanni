package at.hannibal2.skyhanni.config.features.about;

public enum UpdateStream {
    NONE("None", "none"),
    BETA("Beta", "pre"),
    RELEASES("Full", "full");

    private final String label;
    private final String stream;

    UpdateStream(String label, String stream) {
        this.label = label;
        this.stream = stream;
    }

    public String getStream() {
        return stream;
    }

    @Override
    public String toString() {
        return label;
    }
}
