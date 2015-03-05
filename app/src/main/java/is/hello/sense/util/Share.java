package is.hello.sense.util;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.io.OutputStream;

import is.hello.sense.R;
import is.hello.sense.functional.Functions;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;

public class Share {
    public static ImageAction image(@NonNull Bitmap bitmap) {
        return new ImageAction(bitmap);
    }

    public static EmailAction email(@NonNull String address) {
        return new EmailAction(address);
    }

    public static abstract class Action {
        protected final Intent intent;

        protected Action(@NonNull String action, @Nullable String mimeType) {
            this.intent = new Intent(action);
            intent.setType(mimeType);
        }

        public abstract boolean send(@NonNull Activity from);
    }

    public static class EmailAction extends Action {
        public EmailAction(@NonNull String emailAddress) {
            super(Intent.ACTION_SENDTO, null);

            intent.setData(Uri.fromParts("mailto", emailAddress, null));
        }

        public EmailAction withSubject(@NonNull String subject) {
            intent.putExtra(Intent.EXTRA_SUBJECT, subject);
            return this;
        }

        public EmailAction withBody(@NonNull String body) {
            intent.putExtra(Intent.EXTRA_TEXT, body);
            return this;
        }

        public EmailAction withAttachment(@NonNull Uri attachment) {
            intent.putExtra(Intent.EXTRA_STREAM, attachment);
            return this;
        }

        @Override
        public boolean send(@NonNull Activity from) {
            try {
                from.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                SenseAlertDialog alertDialog = new SenseAlertDialog(from);
                alertDialog.setTitle(R.string.dialog_error_title);
                alertDialog.setMessage(R.string.error_no_email_client);
                alertDialog.setPositiveButton(android.R.string.ok, null);
                alertDialog.show();

                return false;
            }

            return true;
        }
    }

    public static class ImageAction extends Action {
        private final Bitmap bitmap;

        private ImageAction(@NonNull Bitmap bitmap) {
            super(Intent.ACTION_SEND, "*/*");

            this.bitmap = bitmap;
        }

        public ImageAction withTitle(@Nullable String title) {
            intent.putExtra(Intent.EXTRA_SUBJECT, title);
            return this;
        }

        public ImageAction withDescription(@Nullable String description) {
            intent.putExtra(Intent.EXTRA_TEXT, description);
            return this;
        }

        @Override
        public boolean send(@NonNull Activity from) {
            ContentResolver contentResolver = from.getContentResolver();
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, intent.getStringExtra(Intent.EXTRA_SUBJECT));
            values.put(MediaStore.Images.Media.DESCRIPTION, intent.getStringExtra(Intent.EXTRA_TEXT));
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            Uri imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            OutputStream imageOut = null;
            try {
                imageOut = contentResolver.openOutputStream(imageUri);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, imageOut);
                imageOut.flush();
            } catch (IOException e) {
                Logger.error(Share.class.getSimpleName(), "Could not share bitmap image", e);

                ErrorDialogFragment.presentError(from.getFragmentManager(), e);

                return false;
            } finally {
                Functions.safeClose(imageOut);
                bitmap.recycle();
            }

            intent.putExtra(Intent.EXTRA_STREAM, imageUri);

            from.startActivity(Intent.createChooser(intent, from.getString(R.string.action_share)));

            return true;
        }
    }
}
