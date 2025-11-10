package com.example.timestampconverter;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.*;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;

import javax.swing.*;
import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class TimestampToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        // --- Input controls ---
        JBTextField input = new JBTextField();
        JButton convert = new JButton("Convert");

        // --- Timezone dropdown ---
        List<String> zones = Arrays.stream(TimeZone.getAvailableIDs())
                .sorted()
                .toList();
        JComboBox<String> zoneSelector = new JComboBox<>(zones.toArray(new String[0]));
        zoneSelector.setSelectedItem("UTC");

        // --- History area (all past conversions) ---
        JBTextArea historyArea = new JBTextArea(15, 30);
        historyArea.setEditable(false);
        historyArea.setLineWrap(true);
        historyArea.setWrapStyleWord(true);
        JScrollPane historyScroll = new JBScrollPane(historyArea);

        // --- Clear history ---
        JButton clearHistory = new JButton("Clear History");
        clearHistory.addActionListener(e -> historyArea.setText(""));

        // --- Input section ---
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

        // --- Bottom (history + clear button) ---
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.add(new JBLabel("History:"), BorderLayout.NORTH);
        bottomPanel.add(historyScroll, BorderLayout.CENTER);

        JPanel clearPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        clearPanel.add(clearHistory);
        bottomPanel.add(clearPanel, BorderLayout.SOUTH);

        // --- Main layout ---
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(bottomPanel, BorderLayout.CENTER);

        // --- Conversion logic ---
        convert.addActionListener(e -> {
            try {
                String inputText = input.getText().trim();
                long ts = Long.parseLong(inputText);

                // Handle seconds vs milliseconds
                if (ts < 10000000000L) ts *= 1000; // seconds → millis
                Instant instant = Instant.ofEpochMilli(ts);

                String zoneId = (String) zoneSelector.getSelectedItem();
                ZoneId zone = ZoneId.of(zoneId);

                String formatted = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        .withZone(zone)
                        .format(instant);

                // Clean timestamp (remove trailing 000 if it was seconds→millis)
                String cleanTs = (inputText.endsWith("000") && inputText.length() > 10)
                        ? inputText.substring(0, inputText.length() - 3)
                        : inputText;

                String result = cleanTs + " → " + formatted + " (" + zoneId + ")";

                // Prepend to history (latest first)
                String existing = historyArea.getText();
                historyArea.setText(result + "\n" + existing);
                historyArea.setCaretPosition(0);
            } catch (Exception ex) {
                historyArea.setText("Invalid timestamp.\n" + historyArea.getText());
            }
        });

        // --- Register content ---
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(panel, "", false);
        toolWindow.getContentManager().addContent(content);
    }
}
