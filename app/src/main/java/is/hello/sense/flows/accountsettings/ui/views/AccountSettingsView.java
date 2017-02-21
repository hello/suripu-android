package is.hello.sense.flows.accountsettings.ui.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.squareup.picasso.Picasso;

import is.hello.sense.R;
import is.hello.sense.api.model.Account;
import is.hello.sense.databinding.ViewAccountSettingsBinding;
import is.hello.sense.mvp.view.BindedSenseView;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.util.DateFormatter;

@SuppressLint("ViewConstructor")
public class AccountSettingsView extends BindedSenseView<ViewAccountSettingsBinding> {
    private final Picasso picasso;
    private final DateFormatter dateFormatter;
    private final UnitFormatter unitFormatter;

    public AccountSettingsView(@NonNull final Activity activity,
                               @NonNull final Picasso picasso,
                               @NonNull final DateFormatter dateFormatter,
                               @NonNull final UnitFormatter unitFormatter) {
        super(activity);
        this.picasso = picasso;
        this.dateFormatter = dateFormatter;
        this.unitFormatter = unitFormatter;

    }

    @Override
    protected int getLayoutRes() {
        return R.layout.view_account_settings;
    }

    @Override
    public void releaseViews() {
        setNameClickListener(null);
    }

    public void setPhoto(@Nullable final String url) {
        if (url == null || url.isEmpty()) {
            this.binding.viewAccountSettingsImage.setBackground(null);
        } else {
            this.picasso.load(url)
                        .centerCrop()
                        .resizeDimen(this.binding.viewAccountSettingsImage.getSizeDimen(),
                                     this.binding.viewAccountSettingsImage.getSizeDimen())
                        .into(this.binding.viewAccountSettingsImage);
        }

    }

    public void updateUiForAccount(@NonNull final Account account) {
        this.binding.viewAccountSettingsName.setValue(account.getFullName());
        this.binding.viewAccountSettingsEmail.setValue(account.getEmail());
        this.binding.viewAccountSettingsBirthday.setValue(account.getFullName());
        this.binding.viewAccountSettingsGender.setValue(getString(account.getGender().nameRes));

        final CharSequence weight = unitFormatter.formatWeight(account.getWeight());
        this.binding.viewAccountSettingsWeight.setValue(weight.toString());

        final CharSequence height = unitFormatter.formatHeight(account.getHeight());
        this.binding.viewAccountSettingsHeight.setValue(height.toString());
    }

    public void setNameClickListener(@Nullable final OnClickListener listener) {
        this.binding.viewAccountSettingsName.setOnClickListener(listener);
    }
}
