package is.hello.sense.ui.activities;

import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import is.hello.sense.R;
import is.hello.sense.ui.common.InjectionActivity;
import is.hello.sense.ui.widget.SpinnerImageView;
import is.hello.sense.util.ListObject;
import is.hello.sense.util.ListObject.ListItem;
import is.hello.sense.util.Player;

public class ListActivity extends InjectionActivity implements Player.OnEventListener {

    private enum PlayerStatus {
        Idle, Loading, Playing
    }

    private static final int NONE = -1;
    private static final String ARG_TITLE = ListActivity.class.getName() + ".ARG_TITLE";
    private static final String ARG_REQUEST_CODE = ListActivity.class.getName() + ".ARG_REQUEST_CODE";
    private static final String ARG_SELECTED_ID = ListActivity.class.getName() + ".ARG_SELECTED_ID";
    private static final String ARG_SELECTED_IDS = ListActivity.class.getName() + ".ARG_SELECTED_IDS";
    private static final String ARG_WANTS_PLAYER = ListActivity.class.getName() + ".ARG_WANTS_PLAYER";
    private static final String ARG_MULTIPLE_OPTIONS = ListActivity.class.getName() + ".ARG_MULTIPLE_OPTIONS";
    private static final String ARG_LIST_OBJECT = ListActivity.class.getName() + ".ARG_LIST_OBJECT";
    public static final String VALUE_ID = ListActivity.class.getName() + ".VALUE_ID";

    private ListAdapter listAdapter;
    private int requestedSoundId = NONE;
    private Player player;
    private PlayerStatus playerStatus = PlayerStatus.Idle;
    private SelectionTracker selectionTracker = new SelectionTracker();

    /**
     * Display with radios.
     *
     * @param fragment    Calling fragment for reference.
     * @param requestCode Request code on finish.
     * @param title       Title to display.
     * @param selectedId  Initial ID value to select.
     * @param wantsPlayer Will display the play icon and have a player.  Must implement interface.
     */
    public static void startActivityForResult(final @NonNull Fragment fragment,
                                              final int requestCode,
                                              final @StringRes int title,
                                              final int selectedId,
                                              final @NonNull ListObject listObject,
                                              final boolean wantsPlayer) {
        Intent intent = new Intent(fragment.getActivity(), ListActivity.class);
        intent.putExtra(ARG_REQUEST_CODE, requestCode);
        intent.putExtra(ARG_TITLE, title);
        intent.putExtra(ARG_SELECTED_ID, selectedId);
        intent.putExtra(ARG_WANTS_PLAYER, wantsPlayer);
        intent.putExtra(ARG_LIST_OBJECT, listObject);
        intent.putExtra(ARG_MULTIPLE_OPTIONS, false);
        fragment.startActivityForResult(intent, requestCode);
    }

