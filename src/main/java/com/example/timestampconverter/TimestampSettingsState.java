package com.example.timestampconverter;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@State(
        name = "TimestampSettingsState",
        storages = @Storage("TimestampConverterPlugin.xml")
)
@Service(Service.Level.APP) // 👈 important: mark as app-level service
public final class TimestampSettingsState implements PersistentStateComponent<TimestampSettingsState> {

    private List<String> history = new ArrayList<>();
    private String lastSelectedZone = "UTC";

    public static TimestampSettingsState getInstance() {
        // ensures it's never null
        return ApplicationManager.getApplication().getService(TimestampSettingsState.class);
    }

    @Nullable
    @Override
    public TimestampSettingsState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull TimestampSettingsState state) {
        this.history = new ArrayList<>(state.history != null ? state.history : new ArrayList<>());
        this.lastSelectedZone = state.lastSelectedZone != null ? state.lastSelectedZone : "UTC";
    }

    public List<String> getHistory() {
        return history;
    }

    public void setHistory(List<String> history) {
        this.history = history;
    }

    public String getLastSelectedZone() {
        return lastSelectedZone;
    }

    public void setLastSelectedZone(String lastSelectedZone) {
        this.lastSelectedZone = lastSelectedZone;
    }

    public void saveState() {
        // no-op, IntelliJ auto-saves
    }
}
