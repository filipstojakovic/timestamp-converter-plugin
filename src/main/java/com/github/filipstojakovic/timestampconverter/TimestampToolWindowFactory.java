package com.github.filipstojakovic.timestampconverter;

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
import java.awt.event.ActionListener;
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
        JButton convertBtn = new JButton("Convert");

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
        String[] columnNames = { "Input", "Selected Zone", "System Time" };
        javax.swing.table.DefaultTableModel model = new javax.swing.table.DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        com.intellij.ui.table.JBTable historyTable = new com.intellij.ui.table.JBTable(model);
        historyTable.setCellSelectionEnabled(true);
        historyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem copyItem = new JMenuItem("Copy");
        copyItem.addActionListener(e -> {
            int row = historyTable.getSelectedRow();
            int col = historyTable.getSelectedColumn();
            if (row >= 0 && col >= 0) {
                Object value = historyTable.getValueAt(row, col);
                if (value != null) {
                    java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(
                            value.toString());
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
                }
            }
        });
        popupMenu.add(copyItem);

        historyTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showMenu(e);
                }
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showMenu(e);
                }
            }

            private void showMenu(java.awt.event.MouseEvent e) {
                int row = historyTable.rowAtPoint(e.getPoint());
                int col = historyTable.columnAtPoint(e.getPoint());
                if (row >= 0 && col >= 0) {
                    historyTable.changeSelection(row, col, false, false);
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        JScrollPane historyScroll = new JBScrollPane(historyTable);

        // Restore saved history
        for (String entry : settings.getHistory()) {
            if (entry.contains("|||")) {
                // New format
                model.addRow(entry.split("\\|\\|\\|"));
            } else {
                // Legacy format: "Input → Selected | System"
                try {
                    String[] parts = entry.split(" → ");
                    if (parts.length == 2) {
                        String inputStr = parts[0];
                        String[] rest = parts[1].split(" \\| ");
                        if (rest.length == 2) {
                            model.addRow(new Object[] { inputStr, rest[0], rest[1] });
                        } else {
                            model.addRow(new Object[] { entry, "", "" });
                        }
                    } else {
                        model.addRow(new Object[] { entry, "", "" });
                    }
                } catch (Exception e) {
                    model.addRow(new Object[] { entry, "", "" });
                }
            }
        }

        // --- Clear history ---
        JButton clearHistory = new JButton("Clear History");
        clearHistory.addActionListener(e -> {
            model.setRowCount(0);
            settings.getHistory().clear();
            settings.saveState();
        });

        // --- Layout setup ---
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.add(new JBLabel("Timestamp:"), BorderLayout.WEST);
        inputPanel.add(input, BorderLayout.CENTER);
        inputPanel.add(convertBtn, BorderLayout.EAST);

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
        ActionListener convertAction = e -> {
            try {
                String inputText = input.getText().trim();
                if (inputText.isEmpty()) {
                    return;
                }
                long ts = Long.parseLong(inputText);
                if (ts < 10000000000L)
                    ts *= 1000; // seconds → millis
                Instant instant = Instant.ofEpochMilli(ts);

                String selectedItem = (String) zoneSelector.getSelectedItem();
                if (selectedItem == null)
                    return;
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

                String selectedCol = formattedSelected + " (" + selectedZone + ")";

                // Add to table (latest first)
                model.insertRow(0, new Object[] { cleanTs, selectedCol, formattedSystem });

                // Save to persistent storage
                String storageString = cleanTs + "|||" + selectedCol + "|||" + formattedSystem;
                settings.getHistory().add(0, storageString);
                settings.setLastSelectedZone(selectedZone);
                settings.saveState();
            } catch (Exception ex) {
                // For table, maybe just show a dialog or status bar?
                // Or just ignore invalid input for now as per previous logic which just printed
                // to text area
                // Let's just not add it to the table if it fails.
            }
        };
        // Assign to button
        convertBtn.addActionListener(convertAction);
        input.addActionListener(convertAction); // Enter key pressed

        // --- Register content ---
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(mainPanel, "", false);
        toolWindow.getContentManager().addContent(content);
    }
}
