package is.hello.sense.flows.expansions.routers;

import is.hello.sense.api.model.v2.expansions.Expansion;

public interface ExpansionSettingsRouter {

    void showExpansionList();

    void showExpansionDetail(final long expansionId);

    void showExpansionAuth(final long expansionId,
                           final String initialUrl,
                           final String completionUrl);

    void showConfigurationSelection(final Expansion expansion);

    void showConfigurationSelection(final long expansionId);


}
