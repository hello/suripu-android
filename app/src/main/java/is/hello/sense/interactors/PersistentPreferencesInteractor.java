package is.hello.sense.interactors;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.sense.graph.annotations.PersistentSharedPreferences;

@Singleton public class PersistentPreferencesInteractor extends BasePreferencesInteractor {

    public static final String FIRMWARE_UPDATE_LAST_COMPLETED = "firmware_update_last_completed_with_device_id_";
    public static final String SENSE_VOICE_TUTORIAL_HAS_SEEN = "sense_voice_tutorial_has_seen_with_account_id_";

    public @Inject
    PersistentPreferencesInteractor(@NonNull final Context context,
                                    @NonNull @PersistentSharedPreferences final SharedPreferences sharedPreferences){
        super(context, sharedPreferences);
    }

    public @Nullable
    DateTime getLastPillUpdateDateTime(final String deviceId) {
        if(contains(getLastUpdatedDeviceKey(deviceId))){
            return new DateTime(getLong(getLastUpdatedDeviceKey(deviceId), 0));
        } else{
            return null;
        }
    }

    public void updateLastUpdatedDevice(@NonNull final String deviceId){
        edit().putLong(
                getLastUpdatedDeviceKey(deviceId),
                DateTimeUtils.currentTimeMillis())
              .apply();
    }

    public boolean hasCompletedVoiceTutorial(@NonNull final String accountId){
        return getBoolean(getHasSeenVoiceTutorialKey(accountId), false);
    }

    public void setHasCompletedVoiceTutorial(@NonNull final String accountId, final boolean hasSeen){
        edit().putBoolean(
                getHasSeenVoiceTutorialKey(accountId),
                hasSeen)
              .apply();
    }

    private String getLastUpdatedDeviceKey(@NonNull final String deviceId){
        return PersistentPreferencesInteractor.FIRMWARE_UPDATE_LAST_COMPLETED + deviceId;
    }

    private String getHasSeenVoiceTutorialKey(@NonNull final String accountId) {
        return PersistentPreferencesInteractor.SENSE_VOICE_TUTORIAL_HAS_SEEN + accountId;
    }

}
