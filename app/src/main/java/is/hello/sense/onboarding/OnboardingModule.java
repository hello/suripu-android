package is.hello.sense.onboarding;

import android.support.annotation.NonNull;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import is.hello.sense.flows.home.interactors.SensorResponseInteractor;
import is.hello.sense.presenters.RoomCheckPresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.fragments.onboarding.BluetoothFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRegisterAudioFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingRoomCheckFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingSenseColorsFragment;
import is.hello.sense.ui.fragments.onboarding.OnboardingUnsupportedDeviceFragment;
import is.hello.sense.ui.fragments.onboarding.PairSenseFragment;
import is.hello.sense.ui.fragments.onboarding.RegisterHeightFragment;
import is.hello.sense.ui.fragments.onboarding.RegisterWeightFragment;
import is.hello.sense.ui.fragments.pill.PairPillFragment;
import is.hello.sense.units.UnitFormatter;
import is.hello.sense.util.RoomCheckResMapper;

@Module(complete = false,
        library = true,
        includes = {
                //todo include after converting fragments to use presenters
                // SenseOTAModule.class,
        },
        injects = {
                OnboardingActivity.class,
                OnboardingUnsupportedDeviceFragment.class,
                RegisterWeightFragment.class,
                RegisterHeightFragment.class,
                BluetoothFragment.class,
                OnboardingRegisterAudioFragment.class,
                PairSenseFragment.class,
                PairPillFragment.class,
                OnboardingSenseColorsFragment.class,
                OnboardingRoomCheckFragment.class,
        }
)
public class OnboardingModule {

        @Provides
        @Singleton
        public RoomCheckResMapper providesRoomCheckResMapper(){
                return new RoomCheckResMapper();
        }

        @Singleton
        @Provides
        public RoomCheckPresenter providesRoomCheckPresenter(@NonNull final SensorResponseInteractor interactor,
                                                             @NonNull final UnitFormatter unitFormatter,
                                                             @NonNull final RoomCheckResMapper roomCheckResMapper){
                return new RoomCheckPresenter(interactor,
                                              unitFormatter,
                                              roomCheckResMapper);
        }

}
