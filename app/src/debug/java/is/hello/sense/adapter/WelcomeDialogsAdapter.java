package is.hello.sense.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import is.hello.sense.R;
import is.hello.sense.debug.WelcomeDialogsActivity;
import is.hello.sense.ui.adapter.ArrayRecyclerAdapter;
import is.hello.sense.ui.handholding.WelcomeDialogFragment;

public class WelcomeDialogsAdapter extends ArrayRecyclerAdapter<WelcomeDialogsActivity.WelcomeDialogStringResource,
        WelcomeDialogsAdapter.ViewHolder> {
    private final Resources resources;
    private final LayoutInflater inflater;
    private final Context context;


    public WelcomeDialogsAdapter(@NonNull Context context, @NonNull List<WelcomeDialogsActivity.WelcomeDialogStringResource> dialogs) {
        super(dialogs);
        this.context = context;
        this.resources = context.getResources();
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view = inflater.inflate(R.layout.item_settings_detail, parent, false);
        return new DetailViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        WelcomeDialogsActivity.WelcomeDialogStringResource item = getItem(position);
        //noinspection unchecked
        holder.bind(item);
    }


    class DetailViewHolder extends ViewHolder<WelcomeDialogsActivity.WelcomeDialogStringResource> {
        final ImageView icon;
        final TextView title;
        final TextView detail;
        final View divider;

        DetailViewHolder(@NonNull View itemView) {
            super(itemView);

            this.icon = (ImageView) itemView.findViewById(R.id.item_settings_detail_icon);
            this.title = (TextView) itemView.findViewById(R.id.item_settings_detail_title);
            this.detail = (TextView) itemView.findViewById(R.id.item_settings_detail_detail);
            this.divider = itemView.findViewById(R.id.item_settings_detail_divider);
        }

        @Override
        void bind(WelcomeDialogsActivity.WelcomeDialogStringResource item) {
            title.setText(item.dialogName);
            detail.setText("");
            icon.setVisibility(View.GONE);
            if (getAdapterPosition() == getItemCount() - 1) {
                divider.setVisibility(View.GONE);
            } else {
                divider.setVisibility(View.VISIBLE);
            }
        }
    }

    abstract class ViewHolder<T extends WelcomeDialogsActivity.WelcomeDialogStringResource> extends RecyclerView.ViewHolder
            implements View.OnClickListener {
        ViewHolder(@NonNull View itemView) {
            super(itemView);

            itemView.setBackgroundResource(R.drawable.selectable_dark_bounded);
            itemView.setOnClickListener(this);
        }

        abstract void bind(T item);

        @Override
        public void onClick(View ignored) {
            final int adapterPosition = getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                WelcomeDialogFragment.markUnshown(context, getItem(adapterPosition).resource);
                remove(adapterPosition);
                notifyDataSetChanged();
            }
        }
    }
}
