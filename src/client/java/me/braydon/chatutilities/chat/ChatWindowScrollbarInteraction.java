package me.braydon.chatutilities.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Wider hit area + thumb/track drag for {@link ChatWindowScrollbar} while chat is open.
 */
public final class ChatWindowScrollbarInteraction {
    private static ChatWindow scrollDragWindow;
    private static int scrollDragAnchorMy;
    private static int scrollDragAnchorScroll;
    private static int scrollDragMaxTravel;
    private static int scrollDragMaxScroll;

    private ChatWindowScrollbarInteraction() {}

    public static void clientTick(Minecraft mc) {
        if (!(mc.screen instanceof ChatScreen) || ChatUtilitiesManager.get().isPositioning()) {
            clearDrag();
            return;
        }
        long handle = mc.getWindow().handle();
        boolean down = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        if (!down) {
            clearDrag();
            return;
        }
        if (scrollDragWindow == null) {
            return;
        }
        int gw = mc.getWindow().getGuiScaledWidth();
        int gh = mc.getWindow().getGuiScaledHeight();
        int my = (int) mc.mouseHandler.getScaledYPos(mc.getWindow());
        ChatWindowGeometry geo =
                ChatWindowGeometry.compute(
                        scrollDragWindow,
                        mc,
                        gw,
                        gh,
                        null,
                        mc.gui.getGuiTicks(),
                        false,
                        true,
                        0,
                        0,
                        false,
                        false);
        ChatWindowScrollbar.ScrollbarMetrics m =
                ChatWindowScrollbar.metrics(mc, scrollDragWindow, geo, 1f);
        if (m == null) {
            clearDrag();
            return;
        }
        int d = my - scrollDragAnchorMy;
        int next =
                Mth.clamp(
                        scrollDragAnchorScroll
                                - (int) Math.round(d * scrollDragMaxScroll / (double) scrollDragMaxTravel),
                        0,
                        scrollDragMaxScroll);
        scrollDragWindow.setHistoryScrollRows(next);
    }

    public static void clearDrag() {
        scrollDragWindow = null;
    }

    /**
     * @return {@code true} if the click is consumed (scrollbar hit).
     */
    public static boolean tryBeginMouseDown(Minecraft mc, int mx, int my, int button) {
        if (button != 0) {
            return false;
        }
        if (!(mc.screen instanceof ChatScreen) || ChatUtilitiesManager.get().isPositioning()) {
            return false;
        }
        int gw = mc.getWindow().getGuiScaledWidth();
        int gh = mc.getWindow().getGuiScaledHeight();
        int guiTick = mc.gui.getGuiTicks();
        ChatUtilitiesManager mgr = ChatUtilitiesManager.get();
        List<ChatWindow> ordered = new ArrayList<>(mgr.getActiveProfileWindows());
        for (int i = ordered.size() - 1; i >= 0; i--) {
            ChatWindow w = ordered.get(i);
            if (!w.isVisible() || w.getLines().isEmpty()) {
                continue;
            }
            ChatWindowGeometry geo =
                    ChatWindowGeometry.compute(w, mc, gw, gh, null, guiTick, false, true, mx, my, false, false);
            ChatWindowScrollbar.ScrollbarMetrics m = ChatWindowScrollbar.metrics(mc, w, geo, 1f);
            if (m == null || !m.contains(mx, my)) {
                continue;
            }
            boolean onThumb = m.thumbContains(my);
            if (onThumb) {
                scrollDragWindow = w;
                scrollDragAnchorMy = my;
                scrollDragAnchorScroll = w.getHistoryScrollRows();
                scrollDragMaxTravel = m.maxTravel;
                scrollDragMaxScroll = m.maxScrollRows;
            } else {
                w.setHistoryScrollRows(m.scrollRowsForTrackClick(my));
            }
            return true;
        }
        return false;
    }
}
