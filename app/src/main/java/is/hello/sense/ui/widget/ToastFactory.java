package is.hello.sense.ui.widget;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.DimenRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import is.hello.sense.R;
import is.hello.sense.util.Logger;

public class ToastFactory {
    private final Context context;
    private LayoutInflater layoutInflater;

    public ToastFactory(@NonNull final Context context) {
        this(context, null);
    }

    public ToastFactory(@NonNull final Context context, @Nullable final LayoutInflater layoutInflater) {
        this.context = context;
        this.layoutInflater = layoutInflater;
    }

    public void setLayoutInflater(@NonNull final LayoutInflater layoutInflater){
        this.layoutInflater = layoutInflater;
    }

    public Toast getCopiedToast(@NonNull final ViewGroup parent){
        return getToast(
                R.string.copied,
                R.drawable.check_mark_small, 0, 0, 0,
                R.dimen.x2, R.dimen.x2, R.dimen.x2,
                parent);
    }

    public Toast getSharedToast(@NonNull final ViewGroup parent){
        return getToast(
                R.string.shared,
                0, R.drawable.check_mark_large, 0, 0,
                R.dimen.x4, R.dimen.x3, R.dimen.x2,
                parent);
    }

    private Toast getToast(@StringRes final int textResId,
                           @DrawableRes final int drawableLeftResId,
                           @DrawableRes final int drawableTopResId,
                           @DrawableRes final int drawableRightResId,
                           @DrawableRes final int drawableBottomResId,
                           @DimenRes final int horizontalPaddingResId,
                           @DimenRes final int verticalPaddingResId,
                           @DimenRes final int drawablePaddingResId,
                           @NonNull final ViewGroup parent){
        if(layoutInflater != null){
            final Resources resources = context.getResources();
            final int horizontalPadding = resources.getDimensionPixelSize(horizontalPaddingResId);
            final int verticalPadding = resources.getDimensionPixelSize(verticalPaddingResId);
            final int drawablePadding = resources.getDimensionPixelSize(drawablePaddingResId);
            final TextView textView = (TextView) layoutInflater.inflate(R.layout.toast_text, parent, false);
            textView.setText(textResId);
            textView.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);
            textView.setCompoundDrawablePadding(drawablePadding);
            textView.setCompoundDrawablesWithIntrinsicBounds(drawableLeftResId,
                                                             drawableTopResId,
                                                             drawableRightResId,
                                                             drawableBottomResId);
            final Toast toast = new Toast(context);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.setView(textView);
            return toast;
        } else{
            Logger.warn(ToastFactory.class.getSimpleName(), "drawable not used because no layout inflater found.");
            return Toast.makeText(context,textResId, Toast.LENGTH_SHORT);
        }
    }
}
