package is.hello.sense.flows.home.interactors;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.v2.voice.VoiceCommandResponse;
import is.hello.sense.graph.InteractorSubject;
import is.hello.sense.interactors.ValueInteractor;
import rx.Observable;

public class VoiceCommandResponseInteractor extends ValueInteractor<VoiceCommandResponse> {
    @Inject
    ApiService apiService;

    public final InteractorSubject<VoiceCommandResponse> commands = this.subject;


    @Override
    protected boolean isDataDisposable() {
        return true;
    }

    @Override
    protected boolean canUpdate() {
        return true;
    }

    @Override
    protected Observable<VoiceCommandResponse> provideUpdateObservable() {
        return this.apiService.getVoiceCommands();
    }
}
