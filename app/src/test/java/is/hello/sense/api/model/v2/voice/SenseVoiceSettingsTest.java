package is.hello.sense.api.model.v2.voice;

import org.junit.Test;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;


public class SenseVoiceSettingsTest {

    @Test
    public void isWithinVolumeThreshold() throws Exception {
        final int baseVolume = 50;
        final int lowerBoundVolume = baseVolume - SenseVoiceSettings.VOLUME_EQUALS_THRESHOLD;
        final int upperBoundVolume = baseVolume + SenseVoiceSettings.VOLUME_EQUALS_THRESHOLD;
        final SenseVoiceSettings baseSetting = new SenseVoiceSettings(baseVolume, false, false);

        for (int volume = lowerBoundVolume; volume <= upperBoundVolume; volume++) {
            assertTrue(baseSetting.isWithinVolumeThreshold(volume));
        }

        assertFalse(baseSetting.isWithinVolumeThreshold(lowerBoundVolume - 1));
        assertFalse(baseSetting.isWithinVolumeThreshold(upperBoundVolume + 1));

    }

    @Test
    public void equals() throws Exception {
        final int baseVolume = 50;
        final int lowerBoundVolume = baseVolume - SenseVoiceSettings.VOLUME_EQUALS_THRESHOLD;
        final int upperBoundVolume = baseVolume + SenseVoiceSettings.VOLUME_EQUALS_THRESHOLD;
        final SenseVoiceSettings baseSetting = new SenseVoiceSettings(baseVolume, false, false);

        for (int volume = lowerBoundVolume; volume <= upperBoundVolume; volume++) {
            final SenseVoiceSettings comparisonSettings = SenseVoiceSettings.newInstance(baseSetting);
            comparisonSettings.setVolume(volume);
            assertTrue(baseSetting.equals(comparisonSettings));
        }
    }

}