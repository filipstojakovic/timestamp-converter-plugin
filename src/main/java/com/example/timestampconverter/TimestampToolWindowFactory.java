package com.example.timestampconverter;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;

import javax.swing.*;
import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

public class TimestampToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        TimestampSettingsState settings = TimestampSettingsState.getInstance();

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));

        // --- Input controls ---
        JBTextField input = new JBTextField();
        JButton convert = new JButton("Convert");

        // --- Timezone dropdown ---
        List<String> zones = Arrays.stream(TimeZone.getAvailableIDs())
                .sorted()
                .collect(Collectors.toList());
        JComboBox<String> zoneSelector = new JComboBox<>(zones.toArray(new String[0]));

        // Restore previously selected zone or default to UTC
        String savedZone = settings.getLastSelectedZone();
        zoneSelector.setSelectedItem(savedZone != null ? savedZone : "UTC");

        // --- History area ---
        JBTextArea historyArea = new JBTextArea(15, 30);
        historyArea.setEditable(false);
        historyArea.setLineWrap(true);
        historyArea.setWrapStyleWord(true);
        JScrollPane historyScroll = new JBScrollPane(historyArea);

        // Restore saved history
        if (!settings.getHistory().isEmpty()) {
            historyArea.setText(String.join("\n", settings.getHistory()) + "\n");
        }

        // --- Clear history ---
        JButton clearHistory = new JButton("Clear History");
        clearHistory.addActionListener(e -> {
            historyArea.setText("");
            settings.getHistory().clear();
            settings.saveState();
        });

        // --- Layout setup ---
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.add(new JBLabel("Timestamp:"), BorderLayout.WEST);
        inputPanel.add(input, BorderLayout.CENTER);
        inputPanel.add(convert, BorderLayout.EAST);

        JPanel zonePanel = new JPanel(new BorderLayout(5, 5));
        zonePanel.add(new JBLabel("Time zone:"), BorderLayout.WEST);
        zonePanel.add(zoneSelector, BorderLayout.CENTER);

        JPanel topPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        topPanel.add(inputPanel);
        topPanel.add(zonePanel);

        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.add(new JBLabel("History:"), BorderLayout.NORTH);
        bottomPanel.add(historyScroll, BorderLayout.CENTER);

        JPanel clearPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        clearPanel.add(clearHistory);
        bottomPanel.add(clearPanel, BorderLayout.SOUTH);

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(bottomPanel, BorderLayout.CENTER);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Conversion logic ---
        convert.addActionListener(e -> {
            try {
                String inputText = input.getText().trim();
                long ts = Long.parseLong(inputText);
                if (ts < 10000000000L) ts *= 1000; // seconds → millis
                Instant instant = Instant.ofEpochMilli(ts);

                String zoneId = (String) zoneSelector.getSelectedItem();
                ZoneId selectedZone = ZoneId.of(zoneId);
                ZoneId systemZone = ZoneId.systemDefault();

                String formattedSelected = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        .withZone(selectedZone)
                        .format(instant);

                String formattedSystem = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        .withZone(systemZone)
                        .format(instant);

                // Remove trailing 000 if present
                String cleanTs = (inputText.endsWith("000") && inputText.length() > 10)
                        ? inputText.substring(0, inputText.length() - 3)
                        : inputText;

                String result = cleanTs + " → " +
                        formattedSelected + " (" + zoneId + ")" +
                        " | " + formattedSystem + " (Local)";

                // Prepend to history (latest first)
                String existing = historyArea.getText();
                String newHistory = result + "\n" + existing;
                historyArea.setText(newHistory);
                historyArea.setCaretPosition(0);

                // Save to persistent storage
                settings.getHistory().add(0, result);
                settings.setLastSelectedZone(zoneId);
                settings.saveState();
            } catch (Exception ex) {
                historyArea.setText("Invalid timestamp.\n" + historyArea.getText());
            }
        });

        // --- Register content ---
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(mainPanel, "", false);
        toolWindow.getContentManager().addContent(content);
    }
}
