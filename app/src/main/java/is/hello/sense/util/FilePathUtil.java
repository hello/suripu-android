package is.hello.sense.util;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;

import java.io.File;


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
        return Uri.parse(getRealPath(uri));
    }

    public String getRealPath(@NonNull final Uri uri){
        if(apiLevel >= Build.VERSION_CODES.KITKAT){
            return getRealPathApi19AndUp(uri);
        } else if(apiLevel >= Build.VERSION_CODES.HONEYCOMB){
            return getRealPathApi18To11(uri);
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
    private String getRealPathApi18To11(@NonNull final Uri uri) {
        final String result;
        final String[] proj = { MediaStore.Images.Media.DATA };

        final CursorLoader cursorLoader = new CursorLoader(
                context, uri, proj, null, null, null);
        final Cursor cursor = cursorLoader.loadInBackground();

        if(cursor != null){
            final int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            result = cursor.getString(column_index);
        } else {
            result = uri.getPath();
        }
        return result;
    }

    private String getRealPathApi19AndUp(@NonNull final Uri uri){
        String filePath = EMPTY_URI_STATE_STRING;
        if(!DocumentsContract.isDocumentUri(context, uri)){
            return getRealPathApi18To11(uri);
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

    public boolean isFoundOnDevice(@NonNull final String filePath) {
        return new File(filePath).exists();
    }
}
