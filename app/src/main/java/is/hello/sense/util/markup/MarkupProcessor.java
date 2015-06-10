/*
 *  Some patterns lifted from the MarkdownJ library.
 *
 *  Copyright (c) 2005, Martian Software
 *  Authors: Pete Bevin, John Mutchek
 *  http://www.martiansoftware.com/markdownj
 *  All rights reserved.
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are
 *  met:
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *  - Neither the name "Markdown" nor the names of its contributors may
 *    be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *  This software is provided by the copyright holders and contributors "as
 *  is" and any express or implied warranties, including, but not limited
 *  to, the implied warranties of merchantability and fitness for a
 *  particular purpose are disclaimed. In no event shall the copyright owner
 *  or contributors be liable for any direct, indirect, incidental, special,
 *  exemplary, or consequential damages (including, but not limited to,
 *  procurement of substitute goods or services; loss of use, data, or
 *  profits; or business interruption) however caused and on any theory of
 *  liability, whether in contract, strict liability, or tort (including
 *  negligence or otherwise) arising in any way out of the use of this
 *  software, even if advised of the possibility of such damage.
*/

package is.hello.sense.util.markup;

import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Spanned;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import is.hello.sense.util.markup.text.MarkupSpan;
import is.hello.sense.util.markup.text.MarkupString;
import is.hello.sense.util.markup.text.MarkupStyleSpan;
import is.hello.sense.util.markup.text.MarkupURLSpan;
import rx.functions.Action1;

/**
 * Renders a small subset of the Markdown language. Tries to follow the spirit of the
 * <a href="http://daringfireball.net/projects/markdown/">Daring Fireball spec</a>.
 *
 * <h2>Currently supported features:</h2>
 * <ol>
 *     <li>Bold and italic emphasis syntax</li>
 *     <li>Links with and without titles</li>
 *     <li>All three variations of unordered lists</li>
 *     <li>Numbered ordered lists</li>
 *     <li>Escape sequences</li>
 * </ol>
 *
 * <h2>Intentionally unsupported features:</h2>
 * <ol>
 *     <li>Inline HTML mark-up</li>
 *     <li>Images</li>
 * </ol>
 */
public final class MarkupProcessor {
    /**
     * The default list deliminator to use during rendering.
     */
    public static final String DEFAULT_LIST_DELIMINATOR = " • ";

    //region Patterns

    /**
     * All possible escape sequences in markdown.
     * <p />
     * backslash immediately proceeded by <code>\`*_{}[]()#+-.!</code>.
     */
    private final Pattern PATTERN_ESCAPE_SANITIZE = Pattern.compile("(\\\\([\\\\`*_{}\\[\\]()#+\\-.!]))");

    /**
     * Captures XML-style escape sequences. The processor converts
     * markdown escapes into XML-style escapes to prevent false matches.
     * <p />
     * <code>&amp;&#20;</code>
     */
    private final Pattern PATTERN_ESCAPE_RENDER = Pattern.compile("(&#(\\d+);)");


    /**
     * Captures bold emphasis markers.
     * <p />
     * <code>**Like this** and also __like this__</code>
     */
    private final Pattern PATTERN_BOLD = Pattern.compile("(\\*\\*|__)(?=\\S)(.+?[*_]*)(?<=\\S)\\1");

    /**
     * Captures italic emphasis markers.
     * <p />
     * <code>*Like this* and also _like this_</code>
     */
    private final Pattern PATTERN_ITALIC = Pattern.compile("(\\*|_)(?=\\S)(.+?)(?<=\\S)\\1");


    /**
     * Captures &lt;a&gt;-style links.
     * <p />
     * <code>[Click here](http://example.com) and [Image](http://example.com/img.jpg "An image")</code>
     */
    private final Pattern PATTERN_LINK = Pattern.compile("(" +
            "\\[(.+?)\\]" + // title – group 2
            "\\(" +
            "([^)\\s]+?)" + // href – group 3
            "([\\s]+\"([^\"]+)\")?" + // title – group 5
            "\\)" +
            ")");


    /**
     * Captures unordered lists and their indentation level.
     * <p />
     * <pre><code>
     *     - This is a list item
     *     - Also this
     *         + This is a nested list item
     *         + So is this.
     *     - No longer nested
     * </code></pre>
     */
    private final Pattern PATTERN_UNORDERED_LIST = Pattern.compile("(" +
                    "^([ \\t]+)?" + // list item indentation – group 2
                    "[-*+]" +
                    "([ \\t]+)" +
                    "(.+)$" + // list item content – group 4
                    ")",
            Pattern.MULTILINE);

    /**
     * Captures ordered lists and their indentation level.
     * <p />
     * <pre><code>
     *     1. This is a list item
     *     2. Also this
     *         1. This is a list item
     *         2. And this
     *         8. This is also fine
     *     3. And so forth
     * </code></pre>
     */
    private final Pattern PATTERN_ORDERED_LIST = Pattern.compile("(" +
                    "^([ \\t]+)?" + // list item indentation – group 2
                    "\\d+\\." +
                    "([ \\t]+)" +
                    "(.+)$" + // list item content – group 4
                    ")",
            Pattern.MULTILINE);

