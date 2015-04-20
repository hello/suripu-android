package is.hello.sense.ui.widget;

import android.app.Dialog;
import android.content.Context;
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

import is.hello.sense.R;
import is.hello.sense.util.Errors;

public class SenseBottomSheet extends Dialog implements View.OnClickListener {
    private static final String SAVED_DIALOG_STATE = SenseBottomSheet.class.getSimpleName() + "#SAVED_DIALOG_STATE";
    private static final String SAVED_TITLE = SenseBottomSheet.class.getSimpleName() + "#SAVED_TITLE";
    private static final String SAVED_ICON_RES = SenseBottomSheet.class.getSimpleName() + "#SAVED_ICON_RES";
    private static final String SAVED_OPTIONS = SenseBottomSheet.class.getSimpleName() + "#SAVED_OPTIONS";

    private final ArrayList<Option> options = new ArrayList<>();
    private final LayoutInflater inflater;

    private @Nullable String title;
    private @DrawableRes int iconRes;

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
        for (Option option : options) {
            addViewForOption(option);
        }

        this.titleText = (TextView) findViewById(R.id.dialog_bottom_sheet_title);
        titleText.setCompoundDrawablesRelativeWithIntrinsicBounds(0, iconRes, 0, 0);
        if (title != null) {
            titleText.setText(title);
        } else {
            titleText.setVisibility(View.GONE);
        }
    }

    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        this.title = savedInstanceState.getString(SAVED_TITLE);
        this.iconRes = savedInstanceState.getInt(SAVED_ICON_RES);

        //noinspection unchecked
        ArrayList<Option> savedOptions = (ArrayList<Option>) savedInstanceState.getSerializable(SAVED_ICON_RES);
        options.clear();
        options.addAll(savedOptions);

        Bundle dialogState = savedInstanceState.getParcelable(SAVED_DIALOG_STATE);
        super.onRestoreInstanceState(dialogState);
    }

    @Override
    public Bundle onSaveInstanceState() {
        Bundle savedState = new Bundle();
        savedState.putString(SAVED_TITLE, title);
        savedState.putInt(SAVED_ICON_RES, iconRes);
        savedState.putSerializable(SAVED_OPTIONS, options);
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
        optionView.setOnClickListener(this);

        TextView title = (TextView) optionView.findViewById(R.id.item_bottom_sheet_option_title);
        TextView description = (TextView) optionView.findViewById(R.id.item_bottom_sheet_option_description);

        if (option.getIcon() != 0) {
            title.setCompoundDrawablesRelativeWithIntrinsicBounds(0, option.getIcon(), 0, 0);
            title.setGravity(Gravity.CENTER);
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

        optionView.setTag(optionsContainer.getChildCount());
        optionsContainer.addView(optionView);
    }

    public void addOption(@NonNull Option option) {
        options.add(option);
        addViewForOption(option);
    }

    public void addOptions(@NonNull Collection<Option> options) {
        this.options.addAll(options);
        for (Option option : options) {
            addViewForOption(option);
        }
    }

    public void clearOptions() {
        options.clear();
        if (optionsContainer != null) {
            optionsContainer.removeAllViews();
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

    public void setIcon(@DrawableRes int iconRes) {
        this.iconRes = iconRes;

        if (titleText != null) {
            titleText.setCompoundDrawablesRelativeWithIntrinsicBounds(0, iconRes, 0, 0);
            if (iconRes != 0) {
                titleText.setGravity(Gravity.CENTER);
            } else {
                titleText.setGravity(Gravity.START);
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
        private @DrawableRes int icon;
        private @Nullable Integer titleColor;
        private @Nullable Errors.Message title;
        private @Nullable Errors.Message description;

        public Option(int optionId) {
            this.optionId = optionId;
        }

        public int getOptionId() {
            return optionId;
        }

        //region Icon

        public Option setIcon(@DrawableRes int icon) {
            this.icon = icon;
            return this;
        }

        public @DrawableRes int getIcon() {
            return icon;
        }

        //endregion


        //region Text

        public Option setTitle(@Nullable String title) {
            if (title != null) {
                this.title = Errors.Message.from(title);
            } else {
                this.title = null;
            }
            return this;
        }

        public Option setTitle(@StringRes int titleRes) {
            if (titleRes != 0) {
                this.title = Errors.Message.from(titleRes);
            } else {
                this.title = null;
            }
            return this;
        }

        @Nullable
        public Errors.Message getTitle() {
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
                this.description = Errors.Message.from(description);
            } else {
                this.description = null;
            }
            return this;
        }

        public Option setDescription(@StringRes int descriptionRes) {
            if (descriptionRes != 0) {
                this.description = Errors.Message.from(descriptionRes);
            } else {
                this.description = null;
            }
            return this;
        }

        @Nullable
        public Errors.Message getDescription() {
            return description;
        }

        //endregion
    }
}
