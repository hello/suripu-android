package is.hello.sense.ui.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.graph.presenters.SmartAlarmPresenter;
import is.hello.sense.ui.common.InjectionFragment;

public class SmartAlarmListFragment extends InjectionFragment {
    @Inject SmartAlarmPresenter smartAlarmPresenter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_smart_alarm_list, container, false);

        return view;
    }
}
