package is.hello.sense.zendesk;

import dagger.Module;
import is.hello.sense.ui.fragments.support.ContactSubmitFragment;
import is.hello.sense.ui.fragments.support.ContactTopicFragment;
import is.hello.sense.ui.fragments.support.TicketListFragment;

@Module(complete = false, injects = {
        ZendeskPresenter.class,
        ContactTopicFragment.class,
        ContactSubmitFragment.class,
        TicketListFragment.class,
})
public class ZendeskModule {
}
