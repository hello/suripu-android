package is.hello.sense.api.sessions;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;

import is.hello.sense.api.model.UserFeatures;
import is.hello.sense.util.Logger;

public class UserFeaturesManager {

    private static final String SHARED_PREFERENCES_NAME = "user_features_preferences";
    private static final String FEATURES_KEY = "user_features";

    private final SharedPreferences preferences;
    private final Gson gson;

    public UserFeaturesManager(@NonNull final Context context, @NonNull final Gson gson){
        this.preferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        this.gson = gson;
    }

    public void setFeatures(@Nullable final UserFeatures userFeatures){
        final SharedPreferences.Editor editor = preferences.edit();
        if(userFeatures == null){
            editor.remove(FEATURES_KEY)
                  .apply();
            Logger.info(UserFeaturesManager.class.getName(), "cleared user features");
            return;
        }

        try {
            final String serializedFeatures = gson.toJson(userFeatures);
            editor.putString(FEATURES_KEY, serializedFeatures)
                  .apply();
        } catch (final JsonSyntaxException e){
            Logger.error(UserFeaturesManager.class.getName(), "could not serialize user features", e);
        }
    }

    public @Nullable UserFeatures getFeatures(){
        try{
            final String deserializedFeatures = preferences.getString(FEATURES_KEY,null);
            return gson.fromJson(deserializedFeatures, UserFeatures.class);
        }catch (final JsonParseException e){
            Logger.error(UserFeaturesManager.class.getName(), "could not deserialize user features", e);
            return null;
        }
    }

    public boolean hasFeatures(){
        return preferences.contains(FEATURES_KEY);
    }

}
