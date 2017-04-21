package is.hello.sense.flows.home.ui.adapters;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.voice.VoiceCommandResponse;
import is.hello.sense.api.model.v2.voice.VoiceCommandTopic;
import is.hello.sense.databinding.ItemVoiceCommandBinding;
import is.hello.sense.ui.adapter.ArrayRecyclerAdapter;
import is.hello.sense.ui.widget.WelcomeCard;


public class VoiceCommandsAdapter extends ArrayRecyclerAdapter<VoiceCommandTopic, ArrayRecyclerAdapter.ViewHolder> {
    private static final int VIEW_ERROR = 0;
    private static final int VIEW_ITEM = 1;
    private static final int VIEW_WELCOME = 2;

    private final Picasso picasso;
    private final int imageSize;

    private boolean hasError = false;
    private View.OnClickListener welcomeCardListener = null;


    public VoiceCommandsAdapter(@NonNull final Context context,
                                @NonNull final Picasso picasso) {
        super(new ArrayList<>());
        this.picasso = picasso;
        this.imageSize = context.getResources().getDimensionPixelSize(R.dimen.x4);
    }

    @Override
    public int getItemCount() {
        if (this.hasError) {
            return 1;
        }
        return super.getItemCount() + (showWelcomeCard()? 0 : 1);
    }

    @Override
    public int getItemViewType(final int position) {
        if (this.hasError) {
            return VIEW_ERROR;
        }

        if (showWelcomeCard() && position == 0) {
            return VIEW_WELCOME;
        }
        return VIEW_ITEM;
    }

    @Override
    public VoiceCommandTopic getItem(final int position) {
        if (this.welcomeCardListener != null && position == 0) {
            return null;
        }

        return super.getItem(position - (showWelcomeCard() ? 0 : 1));
    }


    @Override
    public ArrayRecyclerAdapter.ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        switch (viewType) {
            case VIEW_ERROR:
                return new ErrorViewHolder(parent);
            case VIEW_WELCOME:
                return new WelcomeCardViewHolder(new WelcomeCard(parent.getContext()));
            case VIEW_ITEM:
                return new ItemViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_voice_command, parent, false));
            default:
                throw new IllegalStateException("Unexpected Voice Command type");
        }
    }

    @Override
    public void onBindViewHolder(final ArrayRecyclerAdapter.ViewHolder holder, final int position) {
        holder.bind(position);
    }

    public void showWelcomeCard(@Nullable final View.OnClickListener welcomeCardListener) {
        this.welcomeCardListener = welcomeCardListener;
        this.notifyDataSetChanged();
    }

    public void showError() {
        // if it already has an error return.
        if (this.hasError) {
            return;
        }
        // If there are voice commands don't show the error.
        if (getItemCount() > 1) {
            return;
        }
        if (!showWelcomeCard() && getItemCount() > 0) {
            return;
        }
        this.hasError = true;
        notifyDataSetChanged();
    }

    private boolean showWelcomeCard() {
        return this.welcomeCardListener != null;
    }

    public void add(@NonNull final VoiceCommandResponse voiceCommandResponse) {
        clear();
        this.hasError = false;
        addAll(voiceCommandResponse.getVoiceCommandTopics());
    }

    public abstract class BaseViewHolder extends ArrayRecyclerAdapter.ViewHolder {

        public BaseViewHolder(@NonNull final View itemView) {
            super(itemView);
        }
    }

    public class ItemViewHolder extends BaseViewHolder implements Target {
        private final ItemVoiceCommandBinding binding;

        public ItemViewHolder(@NonNull final View itemView) {
            super(itemView);
            this.binding = DataBindingUtil.bind(itemView);
        }

        @Override
        public void bind(final int position) {
            super.bind(position);
            final VoiceCommandTopic voiceCommandTopic = getItem(position);
            final String photoUrl = voiceCommandTopic.getMultiDensityImage()
                                                     .getUrl(this.binding.getRoot().getResources());
            VoiceCommandsAdapter.this.picasso.load(photoUrl)
                                             .resize(VoiceCommandsAdapter.this.imageSize,
                                                     VoiceCommandsAdapter.this.imageSize)
                                             .error(R.drawable.icon_voice_blueback)
                                             .into(this);
            this.binding.itemVoiceCommandTitle.setText(voiceCommandTopic.getTitle());
            this.binding.itemVoiceCommandBody.setText(voiceCommandTopic.getFirstCommand());
            this.itemView.setOnClickListener(v -> dispatchItemClicked(position, voiceCommandTopic));
            if (position == getItemCount() - 1) {
                this.binding.itemVoiceCommandDivider.setVisibility(View.INVISIBLE);
            }else {
                this.binding.itemVoiceCommandDivider.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onBitmapLoaded(final Bitmap bitmap,
                                   final Picasso.LoadedFrom from) {
            this.binding.itemVoiceCommandSpinner.setVisibility(View.INVISIBLE);
            this.binding.itemVoiceCommandImage.setVisibility(View.VISIBLE);
            this.binding.itemVoiceCommandImage.setImageBitmap(bitmap);
        }

        @Override
        public void onBitmapFailed(final Drawable errorDrawable) {
            this.binding.itemVoiceCommandSpinner.setVisibility(View.INVISIBLE);
            this.binding.itemVoiceCommandImage.setVisibility(View.VISIBLE);
            this.binding.itemVoiceCommandImage.setImageDrawable(errorDrawable);

        }

        @Override
        public void onPrepareLoad(final Drawable placeHolderDrawable) {
            this.binding.itemVoiceCommandSpinner.setVisibility(View.VISIBLE);
            this.binding.itemVoiceCommandImage.setVisibility(View.INVISIBLE);
            this.binding.itemVoiceCommandImage.setImageDrawable(null);
        }
    }

    private class WelcomeCardViewHolder extends BaseViewHolder {

        public WelcomeCardViewHolder(@NonNull final WelcomeCard itemView) {
            super(itemView);
            itemView.setContent(R.drawable.sense_with_voice,
                                R.string.welcome_to_voice_title,
                                R.string.welcome_to_voice_body);
            itemView.setOnCloseButtonListener(VoiceCommandsAdapter.this.welcomeCardListener);
        }
    }


}
