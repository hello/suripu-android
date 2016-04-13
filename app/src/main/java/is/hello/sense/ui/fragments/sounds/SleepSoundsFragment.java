package is.hello.sense.ui.fragments.sounds;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import java.util.List;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.SleepSoundsState;
import is.hello.sense.graph.presenters.SleepSoundsStatePresenter;
import is.hello.sense.ui.adapter.SleepSoundsAdapter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.activities.ListActivity;
import is.hello.sense.ui.handholding.WelcomeDialogFragment;
import is.hello.sense.ui.recycler.DividerItemDecoration;
import is.hello.sense.ui.recycler.InsetItemDecoration;
import is.hello.sense.util.Constants;

public class SleepSoundsFragment extends InjectionFragment implements SleepSoundsAdapter.InteractionListener {
    private final static int SOUNDS_REQUEST_CODE = 1234;
    private final static int DURATION_REQUEST_CODE = 4321;

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private ImageButton playButton;
    private SleepSoundsAdapter adapter;

    @Inject
    SleepSoundsStatePresenter sleepSoundsStatePresenter;
    private SharedPreferences preferences;


    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_sleep_sounds, container, false);
        progressBar = (ProgressBar) view.findViewById(R.id.fragment_sleep_sounds_progressbar);
        playButton = (ImageButton) view.findViewById(R.id.fragment_sleep_sounds_playbutton);
        preferences = getActivity().getSharedPreferences(Constants.SLEEP_SOUNDS_PREFS, 0);
        this.recyclerView = (RecyclerView) view.findViewById(R.id.fragment_sleep_sounds_recycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(null);
        final Resources resources = getResources();
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        final InsetItemDecoration decoration = new InsetItemDecoration();
        decoration.addBottomInset(3, resources.getDimensionPixelSize(R.dimen.gap_smart_alarm_list_bottom));
        recyclerView.addItemDecoration(decoration);
        recyclerView.addItemDecoration(new DividerItemDecoration(resources));
        this.adapter = new SleepSoundsAdapter(getActivity(), preferences, this);
        recyclerView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindAndSubscribe(sleepSoundsStatePresenter.state, this::bind, this::presentError);
        sleepSoundsStatePresenter.update();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        recyclerView = null;
        progressBar = null;
        playButton = null;
        adapter = null;
        preferences = null;

    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            final String value = data.getStringExtra(ListActivity.VALUE_NAME);
            if (value == null) {
                return;
            }
            final String constant;
            if (requestCode == SOUNDS_REQUEST_CODE) {
                constant = Constants.SLEEP_SOUNDS_SOUND_NAME;
            } else {
                constant = Constants.SLEEP_SOUNDS_DURATION_NAME;
            }
            preferences.edit()
                       .putString(constant, value)
                       .apply();
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            final boolean flickerWorkAround = true;
            WelcomeDialogFragment.showIfNeeded(getActivity(), R.xml.welcome_dialog_sleep_sounds, flickerWorkAround);
        }
    }
    private void presentError(final @NonNull Throwable error) {
    }

    private void bind(final @NonNull SleepSoundsState state) {
        progressBar.setVisibility(View.GONE);
        adapter.bind(state.getStatus(), state.getSounds(), state.getDurations());
    }

    @Override
    public void onSoundClick(final @NonNull String currentSound, final @NonNull List<?> sounds) {
        ListActivity.startActivityForResult(
                this,
                SOUNDS_REQUEST_CODE,
                R.string.list_activity_sound_title,
                currentSound,
                sounds);
    }

    @Override
    public void onDurationClick(final @NonNull String currentDuration, final @NonNull List<?> durations) {
        ListActivity.startActivityForResult(
                this,
                DURATION_REQUEST_CODE,
                R.string.list_activity_duration_title,
                currentDuration,
                durations);
    }

}
