package is.hello.sense.flows.accountsettings.ui.widgets;


import android.content.Context;
import android.content.res.TypedArray;
import android.databinding.DataBindingUtil;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import is.hello.sense.R;
import is.hello.sense.databinding.ItemSettingsDetailBinding;

public class ItemSettingsDetailRow extends FrameLayout {

    private final ItemSettingsDetailBinding binding;

    public ItemSettingsDetailRow(final Context context) {
        this(context, null);
    }

    public ItemSettingsDetailRow(final Context context,
                                 final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ItemSettingsDetailRow(final Context context,
                                 final AttributeSet attrs,
                                 final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.binding = DataBindingUtil.bind(LayoutInflater.from(getContext()).inflate(R.layout.item_settings_detail, null));
        addView(binding.getRoot());

        final TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.ItemSettingsDetailRow,
                0, 0);
        try {
            final int drawableRes = a.getResourceId(R.styleable.ItemSettingsDetailRow_ISDR_iconSrc, -1);
            if (drawableRes != -1) {
                this.binding.itemSettingsDetailIcon.setImageResource(drawableRes);
            }
            final int titleRes = a.getResourceId(R.styleable.ItemSettingsDetailRow_ISDR_labelText, -1);
            if (titleRes != -1) {
                this.binding.itemSettingsDetailTitle.setText(titleRes);
            } else {
                this.binding.itemSettingsDetailTitle.setText(null);
            }
            final int valueRes = a.getResourceId(R.styleable.ItemSettingsDetailRow_ISDR_valueText, -1);
            if (valueRes != -1) {
                this.binding.itemSettingsDetailDetail.setText(valueRes);
            } else {
                this.binding.itemSettingsDetailDetail.setText(null);
            }
        } finally {
            a.recycle();
        }
    }

    public void setValue(@Nullable final String value) {
        this.binding.itemSettingsDetailDetail.setText(value);
    }
}