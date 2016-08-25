package is.hello.sense.ui.fragments.sense;

import is.hello.sense.presenters.BasePairSensePresenter;
import is.hello.sense.ui.fragments.BasePresenterFragment;

public abstract class BasePairSenseFragment extends BasePresenterFragment
implements BasePairSensePresenter.Output{

    protected abstract void sendOnCreateAnalytics();

    /**region {@link BasePairSensePresenter.Output}**/

    @Override
    public void finishPairFlow(final int resultCode){
        finishFlowWithResult(resultCode);
    }

    @Override
    public void finishActivity(){
        getActivity().finish();
    }

    //end region
}

