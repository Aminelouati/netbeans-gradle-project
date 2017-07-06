package org.netbeans.gradle.project.others;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.netbeans.gradle.project.util.TestDetectUtils;
import org.openide.modules.ModuleInfo;
import org.openide.util.Lookup;

public final class PluginClassFactory {
    private final String moduleNamePrefix;
    private final AtomicReference<ModuleInfo> moduleInfoRef;

    public PluginClassFactory(String moduleNamePrefix) {
        this.moduleNamePrefix = Objects.requireNonNull(moduleNamePrefix, "moduleNamePrefix");
        this.moduleInfoRef = new AtomicReference<>(null);
    }

    private ModuleInfo tryFindModuleInfo() {
        if (TestDetectUtils.isRunningTests()) {
            // During tests, this might try to install modules, which is not possible
            return null;
        }

        for (ModuleInfo info: Lookup.getDefault().lookupAll(ModuleInfo.class)) {
            String codeName = info.getCodeName();
            if (codeName != null && codeName.startsWith(moduleNamePrefix)) {
                return info;
            }
        }

        return null;
    }

    private ModuleInfo tryGetModuleInfo() {
        ModuleInfo result = moduleInfoRef.get();
        if (result == null) {
            moduleInfoRef.compareAndSet(null, tryFindModuleInfo());
            result = moduleInfoRef.get();
        }
        return result;
    }

    public ClassLoader tryGetModuleClassLoader() {
        ModuleInfo moduleInfo = tryGetModuleInfo();
        if (moduleInfo == null) {
            return null;
        }

        try {
            return moduleInfo.isEnabled() ? moduleInfo.getClassLoader() : null;
        } catch (UnsupportedOperationException | IllegalArgumentException ex) {
            return null;
        }
    }

    public Class<?> tryFindClass(String className) {
        try {
            ClassLoader classLoader = tryGetModuleClassLoader();
            if (classLoader != null) {
                return Class.forName(className, true, classLoader);
            }
        } catch (ClassNotFoundException ex) {
        }

        return null;
    }
}