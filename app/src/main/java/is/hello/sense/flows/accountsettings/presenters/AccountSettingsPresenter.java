package is.hello.sense.flows.accountsettings.presenters;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.fb.model.FacebookProfile;
import is.hello.sense.api.model.Account;
import is.hello.sense.flows.accountsettings.interactors.AccountSettingsInteractorContainer;
import is.hello.sense.flows.accountsettings.ui.views.AccountSettingsView;
import is.hello.sense.functional.Functions;
import is.hello.sense.mvp.presenters.SensePresenter;
import is.hello.sense.ui.common.AccountEditor;
import is.hello.sense.ui.common.ProfileImageManager;
import is.hello.sense.ui.common.SenseFragment;
import is.hello.sense.ui.fragments.settings.ChangeNameFragment;

public class AccountSettingsPresenter extends SensePresenter<AccountSettingsView, AccountSettingsInteractorContainer> {

    private final ProfileImageManager profileImageManager;

    public AccountSettingsPresenter(@NonNull final SenseFragment fragment) {
        super(fragment);
        getInteractorContainer().getFacebookInteractor().init();
        this.profileImageManager = getInteractorContainer().getBuilder().build(fragment);
        getSenseView().setNameClickListener((ignored) -> this.changeName(fragment));

    }

    @Override
    protected AccountSettingsInteractorContainer initializeInteractorContainer(@NonNull final SenseFragment fragment) {
        return new AccountSettingsInteractorContainer(fragment);
    }

    @Override
    protected AccountSettingsView initializeSenseView(@NonNull final Activity activity) {
        return new AccountSettingsView(activity,
                                       getInteractorContainer().getPicasso(),
                                       getInteractorContainer().getDateFormatter(),
                                       getInteractorContainer().getUnitFormatter());
    }

    @Override
    protected void release() {
        this.profileImageManager.hidePictureOptions();
    }

    @Override
    public void bindAndSubscribeAll() {
        bindAndSubscribe(getInteractorContainer().getAccountInteractor().subscriptionSubject,
                         this::bind,
                         Functions.LOG_ERROR);
        bindAndSubscribe(getInteractorContainer().getFacebookInteractor().subscriptionSubject,
                         this::bind,
                         Functions.LOG_ERROR);
    }

    @Override
    public void onActivityResult(final int requestCode,
                                 final int resultCode,
                                 final Intent data) {
        if (this.profileImageManager.onActivityResult(requestCode, resultCode, data)) {
            return;
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode,
                                           @NonNull final String[] permissions,
                                           @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        this.profileImageManager.onRequestPermissionResult(requestCode, permissions, grantResults);
    }

    public void updateAccount() {
        getInteractorContainer().getAccountInteractor().update();
    }

    public void updateFacebook() {
        getInteractorContainer().getFacebookInteractor().update();
    }

    public void bind(@NonNull final FacebookProfile profile) {
        Log.e(getClass().getSimpleName(), "Received facebook profile");
    }

    public void bind(@NonNull final Account account) {
        final String photoUrl = account.getProfilePhotoUrl(getContext().getResources());
        if (photoUrl.isEmpty()) {
            profileImageManager.removeDeleteOption();
        } else {
            profileImageManager.addDeleteOption();
        }
        getSenseView().updateUiForAccount(account);
    }

    public void changeName(@NonNull final SenseFragment targetFragment) {
        final ChangeNameFragment fragment = new ChangeNameFragment();
        fragment.setTargetFragment(targetFragment, 0x00);
        overlayFragmentAllowingStateLoss(fragment, targetFragment.getActivity().getFragmentManager(), getContext().getString(R.string.action_change_name), true);
    }


    public void overlayFragmentAllowingStateLoss(@NonNull final Fragment fragment,
                                                 @NonNull final FragmentManager fragmentManager,
                                                 @Nullable final String title,
                                                 final boolean wantsBackStackEntry) {
        final String tag = fragment.getClass().getSimpleName();
        final FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.add(R.id.activity_navigation_container, fragment, tag);
        if (wantsBackStackEntry) {
            transaction.setBreadCrumbTitle(title);
            transaction.addToBackStack(tag);
        }
        transaction.commitAllowingStateLoss();
    }

    public interface Listener {
        void onNameClick();
    }
}
