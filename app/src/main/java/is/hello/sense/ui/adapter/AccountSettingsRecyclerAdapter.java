package is.hello.sense.ui.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.picasso.Picasso;

import is.hello.sense.R;
import is.hello.sense.ui.widget.ProfileImageView;

public class AccountSettingsRecyclerAdapter extends SettingsRecyclerAdapter {

    private final Picasso picasso;

    public AccountSettingsRecyclerAdapter(@NonNull Context context, @NonNull Picasso picasso) {
        super(context);
        this.picasso = picasso;
    }

    @Override
    public int getItemViewType(int position) {
        if(getItem(position) instanceof CircleItem)
            return CircleItem.ID;
        return super.getItemViewType(position);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if(viewType == CircleItem.ID) {
            final View view = inflater.inflate(R.layout.item_settings_profile_picture, parent, false);
            return new CircleViewHolder(view);
        }
        return super.onCreateViewHolder(parent, viewType);
    }

    class CircleViewHolder extends ViewHolder<Item<String>> {
        final ProfileImageView imageView;

        CircleViewHolder(@NonNull View itemView) {
            super(itemView);
            this.imageView = (ProfileImageView) itemView.findViewById(R.id.item_profile_picture);
            this.imageView.addButtonListener(this);
            this.imageView.setOnClickListener(null);
        }

        @Override
        void bind(Item<String> item){
            if(item.value != null && !item.value.isEmpty()){
                picasso.load(item.value)
                       .centerCrop()
                       .resizeDimen(imageView.getSizeDimen(), imageView.getSizeDimen())
                       .noFade()
                       .into(imageView);
            } else {
                picasso.cancelRequest(imageView);
            }
        }
    }

    public static class CircleItem extends Item<String> {
        static final int ID = 4;
        public CircleItem(@Nullable Runnable onClick) {
            super(onClick);
        }
    }
}
