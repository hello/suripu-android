package is.hello.sense.ui.fragments.support;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;

import java.util.ArrayList;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.SupportTopic;
import is.hello.sense.ui.adapter.SupportTopicAdapter;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;

public class ContactTopicFragment extends InjectionFragment implements AdapterView.OnItemClickListener {
    @Inject ApiService apiService;

    private ProgressBar loadingIndicator;
    private SupportTopicAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.list_view_static, container, false);

        this.loadingIndicator = (ProgressBar) view.findViewById(R.id.list_view_static_loading);

        ListView listView = (ListView) view.findViewById(android.R.id.list);
        listView.setOnItemClickListener(this);

        this.adapter = new SupportTopicAdapter(getActivity());
        listView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadingIndicator.setVisibility(View.VISIBLE);
        bindAndSubscribe(apiService.supportTopics(),
                this::bindSupportTopics,
                this::supportTopicsUnavailable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        this.loadingIndicator = null;
        this.adapter = null;
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        SupportTopic topic = (SupportTopic) parent.getItemAtPosition(position);
        ContactSubmitFragment contactSubmitFragment = ContactSubmitFragment.newInstance(topic);
        ((FragmentNavigation) getActivity()).pushFragmentAllowingStateLoss(contactSubmitFragment,
                topic.displayName, true);
    }


    public void bindSupportTopics(@NonNull ArrayList<SupportTopic> topics) {
        loadingIndicator.setVisibility(View.GONE);

        adapter.clear();
        adapter.addAll(topics);
    }

    public void supportTopicsUnavailable(Throwable e) {
        loadingIndicator.setVisibility(View.GONE);
        adapter.clear();

        ErrorDialogFragment.presentError(getFragmentManager(), e);
    }
}
