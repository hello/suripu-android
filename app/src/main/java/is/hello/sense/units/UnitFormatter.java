package is.hello.sense.units;

import android.support.annotation.NonNull;

import java.util.Locale;

import javax.inject.Inject;

import is.hello.sense.graph.presenters.PreferencesPresenter;
import rx.Observable;

public class UnitFormatter {
    private final PreferencesPresenter preferences;
    private final boolean defaultMetric;

    @Inject public UnitFormatter(@NonNull PreferencesPresenter preferences) {
        this.preferences = preferences;
        String country = Locale.getDefault().getCountry();
        this.defaultMetric = ("US".equals(country) ||
                "LR".equals(country) ||
                "MM".equals(country));
    }

    public Observable<UnitConverter> temperatureConverter() {
        return preferences.observableBoolean(PreferencesPresenter.USE_CELSIUS, defaultMetric)
                          .map(useCelsius -> {
                              if (useCelsius) {
                                  return UnitConverter.IDENTITY;
                              } else {
                                  return UnitOperations::celsiusToFahrenheit;
                              }
                          });
    }

    public Observable<UnitConverter> weightConverter() {
        return preferences.observableBoolean(PreferencesPresenter.USE_GRAMS, defaultMetric)
                          .map(useGrams -> {
                              if (useGrams) {
                                  return UnitOperations::gramsToKilograms;
                              } else {
                                  return UnitOperations::gramsToPounds;
                              }
                          });
    }

    public Observable<UnitConverter> heightConverter() {
        return preferences.observableBoolean(PreferencesPresenter.USE_CENTIMETERS, defaultMetric)
                          .map(useCelsius -> {
                              if (useCelsius) {
                                  return UnitConverter.IDENTITY;
                              } else {
                                  return UnitOperations::centimetersToInches;
                              }
                          });
    }
}
