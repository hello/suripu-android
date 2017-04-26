package is.hello.sense.flows.voicecommands.ui.adapters;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.voice.VoiceCommandSubTopic;
import is.hello.sense.api.model.v2.voice.VoiceCommandTopic;
import is.hello.sense.ui.adapter.ArrayRecyclerAdapter;

public class VoiceCommandDetailAdapter extends ArrayRecyclerAdapter<VoiceCommandDetailAdapter.Item, VoiceCommandDetailAdapter.BaseViewHolder> {
    private static final int IMAGE_TYPE = 1;
    private static final int TOPIC_TITLE_TYPE = 2;
    private static final int SUBTOPIC_TITLE_TYPE = 3;
    private static final int COMMAND_TYPE = 4;

    private final Picasso picasso;
    private final int imageSize;
    private final int margin;
    private final int largeMargin;

    public VoiceCommandDetailAdapter(@NonNull final Context context,
                                     @NonNull final VoiceCommandTopic topic,
                                     @NonNull final Picasso picasso) {
        super(new ArrayList<>());
        this.picasso = picasso;
        this.imageSize = context.getResources().getDimensionPixelSize(R.dimen.x7);
        this.margin = context.getResources().getDimensionPixelSize(R.dimen.x2);
        this.largeMargin = context.getResources().getDimensionPixelSize(R.dimen.x4);
        add(new ImageItem(topic.getMultiDensityImage().getUrl(context.getResources())));
        add(new TopicTitleItem(topic.getTitle()));
        add(new CommandItem(topic.getDescription()));
        for (final VoiceCommandSubTopic subTopic : topic.getVoiceCommandSubTopics()) {
            add(new SubTopicTitleItem(subTopic.getCommandTitle()));
            for (final String command : subTopic.getCommands()) {
                add(new CommandItem(command));
            }
        }
    }

    @Override
    public int getItemViewType(final int position) {
        return getItem(position).getType();
    }

    @Override
    public int getItemCount() {
        return super.getItemCount();
    }

    @Override
    public BaseViewHolder onCreateViewHolder(final ViewGroup parent,
                                             final int viewType) {

        if (viewType == IMAGE_TYPE) {
            return new ImageViewHolder(parent);
        } else if (viewType == COMMAND_TYPE) {
            return new CommandTextViewHolder(parent);
        } else if (viewType == SUBTOPIC_TITLE_TYPE) {
            return new SubtopicTextViewHolder(parent);
        } else {
            return new TopicTextViewHolder(parent);
        }
    }

    @Override
    public void onBindViewHolder(final BaseViewHolder holder,
                                 final int position) {
        holder.bind(position);
    }

    // region Item's
    public static abstract class Item {
        protected final String value;

        public Item(@NonNull final String value) {
            this.value = value;
        }

        protected abstract int getType();

        @NonNull
        protected String getValue() {
            return this.value;
        }

    }

    private class ImageItem extends Item {
        private ImageItem(@NonNull final String value) {
            super(value);
        }

        @Override
        protected int getType() {
            return IMAGE_TYPE;
        }

    }

    private class TopicTitleItem extends Item {
        private TopicTitleItem(@NonNull final String value) {
            super(value);
        }

        @Override
        protected int getType() {
            return TOPIC_TITLE_TYPE;
        }

    }

    private class SubTopicTitleItem extends Item {
        private SubTopicTitleItem(@NonNull final String value) {
            super(value);
        }

        @Override
        protected int getType() {
            return SUBTOPIC_TITLE_TYPE;
        }

    }

    private class CommandItem extends Item {
        private CommandItem(@NonNull final String value) {
            super(value);
        }

        @Override
        protected int getType() {
            return COMMAND_TYPE;
        }

    }

    //endregion

    //region ViewHolders
    public abstract class BaseViewHolder extends ArrayRecyclerAdapter.ViewHolder {

        public BaseViewHolder(@NonNull final View itemView) {
            super(itemView);
        }
    }

    private class ImageViewHolder extends BaseViewHolder
            implements Target {
        private final ImageView imageView;
        private final ProgressBar progressBar;

        private ImageViewHolder(@NonNull final ViewGroup parent) {
            super(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_spinner_image, parent, false));
            this.imageView = (ImageView) this.itemView.findViewById(R.id.item_spinner_image_image);
            this.progressBar = (ProgressBar) this.itemView.findViewById(R.id.item_spinner_image_spinner);

            this.itemView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                                     ViewGroup.LayoutParams.WRAP_CONTENT));
            final RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(VoiceCommandDetailAdapter.this.imageSize,
                                                                                             VoiceCommandDetailAdapter.this.imageSize);
            layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
            layoutParams.topMargin = VoiceCommandDetailAdapter.this.margin;
            this.imageView.setLayoutParams(layoutParams);
        }

        @Override
        public void bind(final int position) {
            super.bind(position);
            VoiceCommandDetailAdapter.this.picasso.load(getItem(position).getValue())
                                                  .resize(VoiceCommandDetailAdapter.this.imageSize,
                                                          VoiceCommandDetailAdapter.this.imageSize)
                                                  .error(R.drawable.icon_voice_blueback)
                                                  .into(this);
        }

        @Override
        public void onBitmapLoaded(final Bitmap bitmap,
                                   final Picasso.LoadedFrom from) {
            this.progressBar.setVisibility(View.INVISIBLE);
            this.imageView.setVisibility(View.VISIBLE);
            this.imageView.setImageBitmap(bitmap);

        }

        @Override
        public void onBitmapFailed(final Drawable errorDrawable) {
            this.progressBar.setVisibility(View.INVISIBLE);
            this.imageView.setVisibility(View.VISIBLE);
            this.imageView.setImageDrawable(errorDrawable);

        }

        @Override
        public void onPrepareLoad(final Drawable placeHolderDrawable) {
            this.progressBar.setVisibility(View.VISIBLE);
            this.imageView.setVisibility(View.INVISIBLE);
            this.imageView.setImageDrawable(null);


        }
    }

    private abstract class TextViewHolder extends BaseViewHolder {
        protected final TextView textView;

        private TextViewHolder(@NonNull final View itemView) {
            super(itemView);
            this.textView = getTextView();
        }

        @Override
        public void bind(final int position) {
            final Item item = getItem(position);
            this.textView.setText(item.getValue());
        }

        protected abstract TextView getTextView();
    }

    private class CommandTextViewHolder extends TextViewHolder {
        private CommandTextViewHolder(@NonNull final ViewGroup parent) {
            super(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_textview_body1_secondary, parent, false));
        }

        @Override
        protected TextView getTextView() {
            return (TextView) this.itemView.findViewById(R.id.item_textview_body1_secondary_text);
        }
    }

    private class SubtopicTextViewHolder extends TextViewHolder {
        private SubtopicTextViewHolder(@NonNull final ViewGroup parent) {
            super(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_textview_body2_primary, parent, false));
            this.textView.setPadding(0, VoiceCommandDetailAdapter.this.largeMargin, 0, 0);
        }

        @Override
        protected TextView getTextView() {
            return (TextView) this.itemView.findViewById(R.id.item_textview_body2_primary_text);
        }
    }

    private class TopicTextViewHolder extends TextViewHolder {
        private TopicTextViewHolder(@NonNull final ViewGroup parent) {
            super(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_textview_title2_primary, parent, false));
            this.textView.setPadding(0, VoiceCommandDetailAdapter.this.margin, 0, 0);
        }

        @Override
        protected TextView getTextView() {
            return (TextView) this.itemView.findViewById(R.id.item_textview_title2_primary_text);
        }
    }
    //endregion

}
