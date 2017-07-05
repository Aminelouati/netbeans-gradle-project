package org.netbeans.gradle.project.view;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationController;
import org.jtrim.cancel.CancellationSource;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.util.NbFileUtils;
import org.netbeans.gradle.project.util.NbTaskExecutors;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public final class DeleteProjectAction extends AbstractAction {
    private static final long serialVersionUID = -386797460711624644L;

    private static final Logger LOGGER = Logger.getLogger(DeleteProjectAction.class.getName());

    private final NbGradleProject project;

    public DeleteProjectAction(NbGradleProject project) {
        super(NbStrings.getDeleteProjectCaption());

        ExceptionHelper.checkNotNullArgument(project, "project");
        this.project = project;
    }

    private void closeAffectedProjects() {
        OpenProjects openProjects = OpenProjects.getDefault();
        FileObject rootDir = project.getProjectDirectory();

        List<Project> toClose = new ArrayList<>();
        toClose.add(project);
        for (Project opened: openProjects.getOpenProjects()) {
            if (FileUtil.isParentOf(rootDir, opened.getProjectDirectory())) {
                toClose.add(project);
            }
        }

        openProjects.close(toClose.toArray(new Project[0]));
    }

    private void doRemoveProject(CancellationToken cancelToken) {
        try {
            closeAffectedProjects();

            NbFileUtils.deleteDirectory(cancelToken, project.getProjectDirectory());
        } catch (Exception ex) {
            LOGGER.log(Level.INFO, "There was an error while trying to remove the project.", ex);
            String title = NbStrings.getErrorDeleteProjectTitle();
            String message = NbStrings.getErrorDeleteProject(ex);
            JOptionPane.showMessageDialog(null, message, title, JOptionPane.WARNING_MESSAGE);
        }
    }

    private ProgressHandle createProgress(final CancellationController cancelController) {
        String caption = NbStrings.getDeleteProjectProgress(project.getDisplayName());
        return ProgressHandle.createHandle(caption, () -> {
            cancelController.cancel();
            return true;
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object[] answers = {NbStrings.getYesOption(), NbStrings.getNoOption()};
        String message = NbStrings.getConfirmDeleteProject(project.getDisplayName());
        String title = NbStrings.getConfirmDeleteProjectTitle();
        if (JOptionPane.showOptionDialog(null, message, title, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, answers, answers[1]) != 0) {
            return;
        }

        CancellationSource cancel = Cancellation.createCancellationSource();
        final ProgressHandle progress = createProgress(cancel.getController());

        progress.start();
        NbTaskExecutors.DEFAULT_EXECUTOR.execute(cancel.getToken(), this::doRemoveProject, (canceled, error) -> {
            NbTaskExecutors.defaultCleanup(canceled, error);
            progress.finish();
        });
    }
}
