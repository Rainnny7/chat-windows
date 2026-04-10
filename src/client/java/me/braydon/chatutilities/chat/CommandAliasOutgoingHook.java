package me.braydon.chatutilities.chat;

import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;

/** Registers Fabric {@link ClientSendMessageEvents#MODIFY_COMMAND} to apply profile command aliases. */
public final class CommandAliasOutgoingHook {
    private CommandAliasOutgoingHook() {}

    public static void register() {
        ClientSendMessageEvents.MODIFY_COMMAND.register(
                command ->
                        CommandOutgoingAliases.modifyCommandMessage(
                                command, ChatUtilitiesManager.get().getEffectiveProfileForOutgoingCommands()));
    }
}
