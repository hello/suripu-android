package is.hello.sense.util;

import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;

import is.hello.sense.BuildConfig;


/**
 * Converts Android media content URIs to full paths
 */
public class FilePathUtil {

    private final Context context;
    private final int apiLevel = Build.VERSION.SDK_INT;
    /**
     * Used instead of null to make Uri <code> @NonNull </code>.
     * It is the equivalent of empty string "".
     */
    private static final Uri EMPTY_URI_STATE = Uri.EMPTY;
    private static final String EMPTY_URI_STATE_STRING = EMPTY_URI_STATE.toString();

    public FilePathUtil(@NonNull final Context context){
        this.context = context;
    }

    public Uri getRealUriPath(@NonNull final Uri uri){
        return Uri.parse(getLocalPath(uri));
    }

    public String getLocalPath(@NonNull final Uri uri){
        if(uri.equals(EMPTY_URI_STATE)){
            return uri.toString();
        }
        else if(apiLevel >= Build.VERSION_CODES.KITKAT){
            return getLocalPathApi19AndUp(uri);
        } else if(apiLevel >= Build.VERSION_CODES.HONEYCOMB){
            return getLocalPathApi18To11(uri);
        } else {
            throw new RuntimeException("unsupported api level" + apiLevel);
        }
    }

    /**
     * This method is successful at fetching absolute paths of uri's that do not match
     * {@link DocumentsContract#isDocumentUri(Context, Uri)} format that API >= 19 expects
     * from {@link android.content.Intent#ACTION_OPEN_DOCUMENT}
     * @param uri
     * @return
     */
    private String getLocalPathApi18To11(@NonNull final Uri uri) {
        final String result;
        final String[] proj = { MediaStore.Images.Media.DATA };

        final CursorLoader cursorLoader = new CursorLoader(
                context, uri, proj, null, null, null);
        final Cursor cursor = cursorLoader.loadInBackground();

        if(cursor != null){
            final int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            result = cursor.getString(column_index);
        } else if(uri.isHierarchical()){
            result = uri.getPath();
        } else{
            result = uri.toString();
        }
        return result;
    }

    /**
     *  Do not remove this method despite supporting only devices api 19+
     *  Many times the uri is not {@link DocumentsContract#isDocumentUri(Context, Uri)}
     *  and so {@link this#getLocalPathApi18To11(Uri)} will handle it.
     * @param uri
     * @return
     */
    private String getLocalPathApi19AndUp(@NonNull final Uri uri){
        String filePath = EMPTY_URI_STATE_STRING;
        if(!DocumentsContract.isDocumentUri(context, uri)){
            return getLocalPathApi18To11(uri);
        }
        final String wholeID = DocumentsContract.getDocumentId(uri);

        // Split at colon, use second item in the array
        // Ex "image:1999" => "1999" is what we want
        final String[] ids = {wholeID.split(":")[1]};

        final String[] column = { MediaStore.Images.Media.DATA };

        // where id is equal to
        final String sel = MediaStore.Images.Media._ID + "=?";

        final Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                                           column, sel, ids, null);
        if(cursor == null) {
            return uri.getPath();
        }

        final int columnIndex = cursor.getColumnIndex(column[0]);

        if (cursor.moveToFirst()) {
            filePath = cursor.getString(columnIndex);
        }
        cursor.close();
        return filePath;
    }

    public boolean isFoundOnDevice(@Nullable final String filePath) {
        return filePath != null && new File(filePath).exists();
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.<br>
     * <br>
     * Callers should check whether the path is local before assuming it
     * represents a local file.
     *
     * @param context The context.
     * @param uri     The Uri to query.
     */
    public static String getPath(final Context context, final Uri uri) {

        if (BuildConfig.DEBUG)
            Log.d( " File -",
                  "Authority: " + uri.getAuthority() +
                          ", Fragment: " + uri.getFragment() +
                          ", Port: " + uri.getPort() +
                          ", Query: " + uri.getQuery() +
                          ", Scheme: " + uri.getScheme() +
                          ", Host: " + uri.getHost() +
                          ", Segments: " + uri.getPathSegments().toString()
                 );

        // DocumentProvider
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }
    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                                                        null);
            if (cursor != null && cursor.moveToFirst()) {
                if (BuildConfig.DEBUG)
                    DatabaseUtils.dumpCursor(cursor);

                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

}
