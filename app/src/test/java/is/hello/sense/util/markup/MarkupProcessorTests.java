package is.hello.sense.util.markup;

import android.graphics.Typeface;

import org.junit.Test;

import is.hello.sense.graph.SenseTestCase;
import is.hello.sense.util.markup.text.MarkupString;
import is.hello.sense.util.markup.text.MarkupStyleSpan;
import is.hello.sense.util.markup.text.MarkupURLSpan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class MarkupProcessorTests extends SenseTestCase {
    private final MarkupProcessor processor = new MarkupProcessor();

    @Test
    public void boldEmphasis() throws Exception {
        MarkupString string = processor.render("**This** is a string with __bold__ in it");
        assertEquals("This is a string with bold in it", string.toString());

        MarkupStyleSpan[] spans = string.getSpans(0, string.length(), MarkupStyleSpan.class);
        assertNotNull(spans);
        assertEquals(2, spans.length);

        assertEquals(Typeface.BOLD, spans[0].getStyle());
        assertEquals(0, string.getSpanStart(spans[0]));
        assertEquals(4, string.getSpanEnd(spans[0]));

        assertEquals(Typeface.BOLD, spans[1].getStyle());
        assertEquals(22, string.getSpanStart(spans[1]));
        assertEquals(26, string.getSpanEnd(spans[1]));
    }

    @Test
    public void italicEmphasis() throws Exception {
        MarkupString string = processor.render("*This* is a string with _italic_ in it");
        assertEquals("This is a string with italic in it", string.toString());

        MarkupStyleSpan[] spans = string.getSpans(0, string.length(), MarkupStyleSpan.class);
        assertNotNull(spans);
        assertEquals(2, spans.length);

        assertEquals(Typeface.ITALIC, spans[0].getStyle());
        assertEquals(0, string.getSpanStart(spans[0]));
        assertEquals(4, string.getSpanEnd(spans[0]));

        assertEquals(Typeface.ITALIC, spans[1].getStyle());
        assertEquals(22, string.getSpanStart(spans[1]));
        assertEquals(28, string.getSpanEnd(spans[1]));
    }

    @Test
    public void boldItalicEmphasis() throws Exception {
        MarkupString string = processor.render("This is *__very__* important");
        assertEquals("This is very important", string.toString());

        MarkupStyleSpan[] spans = string.getSpans(0, string.length(), MarkupStyleSpan.class);
        assertNotNull(spans);
        assertEquals(2, spans.length);

        assertEquals(Typeface.BOLD, spans[0].getStyle());
        assertEquals(8, string.getSpanStart(spans[0]));
        assertEquals(12, string.getSpanEnd(spans[0]));

        assertEquals(Typeface.ITALIC, spans[1].getStyle());
        assertEquals(8, string.getSpanStart(spans[1]));
        assertEquals(12, string.getSpanEnd(spans[1]));
    }

    @Test
    public void unmatchedEmphasisMarkers() throws Exception {
        String contents = "The method to_a is re-entrant*\n\n" +
                "*Except when it isn't.";
        MarkupString string = processor.render(contents);
        assertEquals(contents, string.toString());

        MarkupStyleSpan[] spans = string.getSpans(0, string.length(), MarkupStyleSpan.class);
        assertNotNull(spans);
        assertEquals(0, spans.length);
    }

    @Test
    public void escaping() throws Exception {
        MarkupString simple = processor.render("\\\\\\`\\*\\_\\{\\}\\[\\]\\(\\)\\#\\+\\-\\.\\!");
        assertEquals("\\`*_{}[]()#+-.!", simple.toString());

        MarkupString literalList = processor.render("\\- Item one\n" +
                "\\- Item two\n");
        assertEquals("- Item one\n" +
                "- Item two", literalList.toString());

        MarkupString literalEmphasis = processor.render("\\*Stars\\* \\_Underscores\\_");
        assertEquals("*Stars* _Underscores_", literalEmphasis.toString());
    }

    @Test
    public void links() throws Exception {
        MarkupString string = processor.render("[Call now](tel:555-867-5309) to order your brand new [Sense](http://hello.is \"not an order page\")!");
        assertEquals("Call now to order your brand new Sense!", string.toString());

        MarkupURLSpan[] spans = string.getSpans(0, string.length(), MarkupURLSpan.class);
        assertNotNull(spans);
        assertEquals(2, spans.length);

        assertEquals("tel:555-867-5309", spans[0].getUrl());
        assertNull(spans[0].getTitle());

        assertEquals("http://hello.is", spans[1].getUrl());
        assertEquals("not an order page", spans[1].getTitle());
    }

    @Test
    public void unorderedList() throws Exception {
        MarkupString noIndentationDash = processor.render("- Item one\n" +
                "- Item Two\n" +
                "- Item Three\n");
        assertEquals("• Item one\n" +
                "• Item Two\n" +
                "• Item Three", noIndentationDash.toString());

        MarkupString noIndentationStar = processor.render("* Item one\n" +
                "* Item Two\n" +
                "* Item Three\n");
        assertEquals("• Item one\n" +
                "• Item Two\n" +
                "• Item Three", noIndentationStar.toString());

        MarkupString noIndentationPlus = processor.render("+ Item one\n" +
                "+ Item Two\n" +
                "+ Item Three\n");
        assertEquals("• Item one\n" +
                "• Item Two\n" +
                "• Item Three", noIndentationPlus.toString());


        MarkupString indentationStar = processor.render("* Item one\n" +
                "\t* Nested Item One\n" +
                "* Item Two\n");
        assertEquals("• Item one\n" +
                "\t• Nested Item One\n" +
                "• Item Two", indentationStar.toString());
    }

    @Test
    public void orderedList() throws Exception {
        MarkupString ordered = processor.render("1. Item one\n" +
                "2. Item two\n" +
                "3. Item three\n");
        assertEquals("• Item one\n" +
                "• Item two\n" +
                "• Item three", ordered.toString());

        MarkupString unordered = processor.render("8. Item one\n" +
                "1. Item two\n" +
                "84. Item three\n");
        assertEquals("• Item one\n" +
                "• Item two\n" +
                "• Item three", unordered.toString());
    }
}
