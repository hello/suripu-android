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
import android.util.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import is.hello.sense.util.markup.text.MarkupSpan;
import is.hello.sense.util.markup.text.MarkupString;
import is.hello.sense.util.markup.text.MarkupStyleSpan;
import is.hello.sense.util.markup.text.MarkupURLSpan;
import rx.functions.Action1;

public class MarkupProcessor {
    public static final String DEFAULT_LIST_DELIMINATOR = " • ";

    //region Patterns

    private final Pattern PATTERN_BOLD = Pattern.compile("(\\*\\*|__)(?=\\S)(.+?[*_]*)(?<=\\S)\\1");
    private final Pattern PATTERN_ITALIC = Pattern.compile("(\\*|_)(?=\\S)(.+?)(?<=\\S)\\1");

    private final Pattern PATTERN_LINK = Pattern.compile("(" +
            "\\[(.+?)\\]" + // title – group 2
            "\\(" +
            "([^)\\s]+?)" + // href – group 3
            "([\\s]+\"([^\"]+)\")?" + // title – group 5
            "\\)" +
            ")");

    private final Pattern PATTERN_UNORDERED_LIST = Pattern.compile("(" +
            "^([ \\t]+)?" + // list item indentation – group 2
            "[-*+]" +
            "([ \\t]+)" +
            "(.+)$" + // list item content – group 4
            ")",
            Pattern.MULTILINE);
    private final Pattern PATTERN_ORDERED_LIST = Pattern.compile("(" +
            "^([ \\t]+)?" + // list item indentation – group 2
            "\\d+\\." +
            "([ \\t]+)" +
            "(.+)$" + // list item content – group 4
            ")",
            Pattern.MULTILINE);

    //endregion


    //region Creation

    private final String listDeliminator;

    public MarkupProcessor(String listDeliminator) {
        this.listDeliminator = listDeliminator;
    }

    public MarkupProcessor() {
        this(DEFAULT_LIST_DELIMINATOR);
    }

    //endregion


    //region Public Interface

    public @NonNull MarkupString render(@NonNull String source) {
        Builder string = new Builder(source);

        doBold(string);
        doItalic(string);
        doLinks(string);
        doUnorderedLists(string);
        doOrderedLists(string);

        return string.build();
    }

    //endregion


    //region Rendering

    protected void doPattern(@NonNull Pattern pattern,
                             @NonNull Builder string,
                             @NonNull Action1<Matcher> visitor) {
        string.prepare();

        Matcher matcher = pattern.matcher(string.getStorage());
        while (matcher.find()) {
            visitor.call(matcher);
        }
    }

    protected void debugPrint(@NonNull Builder string) {
        Log.d(getClass().getSimpleName(), string.toString());
    }


    private void doBold(@NonNull Builder string) {
        doPattern(PATTERN_BOLD, string, matcher -> {
            string.replaceWithStyle(matcher.start(), matcher.end(),
                    matcher.group(2),
                    new MarkupStyleSpan(Typeface.BOLD));
        });
    }

    private void doItalic(@NonNull Builder string) {
        doPattern(PATTERN_ITALIC, string, matcher -> {
            string.replaceWithStyle(matcher.start(), matcher.end(),
                    matcher.group(2),
                    new MarkupStyleSpan(Typeface.ITALIC));
        });
    }

    protected void doLinks(@NonNull Builder string) {
        doPattern(PATTERN_LINK, string, matcher -> {
            String linkText = matcher.group(2);
            String linkUrl = matcher.group(3);
            String linkTitle = matcher.group(5);
            string.replaceWithStyle(matcher.start(), matcher.end(),
                    linkText,
                    new MarkupURLSpan(linkUrl, linkTitle));
        });
    }

    protected String renderListItem(@Nullable String indentation, @NonNull String contents) {
        if (indentation != null) {
            return indentation + listDeliminator + contents;
        } else {
            return listDeliminator + contents;
        }
    }

    protected void doUnorderedLists(@NonNull Builder string) {
        doPattern(PATTERN_UNORDERED_LIST, string, matcher -> {
            String itemIndentation = matcher.group(2);
            String itemContents = matcher.group(4);
            string.replace(matcher.start(), matcher.end(),
                    renderListItem(itemIndentation, itemContents));
        });
    }

    protected void doOrderedLists(@NonNull Builder string) {
        doPattern(PATTERN_ORDERED_LIST, string, matcher -> {
            String itemIndentation = matcher.group(2);
            String itemContents = matcher.group(4);
            string.replace(matcher.start(), matcher.end(),
                    renderListItem(itemIndentation, itemContents));
        });
    }

    //endregion


    protected final class Builder {
        private final MarkupString.Builder storage;
        private int offset = 0;

        public Builder(@NonNull String source) {
            this.storage = new MarkupString.Builder(source);
        }

        private void prepare() {
            this.offset = 0;
        }

        public CharSequence getStorage() {
            return storage;
        }

        public MarkupString build() {
            return storage.build();
        }

        @Override
        public String toString() {
            return storage.toString();
        }

        public void replaceWithStyle(int start, int end,
                                     @NonNull String replacement,
                                     @NonNull MarkupSpan style) {
            int adjustedStart = start - offset;
            int adjustedEnd = end - offset;

            storage.setSpan(style, adjustedStart, adjustedEnd, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            storage.replace(adjustedStart, adjustedEnd, replacement);

            int offsetDelta = (end - start) - replacement.length();
            offset += offsetDelta;
        }

        public void replace(int start, int end,
                            @NonNull String replacement) {
            int adjustedStart = start - offset;
            int adjustedEnd = end - offset;

            storage.replace(adjustedStart, adjustedEnd, replacement);

            int offsetDelta = (end - start) - replacement.length();
            offset += offsetDelta;
        }
    }
}
