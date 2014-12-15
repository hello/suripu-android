package is.hello.sense.ui.fragments.settings;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.annotation.XmlRes;

public class StaticPreferencesFragment extends PreferenceFragment {
    private static final String ARG_XML_RES = StaticPreferencesFragment.class.getName() + ".ARG_XML_RES";

    public static Bundle getArguments(@XmlRes int prefsRes) {
        Bundle arguments = new Bundle();
        arguments.putInt(ARG_XML_RES, prefsRes);
        return arguments;
    }

    public static StaticPreferencesFragment newInstance(@XmlRes int prefsRes) {
        StaticPreferencesFragment fragment = new StaticPreferencesFragment();
        fragment.setArguments(getArguments(prefsRes));
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int prefsRes = getArguments().getInt(ARG_XML_RES);
        addPreferencesFromResource(prefsRes);
    }
}
