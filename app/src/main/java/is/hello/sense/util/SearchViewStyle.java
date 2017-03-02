package is.hello.sense.util;
//https://gist.github.com/jaredrummler/c408c9d897fd92d5d116
/*
 * Copyright (C) 2015 Jared Rummler <jared.rummler@gmail.com>
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

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import android.annotation.SuppressLint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.util.SparseArray;
import android.view.Menu;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;

/**
 * Helper class to style a {@link SearchView}.</p>
 * <p>
 * Example usage:</p>
 * <p>
 * <pre>
 * <code>
 * SearchViewStyle.on(searchView)
 *    .setCursorColor(Color.WHITE)
 *    .setTextColor(Color.WHITE)
 *    .setHintTextColor(Color.WHITE)
 *    .setSearchHintDrawable(R.drawable.ic_search_api_material)
 *    .setSearchButtonImageResource(R.drawable.ic_search_api_material)
 *    .setCloseBtnImageResource(R.drawable.ic_clear_material)
 *    .setVoiceBtnImageResource(R.drawable.ic_voice_search_api_material)
 *    .setGoBtnImageResource(R.drawable.ic_go_search_api_material)
 *    .setCommitIcon(R.drawable.ic_commit_search_api_material)
 *    .setSubmitAreaDrawableId(R.drawable.abc_textfield_search_activated_mtrl_alpha)
 *    .setSearchPlateDrawableId(R.drawable.abc_textfield_search_activated_mtrl_alpha)
 *    .setSearchPlateTint(Color.WHITE)
 *    .setSubmitAreaTint(Color.WHITE);
 * </pre>
 * <p>
 * </code>
 *
 * @author Jared Rummler <jared.rummler@gmail.com>
 * @since Oct 24, 2014
 */
public class SearchViewStyle {

    // ===========================================================
    // STATIC FIELDS
    // ===========================================================

    // The root view
    // LinearLayout
    public static final String SEARCH_BAR = "search_bar";

    // This is actually used for the badge icon *or* the badge label (or neither)
    // TextView
    public static final String SEARCH_BADGE = "search_badge";

    // ImageView
    public static final String SEARCH_BUTTON = "search_button";

    // LinearLayout
    public static final String SEARCH_EDIT_FRAME = "search_edit_frame";

    // ImageView
    public static final String SEARCH_MAG_ICON = "search_mag_icon";

    // LinearLayout
    public static final String SEARCH_PLATE = "search_plate";

    // android.widget.SearchView$SearchAutoComplete which extends AutoCompleteTextView
    public static final String SEARCH_SRC_TEXT = "search_src_text";

    // ImageView
    public static final String SEARCH_CLOSE_BTN = "search_close_btn";

    // LinearLayout
    public static final String SUBMIT_AREA = "submit_area";

    // ImageView
    public static final String SEARCH_GO_BTN = "search_go_btn";

    // ImageView
    public static final String SEARCH_VOICE_BTN = "search_voice_btn";

    // ===========================================================
    // STATIC METHODS
    // ===========================================================

    public static SearchViewStyle on(final Menu menu, final int id) {
        return new SearchViewStyle(menu, id);
    }

    public static SearchViewStyle on(final SearchView searchView) {
        return new SearchViewStyle(searchView);
    }

