package is.hello.sense.graph;

import dagger.Module;
import is.hello.sense.graph.presenters.ZendeskPresenter;
import is.hello.sense.ui.fragments.support.ContactUsFragment;
import is.hello.sense.ui.fragments.support.SelectTopicFragment;

@Module(complete = false, injects = {
        ZendeskPresenter.class,
        SelectTopicFragment.class,
        ContactUsFragment.class,
})
public class ZendeskModule {
}
