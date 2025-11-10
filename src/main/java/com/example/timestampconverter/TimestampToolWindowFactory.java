package com.example.timestampconverter;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
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
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TimestampToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        TimestampSettingsState settings = TimestampSettingsState.getInstance();

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));

        // --- Input controls ---
        JBTextField input = new JBTextField();
        JButton convert = new JButton("Convert");

        // --- Timezone dropdown ---
        List<String> zones = ZoneId.getAvailableZoneIds().stream()
                .sorted()
                .map(id -> {
                    ZoneId zone = ZoneId.of(id);
                    ZoneOffset offset = zone.getRules().getOffset(Instant.now());
                    String offsetId = offset.getId().replace("Z", "+00:00");
                    return String.format("%s (%s)", id, offsetId);
                })
                .toList();
        JComboBox<String> zoneSelector = new ComboBox<>(zones.toArray(new String[0]));

        // Restore previously selected zone or default to UTC
        String lastZone = settings.getLastSelectedZone();
        if (lastZone != null) {
            for (int i = 0; i < zoneSelector.getItemCount(); i++) {
                if (zoneSelector.getItemAt(i).startsWith(lastZone)) {
                    zoneSelector.setSelectedIndex(i);
                    break;
                }
            }
        } else {
            zoneSelector.setSelectedItem("UTC");
        }

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

                String selectedItem = (String) zoneSelector.getSelectedItem();
                if (selectedItem == null) return;
                String selectedZone = selectedItem.split(" ")[0]; // extract e.g. "Europe/Vienna"
                ZoneId selectedZoneId = ZoneId.of(selectedZone);
                ZoneId systemZone = ZoneId.systemDefault();

                String formattedSelected = DateTimeFormatter.ofPattern(settings.getDateFormat())
                        .withZone(selectedZoneId)
                        .format(instant);

                String formattedSystem = DateTimeFormatter.ofPattern(settings.getDateFormat())
                        .withZone(systemZone)
                        .format(instant);

                // Remove trailing 000 if present
                String cleanTs = (inputText.endsWith("000") && inputText.length() > 10)
                        ? inputText.substring(0, inputText.length() - 3)
                        : inputText;

                String result = cleanTs + " → " +
                        formattedSelected + " (" + selectedZone + ")" +
                        " | " + formattedSystem + " (Local)";

                // Prepend to history (latest first)
                String existing = historyArea.getText();
                String newHistory = result + "\n" + existing;
                historyArea.setText(newHistory);
                historyArea.setCaretPosition(0);

                // Save to persistent storage
                settings.getHistory().addFirst(result);
                settings.setLastSelectedZone(selectedZone);
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
