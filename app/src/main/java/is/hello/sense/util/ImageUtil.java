package is.hello.sense.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

public class ImageUtil {

    @VisibleForTesting
    final SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
    @VisibleForTesting
    final String imageFileName = "HelloProfile";
    @VisibleForTesting
    String fileFormat = ".jpg";
    @VisibleForTesting
    final String DIRECTORY_NAME = "HelloAppPictures";

    final StorageUtil storageUtil;

    private final Context context;

    public ImageUtil(@NonNull final Context context, @NonNull final StorageUtil storageUtil){
        this.context = context;
        this.storageUtil = storageUtil;
    }

    public boolean hasDeviceCamera(){
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    /**
     * @param isTemporary determines whether or not to make temporary file
     *                    using {@link File#createTempFile(String, String, File)}
     * @return
     * Returns <code>null</code> if exception occurs.
     */
    public @Nullable File createFile(boolean isTemporary){
        final File storageDir = getStorageDirectory();

        try {
            if(isTemporary){
                return File.createTempFile(
                        getFullFileName(),
                        fileFormat,
                        storageDir);
            } else{
                return new File(storageDir,getFullFileName() + fileFormat);
            }
        } catch (IOException exception){
            Logger.error(
                    ImageUtil.class.getSimpleName(),
                    "unable to create image file in directory " + storageDir,
                    exception);
            return null;
        }
    }

    /**
     *  First tries public external storage for pictures then private external storage for pictures.
     *  Last attempt will try to access internal app storage.
     * @return File directory to write image file.
     */
    protected @Nullable File getStorageDirectory(){
        File storageDir = null;
        //Todo figure out how much memory taking a picture costs for a phone
        final long memoryRequirement = 100L;
        final Queue<File> storageOptions = new LinkedList<>();

        if(storageUtil.isExternalStorageAvailableAndWriteable()){
            storageOptions.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES));
            storageOptions.add(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES));
        }
        //internal storage directory
        storageOptions.add(context.getFilesDir());
        do{storageDir = storageUtil.createDirectory(storageOptions.poll(),DIRECTORY_NAME);}
        while((storageUtil.canUse(storageDir, memoryRequirement) || storageOptions.isEmpty()) == false);
        return storageDir;
    }

    protected String getFullFileName(){
        return String.format("%s_%s", imageFileName, timestampFormat.format(new Date()));
    }

}
