package is.hello.sense.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;

import rx.Observable;

public class ImageUtil {

    @VisibleForTesting
    final int FILE_SIZE_LIMIT = 5000 * 1024; //5MB
    @VisibleForTesting
    final int COMPRESSION_QUALITY = 100;
    @VisibleForTesting
    final Bitmap.CompressFormat COMPRESSION_FORMAT = Bitmap.CompressFormat.JPEG;
    @VisibleForTesting
    final SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
    @VisibleForTesting
    final String imageFileName = "HelloProfile";
    @VisibleForTesting
    final String fileFormat = ".jpg";
    @VisibleForTesting
    final String DIRECTORY_NAME = "HelloAppPictures";

    final StorageUtil storageUtil;
    final Picasso picasso;

    private final Context context;

    private final SenseCache.ImageCache imageCache;

    public ImageUtil(@NonNull final Context context, @NonNull final StorageUtil storageUtil, @NonNull final Picasso picasso, @NonNull final SenseCache.ImageCache imageCache) {
        this.context = context;
        this.storageUtil = storageUtil;
        this.picasso = picasso;
        this.imageCache = imageCache;
    }

    public boolean hasDeviceCamera() {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    /**
     * @param isTemporary determines whether or not to make temporary file
     *                    using {@link is.hello.sense.util.SenseCache.ImageCache#getCacheFile(String)}
     * @return Returns <code>null</code> if exception occurs.
     */
    public
    @Nullable
    File createFile(final boolean isTemporary) {
        final File storageDir = getStorageDirectory();
        if (isTemporary) {
            return imageCache.getCacheFile(getFullFileName(imageFileName));
        } else {
            return new File(storageDir, getFullFileName(imageFileName) + fileFormat);
        }
    }

    /**
     * @param path         of file or stream to download from
     * @param mustDownload if <code>true</code> will open connection to download url
     *                     to a new file path made by {@link ImageUtil#createFile(boolean)}
     * @return an {@link Observable<File>} to send and manipulate that has been compressed and down sampled.
     */
    public Observable<File> provideObservableToCompressFile(@NonNull final String path, final boolean mustDownload) {
        return Observable.create((subscriber) -> {
            try {
                if (subscriber.isUnsubscribed()) {
                    return;
                } else if (path.isEmpty()) {
                    subscriber.onNext(null);
                    subscriber.onCompleted();
                    return;
                }
                byte[] compressedByteArray;
                final File freshTempFile = createFile(true);
                if (mustDownload) {
                    compressedByteArray = compressToByteArray(
                            decodeBitmapFromUrlStream(path));
                } else {
                    compressedByteArray = compressToByteArray(
                            applyTransformationAndCompression(path));
                }
                final File compressedFile = writeToFile(freshTempFile, compressedByteArray);
                subscriber.onNext(compressedFile);
                subscriber.onCompleted();

            } catch (IOException e) {
                subscriber.onError(e);
            } finally {
                subscriber.unsubscribe();
            }
        });
    }

    public Bitmap downSampleBitmapFromFile(@NonNull final String path) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        decodeBitmapFromFile(path, options);
        options.inSampleSize = calculateInSampleSize(options, FILE_SIZE_LIMIT);
        options.inJustDecodeBounds = false;
        return decodeBitmapFromFile(path, options);
    }

