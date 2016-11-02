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

public class ExpansionCategoryFormatterTest extends InjectionTestCase{
    @Inject
    PreferencesInteractor preferencesInteractor;

    @Inject
    ExpansionCategoryFormatter expansionCategoryFormatter;


    @Before
    public void setUp(){
        preferencesInteractor.clear();
    }

    @Test
    public void getFormattedValueRangeIgnoresMax() throws Exception {

        assertEquals("0%" , expansionCategoryFormatter.getFormattedValueRange(Category.LIGHT, new ExpansionValueRange(0, 32), getContext()));
        assertEquals("0%" , expansionCategoryFormatter.getFormattedValueRange(Category.LIGHT, new ExpansionValueRange(0, 100), getContext()));
    }

    @Test
    public void getFormattedValueRangeConvertsTemperature() throws Exception {

        assertEquals("0°" , expansionCategoryFormatter.getFormattedValueRange(Category.TEMPERATURE, new ExpansionValueRange(0, 32), getContext()));

        preferencesInteractor.edit().putBoolean(PreferencesInteractor.USE_CELSIUS, false)
                             .apply();

        assertEquals("32°" , expansionCategoryFormatter.getFormattedValueRange(Category.TEMPERATURE, new ExpansionValueRange(0, 100), getContext()));
    }

    @Test
    public void getUnitConverter() throws Exception {
        assertEquals(UnitConverter.IDENTITY, expansionCategoryFormatter.getUnitConverter(Category.LIGHT));
        assertEquals(UnitConverter.IDENTITY, expansionCategoryFormatter.getUnitConverter(Category.UNKNOWN));
    }

    @Test
    public void getFormattedAttributionValueRange() throws Exception {
        assertEquals("set at 0°" , expansionCategoryFormatter.getFormattedAttributionValueRange(Category.TEMPERATURE, new ExpansionValueRange(0, 32), getContext()));

        preferencesInteractor.edit().putBoolean(PreferencesInteractor.USE_CELSIUS, false)
                             .apply();

        assertEquals("set at 32°" , expansionCategoryFormatter.getFormattedAttributionValueRange(Category.TEMPERATURE, new ExpansionValueRange(0, 100), getContext()));
    }

    @Test
    public void getSuffix() throws Exception {
        assertEquals("°", expansionCategoryFormatter.getSuffix(Category.TEMPERATURE));
        assertEquals("%", expansionCategoryFormatter.getSuffix(Category.LIGHT));
        assertEquals("", expansionCategoryFormatter.getSuffix(Category.UNKNOWN));
    }

}