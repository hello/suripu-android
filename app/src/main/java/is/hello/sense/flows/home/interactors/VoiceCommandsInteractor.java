package is.hello.sense.flows.home.interactors;


import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.v2.voice.VoiceCommandResponse;
import is.hello.sense.graph.InteractorSubject;
import is.hello.sense.interactors.ValueInteractor;
import rx.Observable;

@Singleton
public class VoiceCommandsInteractor extends ValueInteractor<VoiceCommandResponse> {
    @Inject
    ApiService apiService;

    public final InteractorSubject<VoiceCommandResponse> voiceCommands = subject;

    @Override
    protected boolean isDataDisposable() {
        return false;
    }

    @Override
    protected boolean canUpdate() {
        return true;
    }

    @Override
    protected Observable<VoiceCommandResponse> provideUpdateObservable() {
        return apiService.getVoiceCommands();
    }
}
