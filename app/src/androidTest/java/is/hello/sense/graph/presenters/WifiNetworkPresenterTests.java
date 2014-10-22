package is.hello.sense.graph.presenters;

import junit.framework.TestCase;

public class WifiNetworkPresenterTests extends TestCase {
    // TODO: Devise plan for testing wifi manager dependent code.

    public void testGetSecurityFromCapabilities() throws Exception {
        assertEquals(WifiNetworkPresenter.SECURITY_EAP, WifiNetworkPresenter.getSecurityFromCapabilities("[" + WifiNetworkPresenter.SECURITY_EAP + "]"));
        assertEquals(WifiNetworkPresenter.SECURITY_PSK, WifiNetworkPresenter.getSecurityFromCapabilities("[" + WifiNetworkPresenter.SECURITY_PSK + "]"));
        assertEquals(WifiNetworkPresenter.SECURITY_WEP, WifiNetworkPresenter.getSecurityFromCapabilities("[" + WifiNetworkPresenter.SECURITY_WEP + "]"));
        assertEquals(WifiNetworkPresenter.SECURITY_OPEN, WifiNetworkPresenter.getSecurityFromCapabilities("[]"));
    }
}
