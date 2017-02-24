package is.hello.sense.flows.accountsettings.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import is.hello.sense.flows.accountsettings.ui.fragments.AccountSettingsFragment;
import is.hello.sense.ui.activities.appcompat.FragmentNavigationActivity;

public class AccountSettingsActivity extends FragmentNavigationActivity {

    public static void startActivity(@NonNull final Context context) {
        context.startActivity(new Intent(context, AccountSettingsActivity.class));
    }

    //region FragmentNavigationActivity
    @Override
    protected boolean shouldInjectToMainGraphObject() {
        return false;
    }

    @Override
    protected void onCreateAction() {
        showAccountSettingsFragment();
    }
    //endregion

    //region methods
    private void showAccountSettingsFragment() {
        pushFragment(new AccountSettingsFragment(), "AccountSettingsFragment", false);
    }
    //endregion
}
