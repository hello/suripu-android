package is.hello.sense.flows.expansions.interactors;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.v2.expansions.Configuration;
import is.hello.sense.graph.InteractorSubject;
import is.hello.sense.interactors.ValueInteractor;
import is.hello.sense.util.Constants;
import rx.Observable;

import static is.hello.sense.api.model.v2.expansions.Expansion.NO_ID;

public class ConfigurationsInteractor extends ValueInteractor<ArrayList<Configuration>> {

    private static final String KEY_EXPANSION_ID = ConfigurationsInteractor.class.getName() + "KEY_EXPANSION_ID";
    @VisibleForTesting
    public static final long FILTER_NULL_EMPTY_CONFIG_LIST_DURATION_MILLIS = 8000;

    private static final int RESUBSCRIBE_DELAY_SECONDS = 2;
    private final ApiService apiService;

    private long expansionId = NO_ID;
    public InteractorSubject<ArrayList<Configuration>> configSubject = this.subject;

    public ConfigurationsInteractor(@NonNull final ApiService apiService){
        this.apiService = apiService;
    }

    @Override
    protected boolean isDataDisposable() {
        return false;
    }

    @Override
    protected boolean canUpdate() {
        return true;
    }

    @Override
    protected Observable<ArrayList<Configuration>> provideUpdateObservable() {
        if (expansionId == NO_ID){
            return Observable.just(new ArrayList<>());
        }
        final long initialRequestTime = System.currentTimeMillis();
        return apiService.getConfigurations(expansionId)
                         .flatMap( list -> {
                             if(shouldResubscribe(list, initialRequestTime)){
                                 return Observable.error(new InvalidConfigurationException());
                             } else {
                                 return Observable.just(list);
                             }
                         }).retryWhen( errorObservable -> errorObservable.flatMap(this::provideErrorNotificationHandler));
    }

    @Override
    public void onRestoreState(@NonNull final Bundle savedState) {
        super.onRestoreState(savedState);
        this.expansionId = savedState.getLong(KEY_EXPANSION_ID, NO_ID);
    }

    @Nullable
    @Override
    public Bundle onSaveState() {
        Bundle bundle = super.onSaveState();
        if(bundle == null){
            bundle = new Bundle();
        }
        bundle.putLong(KEY_EXPANSION_ID, expansionId);
        return bundle;
    }

    public Observable<Configuration> setConfiguration(@NonNull final Configuration configuration){
        return apiService.setConfigurations(expansionId, configuration);
    }

    public void setExpansionId(final long id) {
        this.expansionId = id;
    }

    @VisibleForTesting
    public boolean shouldResubscribe(@Nullable final List<Configuration> configurations,
                                     final long initialRequestTime){
        return (configurations == null || configurations.isEmpty())
                && System.currentTimeMillis() - initialRequestTime <  FILTER_NULL_EMPTY_CONFIG_LIST_DURATION_MILLIS;
    }

    private Observable provideErrorNotificationHandler(@NonNull final Throwable error){
        if (error instanceof InvalidConfigurationException) {
            return Observable.just(null).delay(RESUBSCRIBE_DELAY_SECONDS, TimeUnit.SECONDS);
        } else {
            return Observable.error(error); //do not resubscribe
        }
    }

    /**
     * Use this until server returns selected configuration from endpoint
     * @return {@link is.hello.sense.api.model.v2.expansions.Configuration.Empty} if no selected configurations
     * or null if list is null or empty
     * else the first selected = true configuration from list
     */
    @Nullable
    public static Configuration selectedConfiguration(@Nullable final ArrayList<Configuration> configurations){
        if (configurations == null || configurations.isEmpty()) {
            return null;
        }

        Configuration selectedConfig = null;

        for (int i = 0; i < configurations.size(); i++) {
            final Configuration config = configurations.get(i);
            if (config.isSelected()) {
                selectedConfig = config;
                break;
            }
        }
        if(selectedConfig == null){
            return new Configuration.Empty(Constants.EMPTY_STRING,
                                           Constants.EMPTY_STRING,
                                           R.drawable.icon_warning_24);
        } else {
            return selectedConfig;
        }
    }

    private static class InvalidConfigurationException extends RuntimeException {
        InvalidConfigurationException(){
            super("should not see this exception except for in logs");
        }
    }
}
