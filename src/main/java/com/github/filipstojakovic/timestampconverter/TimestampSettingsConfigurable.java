package com.github.filipstojakovic.timestampconverter;

import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class TimestampSettingsConfigurable implements Configurable {

    private JPanel panel;
    private JTextField dateFormatField;

    private TimestampSettingsState settings;

    public TimestampSettingsConfigurable() {
        settings = TimestampSettingsState.getInstance();
    }

    @Override
    public @Nls String getDisplayName() {
        return "Timestamp Converter";
    }

    @Override
    public @Nullable JComponent createComponent() {
        panel = new JPanel(new BorderLayout(10, 10));

        JLabel label = new JLabel("Date format:");
        dateFormatField = new JTextField(settings.getDateFormat());

        JPanel innerPanel = new JPanel(new BorderLayout(5, 5));
        innerPanel.add(label, BorderLayout.WEST);
        innerPanel.add(dateFormatField, BorderLayout.CENTER);

        panel.add(innerPanel, BorderLayout.NORTH);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        return panel;
    }

    @Override
    public boolean isModified() {
        return !dateFormatField.getText().equals(settings.getDateFormat());
    }

    @Override
    public void apply() {
        settings.setDateFormat(dateFormatField.getText());
    }

    @Override
    public void reset() {
        dateFormatField.setText(settings.getDateFormat());
    }

    @Override
    public void disposeUIResources() {
        panel = null;
        dateFormatField = null;
    }
}
