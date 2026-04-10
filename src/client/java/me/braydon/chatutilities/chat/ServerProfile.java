package me.braydon.chatutilities.chat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

/** One server profile: host rules, chat actions, and chat windows. */
public final class ServerProfile {
    private final String id;
    private String displayName;
    private final List<String> servers = new ArrayList<>();
    private final List<ChatActionGroup> chatActionGroups = new ArrayList<>();
    private final LinkedHashMap<String, ChatWindow> windows = new LinkedHashMap<>();
    private final List<CommandAlias> commandAliases = new ArrayList<>();

    public ServerProfile(String id, String displayName) {
        this.id = Objects.requireNonNull(id);
        this.displayName = displayName == null ? id : displayName;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName == null ? "" : displayName;
    }

    public List<String> getServers() {
        return servers;
    }

    public List<ChatActionGroup> getChatActionGroups() {
        return chatActionGroups;
    }

    public LinkedHashMap<String, ChatWindow> getWindows() {
        return windows;
    }

    public ChatWindow getWindow(String windowId) {
        return windows.get(windowId);
    }

    public boolean hasWindow(String windowId) {
        return windows.containsKey(windowId);
    }

    public List<String> getWindowIds() {
        return new ArrayList<>(windows.keySet());
    }

    public List<CommandAlias> getCommandAliases() {
        return Collections.unmodifiableList(commandAliases);
    }

    List<CommandAlias> commandAliasesMutable() {
        return commandAliases;
    }
}
