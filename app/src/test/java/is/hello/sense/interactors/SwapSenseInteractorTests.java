package is.hello.sense.interactors;

import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;

import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.Sync;

import static junit.framework.Assert.assertTrue;

public class SwapSenseInteractorTests extends InjectionTestCase {

    @Inject SwapSenseInteractor swapSenseInteractor;

    @Before
    public void setUp(){
        swapSenseInteractor.isOkStatus.forget();
    }

    @Test
    public void throwErrorIfNoRequest(){
        Sync.wrapAfter(swapSenseInteractor::update,
                       swapSenseInteractor.isOkStatus)
            .assertThrows(NullPointerException.class);
    }

    /**
     * The json files associated with swap sense are loaded with each response expected
     */
    @Test
    public void throwErrorIfBadStatus(){
        swapSenseInteractor.setRequest("paired_multiple");
        Sync.wrapAfter(swapSenseInteractor::update,
                       swapSenseInteractor.isOkStatus)
            .assertThrows(SwapSenseInteractor.BadSwapStatusException.class);
        swapSenseInteractor.setRequest("different_account");
        Sync.wrapAfter(swapSenseInteractor::update,
                       swapSenseInteractor.isOkStatus)
            .assertThrows(SwapSenseInteractor.BadSwapStatusException.class);
    }

    @Test
    public void trueIfOkStatus(){
        swapSenseInteractor.setRequest("ok");
        final Boolean isOk = Sync.wrapAfter(swapSenseInteractor::update,
                                            swapSenseInteractor.isOkStatus)
                                 .last();
        assertTrue(isOk);
    }


}
