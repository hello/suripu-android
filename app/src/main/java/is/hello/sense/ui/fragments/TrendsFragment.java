package is.hello.sense.ui.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import is.hello.sense.R;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.widget.util.Styles;

public class TrendsFragment extends InjectionFragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_underside_list, container, false);

        ListView listView = (ListView) view.findViewById(android.R.id.list);
        Styles.addCardSpacingHeaderAndFooter(listView);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


    }

    @Override
    public void onResume() {
        super.onResume();


    }
}
