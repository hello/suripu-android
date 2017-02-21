package is.hello.sense.flows.accountsettings.interactors;


import android.support.annotation.NonNull;

import com.squareup.picasso.Picasso;

import javax.inject.Inject;

import is.hello.sense.interactors.AccountInteractor;
import is.hello.sense.interactors.FacebookInteractor;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.mvp.interactors.InteractorContainer;
import is.hello.sense.ui.common.ProfileImageManager;
import is.hello.sense.ui.common.SenseFragment;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.util.DateFormatter;

public class AccountSettingsInteractorContainer extends InteractorContainer {

    @Inject
    Picasso picasso;
    @Inject
    AccountInteractor accountInteractor;
    @Inject
    DateFormatter dateFormatter;
    @Inject
    UnitFormatter unitFormatter;
    @Inject
    PreferencesInteractor preferences;
    @Inject
    FacebookInteractor facebookInteractor;
    @Inject
    ProfileImageManager.Builder builder;

    public AccountSettingsInteractorContainer(@NonNull final SenseFragment senseFragment) {
        super(senseFragment);
    }

    @Override
    public void addInteractors() {
        addInteractor(accountInteractor);
        addInteractor(facebookInteractor);
    }

    public Picasso getPicasso() {
        return picasso;
    }

    public AccountInteractor getAccountInteractor() {
        return accountInteractor;
    }

    public DateFormatter getDateFormatter() {
        return dateFormatter;
    }

    public UnitFormatter getUnitFormatter() {
        return unitFormatter;
    }

    public PreferencesInteractor getPreferences() {
        return preferences;
    }

    public FacebookInteractor getFacebookInteractor() {
        return facebookInteractor;
    }

    public ProfileImageManager.Builder getBuilder() {
        return builder;
    }
}
