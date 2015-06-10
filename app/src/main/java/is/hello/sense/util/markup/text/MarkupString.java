/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package is.hello.sense.util.markup.text;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.text.GetChars;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.util.Log;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * An implementation of the <code>Spanned</code> interface that supports serialization
 * through the built-in Java mechanism and through <code>Parcel</code>.
 * <p />
 * MarkupString only supports spans that implement {@link MarkupSpan},
 * all other span types will be silently ignored.
 */
public class MarkupString implements CharSequence, GetChars, Spanned, Serializable, Parcelable {
    private static final int START = 0;
    private static final int END = 1;
    private static final int FLAGS = 2;
    private static final int COLUMNS = 3;

    private final String storage;
    private final MarkupSpan[] spans;
    private final int[] spanData;

    //region Creation

    public MarkupString(@NonNull CharSequence text, int start, int end) {
        if (start == 0 && end == text.length()) {
            this.storage = text.toString();
        } else {
            this.storage = text.toString().substring(start, end);
        }

        if (text instanceof Spanned) {
            Spanned spanned = (Spanned) text;
            this.spans = spanned.getSpans(start, end, MarkupSpan.class);
            this.spanData = new int[spans.length * COLUMNS];

            for (int i = 0, spansLength = spans.length; i < spansLength; i++) {
                Object span = spans[i];

                spanData[i * COLUMNS + START] = spanned.getSpanStart(span);
                spanData[i * COLUMNS + END] = spanned.getSpanEnd(span);
                spanData[i * COLUMNS + FLAGS] = spanned.getSpanFlags(span);
            }
        } else {
            this.spans = new MarkupSpan[0];
            this.spanData = new int[0];
        }
    }

    public MarkupString(@NonNull CharSequence text) {
        this(text, 0, text.length());
    }

    //endregion


    //region Serialization

    public MarkupString(@NonNull Parcel in) {
        this.storage = in.readString();

        Parcelable[] spans = in.readParcelableArray(MarkupSpan.class.getClassLoader());
        this.spans = new MarkupSpan[spans.length];
        //noinspection SuspiciousSystemArraycopy
        System.arraycopy(spans, 0, this.spans, 0, spans.length);

        this.spanData = in.createIntArray();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(storage);
        out.writeParcelableArray(spans, 0);
        out.writeIntArray(spanData);
    }

    public static final Creator<MarkupString> CREATOR = new Creator<MarkupString>() {
        @Override
        public MarkupString createFromParcel(Parcel source) {
            return new MarkupString(source);
        }

        @Override
        public MarkupString[] newArray(int size) {
            return new MarkupString[size];
        }
    };

    //endregion


    //region CharSequence

    @Override
    public int length() {
        return storage.length();
    }

    @Override
    public char charAt(int index) {
        return storage.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return new MarkupString(this, start, end);
    }

    //endregion


    //region GetChars

    @Override
    public void getChars(int start, int end, char[] destination, int destinationOffset) {
        storage.getChars(start, end, destination, destinationOffset);
    }

    //endregion


    //region Spanned

    private int indexOfSpan(@NonNull Object span) {
        for (int i = 0, spansLength = spans.length; i < spansLength; i++) {
            if (spans[i] == span) {
                return i;
            }
        }

        return -1;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T[] getSpans(int queryStart, int queryEnd, @NonNull Class<T> type) {
        ArrayList<T> matches = null;

        for (int i = 0, spansLength = spans.length; i < spansLength; i++) {
            Object span = spans[i];
            if (!type.isInstance(span)) {
                continue;
            }

            int spanStart = spanData[i * COLUMNS + START];
            int spanEnd = spanData[i * COLUMNS + END];

            if (spanStart > queryEnd) {
                continue;
            }
            if (spanEnd < queryStart) {
                continue;
            }

            if (spanStart != spanEnd && queryStart != queryEnd) {
                if (spanStart == queryEnd) {
                    continue;
                }
                if (spanEnd == queryStart) {
                    continue;
                }
            }

            if (matches == null) {
                matches = new ArrayList<>();
            }

            matches.add((T) span);
        }

        if (matches == null) {
            return (T[]) Array.newInstance(type, 0);
        } else {
            T[] result = (T[]) Array.newInstance(type, matches.size());
            return matches.toArray(result);
        }
    }

    @Override
    public int getSpanStart(Object tag) {
        int index = indexOfSpan(tag);
        if (index != -1) {
            return spanData[index * COLUMNS + START];
        } else {
            return -1;
        }
    }

    @Override
    public int getSpanEnd(Object tag) {
        int index = indexOfSpan(tag);
        if (index != -1) {
            return spanData[index * COLUMNS + END];
        } else {
            return -1;
        }
    }

    @Override
    public int getSpanFlags(Object tag) {
        int index = indexOfSpan(tag);
        if (index != -1) {
            return spanData[index * COLUMNS + FLAGS];
        } else {
            return -1;
        }
    }

    @Override
    public int nextSpanTransition(int start, int limit, Class type) {
        if (type == null) {
            type = Object.class;
        }

        for (int i = 0, spansLength = spans.length; i < spansLength; i++) {
            Object span = spans[i];
            int spanStart = spanData[i * COLUMNS + START];
            int spanEnd = spanData[i * COLUMNS + END];

            if ((spanStart > start) && (spanStart < limit) && type.isInstance(span)) {
                limit = spanStart;
            }
            if ((spanEnd > start) && (spanEnd < limit) && type.isInstance(span)) {
                limit = spanEnd;
            }
        }

        return limit;
    }

    //endregion


    //region Identity

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MarkupString that = (MarkupString) o;

        return (storage.equals(that.storage) &&
                Arrays.equals(spans, that.spans) &&
                Arrays.equals(spanData, that.spanData));
    }

    @Override
    public int hashCode() {
        int result = storage.hashCode();
        result = 31 * result + Arrays.hashCode(spans);
        result = 31 * result + Arrays.hashCode(spanData);
        return result;
    }

    @NonNull
    @Override
    public String toString() {
        return storage;
    }

    //endregion


    public static class Builder extends SpannableStringBuilder {
        public Builder() {
        }

        public Builder(CharSequence text) {
            super(text);
        }

        public Builder(CharSequence text, int start, int end) {
            super(text, start, end);
        }


        @Override
        public void setSpan(Object what, int start, int end, int flags) {
            if (what != null && !MarkupSpan.class.isInstance(what)) {
                Log.w(getClass().getCanonicalName(), "Ignoring span `" + what + "` that does not implement MarkupSpan.");
                return;
            }

            super.setSpan(what, start, end, flags);
        }


        public MarkupString build() {
            return new MarkupString(this);
        }
    }
}
