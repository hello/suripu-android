package is.hello.sense.flows.expansions.ui.views;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.Button;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.mvp.view.PresenterView;
import is.hello.sense.ui.adapter.ConfigurationAdapter;
import is.hello.sense.ui.recycler.DividerItemDecoration;
import is.hello.sense.ui.widget.util.Views;

public class ConfigSelectionView extends PresenterView{

    private final RecyclerView recyclerView;
    private final TextView titleTextView;
    private final TextView subtitleTextView;
    private final Button button;

    public ConfigSelectionView(@NonNull Activity activity, @NonNull final ConfigurationAdapter adapter) {
        super(activity);
        this.titleTextView = (TextView) findViewById(R.id.view_configuration_title);
        this.subtitleTextView = (TextView) findViewById(R.id.view_configuration_subtitle);
        this.button = (Button) findViewById(R.id.view_configuration_button);
        this.recyclerView = (RecyclerView) findViewById(R.id.view_configuration_rv);
        this.recyclerView.setHasFixedSize(true);
        this.recyclerView.setLayoutManager(new LinearLayoutManager(context));
        this.recyclerView.addItemDecoration(new DividerItemDecoration(context));
        this.recyclerView.setAdapter(adapter);
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.view_configuration_selection;
    }

    @Override
    public void releaseViews() {
        recyclerView.setAdapter(null);
        button.setOnClickListener(null);
    }

    public void setTitle(@Nullable final String title){
        this.titleTextView.setText(title);
    }

    public void setSubtitle(@Nullable final String subtitle){
        this.subtitleTextView.setText(subtitle);
    }

    public void setDoneButtonClickListener(@NonNull final OnClickListener listener) {
        Views.setSafeOnClickListener(this.button, listener);
    }

    public CharSequence getTitleText(){
        return this.titleTextView.getText();
    }

    public CharSequence getSubtitleText(){
        return this.subtitleTextView.getText();
    }
}
