package is.hello.sense.flows.expansions.interactors;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.v2.expansions.Category;
import is.hello.sense.api.model.v2.expansions.Expansion;
import is.hello.sense.graph.InteractorSubject;
import is.hello.sense.interactors.ValueInteractor;
import is.hello.sense.util.NotTested;
import rx.Observable;

public class ExpansionsInteractor extends ValueInteractor<ArrayList<Expansion>> {

    private final ApiService apiService;
    public InteractorSubject<ArrayList<Expansion>> expansions = this.subject;

    public ExpansionsInteractor(@NonNull final ApiService apiService){
        this.apiService = apiService;
    }

    @Override
    protected boolean isDataDisposable() {
        return true;
    }

    @Override
    protected boolean canUpdate() {
        return true;
    }

    @Override
    protected Observable<ArrayList<Expansion>> provideUpdateObservable() {
        return apiService.getExpansions();
    }

    public Observable<Expansion> findByCategories(@NonNull final List<Category> category){
        return expansions.flatMap(Observable::from)
                       .filter(expansion -> category.contains(expansion.getCategory()));
    }


    /**
     * Retrieves an Expansion from {@link ExpansionsInteractor#expansions} if it has the same
     * Category.
     *
     * @param category search for an expansion using this category.
     * @return an Expansion for {@link Category#LIGHT} or {@link Category#TEMPERATURE}
     */
    @NotTested
    @Nullable
    public Expansion getExpansion(final Category category) {
        if (!this.expansions.hasValue()) {
            return null;
        }
        final List<Expansion> expansions = this.expansions.getValue();
        for (final Expansion expansion : expansions) {
            if (expansion.getCategory() == category) {
                return expansion;
            }
        }
        return null;
    }
}
