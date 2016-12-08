package is.hello.sense.flows.home.ui.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.mvp.view.PresenterView;
import is.hello.sense.ui.adapter.FooterRecyclerAdapter;
import is.hello.sense.ui.adapter.SettingsRecyclerAdapter;
import is.hello.sense.ui.fragments.settings.AccountSettingsFragment;
import is.hello.sense.ui.fragments.settings.NotificationsSettingsFragment;
import is.hello.sense.ui.fragments.settings.UnitSettingsFragment;
import is.hello.sense.ui.fragments.support.SupportFragment;
import is.hello.sense.ui.recycler.FadingEdgesItemDecoration;
import is.hello.sense.ui.recycler.InsetItemDecoration;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Distribution;

@SuppressLint("ViewConstructor")
public class AppSettingsView extends PresenterView {
    private final TextView version;
    private final SettingsRecyclerAdapter.TextItem expansionItem;
    private final SettingsRecyclerAdapter.TextItem voiceItem;
    private final RecyclerView recyclerView;
    private final SettingsRecyclerAdapter adapter;
    private final ProgressBar progressBar;

    public AppSettingsView(@NonNull final Activity activity,
                           @NonNull final RunnableGenerator generator,
                           @NonNull final View.OnClickListener devicesListener,
                           @NonNull final View.OnClickListener tellAFriendListener,
                           @NonNull final View.OnClickListener expansionsListener,
                           @NonNull final View.OnClickListener voiceListener) {
        super(activity);

        this.progressBar = (ProgressBar) findViewById(R.id.static_recycler_view_loading);
        this.recyclerView = (RecyclerView) findViewById(R.id.static_recycler_view);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
        this.recyclerView.setLayoutManager(layoutManager);
        this.recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(null);
        final Resources resources = getResources();
        final int verticalPadding = resources.getDimensionPixelSize(R.dimen.x1);
        final int sectionPadding = resources.getDimensionPixelSize(R.dimen.x4);
        final InsetItemDecoration decoration = new InsetItemDecoration();
        recyclerView.addItemDecoration(decoration);
        recyclerView.addItemDecoration(new FadingEdgesItemDecoration(layoutManager,
                                                                     resources,
                                                                     FadingEdgesItemDecoration.Style.STRAIGHT));
        show(false);
        adapter = new SettingsRecyclerAdapter(activity);
        decoration.addTopInset(adapter.getItemCount(), verticalPadding);

        this.adapter.add(new SettingsRecyclerAdapter.TextItem(context.getString(R.string.label_account),
                                                              generator.create(AccountSettingsFragment.class, R.string.label_account, true)));

        this.adapter.add(new SettingsRecyclerAdapter.TextItem(context.getString(R.string.label_devices),
                                                              () -> devicesListener.onClick(null)));

        this.adapter.add(new SettingsRecyclerAdapter.TextItem(context.getString(R.string.label_notifications),
                                                              generator.create(NotificationsSettingsFragment.class, R.string.label_notifications, false)));

        this.adapter.add(new SettingsRecyclerAdapter.TextItem(context.getString(R.string.label_units_and_time),
                                                              generator.create(UnitSettingsFragment.class, R.string.label_units_and_time, false)));

        this.expansionItem = new SettingsRecyclerAdapter.TextItem(context.getString(R.string.action_expansions),
                                                                  () -> expansionsListener.onClick(null));
        this.adapter.add(expansionItem);

        decoration.addBottomInset(adapter.getItemCount(), sectionPadding);

        this.voiceItem = new SettingsRecyclerAdapter.TextItem(context.getString(R.string.label_voice),
                                                              () -> voiceListener.onClick(null));
        this.adapter.add(voiceItem);

        showVoiceEnabledRows(false);

        this.adapter.add(new SettingsRecyclerAdapter.TextItem(context.getString(R.string.action_support),
                                                              generator.create(SupportFragment.class, R.string.action_support, false)));

        this.adapter.add(new SettingsRecyclerAdapter.TextItem(context.getString(R.string.label_tell_a_friend),
                                                              () -> tellAFriendListener.onClick(null)));

        this.version = (TextView) LayoutInflater.from(context).inflate(R.layout.item_app_settings_version_footer, recyclerView, false);
        this.version.setText(context.getString(R.string.app_version_fmt, getString(R.string.app_name), BuildConfig.VERSION_NAME));
        if (BuildConfig.DEBUG_SCREEN_ENABLED) {
            Views.setSafeOnClickListener(this.version, ignored -> Distribution.startDebugActivity((Activity) super.context));
        }

        this.recyclerView.setAdapter(new FooterRecyclerAdapter(adapter).addFooter(version)
                                                                       .setFlattenChanges(true));
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.static_recycler;
    }

    @Override
    public final void releaseViews() {
        adapter.clear();
        version.setOnClickListener(null);
    }

    public final void showVoiceEnabledRows(final boolean show) {
        show(true);
        if (show) {
            expansionItem.setVisible(true);
            voiceItem.setVisible(true);
        } else {
            expansionItem.setVisible(false);
            voiceItem.setVisible(false);
        }
    }

    public void show(final boolean isVisible){
        if(isVisible){
            this.progressBar.setVisibility(INVISIBLE);
            this.recyclerView.setVisibility(VISIBLE);
        } else {
            this.recyclerView.setVisibility(INVISIBLE);
            this.progressBar.setVisibility(VISIBLE);
        }
    }

    public interface RunnableGenerator {
        Runnable create(@NonNull final Class<? extends Fragment> fragmentClass,
                                    @StringRes
                                    final int titleRes,
                                    final boolean lockOrientation);
    }
}