    @SuppressLint("NewApi")
    private static void setTint(final Drawable drawable, final int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            drawable.setTint(color);
        } else {
            drawable.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_ATOP);
        }
    }

    // ===========================================================
    // FIELDS
    // ===========================================================

    private final SparseArray<View> mViews;

    private final SearchView mSearchView;

    // ===========================================================
    // INITIALIZERS
    // ===========================================================

    {
        mViews = new SparseArray<View>();
    }

    // ===========================================================
    // CONSTRUCTORS
    // ===========================================================

    private SearchViewStyle(final Menu menu, final int id) {
        this((SearchView) menu.findItem(id).getActionView());
    }

    private SearchViewStyle(final SearchView searchView) {
        mSearchView = searchView;
    }

    // ===========================================================
    // GETTERS AND SETTERS
    // ===========================================================

    public SearchView getSearchView() {
        return mSearchView;
    }

    // ===========================================================
    // METHODS
    // ===========================================================

    // TODO: add javadoc

    private int getId(final String name) {
        return mSearchView.getContext().getResources()
                          .getIdentifier("android:id/" + name, null, null);
    }

    @SuppressWarnings("unchecked")
    public <T extends View> T getView(final String name) {
        final int id = getId(name);
        if (id == 0) {
            return null;
        }
        View view = mViews.get(id);
        if (view == null) {
            view = mSearchView.findViewById(id);
        }
        return (T) view;
    }

    public SearchViewStyle setSearchPlateDrawableId(final int id) {
        final View view = getView(SEARCH_PLATE);
        if (view != null) {
            view.setBackgroundResource(id);
        }
        return this;
    }

    public SearchViewStyle setSubmitAreaDrawableId(final int id) {
        final View view = getView(SUBMIT_AREA);
        if (view != null) {
            view.setBackgroundResource(id);
        }
        return this;
    }

    public SearchViewStyle setSearchPlateTint(final int color) {
        final View view = getView(SEARCH_PLATE);
        if (view != null) {
            final Drawable background = view.getBackground();
            if (background != null) {
                setTint(background, color);
            }
        }
        return this;
    }

    public SearchViewStyle setSubmitAreaTint(final int color) {
        final View view = getView(SUBMIT_AREA);
        if (view != null) {
            final Drawable background = view.getBackground();
            if (background != null) {
                setTint(background, color);
            }
        }
        return this;
    }

    public SearchViewStyle setGoBtnImageResource(final int resId) {
        final ImageView imageView = getView(SEARCH_GO_BTN);
        if (imageView != null) {
            imageView.setImageResource(resId);
        }
        return this;
    }

    public SearchViewStyle setSearchButtonImageResource(final int resId) {
        final ImageView imageView = getView(SEARCH_BUTTON);
        if (imageView != null) {
            imageView.setImageResource(resId);
        }
        return this;
    }

    public SearchViewStyle setTextColor(final int color) {
        final AutoCompleteTextView editText = getView(SEARCH_SRC_TEXT);
        if (editText != null) {
            editText.setTextColor(color);
        }
        return this;
    }

    public SearchViewStyle setHintTextColor(final int color) {
        final AutoCompleteTextView editText = getView(SEARCH_SRC_TEXT);
        if (editText != null) {
            editText.setHintTextColor(color);
        }
        return this;
    }

    public SearchViewStyle setSearchHintDrawable(final int id,
                                                 final String... hint) {
        return setSearchHintDrawable(ContextCompat.getDrawable(mSearchView.getContext(), id), hint);
    }

    public SearchViewStyle setSearchHintDrawable(final Drawable drawable, final String... hint) {
        try {
            // android.widget.SearchView$SearchAutoComplete extends AutoCompleteTextView
            final AutoCompleteTextView editText = getView(SEARCH_SRC_TEXT);
            if (editText == null) {
                return this;
            }
            // http://nlopez.io/how-to-style-the-actionbar-searchview-programmatically/
            final Class<?> clazz = Class.forName("android.widget.SearchView$SearchAutoComplete");
            // Add the icon as an ImageSpan
            final Method textSizeMethod = clazz.getMethod("getTextSize");
            final Float rawTextSize = (Float) textSizeMethod.invoke(editText);
            final int textSize = (int) (rawTextSize * 1.25);
            drawable.setBounds(0, 0, textSize, textSize);
            // Create hint text
            final SpannableStringBuilder stopHint = new SpannableStringBuilder("   ");
            if (hint != null && hint.length == 1) {
                stopHint.append(hint[0]);
            }
            stopHint.setSpan(new ImageSpan(drawable), 1, 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            // Set the new hint text
            final Method setHintMethod = clazz.getMethod("setHint", CharSequence.class);
            setHintMethod.invoke(editText, stopHint);
        } catch (final Exception ignored) {
        }
        return this;
    }

    public SearchViewStyle setCursorResId(final int drawableId) {
        final AutoCompleteTextView editText = getView(SEARCH_SRC_TEXT);
        if (editText != null) {
            try {
                final Field f = TextView.class.getDeclaredField("mCursorDrawableRes");
                f.setAccessible(true);
                f.set(editText, drawableId);
            } catch (final Throwable ignored) {
            }
        }
        return this;
    }

    public SearchViewStyle setCursorColor(final int color) {
        final AutoCompleteTextView editText = getView(SEARCH_SRC_TEXT);
        if (editText != null) {
            try {
                final Field fCursorDrawableRes = TextView.class
                        .getDeclaredField("mCursorDrawableRes");
                fCursorDrawableRes.setAccessible(true);
                final int mCursorDrawableRes = fCursorDrawableRes.getInt(editText);
                final Field fEditor = TextView.class.getDeclaredField("mEditor");
                fEditor.setAccessible(true);
                final Object editor = fEditor.get(editText);
                final Class<?> clazz = editor.getClass();
                final Field fCursorDrawable = clazz.getDeclaredField("mCursorDrawable");
                fCursorDrawable.setAccessible(true);
                final Drawable[] drawables = new Drawable[2];
                drawables[0] = editText.getContext().getResources().getDrawable(mCursorDrawableRes);
                drawables[1] = editText.getContext().getResources().getDrawable(mCursorDrawableRes);
                drawables[0].setColorFilter(color, PorterDuff.Mode.SRC_IN);
                drawables[1].setColorFilter(color, PorterDuff.Mode.SRC_IN);
                fCursorDrawable.set(editor, drawables);
            } catch (final Throwable ignored) {
            }
        }
        return this;
    }

    public SearchViewStyle setCloseBtnImageResource(final int resId) {
        final ImageView imageView = getView(SEARCH_CLOSE_BTN);
        if (imageView != null) {
            imageView.setImageResource(resId);
        }
        return this;
    }

    public SearchViewStyle setVoiceBtnImageResource(final int resId) {
        final ImageView imageView = getView(SEARCH_VOICE_BTN);
        if (imageView != null) {
            imageView.setImageResource(resId);
        }
        return this;
    }

    public SearchViewStyle setCommitIcon(final int drawableId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                final Field commitIcon = mSearchView.getClass().getField(
                        "mSuggestionCommitIconResId");
                commitIcon.setAccessible(true);
                commitIcon.set(mSearchView, drawableId);
            } catch (final Exception ignored) {
            }
        }
        return this;
    }

}