package is.hello.sense.flows.home.ui.adapters;

import android.databinding.DataBindingUtil;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import is.hello.sense.R;
import is.hello.sense.api.gson.Enums;
import is.hello.sense.databinding.ItemVoiceCommandBinding;
import is.hello.sense.ui.adapter.ArrayRecyclerAdapter;
import is.hello.sense.ui.widget.WelcomeCard;


public class VoiceCommandsAdapter extends ArrayRecyclerAdapter<VoiceCommandsAdapter.VoiceCommand, VoiceCommandsAdapter.BaseViewHolder> {
    private static final int VIEW_ITEM = 1;
    private static final int VIEW_WELCOME = 2;
    private final LayoutInflater inflater;

    private View.OnClickListener welcomeCardListener = null;

    public VoiceCommandsAdapter(@NonNull final LayoutInflater inflater) {
        super(new ArrayList<>());
        this.inflater = inflater;
        super.add(VoiceCommand.ALARM);
        super.add(VoiceCommand.SLEEP);
        super.add(VoiceCommand.ROOM);
        super.add(VoiceCommand.EXPANSIONS);
    }

    @Override
    public int getItemCount() {
        return super.getItemCount() + (welcomeCardListener == null ? 0 : 1);
    }

    @Override
    public int getItemViewType(final int position) {
        if (welcomeCardListener != null && position == 0) {
            return VIEW_WELCOME;
        }

        return VIEW_ITEM;
    }

    @Override
    public VoiceCommand getItem(final int position) {
        if (welcomeCardListener != null && position == 0) {
            return null;
        }

        return super.getItem(position - (welcomeCardListener == null ? 0 : 1));
    }

    @Override
    public BaseViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        switch (viewType) {
            case VIEW_WELCOME:
                return new WelcomeCardViewHolder(new WelcomeCard(parent.getContext()));
            case VIEW_ITEM:
                return new ItemViewHolder(VoiceCommandsAdapter.this.inflater.inflate(R.layout.item_voice_command, parent, false));
            default:
                throw new IllegalStateException("Unexpected Voice Command type");
        }
    }

    @Override
    public void onBindViewHolder(final BaseViewHolder holder, final int position) {
        holder.bind(position);
    }


    public void showWelcomeCard(@Nullable final View.OnClickListener welcomeCardListener) {
        this.welcomeCardListener = welcomeCardListener;
        this.notifyDataSetChanged();
    }

    public abstract class BaseViewHolder extends ArrayRecyclerAdapter.ViewHolder {

        public BaseViewHolder(@NonNull final View itemView) {
            super(itemView);
        }
    }

    public class ItemViewHolder extends BaseViewHolder {
        private final ItemVoiceCommandBinding binding;

        public ItemViewHolder(@NonNull final View itemView) {
            super(itemView);
            this.binding = DataBindingUtil.bind(itemView);
        }

        @Override
        public void bind(final int position) {
            super.bind(position);
            final VoiceCommand voiceCommand = getItem(position);
            this.binding.itemVoiceCommandImage.setImageResource(voiceCommand.imageRes);
            this.binding.itemVoiceCommandTitle.setText(voiceCommand.titleRes);
            this.binding.itemVoiceCommandBody.setText(voiceCommand.bodyRes);
            this.itemView.setOnClickListener(v -> dispatchItemClicked(position, voiceCommand));
            if (position == getItemCount() - 1) {
                this.binding.itemVoiceCommandDivider.setVisibility(View.INVISIBLE);
            }
        }
    }

    private class WelcomeCardViewHolder extends BaseViewHolder {

        public WelcomeCardViewHolder(@NonNull final WelcomeCard itemView) {
            super(itemView);
            itemView.setContent(R.drawable.sense_with_voice,
                                R.string.welcome_to_voice_title,
                                R.string.welcome_to_voice_body);
            itemView.setOnCloseButtonListener(welcomeCardListener);
        }
    }

    public enum VoiceCommand implements Enums.FromString {

        ALARM(R.drawable.icon_sounds,
              R.string.voice_alarm_title,
              R.string.voice_alarm_message),
        SLEEP(R.drawable.icon_sleep,
              R.string.voice_sleep_title,
              R.string.voice_sleep_message),
        ROOM(R.drawable.icon_conditions,
             R.string.voice_rc_title,
             R.string.voice_rc_message),
        EXPANSIONS(R.drawable.icon_expansions,
                   R.string.voice_expansion_title,
                   R.string.voice_expansion_message);

        @DrawableRes
        private final int imageRes;

        @StringRes
        private final int titleRes;
        @StringRes
        private final int bodyRes;

        VoiceCommand(@DrawableRes final int imageRes,
                     @StringRes final int titleRes,
                     @StringRes final int bodyRes) {
            this.imageRes = imageRes;
            this.titleRes = titleRes;
            this.bodyRes = bodyRes;
        }

        public static VoiceCommand fromString(@Nullable final String string) {
            return Enums.fromString(string, values(), ALARM);
        }
    }
}
