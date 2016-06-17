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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.inject.Inject;

import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.ui.common.InjectionActivity;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.recycler.FadingEdgesItemDecoration;
import is.hello.sense.ui.widget.SpinnerImageView;
import is.hello.sense.util.IListObject;
import is.hello.sense.util.IListObject.IListItem;
import is.hello.sense.util.Logger;
import is.hello.sense.util.Player;
import is.hello.sense.util.SenseCache.AudioCache;

public class ListActivity extends InjectionActivity implements Player.OnEventListener {
    private static final String TAG = InjectionActivity.class.getName() + ".TAG";

    private enum PlayerStatus {
        Idle, Loading, Playing
    }

    private static final int NONE = -1;
    private static final int VIEW_TITLE = 1;
    private static final int VIEW_NOT_TITLE = 2;
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
    private boolean cancelled = false;
    private RecyclerView recyclerView;
    private
    @StringRes
    int titleRes;

    @Inject
    AudioCache audioCache;

    /**
     * Display with radios.
     *
     * @param fragment    Calling fragment for reference.
     * @param requestCode Request code on finish.
     * @param title       Title to display.
     * @param selectedId  Initial ID value to select.
     * @param wantsPlayer Will display the play icon and have a player.  Must implement interface.
     */
    public static void startActivityForResult(@NonNull final Fragment fragment,
                                              final int requestCode,
                                              @StringRes final int title,
                                              final int selectedId,
                                              @NonNull final IListObject IListObject,
                                              final boolean wantsPlayer) {
        final Intent intent = new Intent(fragment.getActivity(), ListActivity.class);
        intent.putExtra(ARG_REQUEST_CODE, requestCode);
        intent.putExtra(ARG_TITLE, title);
        intent.putExtra(ARG_SELECTED_ID, selectedId);
        intent.putExtra(ARG_WANTS_PLAYER, wantsPlayer);
        intent.putExtra(ARG_LIST_OBJECT, IListObject);
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
    public static void startActivityForResult(@NonNull final Fragment fragment,
                                              final int requestCode,
                                              @StringRes final int title,
                                              @NonNull final ArrayList<Integer> selectedIds,
                                              @NonNull final IListObject IListObject) {
        final Intent intent = new Intent(fragment.getActivity(), ListActivity.class);
        intent.putExtra(ARG_REQUEST_CODE, requestCode);
        intent.putExtra(ARG_TITLE, title);
        intent.putIntegerArrayListExtra(ARG_SELECTED_IDS, selectedIds);
        intent.putExtra(ARG_WANTS_PLAYER, false);
        intent.putExtra(ARG_LIST_OBJECT, IListObject);
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

        titleRes = intent.getIntExtra(ARG_TITLE, -1);
        final IListObject IListObject = (IListObject) intent.getSerializableExtra(ARG_LIST_OBJECT);
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
        recyclerView = (RecyclerView) findViewById(R.id.activity_list_recycler);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        listAdapter = new ListAdapter(IListObject, wantsPlayer);
        recyclerView.addItemDecoration(new FadingEdgesItemDecoration(layoutManager, getResources(),
                                                                     FadingEdgesItemDecoration.Style.ROUNDED_EDGES));
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
        if (isFinishing()) {
            audioCache.trimCache();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        selectionTracker.writeSaveInstanceState(outState);
    }


    @Override
    public void onPlaybackReady(@NonNull final Player player) {
    }

    @Override
    public void onPlaybackStarted(@NonNull final Player player) {
        if (selectionTracker.contains(requestedSoundId)) {
            playerStatus = PlayerStatus.Playing;
            notifyAdapter();
        } else {
            player.stopPlayback();
        }
    }

    @Override
    public void onPlaybackStopped(@NonNull final Player player, final boolean finished) {
        playerStatus = PlayerStatus.Idle;
        notifyAdapter();
    }

    @Override
    public void onPlaybackError(@NonNull final Player player, @NonNull final Throwable error) {
        playerStatus = PlayerStatus.Idle;
        notifyAdapter();
        showError();
    }

    private void setResultAndFinish() {
        cancelled = true;
        final Intent intent = new Intent();
        selectionTracker.writeValue(intent);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void notifyAdapter() {
        if (recyclerView == null) {
            return;
        }
        recyclerView.post(() -> {
            if (listAdapter != null) {
                listAdapter.notifyDataSetChanged();
            }
        });
    }

    private void showError() {
        if (recyclerView == null) {
            return;
        }
        recyclerView.post(() -> {
            final ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment.Builder()
                    .withMessage(StringRef.from(R.string.error_playing_sound))
                    .build();

            errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
        });
    }

    private boolean saveAudioToFile(@NonNull final File file, @NonNull final String urlLocation) {
        InputStream input = null;
        OutputStream output = null;
        HttpURLConnection connection = null;
        try {
            final URL url = new URL(urlLocation);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return false;
            }
            input = connection.getInputStream();
            output = new FileOutputStream(file);
            final byte data[] = new byte[4096];
            int count;
            while ((count = input.read(data)) != -1) {
                if (cancelled) {
                    return false;
                }
                output.write(data, 0, count);
            }
        } catch (final MalformedURLException e) {
            Logger.error(TAG, e.getLocalizedMessage());
            return false;
        } catch (final IOException e) {
            Logger.error(TAG, e.getLocalizedMessage());
            return false;
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
                if (output != null) {
                    output.close();
                }
            } catch (final IOException e) {
                Logger.error(TAG, e.getLocalizedMessage());
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
        return true;
    }


    private class ListAdapter extends RecyclerView.Adapter<BaseViewHolder> {
        final IListObject IListObject;
        final boolean wantsPlayer;

        public ListAdapter(@NonNull final IListObject IListObject, final boolean wantsPlayer) {
            this.IListObject = IListObject;
            this.wantsPlayer = wantsPlayer;
        }

        @Override
        public BaseViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            if (viewType != VIEW_TITLE) {
                if (wantsPlayer) {
                    return new PlayerViewHolder(getLayoutInflater().inflate(R.layout.item_list, parent, false));
                }
                return new SimpleViewHolder(getLayoutInflater().inflate(R.layout.item_list, parent, false));
            }
            return new TitleViewHolder(getLayoutInflater().inflate(R.layout.item_section_title, parent, false));

        }

        @Override
        public void onBindViewHolder(@NonNull final BaseViewHolder holder, final int position) {
            if (position > 0) {
                final IListItem item = IListObject.getListItems().get(position - 1);
                holder.bind(item);
            }
        }

        @Override
        public int getItemCount() {
            return IListObject.getListItems().size() + 1;
        }

        @Override
        public int getItemViewType(final int position) {
            return position == 0 ? VIEW_TITLE : VIEW_NOT_TITLE;
        }
    }

    public abstract class BaseViewHolder extends RecyclerView.ViewHolder {

        public BaseViewHolder(final View itemView) {
            super(itemView);
        }

        abstract void bind(@NonNull IListItem item);
    }

    public class TitleViewHolder extends BaseViewHolder {

        TitleViewHolder(@NonNull final View view) {
            super(view);
            final String title = getString(titleRes);
            ((TextView) view.findViewById(R.id.item_section_title_text)).setText(title.toUpperCase());
            view.findViewById(R.id.item_section_title_divider).setVisibility(View.GONE);
        }

        @Override
        void bind(@NonNull final IListItem item) {

        }
    }

    public class SimpleViewHolder extends BaseViewHolder {
        protected final TextView title;
        protected final TextView status;
        protected final SpinnerImageView image;
        protected final View view;

        @DrawableRes
        protected final int onImage;

        @DrawableRes
        protected final int offImage;


        SimpleViewHolder(@NonNull final View view) {
            super(view);
            this.view = view;
            this.title = (TextView) view.findViewById(R.id.item_list_name);
            this.image = (SpinnerImageView) view.findViewById(R.id.item_list_play_image);
            this.status = (TextView) view.findViewById(R.id.item_list_player_status);

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

        public void bind(@NonNull final IListItem item) {
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

    public class PlayerViewHolder extends SimpleViewHolder {

        @DrawableRes
        private final static int playIcon = R.drawable.sound_preview_play;

        @DrawableRes
        private final static int loadingIcon = R.drawable.sound_preview_loading;

        @DrawableRes
        private final static int stopIcon = R.drawable.sound_preview_stop;

        PlayerViewHolder(@NonNull final View view) {
            super(view);
            image.stopSpinning();
        }

        @Override
        public void bind(@NonNull final IListItem item) {
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
                status.setText(null);
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

        private void enterIdleState(@NonNull final IListItem item) {
            status.setText(R.string.preview);
            image.setOnClickListener(v -> {
                requestedSoundId = item.getId();
                new Thread(() -> {
                    final File file = audioCache.getCacheFile(item.getPreviewUrl());
                    final boolean saved;
                    if (file.exists()) {
                        saved = true;
                    } else {
                        view.post(() -> {
                            playerStatus = PlayerStatus.Loading;
                            enterLoadingState();
                        });
                        saved = saveAudioToFile(file, item.getPreviewUrl());
                    }
                    if (saved) {
                        player.setDataSource(Uri.fromFile(file), true, 1);
                    } else {
                        player.setDataSource(Uri.parse(item.getPreviewUrl()), true, 1);
                    }
                }).start();

            });

            image.setImageResource(playIcon);
            image.stopSpinning();
            image.setRotation(0);
        }

        private void enterPlayingState() {
            status.setText(R.string.stop);
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
            status.setText(null);
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

        private void setMultiple(final boolean isMultiple) {
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

        public Bundle writeSaveInstanceState(@NonNull final Bundle bundle) {
            if (isMultiple) {
                bundle.putIntegerArrayList(ARG_SELECTED_IDS, selectionIds);
            } else {
                bundle.putInt(ARG_SELECTED_ID, selectionIds.get(0));
            }
            return bundle;
        }

        public Intent writeValue(@NonNull final Intent intent) {
            if (isMultiple) {
                intent.putExtra(VALUE_ID, selectionIds);
            } else {
                intent.putExtra(VALUE_ID, selectionIds.get(0));
            }
            return intent;
        }

        public void trackSelection(final int id) {
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

        public void trackSelection(final ArrayList<Integer> ids) {
            if (isMultiple) {
                selectionIds.clear();
                selectionIds.addAll(ids);
            }
        }
    }

}
