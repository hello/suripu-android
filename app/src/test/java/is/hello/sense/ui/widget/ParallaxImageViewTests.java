package is.hello.sense.ui.widget;

import org.junit.Test;

import is.hello.sense.graph.SenseTestCase;

import static is.hello.sense.AssertExtensions.assertThrows;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ParallaxImageViewTests extends SenseTestCase {
    @Test(expected = NumberFormatException.class)
    public void parseAspectRatioEmpty() {
        ParallaxImageView.parseAspectRatio("");
    }

    @Test
    public void parseAspectRatioMalformed() {
        assertThrows(() -> ParallaxImageView.parseAspectRatio("x:y"));
        assertThrows(() -> ParallaxImageView.parseAspectRatio("1:y"));
        assertThrows(() -> ParallaxImageView.parseAspectRatio("x:2"));
    }

    @Test
    public void parseAspectRatioLiteral() {
        assertThat(ParallaxImageView.parseAspectRatio("0.2"), is(equalTo(0.2f)));
        assertThat(ParallaxImageView.parseAspectRatio("0.5"), is(equalTo(0.5f)));
        assertThat(ParallaxImageView.parseAspectRatio("1.0"), is(equalTo(1.0f)));
    }

    @Test
    public void parseAspectRatioRatio() {
        assertThat(ParallaxImageView.parseAspectRatio("1:1"), is(equalTo(1.0f)));
        assertThat(ParallaxImageView.parseAspectRatio("2:1"), is(equalTo(0.5f)));
        assertThat(ParallaxImageView.parseAspectRatio("4:3"), is(equalTo(0.75f)));
        assertThat(ParallaxImageView.parseAspectRatio("5:3"), is(equalTo(0.6f)));
        assertThat(ParallaxImageView.parseAspectRatio("16:9"), is(equalTo(0.5625f)));
        assertThat(ParallaxImageView.parseAspectRatio("16:10"), is(equalTo(0.625f)));
    }
}
