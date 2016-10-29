package org.netbeans.gradle.project.model;

import java.io.IOException;
import java.nio.file.Path;

public interface PersistentModelStore<T> {
    public void persistModel(T model, Path dest) throws IOException;

    public T tryLoadModel(Path src) throws IOException;
}
