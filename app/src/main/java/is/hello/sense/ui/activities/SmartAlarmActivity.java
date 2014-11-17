package is.hello.sense.ui.activities;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.MenuItem;

import is.hello.sense.R;
import is.hello.sense.ui.common.FragmentNavigationActivity;
import is.hello.sense.ui.fragments.SmartAlarmDetailFragment;
import is.hello.sense.ui.fragments.SmartAlarmListFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;

public class SmartAlarmActivity extends FragmentNavigationActivity {
    private boolean editing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setWantsTitleUpdates(false);
        if (savedInstanceState != null) {
            this.editing = savedInstanceState.getBoolean("editing", false);
        }

        if (savedInstanceState == null) {
            showFragment(new SmartAlarmListFragment(), getString(R.string.action_alarm), false);
        }

        updateActionBar();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("editing", editing);
    }

    @Override
    protected int getDefaultTitle() {
        return R.string.action_alarm;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, @NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home && editing) {
            SmartAlarmDetailFragment detailFragment = (SmartAlarmDetailFragment) getTopFragment();
            if (detailFragment != null) {
                detailFragment.saveAlarm();
                return true;
            }
        }
        return super.onMenuItemSelected(featureId, item);
    }

    public void beginEditing() {
        this.editing = true;
        updateActionBar();
    }

    public void finishEditing() {
        this.editing = false;
        updateActionBar();
    }

    @SuppressWarnings("ConstantConditions")
    public void updateActionBar() {
        if (editing) {
            getActionBar().setHomeAsUpIndicator(R.drawable.app_style_ab_done);
            SpannableString titleString = new SpannableString(getString(R.string.action_done));
            titleString.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.light_accent)),
                                0, titleString.length(),
                                Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            getActionBar().setTitle(titleString);
        } else {
            getActionBar().setHomeAsUpIndicator(R.drawable.app_style_ab_up);
            getActionBar().setTitle(R.string.action_alarm);
        }
    }


    @Override
    public void onBackPressed() {
        if (editing) {
            SenseAlertDialog backConfirmation = new SenseAlertDialog(this);
            backConfirmation.setTitle(R.string.dialog_title_smart_alarm_edit_cancel);
            backConfirmation.setMessage(R.string.dialog_message_smart_alarm_edit_cancel);
            backConfirmation.setPositiveButton(R.string.action_exit, (dialog, which) -> {
                super.onBackPressed();
                finishEditing();
            });
            backConfirmation.setNegativeButton(R.string.action_continue, null);
            backConfirmation.setDestructive(true);
            backConfirmation.show();
        } else {
            super.onBackPressed();
        }
    }
}
