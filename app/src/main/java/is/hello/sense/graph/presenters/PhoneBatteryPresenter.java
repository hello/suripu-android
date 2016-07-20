package is.hello.sense.graph.presenters;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import is.hello.sense.graph.PresenterSubject;
import is.hello.sense.util.BatteryUtil;
import rx.Observable;

public class PhoneBatteryPresenter extends ValuePresenter<Boolean>{

    private static final String OPERATIONS_SAVED_STATE_KEY = PhoneBatteryPresenter.class.getName()+"Operations_saved_state_key";
    @Inject
    BatteryUtil batteryUtil;
    private final ArrayList<BatteryUtil.Operation> operationList = new ArrayList<>();
    public PresenterSubject<Boolean> enoughBattery = this.subject;

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
        return Observable.create(subscriber -> {
                try {
                    for(final BatteryUtil.Operation op : operationList){
                        if (batteryUtil.canPerformOperation(op)){
                            subscriber.onNext(true);
                            subscriber.onCompleted();
                            return;
                        }
                    }
                    subscriber.onNext(false);
                    subscriber.onCompleted();
                } catch(final Exception e){
                    subscriber.onError(e);
                }
        });
    }

    //region State Saving

    @Override
    public void onRestoreState(@NonNull final Bundle savedState) {
        super.onRestoreState(savedState);

        if (savedState.containsKey(OPERATIONS_SAVED_STATE_KEY)) {
            //noinspection unchecked
            withAnyOperation((List<BatteryUtil.Operation>) savedState.getSerializable(OPERATIONS_SAVED_STATE_KEY));
        }
    }

    @Nullable
    @Override
    public Bundle onSaveState() {
        final Bundle bundle = super.onSaveState();
        if (bundle != null) {
            bundle.putSerializable(OPERATIONS_SAVED_STATE_KEY, operationList);
        }
        return bundle;
    }


    //endregion

    /*
        Ensure that the battery level is not stale
     */
    public void refreshAndUpdate(@NonNull final Context context){
        batteryUtil.refresh(context);
        update();
    }

    public void withAnyOperation(final @Nullable List<BatteryUtil.Operation> operations){
        this.operationList.clear();
        if(operations != null) {
            this.operationList.addAll(operations);
        }
    }

}