    //endregion


    //region Creation

    /**
     * The deliminator to use when rendering list items,
     * prepended to each list item preserving any indentation
     * from the source markup.
     */
    private final String listDeliminator;

    public MarkupProcessor(@NonNull String listDeliminator) {
        this.listDeliminator = listDeliminator;
    }

    public MarkupProcessor() {
        this(DEFAULT_LIST_DELIMINATOR);
    }

    //endregion


    //region Public Interface

    /**
     * Render a string containing the supported subset of
     * markdown into a new {@link MarkupString} instance.
     * <p />
     * This method may safely be called from multiple threads.
     */
    public @NonNull MarkupString render(@NonNull String source) {
        RenderState state = new RenderState(source);

        // Order matters here
        doEscapeSanitize(state);
        doBold(state);
        doItalic(state);
        doLinks(state);
        doUnorderedLists(state);
        doOrderedLists(state);
        doEscapeRender(state);

        return state.build();
    }

    //endregion


    //region Escaping

    private void doEscapeSanitize(@NonNull RenderState state) {
        doPattern(PATTERN_ESCAPE_SANITIZE, state, matcher -> {
            String character = matcher.group(2);
            state.replace(matcher.start(), matcher.end(),
                    "&#" + Integer.toString(character.charAt(0), 10) + ";");
        });
    }

    private void doEscapeRender(@NonNull RenderState state) {
        doPattern(PATTERN_ESCAPE_RENDER, state, matcher -> {
            char character = (char) Integer.valueOf(matcher.group(2), 10).intValue();
            state.replace(matcher.start(), matcher.end(),
                    Character.toString(character));
        });
    }

    //endregion


    //region Rendering

    private void doPattern(@NonNull Pattern pattern,
                           @NonNull RenderState state,
                           @NonNull Action1<Matcher> visitor) {
        state.prepareForPass();

        Matcher matcher = pattern.matcher(state.getStorage());
        while (matcher.find()) {
            visitor.call(matcher);
        }
    }

    private void doBold(@NonNull RenderState state) {
        doPattern(PATTERN_BOLD, state, matcher -> {
            state.replaceWithStyle(matcher.start(), matcher.end(),
                    matcher.group(2),
                    new MarkupStyleSpan(Typeface.BOLD));
        });
    }

    private void doItalic(@NonNull RenderState state) {
        doPattern(PATTERN_ITALIC, state, matcher -> {
            state.replaceWithStyle(matcher.start(), matcher.end(),
                    matcher.group(2),
                    new MarkupStyleSpan(Typeface.ITALIC));
        });
    }

    private void doLinks(@NonNull RenderState state) {
        doPattern(PATTERN_LINK, state, matcher -> {
            String linkText = matcher.group(2);
            String linkUrl = matcher.group(3);
            String linkTitle = matcher.group(5);
            state.replaceWithStyle(matcher.start(), matcher.end(),
                    linkText,
                    new MarkupURLSpan(linkUrl, linkTitle));
        });
    }

    private String renderListItem(@Nullable String indentation, @NonNull String state) {
        if (indentation != null) {
            return indentation + listDeliminator + state;
        } else {
            return listDeliminator + state;
        }
    }

    private void doUnorderedLists(@NonNull RenderState state) {
        doPattern(PATTERN_UNORDERED_LIST, state, matcher -> {
            String itemIndentation = matcher.group(2);
            String itemContents = matcher.group(4);
            state.replace(matcher.start(), matcher.end(),
                    renderListItem(itemIndentation, itemContents));
        });
    }

    private void doOrderedLists(@NonNull RenderState state) {
        doPattern(PATTERN_ORDERED_LIST, state, matcher -> {
            String itemIndentation = matcher.group(2);
            String itemContents = matcher.group(4);
            state.replace(matcher.start(), matcher.end(),
                    renderListItem(itemIndentation, itemContents));
        });
    }

    //endregion


    private final class RenderState {
        private final MarkupString.Builder storage;
        private int offset = 0;

        private RenderState(@NonNull String source) {
            this.storage = new MarkupString.Builder(source);
        }

        private void prepareForPass() {
            this.offset = 0;
        }

        private CharSequence getStorage() {
            return storage;
        }

        private MarkupString build() {
            return storage.build();
        }

        private void replaceWithStyle(int start, int end,
                                      @NonNull String replacement,
                                      @NonNull MarkupSpan style) {
            int adjustedStart = start - offset;
            int adjustedEnd = end - offset;

            storage.setSpan(style, adjustedStart, adjustedEnd, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            storage.replace(adjustedStart, adjustedEnd, replacement);

            int offsetDelta = (end - start) - replacement.length();
            offset += offsetDelta;
        }

        private void replace(int start, int end,
                             @NonNull String replacement) {
            int adjustedStart = start - offset;
            int adjustedEnd = end - offset;

            storage.replace(adjustedStart, adjustedEnd, replacement);

            int offsetDelta = (end - start) - replacement.length();
            offset += offsetDelta;
        }
    }
}
