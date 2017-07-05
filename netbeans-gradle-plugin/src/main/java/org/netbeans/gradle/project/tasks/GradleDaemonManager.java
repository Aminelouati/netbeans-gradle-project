package org.netbeans.gradle.project.tasks;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.cancel.CancelableWaits;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationController;
import org.jtrim.cancel.CancellationSource;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.gradle.project.api.task.CommandCompleteListener;

public final class GradleDaemonManager {
    private static final Logger LOGGER = Logger.getLogger(GradleDaemonManager.class.getName());

    private static final ReentrantLock QUEUE_LOCK = new ReentrantLock(true);

    private static void runNonBlockingGradleTask(
            CancellationToken cancelToken,
            DaemonTask task,
            ProgressHandle progress) {

        CancelableWaits.lock(cancelToken, QUEUE_LOCK);
        try {
            progress.switchToIndeterminate();
            task.run(cancelToken, progress);
        } finally{
            QUEUE_LOCK.unlock();
        }
    }

    private static void runBlockingGradleTask(
            CancellationToken cancelToken,
            DaemonTask task,
            ProgressHandle progress) {

        // This lock/unlock is here only to wait for pending non-blocking tasks.
        CancelableWaits.lock(cancelToken, QUEUE_LOCK);
        QUEUE_LOCK.unlock();

        progress.switchToIndeterminate();
        task.run(cancelToken, progress);
    }

    public static boolean isRunningExclusiveTask() {
        return QUEUE_LOCK.isHeldByCurrentThread();
    }

    public static void submitGradleTask(
            TaskExecutor executor,
            String caption,
            DaemonTask task,
            boolean nonBlocking,
            CommandCompleteListener listener) {
        submitGradleTask(executor, new DaemonTaskDef(caption, nonBlocking, task), listener);
    }

    public static void submitGradleTask(
            TaskExecutor executor,
            final DaemonTaskDef taskDef,
            CommandCompleteListener listener) {
        submitGradleTask(executor, taskDef.toFactory(), listener);
    }

    public static void submitGradleTask(
            TaskExecutor executor,
            final DaemonTaskDefFactory taskDefFactory,
            final CommandCompleteListener listener) {
        ExceptionHelper.checkNotNullArgument(executor, "executor");
        ExceptionHelper.checkNotNullArgument(taskDefFactory, "taskDefFactory");
        ExceptionHelper.checkNotNullArgument(listener, "listener");

        final CancellationSource cancel = Cancellation.createCancellationSource();
        final String origDisplayName = taskDefFactory.getDisplayName();

        final ReplaceableProgressHandle progress = new ReplaceableProgressHandle(cancel.getController());
        final AtomicBoolean inProgress = new AtomicBoolean(false);

        cancel.getToken().addCancellationListener(() -> {
            if (!inProgress.get()) {
                progress.finish();
            }
        });

        progress.start(origDisplayName);
        executor.execute(cancel.getToken(), (CancellationToken cancelToken) -> {
            inProgress.set(true);
            cancelToken.checkCanceled();

            DaemonTaskDef taskDef;
            try {
                taskDef = taskDefFactory.tryCreateTaskDef(cancelToken);
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Failed to create DaemonTaskDef.", ex);
                return;
            }

            if (taskDef == null) {
                return;
            }

            String displayName = taskDef.getCaption();
            boolean nonBlocking = taskDef.isNonBlocking();
            DaemonTask task = taskDef.getTask();

            if (!Objects.equals(displayName, origDisplayName)) {
                progress.start(displayName);
            }

            if (nonBlocking) {
                runNonBlockingGradleTask(cancelToken, task, progress.getCurrentHandle());
            }
            else {
                runBlockingGradleTask(cancelToken, task, progress.getCurrentHandle());
            }
        }, (boolean canceled, Throwable error) -> {
            try {
                if (!canceled) {
                    listener.onComplete(error);
                }
            } finally {
                progress.finish();
            }

            if (canceled) {
                LOGGER.log(Level.INFO, "Canceled task: {0}", origDisplayName);
            }
        });
    }

    private static final class ReplaceableProgressHandle {
        private final AtomicReference<ProgressHandle> handleRef;
        private final CancellationController cancelController;

        public ReplaceableProgressHandle(CancellationController cancelController) {
            ExceptionHelper.checkNotNullArgument(cancelController, "cancelController");

            this.handleRef = new AtomicReference<>(null);
            this.cancelController = cancelController;
        }

        public void start(String displayName) {
            ProgressHandle newHandle = ProgressHandle.createHandle(displayName, () -> {
                cancelController.cancel();
                return true;
            });

            newHandle.start();
            newHandle.suspend("");

            ProgressHandle prevHandle = handleRef.getAndSet(newHandle);
            if (prevHandle != null) {
                prevHandle.finish();
            }
        }

        public ProgressHandle getCurrentHandle() {
            return handleRef.get();
        }

        public void finish() {
            ProgressHandle prevRef = handleRef.getAndSet(null);
            if (prevRef != null) {
                prevRef.finish();
            }
        }
    }

    private GradleDaemonManager() {
        throw new AssertionError();
    }
}
