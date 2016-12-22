package is.hello.sense.flows.expansions.utils;

import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;

import is.hello.sense.api.model.v2.expansions.Category;
import is.hello.sense.api.model.v2.expansions.ExpansionValueRange;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.units.UnitConverter;

import static junit.framework.Assert.assertEquals;

public class ExpansionCategoryFormatterTest extends InjectionTestCase {
    @Inject
    PreferencesInteractor preferencesInteractor;

    @Inject
    ExpansionCategoryFormatter expansionCategoryFormatter;


    @Before
    public void setUp() {
        preferencesInteractor.clear();
    }

    @Test
    public void getFormattedValueRangeIgnoresMaxForLightCategory() throws Exception {

        assertEquals("0%", expansionCategoryFormatter.getFormattedValueRange(Category.LIGHT, new ExpansionValueRange(0, 32), getContext()));
        assertEquals("0%", expansionCategoryFormatter.getFormattedValueRange(Category.LIGHT, new ExpansionValueRange(0, 100), getContext()));
    }

    @Test
    public void getFormattedValueRangeConvertsTemperature() throws Exception {
        preferencesInteractor.edit().putBoolean(PreferencesInteractor.USE_CELSIUS, true)
                             .commit();

        assertEquals("0°", expansionCategoryFormatter.getFormattedValueRange(Category.TEMPERATURE, new ExpansionValueRange(0, 0), getContext()));

        assertEquals("0° - 32°", expansionCategoryFormatter.getFormattedValueRange(Category.TEMPERATURE, new ExpansionValueRange(0, 32), getContext()));

        preferencesInteractor.edit().putBoolean(PreferencesInteractor.USE_CELSIUS, false)
                             .commit();

        assertEquals("32°", expansionCategoryFormatter.getFormattedValueRange(Category.TEMPERATURE, new ExpansionValueRange(0, 0), getContext()));

        assertEquals("32° - 89°", expansionCategoryFormatter.getFormattedValueRange(Category.TEMPERATURE, new ExpansionValueRange(0, 32), getContext()));

    }

    @Test
    public void getUnitConverter() throws Exception {
        assertEquals(UnitConverter.IDENTITY, expansionCategoryFormatter.getUnitConverter(Category.LIGHT));
        assertEquals(UnitConverter.IDENTITY, expansionCategoryFormatter.getUnitConverter(Category.UNKNOWN));
    }

    @Test
    public void getFormattedAttributionValueRange() throws Exception {
        preferencesInteractor.edit().putBoolean(PreferencesInteractor.USE_CELSIUS, true)
                             .commit();

        assertEquals("set at 0°", expansionCategoryFormatter.getFormattedAttributionValueRange(Category.TEMPERATURE, new ExpansionValueRange(0, 0), getContext()));

        assertEquals("set at 0° - 32°", expansionCategoryFormatter.getFormattedAttributionValueRange(Category.TEMPERATURE, new ExpansionValueRange(0, 32), getContext()));

        preferencesInteractor.edit().putBoolean(PreferencesInteractor.USE_CELSIUS, false)
                             .commit();

        assertEquals("set at 32° - 89°", expansionCategoryFormatter.getFormattedAttributionValueRange(Category.TEMPERATURE, new ExpansionValueRange(0, 32), getContext()));
    }

    @Test
    public void getSuffix() throws Exception {
        assertEquals("°", expansionCategoryFormatter.getSuffix(Category.TEMPERATURE));
        assertEquals("%", expansionCategoryFormatter.getSuffix(Category.LIGHT));
        assertEquals("", expansionCategoryFormatter.getSuffix(Category.UNKNOWN));
    }

    @Test
    public void hasCorrectIdealValues() throws Exception {
        ExpansionValueRange expansionValueRange = new ExpansionValueRange(9, 32);
        assertEquals(new ExpansionValueRange(15, 19), expansionCategoryFormatter.getIdealValueRange(Category.TEMPERATURE, expansionValueRange));

        expansionValueRange = new ExpansionValueRange(1, 100);
        assertEquals(new ExpansionValueRange(20, 20), expansionCategoryFormatter.getIdealValueRange(Category.LIGHT, expansionValueRange));
    }
}