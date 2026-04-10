package me.braydon.chatutilities.chat;

import net.minecraft.client.gui.components.EditBox;
import org.jspecify.annotations.Nullable;

/** Marks the vanilla chat-screen search {@link EditBox} so mixins can adjust its look (outline only). */
public final class ChatSearchUi {
    private static @Nullable EditBox bound;

    private ChatSearchUi() {}

    public static void bind(EditBox field) {
        bound = field;
    }

    public static void clear() {
        bound = null;
    }

    public static boolean isVanillaSearch(EditBox box) {
        return box != null && box == bound;
    }

    /**
     * Vanilla chat search field: skip only the outline stroke while keeping the bordered fill (see
     * {@link me.braydon.chatutilities.mixin.client.GuiGraphicsSearchOutlineMixin}).
     */
    public static boolean shouldSuppressBoundSearchOutline(int x, int y, int w, int h) {
        EditBox b = bound;
        if (b == null || !b.isBordered()) {
            return false;
        }
        return b.getX() == x && b.getY() == y && b.getWidth() == w && b.getHeight() == h;
    }
}
