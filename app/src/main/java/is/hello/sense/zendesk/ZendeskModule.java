package is.hello.sense.zendesk;

import dagger.Module;
import is.hello.sense.ui.fragments.support.TicketDetailFragment;
import is.hello.sense.ui.fragments.support.TicketListFragment;
import is.hello.sense.ui.fragments.support.TicketSelectTopicFragment;
import is.hello.sense.ui.fragments.support.TicketSubmitFragment;

@Module(complete = false, injects = {
        TicketsInteractor.class,
        TicketSelectTopicFragment.class,
        TicketSubmitFragment.class,
        TicketListFragment.class,
        TicketDetailFragment.class,
})
public class ZendeskModule {
}
