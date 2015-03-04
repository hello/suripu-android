package is.hello.sense.ui.handholding;

public enum Interaction {
    TAP(false),
    SLIDE_LEFT(false),
    SLIDE_RIGHT(false),
    SLIDE_UP(true),
    SLIDE_DOWN(true);

    public final boolean isVertical;

    private Interaction(boolean isVertical) {
        this.isVertical = isVertical;
    }
}
