package org.netbeans.gradle.project.license;

import java.nio.file.Path;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.gradle.project.util.NbBiFunction;
import org.netbeans.gradle.project.util.NbFileUtils;
import org.netbeans.gradle.project.util.NbFunction;
import org.netbeans.gradle.project.util.NbTaskExecutors;

public final class LicenseManagers {
    public static LicenseManager<NbGradleModel> createProjectLicenseManager(LicenseStore<DefaultLicenseDef> licenseStore) {
        NbFunction<NbGradleModel, Path> licenseRootProvider = new NbFunction<NbGradleModel, Path>() {
            @Override
            public Path apply(NbGradleModel ownerModel) {
                return ownerModel.getSettingsDir();
            }
        };

        NbFunction<NbGradleModel, String> modelNameProvider = new NbFunction<NbGradleModel, String>() {
            @Override
            public String apply(NbGradleModel ownerModel) {
                return NbFileUtils.getFileNameStr(ownerModel.getSettingsDir());
            }
        };

        TaskExecutor executor = NbTaskExecutors.DEFAULT_EXECUTOR;

        return createLicenseManager(executor, licenseStore, licenseRootProvider, modelNameProvider);
    }

    public static <T> LicenseManager<T> createLicenseManager(
            final TaskExecutor executor,
            final LicenseStore<DefaultLicenseDef> licenseStore,
            final NbFunction<? super T, ? extends Path> licenseRootProvider,
            final NbFunction<? super T, ? extends String> modelNameProvider) {

        ExceptionHelper.checkNotNullArgument(licenseStore, "licenseStore");
        ExceptionHelper.checkNotNullArgument(licenseRootProvider, "licenseRootProvider");
        ExceptionHelper.checkNotNullArgument(modelNameProvider, "modelNameProvider");

        NbBiFunction<T, LicenseHeaderInfo, DefaultLicenseKey> licenseKeyFactory = new NbBiFunction<T, LicenseHeaderInfo, DefaultLicenseKey>() {
            @Override
            public DefaultLicenseKey apply(T ownerModel, LicenseHeaderInfo licenseHeader) {
                return tryCreateLicenseKey(ownerModel, licenseHeader, licenseRootProvider);
            }
        };

        NbBiFunction<T, DefaultLicenseKey, DefaultLicenseDef> licenseDefFactory = new NbBiFunction<T, DefaultLicenseKey, DefaultLicenseDef>() {
            @Override
            public DefaultLicenseDef apply(T ownerModel, DefaultLicenseKey licenseKey) {
                return createLicenseDef(ownerModel, licenseKey, modelNameProvider);
            }
        };

        return new LicenseManager<>(executor, licenseStore, licenseKeyFactory, licenseDefFactory);
    }

    private static <T> DefaultLicenseKey tryCreateLicenseKey(
            T ownerModel,
            LicenseHeaderInfo licenseHeader,
            NbFunction<? super T, ? extends Path> licenseRootProvider) {
        Path licenseTemplateFile = licenseHeader.getLicenseTemplateFile();
        if (licenseTemplateFile == null) {
            return null;
        }

        Path rootPath = licenseRootProvider.apply(ownerModel);
        Path srcPath = rootPath.resolve(licenseTemplateFile);
        return new DefaultLicenseKey(licenseHeader.getLicenseName(), srcPath);
    }

    private static <T> DefaultLicenseDef createLicenseDef(
            T ownerModel,
            DefaultLicenseKey licenseKey,
            NbFunction<? super T, ? extends String> modelNameProvider) {

        String name = licenseKey.getName();
        String modelName = modelNameProvider.apply(ownerModel);
        String displayName = name + " (" + modelName + ")";
        return new DefaultLicenseDef(licenseKey.getSrcPath(), name, displayName);
    }

    private LicenseManagers() {
        throw new AssertionError();
    }
}
