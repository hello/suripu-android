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
import android.text.Spanned;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import is.hello.sense.util.markup.text.MarkupString;
import is.hello.sense.util.markup.text.SerializableSpan;
import is.hello.sense.util.markup.text.SerializableStyleSpan;
import rx.functions.Action1;

public class MarkupProcessor {
    //region Patterns

    private final Pattern PATTERN_BOLD = Pattern.compile("(\\*\\*|__)(?=\\S)(.+?[*_]*)(?<=\\S)\\1");
    private final Pattern PATTERN_ITALIC = Pattern.compile("(\\*|_)(?=\\S)(.+?)(?<=\\S)\\1");

    //endregion


    //region Public Interface

    public @NonNull CharSequence render(@NonNull String source) {
        RenderState state = new RenderState(source);

        renderEmphasis(state);

        return state.getStorage();
    }

    //endregion


    //region Rendering

    protected void renderPattern(@NonNull Pattern pattern,
                                 @NonNull RenderState state,
                                 @NonNull Action1<Matcher> visitor) {
        state.prepare();

        Matcher matcher = pattern.matcher(state.getStorage());
        while (matcher.find()) {
            visitor.call(matcher);
        }
    }


    private void renderBolds(@NonNull RenderState state) {
        renderPattern(PATTERN_BOLD, state, matcher -> {
            state.replace(matcher.start(), matcher.end(),
                    matcher.group(2),
                    new SerializableStyleSpan(Typeface.BOLD));
        });
    }

    private void renderItalics(@NonNull RenderState state) {
        renderPattern(PATTERN_ITALIC, state, matcher -> {
            state.replace(matcher.start(), matcher.end(),
                    matcher.group(2),
                    new SerializableStyleSpan(Typeface.ITALIC));
        });
    }

    protected void renderEmphasis(@NonNull RenderState state) {
        renderBolds(state);
        renderItalics(state);
    }

    //endregion


    protected final class RenderState {
        private final MarkupString storage;
        private int offset = 0;

        public RenderState(@NonNull String source) {
            this.storage = new MarkupString(source);
        }

        private void prepare() {
            this.offset = 0;
        }

        public MarkupString getStorage() {
            return storage;
        }

        public void replace(int start, int end,
                            @NonNull String value,
                            @NonNull SerializableSpan span) {
            int adjustedStart = start - offset;
            int adjustedEnd = end - offset;
            storage.replace(adjustedStart, adjustedEnd, value);

            int spanEnd = adjustedStart + value.length();
            storage.setSpan(span, adjustedStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            int offsetDelta = (start - end) - value.length();
            offset += offsetDelta;
        }
    }
}
