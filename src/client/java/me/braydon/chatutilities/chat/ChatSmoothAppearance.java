package me.braydon.chatutilities.chat;

import me.braydon.chatutilities.client.ChatUtilitiesClientOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

/** Fade-in multiplier for new chat lines (vanilla chat and chat windows). */
public final class ChatSmoothAppearance {

    private static final float TICKS_PER_SECOND = 20f;
    /** Max vertical offset (px) while a line is fading in — text starts lower and moves up with opacity. */
    private static final int SMOOTH_CHAT_SLIDE_PX = 8;

    /**
     * During {@link net.minecraft.client.gui.components.ChatComponent} line iteration, vanilla draws each line
     * while this holds the slide offset for that line (chat-local Y, same space as row layout).
     */
    private static final ThreadLocal<Integer> VANILLA_CHAT_LINE_SLIDE_Y = new ThreadLocal<>();

    private ChatSmoothAppearance() {}

    /**
     * Multiplier 0–1 for how visible a line is during its fade-in window. When smooth chat is off, always 1.
     *
     * @param addedGuiTick {@link net.minecraft.client.GuiMessage.Line#addedTime()} or {@link ChatWindowLine#addedGuiTick()}
     */
    public static float fadeInMultiplier(int addedGuiTick) {
        if (!ChatUtilitiesClientOptions.isSmoothChat()) {
            return 1f;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return 1f;
        }
        int now = mc.gui.getGuiTicks();
        float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float ageTicks = (now - addedGuiTick) + partial;
        int ms = ChatUtilitiesClientOptions.getSmoothChatFadeMs();
        float durationTicks = Math.max(ms / (1000f / TICKS_PER_SECOND), 0.25f);
        return Mth.clamp(ageTicks / durationTicks, 0f, 1f);
    }

    /**
     * Pixels to add to draw Y so the line moves up into place while fading. Uses ease-out on the motion so it settles
     * smoothly; line backdrops stay fixed (vanilla draws backgrounds in a separate pass without this offset).
     */
    public static int fadeSlideOffsetYPixels(int addedGuiTick) {
        if (!ChatUtilitiesClientOptions.isSmoothChat()) {
            return 0;
        }
        float t = fadeInMultiplier(addedGuiTick);
        float inv = 1f - t;
        float easedInv = inv * inv * inv;
        return Math.round(easedInv * SMOOTH_CHAT_SLIDE_PX);
    }

    public static void setVanillaChatLineSlideYPixels(int px) {
        if (px <= 0) {
            VANILLA_CHAT_LINE_SLIDE_Y.remove();
        } else {
            VANILLA_CHAT_LINE_SLIDE_Y.set(px);
        }
    }

    public static void clearVanillaChatLineSlideYPixels() {
        VANILLA_CHAT_LINE_SLIDE_Y.remove();
    }

    public static int vanillaChatLineSlideYPixels() {
        Integer v = VANILLA_CHAT_LINE_SLIDE_Y.get();
        return v == null ? 0 : v;
    }
}
