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

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.provider.Browser;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;

public class MarkupURLSpan extends ClickableSpan implements MarkupSpan {
    private final String url;
    private final String title;

    public MarkupURLSpan(@NonNull String url, @Nullable String title) {
        this.url = url;
        this.title = title;
    }


    //region Serialization

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(url);
        out.writeString(title);
    }

    public static final Creator<MarkupURLSpan> CREATOR = new Creator<MarkupURLSpan>() {
        @Override
        public MarkupURLSpan createFromParcel(Parcel source) {
            return new MarkupURLSpan(source.readString(), source.readString());
        }

        @Override
        public MarkupURLSpan[] newArray(int size) {
            return new MarkupURLSpan[size];
        }
    };

    //endregion

    //region Attributes

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    //endregion


    @Override
    public void onClick(View widget) {
        Uri uri = Uri.parse(getUrl());
        Context context = widget.getContext();
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.getPackageName());
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.w(getClass().getSimpleName(), "Activity was not found for intent, " + intent.toString());
        }
    }

    @Override
    public String toString() {
        return "SerializableURLSpan{" +
                "url='" + url + '\'' +
                ", title='" + title + '\'' +
                '}';
    }
}
