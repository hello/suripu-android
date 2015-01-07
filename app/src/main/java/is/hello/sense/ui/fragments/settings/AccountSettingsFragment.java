package is.hello.sense.ui.fragments.settings;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Account;
import is.hello.sense.graph.presenters.AccountPresenter;
import is.hello.sense.ui.adapter.StaticItemAdapter;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;

public class AccountSettingsFragment extends InjectionFragment implements AdapterView.OnItemClickListener {
    @Inject AccountPresenter accountPresenter;

    private StaticItemAdapter.Item nameItem;
    private StaticItemAdapter.Item emailItem;

    private @Nullable String accountEmail;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        accountPresenter.update();
        addPresenter(accountPresenter);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.simple_list_view, container, false);

        ListView listView = (ListView) view.findViewById(android.R.id.list);
        listView.setOnItemClickListener(this);

        StaticItemAdapter adapter = new StaticItemAdapter(getActivity());

        String placeholder = getString(R.string.missing_data_placeholder);
        this.nameItem = adapter.addItem(getString(R.string.label_name), placeholder);
        this.emailItem = adapter.addItem(getString(R.string.label_email), placeholder, this::changeEmail);
        adapter.addItem(getString(R.string.title_change_password), null, this::changePassword);

        listView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindAndSubscribe(accountPresenter.account, this::bindAccount, this::accountUnavailable);
    }


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        StaticItemAdapter.Item item = (StaticItemAdapter.Item) adapterView.getItemAtPosition(position);
        if (item.getAction() != null) {
            item.getAction().run();
        }
    }


    public void bindAccount(@NonNull Account account) {
        this.accountEmail = account.getEmail();

        nameItem.setValue(account.getName());
        emailItem.setValue(account.getEmail());
    }

    public void accountUnavailable(Throwable e) {
        ErrorDialogFragment.presentError(getFragmentManager(), e);
    }


    public void changeEmail() {
        FragmentNavigation navigation = (FragmentNavigation) getActivity();
        navigation.showFragment(new ChangeEmailFragment(), getString(R.string.title_change_email), true);
    }

    public void changePassword() {
        if (accountEmail != null) {
            FragmentNavigation navigation = (FragmentNavigation) getActivity();
            navigation.showFragment(ChangePasswordFragment.newInstance(accountEmail), getString(R.string.title_change_password), true);
        }
    }
}
