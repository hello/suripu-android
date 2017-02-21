package is.hello.sense.flows.accountsettings.ui.fragments;


import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.view.View;

import is.hello.sense.flows.accountsettings.presenters.AccountSettingsPresenter;
import is.hello.sense.mvp.fragments.PresenterFragment;
import is.hello.sense.ui.common.ProfileImageManager;
import is.hello.sense.util.Analytics;
import retrofit.mime.TypedFile;

public class AccountSettingsFragment extends PresenterFragment<AccountSettingsPresenter>
implements ProfileImageManager.Listener{

    @Override
    public AccountSettingsPresenter initializeSensePresenter() {
        return new AccountSettingsPresenter(this);
    }

    @Override
    public void onViewCreated(final View view,
                              final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getSensePresenter().updateAccount();
    }

    @Override
    public void onImportFromFacebook() {

    }

    @Override
    public void onFromCamera(@NonNull Uri imageUri) {

    }

    @Override
    public void onFromGallery(@NonNull Uri imageUri) {

    }

    @Override
    public void onRemove() {

    }

    @Override
    public void onImageCompressedSuccess(@NonNull TypedFile compressedImage, @NonNull Analytics.ProfilePhoto.Source source) {

    }

    @Override
    public void onImageCompressedError(@NonNull Throwable e, @StringRes int titleRes, @StringRes int messageRes) {

    }
}
