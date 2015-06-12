package is.hello.sense.util.markup;

import android.support.annotation.NonNull;

import com.fasterxml.jackson.databind.module.SimpleModule;

import is.hello.sense.util.markup.text.MarkupString;

public final class MarkupJacksonModule extends SimpleModule {
    public MarkupJacksonModule(@NonNull MarkupProcessor processor) {
        super("MarkupJacksonModule");

        addDeserializer(MarkupString.class, new MarkupDeserializer(processor));
    }

    public MarkupJacksonModule() {
        this(new MarkupProcessor());
    }
}
