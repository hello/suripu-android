package is.hello.sense.ui.fragments.onboarding;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import java.util.Locale;

import is.hello.sense.R;
import is.hello.sense.api.model.Account;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.AccountEditor;
import is.hello.sense.ui.common.SenseFragment;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.DateFormatter;

public class OnboardingRegisterBirthdayFragment extends SenseFragment {
    private static final int NUM_FIELDS = 3;
    private static final String LEADING_ZERO = "0";


    private final DateTime today = DateTime.now();

    private final TextView[] fields = new TextView[NUM_FIELDS];
    private final Validator[] fieldValidators = new Validator[NUM_FIELDS];
    private final int[] fieldLimits = new int[NUM_FIELDS];
    private final int[] minInsertZeroValues = new int[NUM_FIELDS];

    private int activeField = 0;
    private TextView monthText;
    private TextView dayText;
    private TextView yearText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null && getActivity() instanceof OnboardingActivity) {
            Analytics.trackEvent(Analytics.Onboarding.EVENT_BIRTHDAY, null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_onboarding_register_birthday, container, false);

        final LinearLayout fieldContainer = (LinearLayout) view.findViewById(R.id.fragment_onboarding_birthday_fields);
        final int hintColor = getResources().getColor(R.color.text_dim_placeholder);

        final char[] dateFormat = DateFormatter.getDateFormatOrder(getActivity());
        int index = 0;
        for (char field : dateFormat) {
            final TextView component = (TextView) inflater.inflate(R.layout.item_onboarding_birthday_field, fieldContainer, false);
            component.setHintTextColor(hintColor);

            if (field == 'd') {
                component.setHint(R.string.hint_day);

                this.fieldLimits[index] = 2;
                this.minInsertZeroValues[index] = 4;
                this.fieldValidators[index] = this::validateDay;

                this.dayText = component;
            } else if (field == 'M') {
                component.setHint(R.string.hint_month);

                this.fieldLimits[index] = 2;
                this.minInsertZeroValues[index] = 2;
                this.fieldValidators[index] = this::validateMonth;

                this.monthText = component;
            } else if (field == 'y') {
                component.setHint(R.string.hint_year);

                this.fieldLimits[index] = 4;
                this.minInsertZeroValues[index] = Integer.MAX_VALUE;
                this.fieldValidators[index] = this::validateYear;

                this.yearText = component;
            } else {
                continue;
            }

            if (index == NUM_FIELDS / 2) {
                final int margin = getResources().getDimensionPixelSize(R.dimen.gap_large);
                final ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) component.getLayoutParams();
                layoutParams.leftMargin = margin;
                layoutParams.rightMargin = margin;
            }
            fieldContainer.addView(component);
            fields[index] = component;

            if (++index >= NUM_FIELDS) {
                break;
            }
        }

        final Account account = AccountEditor.getContainer(this).getAccount();
        if (account.getBirthDate() != null) {
            final LocalDate birthDate = account.getBirthDate();
            monthText.setHint(String.format(Locale.getDefault(), "%02d", birthDate.getMonthOfYear()));
            dayText.setHint(String.format(Locale.getDefault(), "%02d", birthDate.getDayOfMonth()));
            yearText.setHint(String.format(Locale.getDefault(), "%04d", birthDate.getYear()));
        }


        final TableLayout keys = (TableLayout) view.findViewById(R.id.fragment_onboarding_register_birthday_keys);

        final Button skip = (Button) keys.findViewById(R.id.fragment_onboarding_register_birthday_skip);
        Views.setSafeOnClickListener(skip, this::skip);
        if (!AccountEditor.getWantsSkipButton(this)) {
            skip.setText(android.R.string.cancel);
        }

        final Button backspace = (Button) keys.findViewById(R.id.fragment_onboarding_register_birthday_delete);
        backspace.setOnClickListener(this::backspace);

        final View.OnClickListener appendNumber = this::appendNumber;
        for (int row = 0, rowCount = keys.getChildCount(); row < rowCount; row++) {
            TableRow rowLayout = (TableRow) keys.getChildAt(row);
            for (int column = 0, columnCount = rowLayout.getChildCount(); column < columnCount; column++) {
                Button columnButton = (Button) rowLayout.getChildAt(column);
                if ("-1".equals(columnButton.getTag())) {
                    continue;
                }

                columnButton.setOnClickListener(appendNumber);
            }
        }

        setActiveField(0);

        return view;
    }


    private int parseString(@NonNull CharSequence value) throws NumberFormatException {
        return Integer.valueOf(value.toString(), 10);
    }

    private boolean validateMonth(@NonNull CharSequence month) {
        if (month.equals(LEADING_ZERO)) {
            return true;
        }

        try {
            int value = parseString(month);
            return (value > 0 && value <= 12);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean validateDay(@NonNull CharSequence day) {
        if (day.equals(LEADING_ZERO)) {
            return true;
        }

        try {
            int value = parseString(day);
            return (value > 0 && value <= 31);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean validateYear(@NonNull CharSequence year) {
        try {
            int value = parseString(year);
            return (value > 0 && value <= today.getYear());
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean validateAll() {
        int year = parseString(yearText.getText());
        if (year == today.getYear()) {
            int month = parseString(monthText.getText());
            int day = parseString(dayText.getText());
            return (month <= today.getMonthOfYear() && day <= today.getDayOfMonth());
        } else {
            return (year < today.getYear());
        }
    }

    private void setActiveField(int activeField) {
        int activeColor = getResources().getColor(R.color.light_accent),
                inactiveColor = getResources().getColor(R.color.text_dark);
        for (int i = 0; i < fields.length; i++) {
            TextView field = fields[i];
            int padding = field.getPaddingTop();
            if (activeField == i) {
                field.setBackgroundResource(R.drawable.edit_text_background_focused);
                field.setTextColor(activeColor);
            } else {
                if (TextUtils.isEmpty(field.getText())) {
                    field.setBackgroundResource(R.drawable.edit_text_background_disabled);
                } else {
                    field.setBackgroundResource(R.drawable.edit_text_background_normal);
                }
                field.setTextColor(inactiveColor);
            }
            field.setPadding(padding, padding, padding, padding);
        }

        this.activeField = activeField;
    }

    private void decrementActiveField() {
        setActiveField(activeField - 1);
    }

    private void incrementActiveField() {
        setActiveField(activeField + 1);
    }

    private boolean shouldAddLeadingZero(@NonNull String newText) {
        return (newText.length() < 2 &&
                !newText.startsWith(LEADING_ZERO) &&
                parseString(newText) >= minInsertZeroValues[activeField]);
    }


    public void backspace(@NonNull View sender) {
        sender.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);

        TextView field = fields[activeField];
        if (TextUtils.isEmpty(field.getText())) {
            if (activeField == 0) {
                return;
            } else {
                decrementActiveField();

                field = fields[activeField];
            }
        }

        CharSequence text = field.getText();
        CharSequence textDelta = text.subSequence(0, text.length() - 1);
        field.setText(textDelta);
    }

    public void appendNumber(@NonNull View sender) {
        sender.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);

        String numberValue = sender.getTag().toString();
        TextView field = fields[activeField];

        CharSequence text = field.getText();
        if (text.length() == fieldLimits[activeField]) {
            return;
        }

        String newText = text + numberValue;
        if (fieldValidators[activeField].validate(newText)) {
            if (shouldAddLeadingZero(newText)) {
                newText = LEADING_ZERO + newText;
            }

            field.setText(newText);

            if (newText.length() == fieldLimits[activeField]) {
                if (activeField == NUM_FIELDS - 1) {
                    if (validateAll()) {
                        next();
                    } else {
                        field.setText(newText.substring(0, newText.length() - 1));
                    }
                } else {
                    incrementActiveField();
                }
            }
        }
    }

    public void skip(@NonNull View sender) {
        sender.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);

        Analytics.trackEvent(Analytics.Onboarding.EVENT_SKIP, Analytics.createProperties(Analytics.Onboarding.PROP_SKIP_SCREEN, "birthday"));
        AccountEditor.getContainer(this).onAccountUpdated(this);
    }

    public void next() {
        int year = parseString(yearText.getText());
        int month = parseString(monthText.getText());
        int day = parseString(dayText.getText());

        LocalDate dateWithoutDay = new LocalDate(year, month, 1);
        LocalDate date;
        if (day > dateWithoutDay.dayOfMonth().getMaximumValue()) {
            date = dateWithoutDay.withDayOfMonth(dateWithoutDay.dayOfMonth().getMaximumValue());
        } else {
            date = dateWithoutDay.withDayOfMonth(day);
        }

        final AccountEditor.Container container = AccountEditor.getContainer(this);
        container.getAccount().setBirthDate(date);
        container.onAccountUpdated(this);
    }


    private interface Validator {
        boolean validate(@NonNull CharSequence value);
    }
}
