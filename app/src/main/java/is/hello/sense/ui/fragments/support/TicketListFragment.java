package is.hello.sense.ui.fragments.support;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;

import is.hello.sense.R;
import is.hello.sense.ui.common.InjectionFragment;

public class TicketListFragment extends InjectionFragment {

    private ProgressBar activityIndicator;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.list_view_static, container, false);

        this.activityIndicator = (ProgressBar) view.findViewById(R.id.list_view_static_loading);

        ListView listView = (ListView) view.findViewById(android.R.id.list);

        return view;
    }
}
