package me.braydon.chatutilities;

import com.mojang.blaze3d.platform.InputConstants;
import me.braydon.chatutilities.chat.*;
import me.braydon.chatutilities.command.ChatUtilitiesCommands;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;

public class ChatUtilitiesModClient implements ClientModInitializer {
    /** Keybind to open the Chat Utilities menu. Unbound by default; configurable in Controls. */
    public static KeyMapping OPEN_MENU_KEY;

    @Override
    public void onInitializeClient() {
        OPEN_MENU_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.chatutilities.open_menu",
                InputConstants.UNKNOWN.getValue(),
                KeyMapping.Category.MISC
        ));

        ChatUtilitiesManager.get().init();
        ChatUtilitiesHud.register();
        ChatUtilitiesScreenHooks.register();
        ChatUtilitiesTick.register();
        ChatUtilitiesCommands.register();
    }
}
