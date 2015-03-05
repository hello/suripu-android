package is.hello.sense.ui.handholding;

public enum Interaction {
    TAP(false),
    SWIPE_LEFT(false),
    SWIPE_RIGHT(false),
    SWIPE_UP(true),
    SWIPE_DOWN(true);

    public final boolean isVertical;

    private Interaction(boolean isVertical) {
        this.isVertical = isVertical;
    }
}
