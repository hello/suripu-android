package is.hello.sense.flows.accountsettings.ui.widgets;


import android.content.Context;
import android.content.res.TypedArray;
import android.databinding.DataBindingUtil;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import is.hello.sense.R;
import is.hello.sense.databinding.ItemSettingsCheckboxBinding;

public class ItemSettingsCheckBoxRow extends FrameLayout {
    public ItemSettingsCheckBoxRow(final Context context) {
        this(context, null);
    }

    public ItemSettingsCheckBoxRow(final Context context,
                                   final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ItemSettingsCheckBoxRow(final Context context,
                                   final AttributeSet attrs,
                                   final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        final ItemSettingsCheckboxBinding binding = DataBindingUtil.bind(LayoutInflater.from(getContext()).inflate(R.layout.item_settings_checkbox, null));
        addView(binding.getRoot());

        final TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.ItemSettingsCheckBoxRow,
                0, 0);
        try {
            final int titleRes = a.getResourceId(R.styleable.ItemSettingsCheckBoxRow_ISCBR_labelText, -1);
            if (titleRes != -1) {
                binding.itemText.setText(titleRes);
            } else {
                binding.itemText.setText(null);
            }
        } finally {
            a.recycle();
        }

    }

}
