package com.puddingkc.vrchatstatustask.vrchatstatustask.gui;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.puddingkc.vrchatstatustask.vrchatstatustask.VRChatStatusService;
import com.puddingkc.vrchatstatustask.vrchatstatustask.configs.VRChatStatusSettings;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class VRChatStatusConfigurable implements Configurable {

    private JTextField addressField;
    private JSpinner portSpinner;
    private JSpinner intervalSpinner;
    private JTextArea messageTemplateArea;
    private JCheckBox enabledOsc;
    private JCheckBox textCropping;

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return "VRChatStatusTask";
    }

    @Override
    public @Nullable JComponent createComponent() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(5);

        addressField = new JTextField();
        portSpinner = new JSpinner(new SpinnerNumberModel(9000, 1, 65535, 1));
        intervalSpinner = new JSpinner(new SpinnerNumberModel(5, 3, 60, 1));
        enabledOsc = new JCheckBox("启用 OSC 推送");
        textCropping = new JCheckBox("启用长文本裁切");

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("OSC 地址:"), gbc);
        gbc.gridx = 1; panel.add(addressField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("OSC 端口:"), gbc);
        gbc.gridx = 1; panel.add(portSpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("更新间隔:"), gbc);
        gbc.gridx = 1; panel.add(intervalSpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        panel.add(enabledOsc, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        panel.add(textCropping, gbc);

        gbc.insets = JBUI.insets(15, 5, 5, 5);
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 1;
        panel.add(new JLabel("消息模板:"), gbc);

        messageTemplateArea = new JTextArea(6, 30);
        messageTemplateArea.setBorder(BorderFactory.createEtchedBorder());
        messageTemplateArea.setLineWrap(true);
        messageTemplateArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JBScrollPane(messageTemplateArea);
        gbc.gridx = 1;
        panel.add(scrollPane, gbc);

        gbc.insets = JBUI.insets(0, 5, 5, 5);
        JLabel helpLabel = new JLabel("可用变量: {project}, {file}, {errors}, {warnings}, {uptime}");
        helpLabel.setFont(JBUI.Fonts.smallFont());
        helpLabel.setEnabled(false);
        gbc.gridx = 1; gbc.gridy = 6;
        panel.add(helpLabel, gbc);

        gbc.insets = JBUI.insets(20, 5, 5, 5);
        JLabel authorLabel = new JLabel("Author: PuddingKC");
        authorLabel.setEnabled(false);
        authorLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 2;
        panel.add(authorLabel, gbc);

        gbc.gridy = 8;
        gbc.weighty = 1.0;
        panel.add(new JLabel(""), gbc);

        return panel;
    }

    @Override
    public boolean isModified() {
        VRChatStatusSettings settings = VRChatStatusSettings.getInstance();
        return !addressField.getText().equals(settings.oscAddress) ||
                (int) portSpinner.getValue() != settings.oscPort ||
                (int) intervalSpinner.getValue() != settings.updateInterval ||
                !messageTemplateArea.getText().equals(settings.messageTemplate) ||
                enabledOsc.isSelected() != settings.enabledOsc ||
                textCropping.isSelected() != settings.textCropping;
    }

    @Override
    public void apply() {
        VRChatStatusSettings settings = VRChatStatusSettings.getInstance();
        settings.oscAddress = addressField.getText();
        settings.oscPort = (int) portSpinner.getValue();
        settings.updateInterval = (int) intervalSpinner.getValue();
        settings.messageTemplate = messageTemplateArea.getText();
        settings.enabledOsc = enabledOsc.isSelected();
        settings.textCropping = textCropping.isSelected();

        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            VRChatStatusService service = project.getService(VRChatStatusService.class);
            if (service != null) {
                service.restart();
            }
        }
    }

    @Override
    public void reset() {
        VRChatStatusSettings settings = VRChatStatusSettings.getInstance();
        addressField.setText(settings.oscAddress);
        portSpinner.setValue(settings.oscPort);
        intervalSpinner.setValue(settings.updateInterval);
        messageTemplateArea.setText(settings.messageTemplate);
        enabledOsc.setSelected(settings.enabledOsc);
        textCropping.setSelected(settings.textCropping);
    }
}