    public Bitmap rotateBitmap(@NonNull final Bitmap source, final float rotateDegrees) {
        if (rotateDegrees % 360 == 0) {
            return source;
        }
        final Matrix rotationMatrix = new Matrix();
        rotationMatrix.setRotate(rotateDegrees);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), rotationMatrix, false);
    }

    /**
     * First tries public external storage for pictures then private external storage for pictures.
     * Last attempt will try to access internal app storage.
     *
     * @return File directory to write image file.
     */
    protected
    @Nullable
    File getStorageDirectory() {
        File storageDir;
        final long memoryRequirement = FILE_SIZE_LIMIT;
        final Queue<File> storageOptions = new LinkedList<>();

        if (storageUtil.isExternalStorageAvailableAndWriteable()) {
            storageOptions.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES));
            storageOptions.add(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES));
        }
        //internal storage directory
        storageOptions.add(context.getFilesDir());
        do {
            storageDir = storageUtil.createDirectory(storageOptions.poll(), DIRECTORY_NAME);
        }
        while (!(storageUtil.canUse(storageDir, memoryRequirement) || storageOptions.isEmpty()));
        return storageDir;
    }

    protected String getFullFileName(@NonNull final String fileName) {
        return String.format("%s_%s", fileName, timestampFormat.format(new Date()));
    }

    private File writeToFile(@NonNull final File file, @NonNull final byte[] byteArray) throws IOException {
        try (final FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(byteArray);
        }
        return file;
    }

    private Bitmap applyTransformationAndCompression(@NonNull final String path) throws IOException {
        return rotateBitmap(
                downSampleBitmapFromFile(path),
                getRotationFromExifData(path));
    }

    private byte[] compressToByteArray(@NonNull final Bitmap bitmap) throws IOException {
        try (final ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            bitmap.compress(COMPRESSION_FORMAT, COMPRESSION_QUALITY, bos);
            bitmap.recycle();
            final byte[] byteArray = bos.toByteArray();
            bos.close();
            return byteArray;
        }
    }

    /**
     * @param options     contains bitmap info to determine how much down sampling is required
     * @param memoryLimit references {@link ImageUtil#FILE_SIZE_LIMIT}
     * @return largest inSampleSize that meets minimum required width and height.
     */
    private int calculateInSampleSize(@NonNull final BitmapFactory.Options options, final int memoryLimit) {
        final int pixelByteDensity = getPixelByteDensity(options.inPreferredConfig);
        final int rawHeight = options.outHeight;
        final int rawWidth = options.outWidth;
        int inSampleSize = 1;

        if (getSizeInMemory(rawWidth, rawHeight, pixelByteDensity) <= memoryLimit) {
            return inSampleSize;
        } else {
            do {
                inSampleSize *= 2;
            }
            while (getSizeInMemory((rawHeight / inSampleSize), (rawWidth / inSampleSize), pixelByteDensity) > memoryLimit);
            return inSampleSize;
        }
    }

    /**
     * @param config of the bitmap whose pixel density you want to check.
     * @return Returns number of bytes per pixel given a {@link android.graphics.Bitmap.Config}.
     * If unknown config, returns {@link Bitmap#DENSITY_NONE}.
     */
    private int getPixelByteDensity(@NonNull final Bitmap.Config config) {
        switch (config) {
            case ARGB_8888:
                return 4;
            case RGB_565:
                return 2;
            case ARGB_4444:
                return 2;
            case ALPHA_8:
                return 1;
            default:
                Logger.error(getClass().getSimpleName(), "unknown bitmap config " + config);
                return Bitmap.DENSITY_NONE;
        }
    }

    private int getSizeInMemory(final int width, final int height, final int pixelByteDensity) {
        return width * height * pixelByteDensity;
    }

    private Bitmap decodeBitmapFromFile(@NonNull final String path, @NonNull final BitmapFactory.Options options) {
        return BitmapFactory.decodeFile(path, options);
    }

    private Bitmap decodeBitmapFromUrlStream(@NonNull final String path) throws IOException {
        /*final URL url = new URL(path);
        final InputStream fis = url.openStream();
        final Bitmap bitmap = BitmapFactory.decodeStream(fis, new Rect(0,0,0,0), options);
        fis.close();
        return bitmap;*/
        //TODO hardcoded to resize to 1000. Tested 1100 but is rejected by server
        return picasso.load(Uri.parse(path))
                .resize(1000,1000)
                .config(Bitmap.Config.ARGB_8888)
                .centerInside()
                .onlyScaleDown()
                .get();
    }

    private int getRotationFromExifData(@NonNull final String path) throws IOException {
        final ExifInterface exif = new ExifInterface(path);
        final int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                                                     ExifInterface.ORIENTATION_NORMAL);
        switch (orientation) {
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

    public void trimCache() {
        imageCache.trimCache();
    }


}
