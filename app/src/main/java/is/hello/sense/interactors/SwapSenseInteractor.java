package is.hello.sense.interactors;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.buruberi.bluetooth.errors.BuruberiException;
import is.hello.commonsense.util.Errors;
import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.SenseDevice;
import is.hello.sense.graph.InteractorSubject;
import is.hello.sense.util.Analytics;
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

    /**
     * Used in cases where the value presenter value is not needed to be retained
     */
    public Observable<Boolean> canSwap(){
        return provideUpdateObservable();
    }

    public void setRequest(final String senseId) {
        this.request = new SenseDevice.SwapRequest(senseId);
    }

    public static class BadSwapStatusException extends BuruberiException implements Errors.Reporting {

        public BadSwapStatusException() {
            super("Swap api returned not ok status");
        }

        @Nullable
        @Override
        public String getContextInfo() {
            return Analytics.SenseUpgrade.ERROR_SWAP_API_STATUS;
        }

        @NonNull
        @Override
        public StringRef getDisplayMessage() {
            return StringRef.from(R.string.error_sense_upgrade_failed_message);
        }
    }
}
