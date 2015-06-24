package is.hello.sense.ui.widget;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import is.hello.buruberi.util.StringRef;
import is.hello.sense.R;

public class SenseBottomSheet extends Dialog implements View.OnClickListener {
    private static final String SAVED_DIALOG_STATE = SenseBottomSheet.class.getSimpleName() + "#SAVED_DIALOG_STATE";
    private static final String SAVED_TITLE = SenseBottomSheet.class.getSimpleName() + "#SAVED_TITLE";
    private static final String SAVED_OPTIONS = SenseBottomSheet.class.getSimpleName() + "#SAVED_OPTIONS";
    private static final String SAVED_WANTS_DIVIDERS = SenseBottomSheet.class.getSimpleName() + "#SAVED_WANTS_DIVIDERS";

    private final ArrayList<Option> options = new ArrayList<>();
    private final LayoutInflater inflater;

    private @Nullable String title;
    private boolean wantsDividers = false;

    private @Nullable LinearLayout optionsContainer;
    private @Nullable TextView titleText;

    private @Nullable OnOptionSelectedListener onOptionSelectedListener;

    //region Lifecycle

    public SenseBottomSheet(@NonNull Context context) {
        super(context, R.style.AppTheme_Dialog_BottomSheet);

        this.inflater = LayoutInflater.from(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.dialog_bottom_sheet);
        setCancelable(true);
        setCanceledOnTouchOutside(true);

        Window window = getWindow();
        window.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        this.optionsContainer = (LinearLayout) findViewById(R.id.dialog_bottom_sheet_options);
        if (wantsDividers) {
            optionsContainer.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
        } else {
            optionsContainer.setShowDividers(LinearLayout.SHOW_DIVIDER_NONE);
        }

        Drawable divider = getContext().getResources().getDrawable(R.drawable.divider_horizontal_inset);
        optionsContainer.setDividerDrawable(divider);

        for (Option option : options) {
            addViewForOption(option);
        }

        this.titleText = (TextView) findViewById(R.id.dialog_bottom_sheet_title);
        if (title != null) {
            titleText.setText(title);
        } else {
            titleText.setVisibility(View.GONE);
        }
    }

    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        this.title = savedInstanceState.getString(SAVED_TITLE);
        this.wantsDividers = savedInstanceState.getBoolean(SAVED_WANTS_DIVIDERS);

        //noinspection unchecked
        ArrayList<Option> savedOptions = (ArrayList<Option>) savedInstanceState.getSerializable(SAVED_OPTIONS);
        options.clear();
        options.addAll(savedOptions);

