package is.hello.sense.ui.widget;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.res.ResourcesCompat;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;

import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import rx.functions.Action1;

import static is.hello.go99.animators.MultiAnimator.animatorFor;

public class SenseBottomSheet extends Dialog implements View.OnClickListener {
    private static final String SAVED_DIALOG_STATE = SenseBottomSheet.class.getSimpleName() + "#SAVED_DIALOG_STATE";
    private static final String SAVED_TITLE = SenseBottomSheet.class.getSimpleName() + "#SAVED_TITLE";
    private static final String SAVED_MESSAGE = SenseBottomSheet.class.getSimpleName() + "#SAVED_MESSAGE";
    private static final String SAVED_OPTIONS = SenseBottomSheet.class.getSimpleName() + "#SAVED_OPTIONS";
    private static final String SAVED_WANTS_DIVIDERS = SenseBottomSheet.class.getSimpleName() + "#SAVED_WANTS_DIVIDERS";
    private static final String SAVED_WANTS_BIG_TITLE = SenseBottomSheet.class.getSimpleName() + "#SAVED_WANTS_BIG_TITLE";

    private final ArrayList<Option> options = new ArrayList<>();
    private final LayoutInflater inflater;

    private final RelativeLayout contentRoot;
    private final LinearLayout optionsContainer;
    private final TextView titleText;
    private final TextView messageText;
    private final View messageDivider;

    private boolean wantsDividers = false;
    private boolean wantsBigTitle = false;

    private @Nullable View replacementContent;
    private @Nullable OnOptionSelectedListener onOptionSelectedListener;


    //region Lifecycle

    public SenseBottomSheet(@NonNull Context context) {
        super(context, R.style.AppTheme_Dialog_BottomSheet);

        setContentView(R.layout.dialog_bottom_sheet);
        setCancelable(true);
        setCanceledOnTouchOutside(true);

        this.inflater = LayoutInflater.from(context);

        this.contentRoot = (RelativeLayout) findViewById(R.id.dialog_bottom_sheet_content);
        this.optionsContainer = (LinearLayout) findViewById(R.id.dialog_bottom_sheet_options);

        Drawable divider = ResourcesCompat.getDrawable(getContext().getResources(), R.drawable.divider_horizontal_inset, null);
        optionsContainer.setDividerDrawable(divider);

        this.titleText = (TextView) findViewById(R.id.dialog_bottom_sheet_title);
        this.messageText = (TextView) findViewById(R.id.dialog_bottom_sheet_message);
        this.messageDivider = findViewById(R.id.dialog_bottom_sheet_message_divider);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        setTitle(savedInstanceState.getString(SAVED_TITLE));
        setMessage(savedInstanceState.getString(SAVED_MESSAGE));
        setWantsDividers(savedInstanceState.getBoolean(SAVED_WANTS_DIVIDERS));
        setWantsBigTitle(savedInstanceState.getBoolean(SAVED_WANTS_BIG_TITLE));

        ArrayList<Option> savedOptions = savedInstanceState.getParcelableArrayList(SAVED_OPTIONS);
        clearOptions();
        addOptions(savedOptions);

        Bundle dialogState = savedInstanceState.getParcelable(SAVED_DIALOG_STATE);
        super.onRestoreInstanceState(dialogState);
    }

    @Override
    public Bundle onSaveInstanceState() {
        Bundle savedState = new Bundle();

        savedState.putString(SAVED_TITLE, titleText.getText().toString());
        savedState.putString(SAVED_MESSAGE, messageText.getText().toString());

        savedState.putParcelableArrayList(SAVED_OPTIONS, options);

        savedState.putBoolean(SAVED_WANTS_DIVIDERS, wantsDividers);
        savedState.putBoolean(SAVED_WANTS_BIG_TITLE, wantsBigTitle);

        savedState.putParcelable(SAVED_DIALOG_STATE, super.onSaveInstanceState());

        return savedState;
    }

    //endregion


    //region Populating