    /**
     * Display with checkboxes. Not supporting player. Yet
     *
     * @param fragment    Calling fragment for reference.
     * @param requestCode Request code on finish.
     * @param title       Title to display.
     * @param selectedIds Initial ID values to select.
     */
    public static void startActivityForResult(final @NonNull Fragment fragment,
                                              final int requestCode,
                                              final @StringRes int title,
                                              final @NonNull ArrayList<Integer> selectedIds,
                                              final @NonNull ListObject listObject) {
        Intent intent = new Intent(fragment.getActivity(), ListActivity.class);
        intent.putExtra(ARG_REQUEST_CODE, requestCode);
        intent.putExtra(ARG_TITLE, title);
        intent.putIntegerArrayListExtra(ARG_SELECTED_IDS, selectedIds);
        intent.putExtra(ARG_WANTS_PLAYER, false);
        intent.putExtra(ARG_LIST_OBJECT, listObject);
        intent.putExtra(ARG_MULTIPLE_OPTIONS, true);
        fragment.startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        final Intent intent = getIntent();
        if (intent == null) {
            finish();
            return;
        }

        final @StringRes int title = intent.getIntExtra(ARG_TITLE, -1);
        final ListObject listObject = (ListObject) intent.getSerializableExtra(ARG_LIST_OBJECT);
        final boolean wantsPlayer = intent.getBooleanExtra(ARG_WANTS_PLAYER, false);
        final boolean multipleOptions = intent.getBooleanExtra(ARG_MULTIPLE_OPTIONS, false);

        selectionTracker.setMultiple(multipleOptions);
        if (multipleOptions) {
            selectionTracker.trackSelection(intent.getIntegerArrayListExtra(ARG_SELECTED_IDS));
        } else {
            selectionTracker.trackSelection(intent.getIntExtra(ARG_SELECTED_ID, -1));
        }
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(ARG_SELECTED_ID)) {
                selectionTracker.trackSelection(savedInstanceState.getInt(ARG_SELECTED_ID));
            }
            if (savedInstanceState.containsKey(ARG_SELECTED_IDS)) {
                selectionTracker.trackSelection(savedInstanceState.getIntegerArrayList(ARG_SELECTED_IDS));
            }
        }

        if (wantsPlayer) {
            player = new Player(this, this, null);
        }
        findViewById(R.id.item_section_title_divider).setVisibility(View.GONE);
        final TextView titleTextView = (TextView) findViewById(R.id.item_section_title_text);
        final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.activity_list_recycler);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        listAdapter = new ListAdapter(listObject, wantsPlayer);

        titleTextView.setText(title);

        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(null);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(listAdapter);
    }

    @Override
    public void onBackPressed() {
        setResultAndFinish();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        setResultAndFinish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.stopPlayback();
            player.recycle();
        }
        listAdapter = null;
        selectionTracker = null;
    }

    @Override
    protected void onSaveInstanceState(final @NonNull Bundle outState) {
        selectionTracker.writeSaveInstanceState(outState);
    }


    @Override
    public void onPlaybackReady(@NonNull Player player) {
    }

    @Override
    public void onPlaybackStarted(final @NonNull Player player) {
        if (selectionTracker.contains(requestedSoundId)) {
            playerStatus = PlayerStatus.Playing;
            notifyAdapter();
        } else {
            player.stopPlayback();
        }
    }

    @Override
    public void onPlaybackStopped(final @NonNull Player player, final boolean finished) {
        playerStatus = PlayerStatus.Idle;
        notifyAdapter();
    }

    @Override
    public void onPlaybackError(final @NonNull Player player, final @NonNull Throwable error) {
        playerStatus = PlayerStatus.Idle;
        notifyAdapter();
    }

    private void setResultAndFinish() {
        final Intent intent = new Intent();
        selectionTracker.writeValue(intent);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void notifyAdapter() {
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    private class ListAdapter extends RecyclerView.Adapter<BaseViewHolder> {
        final ListObject listObject;
        final boolean wantsPlayer;

        public ListAdapter(final @NonNull ListObject listObject, boolean wantsPlayer) {
            this.listObject = listObject;
            this.wantsPlayer = wantsPlayer;
        }

        @Override
        public BaseViewHolder onCreateViewHolder(final @NonNull ViewGroup parent, final int viewType) {
            if (wantsPlayer) {
                return new PlayerViewHolder(getLayoutInflater().inflate(R.layout.item_list, null));
            }
            return new BaseViewHolder(getLayoutInflater().inflate(R.layout.item_list, null));

        }

        @Override
        public void onBindViewHolder(final @NonNull BaseViewHolder holder, final int position) {
            final ListItem item = listObject.getListItems().get(position);
            holder.bind(item);
        }

        @Override
        public int getItemCount() {
            return listObject.getListItems().size();
        }
    }

    public class BaseViewHolder extends RecyclerView.ViewHolder {
        protected final TextView title;
        protected final SpinnerImageView image;
        protected final View view;

        @DrawableRes
        protected final int onImage;

        @DrawableRes
        protected final int offImage;


        BaseViewHolder(final @NonNull View view) {
            super(view);
            this.view = view;
            this.title = (TextView) view.findViewById(R.id.item_list_name);
            this.image = (SpinnerImageView) view.findViewById(R.id.item_list_play_image);

            if (selectionTracker.isMultiple) {
                this.onImage = R.drawable.holo_check_on;
                this.offImage = R.drawable.holo_check_off;
            } else {
                this.onImage = R.drawable.radio_on;
                this.offImage = R.drawable.radio_off;
            }

        }

        protected void selectedState() {
            title.setCompoundDrawablesWithIntrinsicBounds(onImage, 0, 0, 0);
        }

        public void unselectedState() {
            title.setCompoundDrawablesWithIntrinsicBounds(offImage, 0, 0, 0);
        }

        public String getTitle() {
            return title.getText().toString();
        }

        public void bind(final @NonNull ListItem item) {
            final int itemId = item.getId();
            title.setText(item.getName());
            if (selectionTracker.contains(itemId)) {
                selectedState();
            } else {
                unselectedState();
            }

            title.setOnClickListener(v -> {
                if (selectionTracker.contains(itemId) && !selectionTracker.isMultiple) {
                    return;
                }
                selectionTracker.trackSelection(itemId);
                notifyAdapter();
                if (!selectionTracker.isMultiple) {
                    setResultAndFinish();
                }
            });
        }
    }

    public class PlayerViewHolder extends BaseViewHolder {

        @DrawableRes
        private final static int playIcon = R.drawable.sound_preview_play;

        @DrawableRes
        private final static int loadingIcon = R.drawable.sound_preview_loading;

        @DrawableRes
        private final static int stopIcon = R.drawable.sound_preview_stop;

        PlayerViewHolder(@NonNull View view) {
            super(view);
            image.stopSpinning();
        }

        @Override
        public void bind(final @NonNull ListItem item) {
            final int itemId = item.getId();
            title.setText(item.getName());
            if (selectionTracker.contains(itemId)) {
                image.setVisibility(View.VISIBLE);
                selectedState();
                switch (playerStatus) {
                    case Idle:
                        enterIdleState(item);
                        break;
                    case Playing:
                        enterPlayingState();
                }
            } else {
                unselectedState();
                image.setVisibility(View.INVISIBLE);
                image.stopSpinning();
            }

            title.setOnClickListener(v -> {
                if (selectionTracker.contains(itemId)) {
                    return;
                }
                player.stopPlayback();
                selectionTracker.trackSelection(itemId);
                notifyAdapter();
            });
        }

        private void enterIdleState(final @NonNull ListItem item) {
            image.setOnClickListener(v -> {
                requestedSoundId = item.getId();
                playerStatus = PlayerStatus.Loading;
                player.setDataSource(Uri.parse(item.getPreviewUrl()), true);
                enterLoadingState();
            });
            image.setImageResource(playIcon);
            image.stopSpinning();
            image.setRotation(0);
        }

        private void enterPlayingState() {
            image.setOnClickListener(v -> {
                player.stopPlayback();
                playerStatus = PlayerStatus.Idle;
                notifyAdapter();
                image.setOnClickListener(null);

            });
            image.setImageResource(stopIcon);
            image.stopSpinning();
            image.setRotation(0);
        }

        private void enterLoadingState() {
            image.setOnClickListener(null);
            image.setImageResource(loadingIcon);
            image.startSpinning();
        }

    }

    private class SelectionTracker {
        private boolean isMultiple = false;
        private boolean isSet = false;
        private final ArrayList<Integer> selectionIds = new ArrayList<>();

        public SelectionTracker() {
            selectionIds.add(NONE);
        }

        private void setMultiple(boolean isMultiple) {
            if (isSet) {
                return;
            }
            isSet = true;
            this.isMultiple = isMultiple;
        }

        public boolean contains(final int id) {
            if (isMultiple) {
                return selectionIds.contains(id);
            }
            return id == selectionIds.get(0);
        }

        public Bundle writeSaveInstanceState(final @NonNull Bundle bundle) {
            if (isMultiple) {
                bundle.putIntegerArrayList(ARG_SELECTED_IDS, selectionIds);
            } else {
                bundle.putInt(ARG_SELECTED_ID, selectionIds.get(0));
            }
            return bundle;
        }

        public Intent writeValue(final @NonNull Intent intent) {
            if (isMultiple) {
                intent.putExtra(VALUE_ID, selectionIds);
            } else {
                intent.putExtra(VALUE_ID, selectionIds.get(0));
            }
            return intent;
        }

        public void trackSelection(int id) {
            if (isMultiple) {
                if (!selectionIds.contains(id)) {
                    selectionIds.add(id);
                } else {
                    selectionIds.remove((Integer) id);
                }
            } else {
                selectionIds.set(0, id);
            }
        }

        public void trackSelection(ArrayList<Integer> ids) {
            if (isMultiple) {
                selectionIds.clear();
                selectionIds.addAll(ids);
            }
        }
    }

}
