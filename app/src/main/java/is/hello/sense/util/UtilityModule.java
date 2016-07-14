package is.hello.sense.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.support.annotation.NonNull;

import com.squareup.picasso.Picasso;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import is.hello.sense.graph.presenters.PhoneBatteryPresenter;
import is.hello.sense.ui.activities.ListActivity;
import is.hello.sense.ui.common.ProfileImageManager;
import is.hello.sense.ui.fragments.onboarding.RegisterFragment;
import is.hello.sense.ui.fragments.settings.AccountSettingsFragment;

@Module(complete = false, injects = {
        AccountSettingsFragment.class,
        RegisterFragment.class,
        ListActivity.class,
        PhoneBatteryPresenter.class
})
public class UtilityModule {

    @Provides
    @Singleton
    public StorageUtil providesStorageUtil() {
        return new StorageUtil();
    }

    @Provides
    @Singleton
    public ImageUtil providesImageUtil(@NonNull final Context context,
                                       @NonNull final StorageUtil storageUtil,
                                       @NonNull final Picasso picasso,
                                       @NonNull final SenseCache.ImageCache imageCache) {
        return new ImageUtil(context, storageUtil, picasso, imageCache);
    }

    @Provides
    @Singleton
    FilePathUtil providesFilePathUtil(@NonNull final Context context) {
        return new FilePathUtil(context);
    }

    @Provides
    @Singleton
    ConnectivityManager providesConnectivityManager(@NonNull final Context context) {
        return (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Provides
    @Singleton
    ProfileImageManager.Builder providesProfileImageManagerBuilder(@NonNull final ImageUtil imageUtil, @NonNull final FilePathUtil filePathUtil) {
        return new ProfileImageManager.Builder(imageUtil, filePathUtil);
    }

    @Provides
    @Singleton
    SenseCache.AudioCache providesAudioCache(@NonNull final Context context) {
        return new SenseCache.AudioCache(context);
    }

    @Provides
    @Singleton
    SenseCache.ImageCache providesImageCache(@NonNull final Context context) {
        return new SenseCache.ImageCache(context);
    }

    @Provides
    @Singleton
    BatteryUtil providesBatteryUtil(@NonNull final Context context){
        return new BatteryUtil(context);
    }

   /*
    failing for tests because it isn't being used yet.
   @Provides
    @Singleton
    SenseCache.FirmwareCache providesFirmwareCache(@NonNull final Context context) {
        return new SenseCache.FirmwareCache(context);
    }*/
}
