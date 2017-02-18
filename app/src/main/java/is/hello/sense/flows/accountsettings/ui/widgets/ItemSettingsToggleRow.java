package is.hello.sense.flows.accountsettings.ui.widgets;


import android.content.Context;
import android.content.res.TypedArray;
import android.databinding.DataBindingUtil;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import is.hello.sense.R;
import is.hello.sense.databinding.ItemSettingsToggleBinding;

public class ItemSettingsToggleRow extends FrameLayout {
    public ItemSettingsToggleRow(final Context context) {
        this(context, null);
    }

    public ItemSettingsToggleRow(final Context context,
                                 final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ItemSettingsToggleRow(final Context context,
                                 final AttributeSet attrs,
                                 final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        final ItemSettingsToggleBinding binding = DataBindingUtil.bind(LayoutInflater.from(getContext()).inflate(R.layout.item_settings_toggle, null));
        addView(binding.getRoot());

        final TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.ItemSettingsToggleRow,
                0, 0);
        try {
            final int drawableRes = a.getResourceId(R.styleable.ItemSettingsToggleRow_ISTR_iconSrc, -1);
            if (drawableRes != -1) {
                binding.itemSettingsToggleIcon.setImageResource(drawableRes);
            }
            final int titleRes = a.getResourceId(R.styleable.ItemSettingsToggleRow_ISTR_labelText, -1);
            if (titleRes != -1) {
                binding.itemSettingsToggleCheckTitle.setText(titleRes);
            } else {
                binding.itemSettingsToggleCheckTitle.setText(null);
            }
        } finally {
            a.recycle();
        }

    }

}
