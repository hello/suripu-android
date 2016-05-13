package is.hello.sense.ui.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import is.hello.sense.R;

public class ProfileImageView extends FrameLayout implements Target {

    private ImageButton plusButton;
    private ImageView profileImage;

    public ProfileImageView(Context context) {
        this(context, null);
    }

    public ProfileImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ProfileImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        final View view = LayoutInflater.from(context).inflate(R.layout.item_profile_picture,this,false);
        this.profileImage = (ImageView) view.findViewById(R.id.item_profile_picture_image);
        this.plusButton = (ImageButton) view.findViewById(R.id.item_profile_picture_button);

        this.addView(view);
    }

    public int getSizeDimen() {
        return R.dimen.profile_image_size;
    }

    @Override
    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
        profileImage.setImageDrawable(new BitmapDrawable(getResources(), bitmap));
    }

    @Override
    public void onBitmapFailed(Drawable errorDrawable) {

    }

    @Override
    public void onPrepareLoad(Drawable placeHolderDrawable) {
        profileImage.setImageDrawable(placeHolderDrawable);
    }
}
