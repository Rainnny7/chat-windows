package me.braydon.chatutilities.gui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Legacy entry point: opens the main menu on {@link ChatUtilitiesRootScreen.Panel#CHAT_ACTIONS}.
 */
public final class ProfileMessageSoundsScreen extends Screen implements ProfileWorkflowScreen {

    private final String profileId;
    private final ChatUtilitiesRootScreen chatRoot;

    public ProfileMessageSoundsScreen(String profileId, ChatUtilitiesRootScreen chatRoot) {
        super(Component.literal("Chat Sounds"));
        this.profileId = profileId;
        this.chatRoot = chatRoot;
    }

    @Override
    public ChatUtilitiesRootScreen getChatRoot() {
        return chatRoot;
    }

    @Override
    public Screen recreateForProfile() {
        return new ProfileMessageSoundsScreen(profileId, chatRoot);
    }

    @Override
    protected void init() {
        if (minecraft != null) {
            minecraft.setScreen(
                    new ChatUtilitiesRootScreen(chatRoot.getMenuParent(), profileId, ChatUtilitiesRootScreen.Panel.CHAT_ACTIONS));
        }
    }
}