        Bundle dialogState = savedInstanceState.getParcelable(SAVED_DIALOG_STATE);
        super.onRestoreInstanceState(dialogState);
    }

    @Override
    public Bundle onSaveInstanceState() {
        Bundle savedState = new Bundle();
        savedState.putString(SAVED_TITLE, title);
        savedState.putSerializable(SAVED_OPTIONS, options);
        savedState.putBoolean(SAVED_WANTS_DIVIDERS, wantsDividers);
        savedState.putParcelable(SAVED_DIALOG_STATE, super.onSaveInstanceState());
        return savedState;
    }

    //endregion


    //region Populating

    protected void addViewForOption(@NonNull Option option) {
        if (optionsContainer == null) {
            return;
        }

        View optionView = inflater.inflate(R.layout.item_bottom_sheet_option, optionsContainer, false);

        TextView title = (TextView) optionView.findViewById(R.id.item_bottom_sheet_option_title);
        TextView description = (TextView) optionView.findViewById(R.id.item_bottom_sheet_option_description);

        if (option.iconRes != 0) {
            title.setCompoundDrawablesRelativeWithIntrinsicBounds(option.iconRes, 0, 0, 0);
        }

        if (option.getTitle() != null) {
            String itemTitle = option.getTitle().resolve(getContext());
            title.setText(itemTitle);
        }

        if (option.getTitleColor() != null) {
            title.setTextColor(option.getTitleColor());
        }

        if (option.getDescription() != null) {
            String itemDescription = option.getDescription().resolve(getContext());
            description.setText(itemDescription);
        } else {
            description.setVisibility(View.GONE);
        }

        if (option.enabled) {
            optionView.setOnClickListener(this);
        } else {
            title.setEnabled(false);
            description.setEnabled(false);

            Drawable icon = title.getCompoundDrawablesRelative()[0];
            if (icon != null) {
                icon.setAlpha(0x77);
            }
        }

        optionView.setTag(optionsContainer.getChildCount());
        optionsContainer.addView(optionView);
    }

    public void addOption(@NonNull Option option) {
        this.options.add(option);
        addViewForOption(option);
    }

    public void addOptions(@NonNull Collection<Option> options) {
        this.options.addAll(options);
        for (Option option : options) {
            addViewForOption(option);
        }
    }

    //endregion


    //region Attributes

    @Override
    public void setTitle(@Nullable CharSequence title) {
        if (title != null) {
            this.title = title.toString();
        } else {
            this.title = null;
        }

        if (titleText != null) {
            titleText.setText(title);
            if (title != null) {
                titleText.setVisibility(View.VISIBLE);
            } else {
                titleText.setVisibility(View.GONE);
            }
        }
    }

    public void setWantsDividers(boolean wantsDividers) {
        this.wantsDividers = wantsDividers;

        if (optionsContainer != null) {
            if (wantsDividers) {
                optionsContainer.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
            } else {
                optionsContainer.setShowDividers(LinearLayout.SHOW_DIVIDER_NONE);
            }
        }
    }

    public void setOnOptionSelectedListener(@Nullable OnOptionSelectedListener onOptionSelectedListener) {
        this.onOptionSelectedListener = onOptionSelectedListener;
    }

    //endregion


    @Override
    public void onClick(View view) {
        if (onOptionSelectedListener != null) {
            int position = (int) view.getTag();
            Option option = options.get(position);
            onOptionSelectedListener.onOptionSelected(position, option);
        }

        dismiss();
    }


    public interface OnOptionSelectedListener {
        void onOptionSelected(int position, @NonNull Option option);
    }

    public static class Option implements Serializable {
        private final int optionId;
        private @Nullable Integer titleColor;
        private @Nullable StringRef title;
        private @Nullable StringRef description;
        private @DrawableRes int iconRes;
        private boolean enabled = true;

        public Option(int optionId) {
            this.optionId = optionId;
        }

        public int getOptionId() {
            return optionId;
        }


        //region Text

        public Option setTitle(@Nullable String title) {
            if (title != null) {
                this.title = StringRef.from(title);
            } else {
                this.title = null;
            }
            return this;
        }

        public Option setTitle(@StringRes int titleRes) {
            if (titleRes != 0) {
                this.title = StringRef.from(titleRes);
            } else {
                this.title = null;
            }
            return this;
        }

        @Nullable
        public StringRef getTitle() {
            return title;
        }

        public Option setTitleColor(@Nullable Integer titleColor) {
            this.titleColor = titleColor;
            return this;
        }

        @Nullable
        public Integer getTitleColor() {
            return titleColor;
        }

        public Option setDescription(@Nullable String description) {
            if (description != null) {
                this.description = StringRef.from(description);
            } else {
                this.description = null;
            }
            return this;
        }

        public Option setDescription(@StringRes int descriptionRes) {
            if (descriptionRes != 0) {
                this.description = StringRef.from(descriptionRes);
            } else {
                this.description = null;
            }
            return this;
        }

        @Nullable
        public StringRef getDescription() {
            return description;
        }

        public Option setIcon(@DrawableRes int iconRes) {
            this.iconRes = iconRes;
            return this;
        }

        public Option setEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        //endregion


        @Override
        public String toString() {
            return "Option{" +
                    "optionId=" + optionId +
                    ", title=" + title +
                    ", enabled=" + enabled +
                    '}';
        }
    }
}
