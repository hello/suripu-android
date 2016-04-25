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

import java.io.Serializable;
import java.util.List;

import is.hello.sense.R;
import is.hello.sense.ui.common.InjectionActivity;
import is.hello.sense.ui.widget.SpinnerImageView;
import is.hello.sense.util.Player;

public class ListActivity extends InjectionActivity implements Player.OnEventListener {

    private enum PlayerStatus {
        Idle, Loading, Playing
    }

    private static final int NONE = -1;
    private static final String ARG_TITLE = ListActivity.class.getName() + ".ARG_TITLE";
    private static final String ARG_REQUEST_CODE = ListActivity.class.getName() + ".ARG_REQUEST_CODE";
    private static final String ARG_SELECTED_ID = ListActivity.class.getName() + ".ARG_SELECTED_ID";
    private static final String ARG_WANTS_PLAYER = ListActivity.class.getName() + ".ARG_WANTS_PLAYER";
    private static final String ARG_LIST_OBJECT = ListActivity.class.getName() + ".ARG_LIST_OBJECT";
    public static final String VALUE_ID = ListActivity.class.getName() + ".VALUE_ID";

    private ListAdapter listAdapter;
    private int selectedId = -1;
    private int requestedSoundId = NONE;
    private Player player;
    private PlayerStatus playerStatus = PlayerStatus.Idle;

    /**
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
                                              final boolean wantsPlayer,
                                              final @NonNull ListObject listObject) {
        Intent intent = new Intent(fragment.getActivity(), ListActivity.class);
        intent.putExtra(ARG_REQUEST_CODE, requestCode);
        intent.putExtra(ARG_TITLE, title);
        intent.putExtra(ARG_SELECTED_ID, selectedId);
        intent.putExtra(ARG_WANTS_PLAYER, wantsPlayer);
        intent.putExtra(ARG_LIST_OBJECT, listObject);
        fragment.startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        Intent intent = getIntent();
        if (intent == null) {
            finish();
            return;
        }

        final @StringRes int title = intent.getIntExtra(ARG_TITLE, -1);
        selectedId = intent.getIntExtra(ARG_SELECTED_ID, -1);
        final ListObject listObject = (ListObject) intent.getSerializableExtra(ARG_LIST_OBJECT);
        final boolean wantsPlayer = intent.getBooleanExtra(ARG_WANTS_PLAYER, false);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(ARG_SELECTED_ID)) {
                selectedId = savedInstanceState.getInt(ARG_SELECTED_ID);
            }
        }
        if (title == -1 || selectedId == -1 || listObject == null) {
            finish();
            return;
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
    public boolean onOptionsItemSelected(MenuItem item) {
        setResultAndFinish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        player.stopPlayback();
        player.recycle();
        listAdapter = null;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(ARG_SELECTED_ID, selectedId);
    }


    @Override
    public void onPlaybackReady(@NonNull Player player) {
    }

    @Override
    public void onPlaybackStarted(@NonNull Player player) {
        if (requestedSoundId == selectedId) {
            playerStatus = PlayerStatus.Playing;
            notifyAdapter();
        } else {
            player.stopPlayback();
        }
    }

    @Override
    public void onPlaybackStopped(@NonNull Player player, boolean finished) {
        playerStatus = PlayerStatus.Idle;
        notifyAdapter();
    }

    @Override
    public void onPlaybackError(@NonNull Player player, @NonNull Throwable error) {
        playerStatus = PlayerStatus.Idle;
        notifyAdapter();
    }

    private void setResultAndFinish() {
        final Intent intent = new Intent();
        intent.putExtra(VALUE_ID, selectedId);
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
        public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (wantsPlayer) {
                return new PlayerViewHolder(getLayoutInflater().inflate(R.layout.item_list, null));
            }
            return new BaseViewHolder(getLayoutInflater().inflate(R.layout.item_list, null));

        }

        @Override
        public void onBindViewHolder(BaseViewHolder holder, int position) {
            ListItem item = listObject.getListOptions().get(position);
            holder.bind(item);
        }

        @Override
        public int getItemCount() {
            return listObject.getListOptions().size();
        }
    }

    public class BaseViewHolder extends RecyclerView.ViewHolder {
        protected final TextView title;
        protected final SpinnerImageView image;
        protected final View view;

        BaseViewHolder(final @NonNull View view) {
            super(view);
            this.view = view;
            this.title = (TextView) view.findViewById(R.id.item_list_name);
            this.image = (SpinnerImageView) view.findViewById(R.id.item_list_play_image);

        }

        protected void selectedState() {
            title.setCompoundDrawablesWithIntrinsicBounds(R.drawable.radio_on, 0, 0, 0);
        }

        public void unselectedState() {
            title.setCompoundDrawablesWithIntrinsicBounds(R.drawable.radio_off, 0, 0, 0);
        }

        public String getTitle() {
            return title.getText().toString();
        }

        public void bind(ListItem item) {
            int itemId = item.getId();
            title.setText(item.getName());
            if (itemId == selectedId) {
                selectedState();
            } else {
                unselectedState();
            }

            title.setOnClickListener(v -> {
                if (selectedId == itemId) {
                    return;
                }
                selectedId = itemId;
                notifyAdapter();
                setResultAndFinish();
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
        public void bind(ListItem item) {
            int itemId = item.getId();
            title.setText(item.getName());
            if (itemId == selectedId) {
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
                if (selectedId == itemId) {
                    return;
                }
                player.stopPlayback();
                selectedId = itemId;
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


    public interface ListObject extends Serializable {
        List<? extends ListItem> getListOptions();
    }

    public interface ListItem {
        String getName();

        int getId();

        String getPreviewUrl();
    }
}
