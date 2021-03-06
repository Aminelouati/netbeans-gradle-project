package org.netbeans.gradle.project.util;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jtrim.utils.ExceptionHelper;

public final class NbGuiUtils {
    public static void enableBasedOnCheck(
            final AbstractButton check,
            final boolean enableValue,
            JComponent... components) {
        ExceptionHelper.checkNotNullArgument(check, "check");

        final JComponent[] componentsSnapshot = components.clone();
        ExceptionHelper.checkNotNullElements(componentsSnapshot, "components");

        ChangeListener changeListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                boolean enable = check.isSelected() == enableValue;
                for (JComponent component: componentsSnapshot) {
                    component.setEnabled(enable);
                }
            }
        };

        changeListener.stateChanged(new ChangeEvent(check));
        check.getModel().addChangeListener(changeListener);
    }

    private NbGuiUtils() {
        throw new AssertionError();
    }
}
