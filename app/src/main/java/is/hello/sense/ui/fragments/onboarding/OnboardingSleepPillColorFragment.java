package is.hello.sense.ui.fragments.onboarding;

import android.app.Fragment;
import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ToggleButton;

import is.hello.sense.R;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.widget.SelectorLinearLayout;

public class OnboardingSleepPillColorFragment extends Fragment implements SelectorLinearLayout.OnSelectionChangedListener {

    private SelectorLinearLayout colorSelector;
    private Button nextButton;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_sleep_pill_color, container, false);

        this.colorSelector = (SelectorLinearLayout) view.findViewById(R.id.fragment_onboarding_sleep_pill_colors);
        colorSelector.setOnSelectionChangedListener(this);
        populateSelector(inflater);

        this.nextButton = (Button) view.findViewById(R.id.fragment_onboarding_next);
        nextButton.setOnClickListener(ignored -> ((OnboardingActivity) getActivity()).showPairPill(colorSelector.getSelectedIndex()));

        return view;
    }

    private void populateSelector(@NonNull LayoutInflater inflater) {
        String[] names = getResources().getStringArray(R.array.sleep_pill_color_names);
        String[] values = getResources().getStringArray(R.array.sleep_pill_color_values);
        int intrinsicSize = getResources().getDimensionPixelSize(R.dimen.pill_color_size);
        for (int i = 0, count = names.length; i < count; i++) {
            String name = names[i];
            int color = Color.parseColor(values[i]);
            ShapeDrawable drawable = new ShapeDrawable(new OvalShape());
            drawable.setIntrinsicHeight(intrinsicSize);
            drawable.setIntrinsicWidth(intrinsicSize);
            drawable.getPaint().setColor(color);

            ToggleButton button = (ToggleButton) inflater.inflate(R.layout.item_sleep_pill_color, colorSelector, false);
            button.setTextOn(name);
            button.setTextOff(name);
            button.setText(name);
            button.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null);
            colorSelector.addView(button);
        }
    }


    @Override
    public void onSelectionChanged(int newSelectionIndex) {
        nextButton.setEnabled(newSelectionIndex != SelectorLinearLayout.NOTHING_SELECTED);
    }
}
