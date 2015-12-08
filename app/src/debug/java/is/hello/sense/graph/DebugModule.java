package is.hello.sense.graph;

import android.support.annotation.NonNull;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import is.hello.sense.api.DelegatingTimelineService;
import is.hello.sense.api.TimelineService;
import is.hello.sense.debug.PiruPeaActivity;
import is.hello.sense.debug.TimelineSourceActivity;
import retrofit.RestAdapter;

@Module(complete = false,
        library = true,
        overrides = true,
        injects = {
                PiruPeaActivity.class,
                TimelineSourceActivity.class,
        })
public class DebugModule {
    @Singleton @Provides DelegatingTimelineService provideDelegatingTimelineService(@NonNull RestAdapter adapter) {
        final TimelineService delegate = adapter.create(TimelineService.class);
        return new DelegatingTimelineService(delegate);
    }

    @Singleton @Provides TimelineService provideTimelineService(@NonNull DelegatingTimelineService service) {
        return service;
    }
}
