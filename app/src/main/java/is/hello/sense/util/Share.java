package is.hello.sense.util;

import android.app.Activity;
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

public class Share {
    public static ImageFacade image(@NonNull Bitmap bitmap) {
        return new ImageFacade(bitmap);
    }

    public static abstract class Facade {
        protected final Intent intent;

        protected Facade(@NonNull String action, @NonNull String mimeType) {
            this.intent = new Intent(action);
            intent.setType(mimeType);
        }

        public abstract boolean send(@NonNull Activity from);
    }

    public static class ImageFacade extends Facade {
        private final Bitmap bitmap;

        private ImageFacade(@NonNull Bitmap bitmap) {
            super(Intent.ACTION_SEND, "*/*");

            this.bitmap = bitmap;
        }

        public ImageFacade withTitle(@Nullable String title) {
            intent.putExtra(Intent.EXTRA_SUBJECT, title);
            return this;
        }

        public ImageFacade withDescription(@Nullable String description) {
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
                return false;
            } finally {
                Functions.safeClose(imageOut);
            }

            intent.putExtra(Intent.EXTRA_STREAM, imageUri);

            from.startActivity(Intent.createChooser(intent, from.getString(R.string.action_share)));
            return true;
        }
    }
}
