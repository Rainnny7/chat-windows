package me.braydon.chatutilities.chat;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;

/**
 * Mouse in the same GUI space as {@link net.minecraft.client.gui.screens.ChatScreen#render} / click events.
 * {@link ChatUtilitiesTick} used scaled cursor math that can disagree with screen coordinates; during layout we
 * prefer these samples.
 */
public final class ChatLayoutMouse {
    private static int sampleX;
    private static int sampleY;
    private static boolean hasSample;

    private ChatLayoutMouse() {}

    public static void setFromRender(int mouseX, int mouseY) {
        sampleX = mouseX;
        sampleY = mouseY;
        hasSample = true;
    }

    public static void setFromClick(int mouseX, int mouseY) {
        sampleX = mouseX;
        sampleY = mouseY;
        hasSample = true;
    }

    public static void clear() {
        hasSample = false;
    }

    public static int layoutMouseX(Minecraft mc) {
        if (mc.screen instanceof ChatScreen
                && ChatUtilitiesManager.get().isPositioning()
                && hasSample) {
            return sampleX;
        }
        Window win = mc.getWindow();
        return (int) mc.mouseHandler.getScaledXPos(win);
    }

    public static int layoutMouseY(Minecraft mc) {
        if (mc.screen instanceof ChatScreen
                && ChatUtilitiesManager.get().isPositioning()
                && hasSample) {
            return sampleY;
        }
        Window win = mc.getWindow();
        return (int) mc.mouseHandler.getScaledYPos(win);
    }
}
