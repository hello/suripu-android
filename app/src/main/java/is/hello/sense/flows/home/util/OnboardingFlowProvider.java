package is.hello.sense.flows.home.util;

import is.hello.sense.ui.activities.OnboardingActivity;

/**
 *  Provide context of the previous {@link is.hello.sense.ui.activities.OnboardingActivity.Flow}
 */

public interface OnboardingFlowProvider {

    @OnboardingActivity.Flow
    int getOnboardingFlow();
}
