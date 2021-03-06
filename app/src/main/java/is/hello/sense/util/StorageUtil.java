package is.hello.sense.util;

import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;

public class StorageUtil {

    private boolean externalStorageAvailable;
    private boolean externalStorageWriteable;
    private final long usableSpaceThreshold = 5000 * 1024; //5 Mbs of free space needed

    public StorageUtil(){
        checkState();
    }

    public void checkState(){
        String state = Environment.getExternalStorageState();

        if(state.equals(Environment.MEDIA_MOUNTED)) {
            externalStorageAvailable = true;
            externalStorageWriteable = true;
        }

        else if(state.equals(Environment.MEDIA_MOUNTED_READ_ONLY)){
            externalStorageAvailable = true;
            externalStorageWriteable = false;
        }

        else {
            externalStorageAvailable = false;
            externalStorageWriteable = false;
        }
    }

    public boolean hasEnoughMemory(@NonNull final File directory, final long requiredSpace){

        long availableRemainingSpace = directory.getUsableSpace() - requiredSpace;

        final boolean hasEnoughMemory = availableRemainingSpace > usableSpaceThreshold;
        if(!hasEnoughMemory){
            Logger.warn(StorageUtil.class.getSimpleName(),"not enough memory to create file in directory " + directory);
        }
        return hasEnoughMemory;
    }

    public boolean isExternalStorageAvailable() {
        checkState();
        return externalStorageAvailable;
    }

    public boolean isExternalStorageWriteable() {
        checkState();
        return externalStorageWriteable;
    }

    public boolean isExternalStorageAvailableAndWriteable() {
        checkState();
        return externalStorageAvailable && externalStorageWriteable;
    }

    public boolean canUse(@Nullable final File directory, final long requiredSpace) {
        return directory != null &&
                directory.exists()
                && directory.canWrite()
                && hasEnoughMemory(directory, requiredSpace);
    }

    public @Nullable File createDirectory(@NonNull final File parentDirectory, @NonNull final String directoryName) {
        final File directory = new File(parentDirectory, directoryName);
        if(!directory.exists() && !directory.mkdir()){
            Logger.error(StorageUtil.class.getSimpleName(), "could not create directory " + directory);
            return null;
        }
        return directory;
    }
}
