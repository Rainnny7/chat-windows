package me.braydon.chatutilities.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.util.Mth;

/** Screen-space bounds of the expanded vanilla chat text column (matches {@link VanillaChatLinePicker}). */
public final class VanillaChatOverlayBounds {
    private VanillaChatOverlayBounds() {}

    /**
     * First screen X (inclusive) where chat row backdrops are drawn — vanilla uses a small inset from the left edge.
     */
    public static int textColumnLeftIncl(Minecraft mc) {
        return 2;
    }

    /**
     * Screen X (exclusive) at the outer right of the chat row hit area: local {@code rowInnerWidth + 8} with the same
     * scale/translate as {@link VanillaChatLinePicker.PickerState}.
     */
    public static int textColumnRightExcl(Minecraft mc) {
        ChatComponent chat = mc.gui.getChat();
        double sf = chat.getScale();
        int inner = Mth.ceil(chat.getWidth() / sf);
        return (int) Math.ceil((inner + 12) * sf);
    }
}