    protected void addViewForOption(@NonNull Option option) {
        View optionView = inflater.inflate(R.layout.item_bottom_sheet_option, optionsContainer, false);

        TextView title = (TextView) optionView.findViewById(R.id.item_bottom_sheet_option_title);
        TextView description = (TextView) optionView.findViewById(R.id.item_bottom_sheet_option_description);
        ImageView imageIcon = (ImageView) optionView.findViewById(R.id.item_bottom_sheet_option_icon);

        if (option.iconRes != 0) {
            imageIcon.setVisibility(View.VISIBLE);
            imageIcon.setImageResource(option.iconRes);
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

    protected void clearOptions() {
        options.clear();
        optionsContainer.removeAllViews();
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

    //endregion


    //region Attributes

    @Override
    public void setTitle(@Nullable CharSequence title) {
        titleText.setText(title);

        if (!TextUtils.isEmpty(title)) {
            titleText.setVisibility(View.VISIBLE);
        } else {
            titleText.setVisibility(View.GONE);
        }
    }

    public void setMessage(@Nullable String message) {
        messageText.setText(message);

        if (!TextUtils.isEmpty(message)) {
            messageText.setVisibility(View.VISIBLE);
            messageDivider.setVisibility(View.VISIBLE);
        } else {
            messageText.setVisibility(View.GONE);
            messageDivider.setVisibility(View.GONE);
        }
    }

    public void setMessage(@StringRes int messageRes) {
        setMessage(getContext().getString(messageRes));
    }

    public void setWantsDividers(boolean wantsDividers) {
        this.wantsDividers = wantsDividers;

        if (wantsDividers) {
            optionsContainer.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
        } else {
            optionsContainer.setShowDividers(LinearLayout.SHOW_DIVIDER_NONE);
        }
    }

    public void setWantsBigTitle(boolean wantsBigTitle) {
        this.wantsBigTitle = wantsBigTitle;

        if (wantsBigTitle) {
            titleText.setTextAppearance(getContext(), R.style.AppTheme_Text_Body);
            titleText.setAllCaps(false);
        } else {
            titleText.setTextAppearance(getContext(), R.style.AppTheme_Text_SectionHeading);
        }
    }

    public void replaceContent(@NonNull View replacementContent,
                               @Nullable Action1<Boolean> onAnimationFinished) {
        if (this.replacementContent != null) {
            View oldContent = this.replacementContent;
            animatorFor(oldContent)
                    .fadeOut(View.INVISIBLE)
                    .addOnAnimationCompleted(finished -> {
                        contentRoot.removeView(oldContent);
                    })
                    .start();
        } else {
            for (int i = 0, size = contentRoot.getChildCount(); i < size; i++) {
                View child = contentRoot.getChildAt(i);
                animatorFor(child)
                        .fadeOut(View.INVISIBLE)
                        .addOnAnimationCompleted(finished -> {
                            contentRoot.removeView(child);
                        })
                        .start();
            }
        }

        int minHeight = getContext().getResources().getDimensionPixelSize(R.dimen.bottom_sheet_replacement_content_min_height);
        int contentHeight = contentRoot.getMeasuredHeight();
        int height = Math.max(minHeight, contentHeight);
        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, height);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        replacementContent.setVisibility(View.INVISIBLE);
        contentRoot.addView(replacementContent, layoutParams);
        animatorFor(replacementContent)
                .fadeIn()
                .addOnAnimationCompleted(finished -> {
                    if (onAnimationFinished != null) {
                        onAnimationFinished.call(finished);
                    }
                })
                .start();

        this.replacementContent = contentRoot;
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
            if (!onOptionSelectedListener.onOptionSelected(option)) {
                return;
            }
        }

        dismiss();
    }


    public interface OnOptionSelectedListener {
        boolean onOptionSelected(@NonNull Option option);
    }

    public static class Option implements Parcelable {
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


        //region Parceling

        public Option(Parcel in) {
            this.optionId = in.readInt();
            this.titleColor = (Integer) in.readValue(Integer.class.getClassLoader());
            this.title = in.readParcelable(StringRef.class.getClassLoader());
            this.description = in.readParcelable(StringRef.class.getClassLoader());
            this.iconRes = in.readInt();
            this.enabled = in.readByte() != 0;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeInt(optionId);
            out.writeValue(titleColor);
            out.writeParcelable(title, flags);
            out.writeParcelable(description, flags);
            out.writeInt(iconRes);
            out.writeByte((byte) (enabled ? 1 : 0));
        }

        public static final Creator<Option> CREATOR = new Creator<Option>() {
            @Override
            public Option createFromParcel(Parcel in) {
                return new Option(in);
            }

            @Override
            public Option[] newArray(int size) {
                return new Option[size];
            }
        };

        //endregion

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
