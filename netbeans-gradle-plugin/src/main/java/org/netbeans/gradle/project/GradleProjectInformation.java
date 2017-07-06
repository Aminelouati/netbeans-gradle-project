package org.netbeans.gradle.project;

import java.beans.PropertyChangeListener;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.Icon;
import org.jtrim2.property.PropertyFactory;
import org.jtrim2.property.PropertySource;
import org.jtrim2.swing.concurrent.SwingExecutors;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.gradle.project.properties.SwingPropertyChangeForwarder;

public final class GradleProjectInformation implements ProjectInformation {
    private final NbGradleProject project;
    private final AtomicReference<SwingPropertyChangeForwarder> changeListenersRef;

    public GradleProjectInformation(NbGradleProject project) {
        this.project = project;
        this.changeListenersRef = new AtomicReference<>(null);
    }

    private SwingPropertyChangeForwarder getChangeListeners() {
        SwingPropertyChangeForwarder result = changeListenersRef.get();
        if (result == null) {
            SwingPropertyChangeForwarder.Builder combinedListeners
                    = new SwingPropertyChangeForwarder.Builder(SwingExecutors.getStrictExecutor(false));
            PropertySource<String> displayName = PropertyFactory.lazilyNotifiedSource(project.getDisplayInfo().displayName());
            combinedListeners.addProperty(PROP_DISPLAY_NAME, displayName);
            result = combinedListeners.create();

            if (!changeListenersRef.compareAndSet(null, result)) {
                result = changeListenersRef.get();
            }
        }
        return result;
    }

    @Override
    public Icon getIcon() {
        return NbIcons.getGradleIconAsIcon();
    }

    @Override
    public String getName() {
        return project.getName();
    }

    @Override
    public String getDisplayName() {
        return project.getDisplayInfo().displayName().getValue();
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        getChangeListeners().addPropertyChangeListener(pcl);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener pcl) {
        getChangeListeners().removePropertyChangeListener(pcl);
    }

    @Override
    public Project getProject() {
        return project;
    }

}
