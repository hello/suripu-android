package is.hello.sense.interactors;

import android.support.annotation.NonNull;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.SenseDevice;
import is.hello.sense.graph.InteractorSubject;
import rx.Observable;

public class SwapSenseInteractor extends ValueInteractor<Boolean>{

    private final ApiService apiService;
    private SenseDevice.SwapRequest request;

    public InteractorSubject<Boolean> isOkStatus = this.subject;

    public SwapSenseInteractor(@NonNull final ApiService apiService){
        this.apiService = apiService;
    }

    @Override
    protected boolean isDataDisposable() {
        return true;
    }

    @Override
    protected boolean canUpdate() {
        return true;
    }

    @Override
    protected Observable<Boolean> provideUpdateObservable() {
        if(request == null){
            return Observable.error(new NullPointerException("no request found"));
        }
        return apiService.swapDevices(request)
                .flatMap( response -> {
                    if(!SenseDevice.SwapResponse.isOK(response.status)){
                        return Observable.error(new BadSwapStatusException());
                    }
                    return Observable.just(true);
                });
    }

    public void setRequest(final String senseId) {
        this.request = new SenseDevice.SwapRequest(senseId);
    }

    public static class BadSwapStatusException extends Throwable {
        BadSwapStatusException(){
            //todo replace with real error copy
            super("Unable to update your new Sense at the moment. This is temporary copy for swap api error.");
        }
    }
}
