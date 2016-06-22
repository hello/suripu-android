package is.hello.sense.util;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;

public abstract class Fetch {

    protected final Intent intent;

    protected final int requestCode;

    protected Fetch(@NonNull String action) {
        this(action, 0);
    }

    protected Fetch(@NonNull String action, int requestCode) {
        this.intent = new Intent(action);
        this.requestCode = requestCode;
    }

    public abstract void fetch(@NonNull Activity to);

    public abstract void fetch(@NonNull Fragment to);

    public int getRequestCode() {
        return requestCode;
    }


    public static Image imageFromCamera() {
        return new Image(MediaStore.ACTION_IMAGE_CAPTURE, Image.REQUEST_CODE_CAMERA);
    }

    //Todo unimplemented
    public static Image imageFromCustomCamera() {
        return new Image(MediaStore.ACTION_IMAGE_CAPTURE, Image.REQUEST_CODE_CAMERA);
    }

    public static Image imageFromGallery() {
        // When using Intent.ACTION_OPEN_DOCUMENT and setting type to Image, non image files also appear
        // Sticking with ACTION_PICK for now.
        final Image image = new Image(Intent.ACTION_PICK, Image.REQUEST_CODE_GALLERY);
        image.intent.setType(Image.type);
        //image.intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        image.intent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        return image;
    }

    public static class Image extends Fetch {
        public static final int REQUEST_CODE_CAMERA = 0x11;
        public static final int REQUEST_CODE_GALLERY = 0x12;
        public static final String type = "image/*";

        protected Image(@NonNull String action, int requestCode) {
            super(action, requestCode);
        }

        public void fetch(@NonNull Fragment to, @NonNull Uri imageUri) {
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            fetch(to);
        }

        @Override
        public void fetch(@NonNull Activity to) {
            to.startActivityForResult(intent, requestCode);
        }

        @Override
        public void fetch(@NonNull Fragment to) {
            to.startActivityForResult(intent, requestCode);
        }
    }
}
