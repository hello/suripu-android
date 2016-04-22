package is.hello.sense.ui.activities;

;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;


import is.hello.sense.R;
import is.hello.sense.api.gson.Enums;
import is.hello.sense.api.model.ApiResponse;
import is.hello.sense.api.model.v2.SleepDurations;
import is.hello.sense.api.model.v2.SleepSoundStatus;
import is.hello.sense.api.model.v2.SleepSounds;
import is.hello.sense.ui.adapter.SleepSoundsListAdapter;
import is.hello.sense.ui.common.InjectionActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.recycler.DividerItemDecoration;


public class SleepSoundsListActivity extends InjectionActivity implements SleepSoundsListAdapter.Callback {
    public final static int SOUNDS_REQUEST_CODE = 1234;
    public final static int DURATION_REQUEST_CODE = 4321;
    public final static int VOLUME_REQUEST_CODE = 2143;
    public static final String VALUE_ID = SleepSoundsListActivity.class.getName() + ".VALUE_ID";

    private static final int NONE = -1;
    private static final String ARG_API_RESPONSE = SleepSoundsListActivity.class.getName() + ".ARG_API_RESPONSE";
    private static final String ARG_SELECTED_ID = SleepSoundsListActivity.class.getName() + ".ARG_SELECTED_ID";
    private static final String ARG_LIST_TYPE = SleepSoundsListActivity.class.getName() + ".ARG_LIST_TYPE";

    private SleepSoundsListAdapter adapter;


    public static void startActivityForResult(
            final @NonNull InjectionFragment fragment,
            final int selectedId,
            final @NonNull ApiResponse response,
            final @NonNull ListType type) {

        final Intent intent = new Intent(fragment.getActivity(), SleepSoundsListActivity.class);
        intent.putExtra(ARG_API_RESPONSE, response);
        intent.putExtra(ARG_SELECTED_ID, selectedId);
        intent.putExtra(ARG_LIST_TYPE, type);
        fragment.startActivityForResult(intent, type.requestCode);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        final Intent intent = getIntent();
        if (intent == null) {
            return;
        }
        final int selectedId;
        if (savedInstanceState != null && savedInstanceState.containsKey(ARG_SELECTED_ID)) {
            selectedId = savedInstanceState.getInt(ARG_SELECTED_ID);
        } else {
            selectedId = intent.getIntExtra(ARG_SELECTED_ID, NONE);
        }
        final ListType listType = (ListType) intent.getSerializableExtra(ARG_LIST_TYPE);
        ApiResponse apiResponse = (ApiResponse) intent.getSerializableExtra(ARG_API_RESPONSE);

        final int titleRes = listType.titleRes;
        final TextView title = (TextView) findViewById(R.id.item_section_title_text);
        title.setText(titleRes);
        findViewById(R.id.item_section_title_divider).setVisibility(View.GONE);
        final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.activity_list_recycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(null);

        final LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        if (apiResponse instanceof SleepSounds) {
            adapter = new SleepSoundsListAdapter.SoundsListAdapter(this, selectedId, (SleepSounds) apiResponse);
        } else if (apiResponse instanceof SleepDurations) {
            adapter = new SleepSoundsListAdapter.DurationsListAdapter(this, this, selectedId, (SleepDurations) apiResponse);
        } else if (apiResponse instanceof SleepSoundStatus) {
            adapter = new SleepSoundsListAdapter.VolumeListAdapter(this, this, selectedId, (SleepSoundStatus) apiResponse);
        } else {
            //todo display error or throw error.
            finish();
            return;
        }
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onDestroy() {
        adapter.destroy();
        adapter = null;
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        setResultAndFinish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        setResultAndFinish();
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(ARG_SELECTED_ID, adapter.getSelectedId());
        adapter.finish();
    }

    @Override
    public void setResultAndFinish() {
        adapter.finish();
        final Intent intent = new Intent();
        intent.putExtra(VALUE_ID, adapter.getSelectedId());
        setResult(RESULT_OK, intent);
        finish();
    }

    public enum ListType implements Enums.FromString {
        SLEEP_SOUNDS(R.string.list_activity_sound_title, SOUNDS_REQUEST_CODE),
        SLEEP_DURATIONS(R.string.list_activity_duration_title, DURATION_REQUEST_CODE),
        SLEEP_VOLUME(R.string.list_activity_volume_title, VOLUME_REQUEST_CODE);

        @StringRes
        public final int titleRes;
        public final int requestCode;

        ListType(final @StringRes int titleRes, int requestCode) {
            this.titleRes = titleRes;
            this.requestCode = requestCode;

        }
    }
}
