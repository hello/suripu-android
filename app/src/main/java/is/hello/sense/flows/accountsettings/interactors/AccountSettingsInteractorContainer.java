package is.hello.sense.flows.accountsettings.interactors;


import com.squareup.picasso.Picasso;

import javax.inject.Inject;

import is.hello.sense.interactors.AccountInteractor;
import is.hello.sense.interactors.FacebookInteractor;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.mvp.interactors.InteractorContainer;
import is.hello.sense.ui.common.ProfileImageManager;
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

    @Override
    public void addInteractors() {
        addInteractor(accountInteractor);
        addInteractor(preferences);
        addInteractor(facebookInteractor);
    }
}
