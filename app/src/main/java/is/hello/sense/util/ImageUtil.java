package is.hello.sense.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

import rx.Observable;

public class ImageUtil {

    @VisibleForTesting
    final int FILE_SIZE_LIMIT =  5000 * 1024; //5MB
    @VisibleForTesting
    final int COMPRESSION_QUALITY = 100;
    @VisibleForTesting
    final Bitmap.CompressFormat COMPRESSION_FORMAT = Bitmap.CompressFormat.JPEG;
    @VisibleForTesting
    final SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
    @VisibleForTesting
    final String imageFileName = "HelloProfile";
    @VisibleForTesting
    final String fileFormat = ".jpg";
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

    public Observable<File> provideObservableToCompressFile(@NonNull final String path){
        return Observable.create((subscriber) -> {
            try{
                final File compressedFile = compressFile(path);
                subscriber.onNext(compressedFile);
            } catch (IOException e){
                subscriber.onError(e);
            } finally {
                subscriber.onCompleted();
                subscriber.unsubscribe();
            }
        });
    }

    public File compressFile(@NonNull final String path) throws IOException{
        final byte[] compressedByteArray = compressToByteArray(path);
        final File file = new File(path);
        try(final FileOutputStream fos = new FileOutputStream(file)){
            fos.write(compressedByteArray);
        }
        return file;
    }

    public byte[] compressToByteArray(@NonNull final String path) throws IOException{

        Bitmap bitmap = downSampleBitmapFromFile(path);
        bitmap = rotateBitmap(bitmap, getRotationFromExifData(path));
        try(
                final ByteArrayOutputStream bos = new ByteArrayOutputStream()
        ){
            bitmap.compress(COMPRESSION_FORMAT, COMPRESSION_QUALITY, bos);
            bitmap.recycle();
            final byte[] byteArray = bos.toByteArray();
            bos.close();
            return byteArray;
        }
    }

    public Bitmap downSampleBitmapFromFile(@NonNull final String path){
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        options.inSampleSize = calculateInSampleSize(options, FILE_SIZE_LIMIT);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }

    public int getRotationFromExifData(@NonNull final String path) throws IOException{
        final ExifInterface exif = new ExifInterface(path);
        final int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                                                     ExifInterface.ORIENTATION_NORMAL);
        switch (orientation){
            case ExifInterface.ORIENTATION_ROTATE_90:
                return 90;
            case ExifInterface.ORIENTATION_ROTATE_180:
                return 180;
            case ExifInterface.ORIENTATION_ROTATE_270:
                return 270;
            case ExifInterface.ORIENTATION_UNDEFINED:
                Logger.warn(ImageUtil.class.getSimpleName(), "exif data orientation undefined");
            default:
                return 0;
        }

    }

    public Bitmap rotateBitmap(@NonNull final Bitmap source, final float rotateDegrees){
        if(rotateDegrees % 360 == 0) {
            return source;
        }
        final Matrix rotationMatrix = new Matrix();
        rotationMatrix.setRotate(rotateDegrees);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), rotationMatrix, false);
    }

    /**
     * @param options
     * @param memoryLimit references {@link ImageUtil#FILE_SIZE_LIMIT}
     * @return largest inSampleSize that meets minimum required width and height.
     */
    private int calculateInSampleSize(@NonNull final BitmapFactory.Options options, final int memoryLimit){
        final int pixelByteDensity = getPixelByteDensity(options.inPreferredConfig);
        final int rawHeight = options.outHeight;
        final int rawWidth = options.outWidth;
        int inSampleSize = 1;

        if( getSizeInMemory(rawWidth,rawHeight,pixelByteDensity) <= memoryLimit){
            return inSampleSize;
        } else{
            do{ inSampleSize *= 2;}
            while(getSizeInMemory((rawHeight / inSampleSize), (rawWidth / inSampleSize), pixelByteDensity) > memoryLimit);
            return inSampleSize;
        }
    }

    /**
     * @param config
     * @return Returns number of bytes per pixel given a {@link android.graphics.Bitmap.Config}.
     * If unknown config, returns {@link Bitmap#DENSITY_NONE}.
     */
    private int getPixelByteDensity(@NonNull final Bitmap.Config config){
        switch (config){
            case ARGB_8888:
                return 4;
            case RGB_565:
                return 3;
            case ARGB_4444:
                return 2;
            case ALPHA_8:
                return 1;
            default:
                Logger.error(ImageUtil.class.getSimpleName(), "unknown bitmap config " + config);
                return Bitmap.DENSITY_NONE;
        }
    }

    private int getSizeInMemory(final int width, final int height, final int pixelByteDensity){
        return width * height * pixelByteDensity;
    }

    /**
     *  First tries public external storage for pictures then private external storage for pictures.
     *  Last attempt will try to access internal app storage.
     * @return File directory to write image file.
     */
    protected @Nullable File getStorageDirectory(){
        File storageDir;
        final long memoryRequirement = FILE_SIZE_LIMIT;
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
