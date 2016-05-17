package is.hello.sense.util;

import android.content.Context;
import android.support.annotation.NonNull;

import com.facebook.CallbackManager;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import is.hello.sense.ui.fragments.settings.AccountSettingsFragment;

@Module(complete = false, injects = {
        AccountSettingsFragment.class
})
public class UtilityModule {

    @Provides
    @Singleton
    public StorageUtil providesStorageUtil(){
        return new StorageUtil();
    }

    @Provides
    @Singleton
    public ImageUtil providesImageUtil(@NonNull final Context context, @NonNull final StorageUtil storageUtil){
        return new ImageUtil(context, storageUtil);
    }

    @Provides
    @Singleton
    public CallbackManager providesCallbackManager(){
        return CallbackManager.Factory.create();
    }
}
