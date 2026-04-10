package me.braydon.chatutilities.chat;

import me.braydon.chatutilities.client.ChatUtilitiesClientOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.util.Mth;

/** Shared Y placement for the chat search row (vanilla {@link ChatScreen}). */
public final class ChatSearchBarLayout {

    private ChatSearchBarLayout() {}

    /**
     * Vertical position (top) for the search {@link net.minecraft.client.gui.components.EditBox}.
     *
     * <p>{@link ChatUtilitiesClientOptions.ChatSearchBarPosition#ABOVE_CHAT} places the row just above the visible
     * chat lines. {@code BELOW_CHAT} places it in the gap between the bottom of that stack and the input bar.
     */
    public static int searchFieldY(
            Minecraft mc, int inputBarTopY, int searchBarHeight) {
        int chatBottomY = VanillaChatLinePicker.expandedChatLowestLineBottomScreenY(mc);
        int chatTopY = VanillaChatLinePicker.expandedChatTopVisibleLineTopScreenY(mc);
        if (ChatUtilitiesClientOptions.getChatSearchBarPosition()
                == ChatUtilitiesClientOptions.ChatSearchBarPosition.ABOVE_CHAT) {
            int y = chatTopY - searchBarHeight - 4;
            return Mth.clamp(y, 8, inputBarTopY - searchBarHeight - 2);
        }
        int y = chatBottomY + 4;
        return Mth.clamp(y, 8, inputBarTopY - searchBarHeight - 2);
    }
}
