package me.braydon.chatutilities.chat;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public record ChatWindowLine(Component baseContent, int addedGuiTick, int stackCount) {
    public ChatWindowLine {
        stackCount = Math.max(1, stackCount);
    }

    public static ChatWindowLine single(Component message, int tick) {
        return new ChatWindowLine(message, tick, 1);
    }

    /** Full line for rendering and hit-testing (includes gray {@code (xN)} when stacked). */
    public Component styled() {
        if (stackCount <= 1) {
            return baseContent;
        }
        return Component.empty()
                .append(baseContent)
                .append(Component.literal(" (x" + stackCount + ")").withStyle(ChatFormatting.GRAY));
    }

    boolean sameStackAs(ChatWindowLine incoming) {
        return ChatUtilitiesManager.plainTextForMatching(baseContent)
                .equals(ChatUtilitiesManager.plainTextForMatching(incoming.baseContent));
    }

    ChatWindowLine mergedWithRepeat() {
        return new ChatWindowLine(baseContent, addedGuiTick, stackCount + 1);
    }
}
