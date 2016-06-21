package is.hello.sense.ui.widget;

import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
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
        this.context = context;
    }

    public void setLayoutInflater(@NonNull final LayoutInflater layoutInflater){
        this.layoutInflater = layoutInflater;
    }

    public Toast getCopiedToast(@NonNull final ViewGroup parent){
        return getToast(
                R.string.copied,
                R.drawable.check_mark_small, 0, 0, 0,
                parent);
    }

    public Toast getSharedToast(@NonNull final ViewGroup parent){
        return getToast(
                R.string.shared,
                0, R.drawable.check_mark_large, 0, 0,
                parent);
    }

    private Toast getToast(@StringRes final int textResId,
                           @DrawableRes final int drawableLeftResId,
                           @DrawableRes final int drawableTopResId,
                           @DrawableRes final int drawableRightResId,
                           @DrawableRes final int drawableBottomResId,
                           @NonNull final ViewGroup parent){
        final Toast toast = new Toast(context);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.setDuration(Toast.LENGTH_SHORT);

        if(layoutInflater != null){
            final TextView textView = (TextView) layoutInflater.inflate(R.layout.toast_text, parent, false);
            textView.setText(textResId);
            textView.setCompoundDrawablePadding(context.getResources().getDimensionPixelSize(R.dimen.gap_medium));
            textView.setCompoundDrawablesWithIntrinsicBounds(drawableLeftResId,
                                                             drawableTopResId,
                                                             drawableRightResId,
                                                             drawableBottomResId);
            toast.setView(textView);
        } else{
            Logger.warn(ToastFactory.class.getSimpleName(), "no layout inflater found.");
            toast.setText(textResId);
        }

        return toast;
    }
}
