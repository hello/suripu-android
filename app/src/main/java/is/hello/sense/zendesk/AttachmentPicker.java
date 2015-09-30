package is.hello.sense.zendesk;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.zendesk.sdk.attachment.UriToFileUtil;
import com.zendesk.sdk.network.impl.ZendeskConfig;

import org.joda.time.DateTime;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import is.hello.buruberi.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.ui.common.SenseFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.widget.SenseBottomSheet;

public class AttachmentPicker {
    public static final int REQUEST_CODE_TAKE_IMAGE = 0x01;
    public static final int REQUEST_CODE_PICK_IMAGE = 0x02;

    private final String SAVED_PENDING_CAPTURE_PATH = AttachmentPicker.class.getName() + ".SAVED_PENDING_CAPTURE_PATH";

    private final SenseFragment fragment;
    private String pendingCapturePath;

    public AttachmentPicker(@NonNull SenseFragment fragment,
                            @Nullable Bundle inState) {
        this.fragment = fragment;

        if (inState != null) {
            this.pendingCapturePath = inState.getString(SAVED_PENDING_CAPTURE_PATH);
        }
    }

    public void saveInstanceState(@NonNull Bundle outState) {
        outState.putString(SAVED_PENDING_CAPTURE_PATH, pendingCapturePath);
    }

    public boolean showOptions() {
        if (!ZendeskConfig.INSTANCE.isInitialized()) {
            return false;
        }

        SenseBottomSheet chooseSource = new SenseBottomSheet(fragment.getActivity());
        chooseSource.setTitle(R.string.action_add_attachment);

        chooseSource.addOption(new SenseBottomSheet.Option(0)
                .setTitle(R.string.action_take_photo));
        chooseSource.addOption(new SenseBottomSheet.Option(1)
                .setTitle(R.string.action_pick_image));

        chooseSource.setOnOptionSelectedListener(option -> {
            switch (option.getOptionId()) {
                case 0:
                    captureImage();
                    break;
                case 1:
                    pickImage();
                    break;
                default:
                    new IllegalArgumentException();
            }
            return true;
        });
        chooseSource.show();

        return true;
    }

    public void captureImage() {
        String filename = "JPEG_" + DateTime.now().toString();
        File pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        try {
            File imageLocation = File.createTempFile(filename, ".jpg", pictures);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imageLocation));
            fragment.startActivityForResult(intent, REQUEST_CODE_TAKE_IMAGE);

            this.pendingCapturePath = imageLocation.getAbsolutePath();
        } catch (IOException | ActivityNotFoundException e) {
            ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment.Builder(e, fragment.getResources())
                    .withMessage(StringRef.from(R.string.error_image_capture_failed))
                    .build();
            errorDialogFragment.showAllowingStateLoss(fragment.getFragmentManager(), ErrorDialogFragment.TAG);
        }
    }

    public void pickImage() {
        try {
            Intent getContentIntent = new Intent(Intent.ACTION_GET_CONTENT);
            getContentIntent.setType("image/*");
            getContentIntent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            getContentIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

            Intent pickerIntent = Intent.createChooser(getContentIntent, fragment.getString(R.string.action_pick_image));
            fragment.startActivityForResult(pickerIntent, REQUEST_CODE_PICK_IMAGE);
        } catch (ActivityNotFoundException e) {
            ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment.Builder(e, fragment.getResources())
                    .withMessage(StringRef.from(R.string.error_pick_image_failed))
                    .build();
            errorDialogFragment.showAllowingStateLoss(fragment.getFragmentManager(), ErrorDialogFragment.TAG);
        }
    }

    public List<File> getFilesFromResult(int requestCode,
                                         int resultCode,
                                         @Nullable Intent response) {
        List<File> files = new ArrayList<>();

        Context context = fragment.getActivity();
        if (requestCode == REQUEST_CODE_TAKE_IMAGE && resultCode == Activity.RESULT_OK) {
            if (response != null && response.getData() != null) {
                Uri imageLocation = response.getData();
                files.add(UriToFileUtil.getFile(context, imageLocation));
            } else if (pendingCapturePath != null) {
                files.add(new File(pendingCapturePath));
            }
        } else if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            if (response != null) {
                if (response.getClipData() != null) {
                    ClipData clipData = response.getClipData();
                    for (int i = 0, count = clipData.getItemCount(); i < count; i++) {
                        ClipData.Item item = clipData.getItemAt(i);
                        Uri uri = item.getUri();
                        if (uri != null) {
                            files.add(UriToFileUtil.getFile(context, uri));
                        }
                    }
                } else {
                    Uri uri = response.getData();
                    files.add(UriToFileUtil.getFile(context, uri));
                }
            }
        }

        return files;
    }
}
