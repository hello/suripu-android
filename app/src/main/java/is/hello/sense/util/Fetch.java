package is.hello.sense.util;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.provider.MediaStore;
import android.support.annotation.NonNull;

public abstract class Fetch {

    protected final Intent intent;

    protected final int requestCode;

    protected Fetch(@NonNull String action) {
        this(action,0);
    }

    protected Fetch(@NonNull String action, int requestCode){
        this.intent = new Intent(action);
        this.requestCode = requestCode;
    }

    public abstract void fetch(@NonNull Activity to);

    public abstract void fetch(@NonNull Fragment to);

    public int getRequestCode(){
        return requestCode;
    }


    public static Image image(final int requestCode){
        return new Image("profilePicture", requestCode);
    }

    public static class Image extends Fetch{

        protected Image(@NonNull String fileName, int requestCode) {
            super(MediaStore.ACTION_IMAGE_CAPTURE, requestCode);
        }

        @Override
        public void fetch(@NonNull Activity to) {
            to.startActivityForResult(intent,requestCode);
        }

        @Override
        public void fetch(@NonNull Fragment to) {
            to.startActivityForResult(intent,requestCode);
        }
    }
}
