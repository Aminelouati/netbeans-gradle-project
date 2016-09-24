package org.netbeans.gradle.project;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.api.entry.GradleProjectIDs;
import org.netbeans.gradle.project.extensions.NbGradleExtensionRef;
import org.netbeans.gradle.project.lookups.DynamicLookup;
import org.netbeans.gradle.project.lookups.ProjectLookupHack;
import org.netbeans.spi.project.LookupProvider;
import org.netbeans.spi.project.support.LookupProviderSupport;
import org.netbeans.spi.project.ui.support.UILookupMergerSupport;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;

public final class NbGradleProjectLookups {
    private final Lookup defaultLookup;
    private final DynamicLookup mainLookup;

    public NbGradleProjectLookups(Lookup defaultLookup) {
        ExceptionHelper.checkNotNullArgument(defaultLookup, "defaultLookup");

        this.defaultLookup = defaultLookup;
        this.mainLookup = new DynamicLookup(defaultLookup);
    }

    public void updateExtensions(
            final NbGradleProject project,
            NbGradleProjectExtensions extensions) {

        List<NbGradleExtensionRef> extensionRefs = extensions.getExtensionRefs();

        List<LookupProvider> allLookupProviders = new ArrayList<>(extensionRefs.size() + 3);

        allLookupProviders.add(moveToLookupProvider(defaultLookup));
        for (NbGradleExtensionRef extension: extensionRefs) {
            allLookupProviders.add(moveToLookupProvider(extension.getProjectLookup()));
        }

        allLookupProviders.add(moveToLookupProvider(getLookupMergers()));

        allLookupProviders.addAll(moveToLookupProvider(getLookupsFromAnnotations(defaultLookup)));

        Lookup combinedLookupProviders = LookupProviderSupport.createCompositeLookup(Lookup.EMPTY, Lookups.fixed(allLookupProviders.toArray()));
        final Lookup combinedAllLookups = new ProxyLookup(combinedLookupProviders);
        mainLookup.replaceLookups(new ProjectLookupHack(new ProjectLookupHack.LookupContainer() {
            @Override
            public NbGradleProject getProject() {
                return project;
            }

            @Override
            public Lookup getLookup() {
                return combinedAllLookups;
            }

            @Override
            public Lookup getLookupAndActivate() {
                project.ensureLoadRequested();
                return combinedAllLookups;
            }
        }));
    }

    public Lookup getMainLookup() {
        // TODO: We could protect the returned Lookup from being cast to DynamicLookup by
        //       the caller and then being modified.
        return mainLookup;
    }

    private static List<LookupProvider> moveToLookupProvider(List<Lookup> lookups) {
        List<LookupProvider> result = new ArrayList<>(lookups.size());
        for (Lookup lookup: lookups) {
            result.add(moveToLookupProvider(lookup));
        }
        return result;
    }

    private static LookupProvider moveToLookupProvider(final Lookup lookup) {
        ExceptionHelper.checkNotNullArgument(lookup, "lookup");
        return new LookupProvider() {
            @Override
            public Lookup createAdditionalLookup(Lookup baseContext) {
                return lookup;
            }
        };
    }

    private static List<Lookup> extractLookupsFromProviders(
            Lookup baseContext,
            Lookup providerContainer) {
        // baseContext must contain the Project instance.

        List<Lookup> result = new LinkedList<>();
        for (LookupProvider provider: providerContainer.lookupAll(LookupProvider.class)) {
            result.add(provider.createAdditionalLookup(baseContext));
        }

        return result;
    }

    private static List<Lookup> getLookupsFromAnnotations(Lookup baseContext) {
        Lookup lookupProviders = Lookups.forPath("Projects/" + GradleProjectIDs.MODULE_NAME + "/Lookup");
        return extractLookupsFromProviders(baseContext, lookupProviders);
    }

    private static Lookup getLookupMergers() {
        return Lookups.fixed(
                UILookupMergerSupport.createPrivilegedTemplatesMerger(),
                UILookupMergerSupport.createProjectProblemsProviderMerger(),
                UILookupMergerSupport.createRecommendedTemplatesMerger()
        );
    }
}
