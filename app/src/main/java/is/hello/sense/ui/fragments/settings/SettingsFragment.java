package is.hello.sense.ui.fragments.settings;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.annotation.XmlRes;

import is.hello.sense.ui.activities.SettingsActivity;

public class SettingsFragment extends PreferenceFragment {
    private static final String ARG_XML_RES = SettingsFragment.class.getName() + ".ARG_XML_RES";

    public static SettingsFragment newInstance(@XmlRes int prefsRes) {
        SettingsFragment fragment = new SettingsFragment();

        Bundle arguments = new Bundle();
        arguments.putInt(ARG_XML_RES, prefsRes);
        fragment.setArguments(arguments);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int prefsRes = getArguments().getInt(ARG_XML_RES);
        addPreferencesFromResource(prefsRes);
    }
}
