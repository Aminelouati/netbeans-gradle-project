package org.netbeans.gradle.project.properties.global;

import java.net.URL;
import org.netbeans.gradle.project.util.NbFileUtils;

@SuppressWarnings("serial")
public class OtherOptionsPanel extends javax.swing.JPanel implements GlobalSettingsEditor {
    private static final URL HELP_URL = NbFileUtils.getSafeURL("https://github.com/kelemen/netbeans-gradle-project/wiki/Global-Settings");

    public OtherOptionsPanel() {
        initComponents();
    }

    @Override
    public void updateSettings(GlobalGradleSettings globalSettings) {
        jCompileOnSaveCheckbox.setSelected(globalSettings.compileOnSave().getValue());
        jProjectCacheSize.setValue(globalSettings.projectCacheSize().getValue());
    }

    @Override
    public void saveSettings(GlobalGradleSettings globalSettings) {
        globalSettings.projectCacheSize().setValue(getProjectCacheSize(globalSettings));
        globalSettings.compileOnSave().setValue(jCompileOnSaveCheckbox.isSelected());
    }

    @Override
    public SettingsEditorProperties getProperties() {
        SettingsEditorProperties.Builder result = new SettingsEditorProperties.Builder(this);
        result.setHelpUrl(HELP_URL);

        return result.create();
    }

    private int getProjectCacheSize(GlobalGradleSettings globalSettings) {
        Object value = jProjectCacheSize.getValue();
        int result;
        if (value instanceof Number) {
            result = ((Number)value).intValue();
        }
        else {
            result = globalSettings.projectCacheSize().getValue();
        }
        return result > 0 ? result : 1;
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jCompileOnSaveCheckbox = new javax.swing.JCheckBox();
        jProjectCacheSizeLabel = new javax.swing.JLabel();
        jProjectCacheSize = new javax.swing.JSpinner();

        org.openide.awt.Mnemonics.setLocalizedText(jCompileOnSaveCheckbox, org.openide.util.NbBundle.getMessage(OtherOptionsPanel.class, "OtherOptionsPanel.jCompileOnSaveCheckbox.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jProjectCacheSizeLabel, org.openide.util.NbBundle.getMessage(OtherOptionsPanel.class, "OtherOptionsPanel.jProjectCacheSizeLabel.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jProjectCacheSizeLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jProjectCacheSize, javax.swing.GroupLayout.PREFERRED_SIZE, 95, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jCompileOnSaveCheckbox))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jProjectCacheSizeLabel)
                    .addComponent(jProjectCacheSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jCompileOnSaveCheckbox)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox jCompileOnSaveCheckbox;
    private javax.swing.JSpinner jProjectCacheSize;
    private javax.swing.JLabel jProjectCacheSizeLabel;
    // End of variables declaration//GEN-END:variables
}
