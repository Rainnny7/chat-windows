package me.braydon.chatutilities.chat;

import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import me.braydon.chatutilities.ChatUtilitiesModClient;
import me.braydon.chatutilities.gui.ChatUtilitiesRootScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;

import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.List;

public final class ChatUtilitiesTick {
    private static final int SAVE_THROTTLE_TICKS = 40;

    private enum DragKind { NONE, MOVE, RESIZE_E, RESIZE_N, RESIZE_NE }

    private static boolean wasChatScreenOpen;
    private static boolean escapeWasDown;
    private static int throttle;
    private static boolean wasMouseDown;
    private static DragKind dragKind = DragKind.NONE;
    /** ID of the specific window currently being dragged. */
    private static String dragTargetId = null;
    private static float pressAnchorX;
    private static float pressAnchorY;
    private static int pressMx;
    private static int pressMy;
    private static int pressMaxLines;
    private static int pressBoxW;

    private ChatUtilitiesTick() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(ChatUtilitiesTick::onEndTick);
    }

    private static void onEndTick(Minecraft mc) {
        ProfileFaviconCache.tick(mc);

        // Keybind to open Chat Utilities menu (checked before everything else)
        if (mc.screen == null && !ChatUtilitiesManager.get().isPositioning()) {
            while (ChatUtilitiesModClient.OPEN_MENU_KEY.consumeClick()) {
                mc.execute(() -> mc.setScreen(new ChatUtilitiesRootScreen(null)));
            }
        }

        boolean chatNow = mc.screen instanceof ChatScreen;
        if (wasChatScreenOpen && !chatNow) {
            for (ChatWindow w : ChatUtilitiesManager.get().getActiveProfileWindows()) {
                w.resetHistoryScroll();
            }
        }
        wasChatScreenOpen = chatNow;

        ChatUtilitiesManager mgr = ChatUtilitiesManager.get();
        boolean allowHudInput =
                mc.screen == null || (mc.screen instanceof ChatScreen && mgr.isPositioning());
        if (!allowHudInput) {
            escapeWasDown = false;
            wasMouseDown = false;
            dragKind = DragKind.NONE;
            dragTargetId = null;
            return;
        }

        long handle = mc.getWindow().handle();
        boolean escapeDown = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS;
        if (escapeDown && !escapeWasDown && mgr.isPositioning()) {
            mgr.clearAllPositioningModes();
            mgr.save();
            mgr.runRestoreScreenAfterPositionIfAny(mc);
            dragKind = DragKind.NONE;
            dragTargetId = null;
            wasMouseDown = false;
        }
        escapeWasDown = escapeDown;

        // Collect all windows currently in positioning mode
        List<ChatWindow> positionedWindows = new ArrayList<>();
        for (ChatWindow w : mgr.getActiveProfileWindows()) {
            if (w.isPositioningMode()) {
                positionedWindows.add(w);
            }
        }
        if (positionedWindows.isEmpty()) {
            resetGuiCursor(mc);
            wasMouseDown = false;
            dragKind = DragKind.NONE;
            dragTargetId = null;
            return;
        }

        int gw = mc.getWindow().getGuiScaledWidth();
        int gh = mc.getWindow().getGuiScaledHeight();
        int sw = mc.getWindow().getScreenWidth();
        int sh = mc.getWindow().getScreenHeight();

        double mxFb, myFb;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            DoubleBuffer xb = stack.mallocDouble(1);
            DoubleBuffer yb = stack.mallocDouble(1);
            GLFW.glfwGetCursorPos(handle, xb, yb);
            mxFb = xb.get(0);
            myFb = yb.get(0);
        }

        int mxGui = sw > 0 ? (int) Math.round(mxFb * gw / (double) sw) : 0;
        int myGui = sh > 0 ? (int) Math.round(myFb * gh / (double) sh) : 0;

        boolean down = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        if (down) {
            if (!wasMouseDown) {
                // First frame of press: find which positioned window is under the cursor
                dragTargetId = null;
                dragKind = DragKind.NONE;
                for (ChatWindow w : positionedWindows) {
                    Component ph = w.getLines().isEmpty() ? Component.literal("[empty]") : null;
                    ChatWindowGeometry geo = ChatWindowGeometry.compute(w, mc, gw, gh, ph);
                    DragKind kind = pointerToDrag(ChatWindowGeometry.positioningPointerAt(mxGui, myGui, geo));
                    if (kind != DragKind.NONE) {
                        dragTargetId = w.getId();
                        dragKind = kind;
                        pressAnchorX = w.getAnchorX();
                        pressAnchorY = w.getAnchorY();
                        pressMx = mxGui;
                        pressMy = myGui;
                        pressMaxLines = w.getMaxVisibleLines();
                        pressBoxW = geo.boxW;
                        break;
                    }
                }
            } else if (dragKind != DragKind.NONE && dragTargetId != null) {
                // Continue dragging the chosen window
                ChatWindow target = findById(positionedWindows, dragTargetId);
                if (target != null) {
                    applyDrag(target, dragKind, gw, gh, mxGui, myGui);
                    if (++throttle >= SAVE_THROTTLE_TICKS) {
                        throttle = 0;
                        mgr.save();
                    }
                }
            }
        } else {
            if (wasMouseDown && dragKind != DragKind.NONE) {
                mgr.save();
            }
            dragKind = DragKind.NONE;
            dragTargetId = null;
            throttle = 0;
        }

        wasMouseDown = down;

        // Cursor: reflect current drag or hover state
        if (down && dragKind != DragKind.NONE) {
            applyDragCursor(mc, dragKind);
        } else {
            // Find hover cursor from any positioned window
            ChatWindowGeometry.PositioningPointer hoverPtr = ChatWindowGeometry.PositioningPointer.NONE;
            for (ChatWindow w : positionedWindows) {
                Component ph = w.getLines().isEmpty() ? Component.literal("[empty]") : null;
                ChatWindowGeometry geo = ChatWindowGeometry.compute(w, mc, gw, gh, ph);
                ChatWindowGeometry.PositioningPointer ptr = ChatWindowGeometry.positioningPointerAt(mxGui, myGui, geo);
                if (ptr != ChatWindowGeometry.PositioningPointer.NONE) {
                    hoverPtr = ptr;
                    break;
                }
            }
            applyHoverCursor(mc, hoverPtr);
        }
    }

    private static ChatWindow findById(List<ChatWindow> list, String id) {
        for (ChatWindow w : list) {
            if (w.getId().equals(id)) return w;
        }
        return null;
    }

    private static DragKind pointerToDrag(ChatWindowGeometry.PositioningPointer ptr) {
        return switch (ptr) {
            case RESIZE_NE -> DragKind.RESIZE_NE;
            case RESIZE_E  -> DragKind.RESIZE_E;
            case RESIZE_N  -> DragKind.RESIZE_N;
            case MOVE      -> DragKind.MOVE;
            default        -> DragKind.NONE;
        };
    }

    private static void applyDrag(ChatWindow w, DragKind kind, int gw, int gh, int mx, int my) {
        switch (kind) {
            case MOVE -> {
                w.setAnchorX(pressAnchorX + (mx - pressMx) / (float) gw);
                w.setAnchorY(pressAnchorY + (my - pressMy) / (float) gh);
            }
            case RESIZE_E -> {
                int newW = pressBoxW + (mx - pressMx);
                w.setWidthFrac(newW / (float) gw);
            }
            case RESIZE_N -> {
                int dLines = Math.round((pressMy - my) / (float) ChatWindowGeometry.lineHeight());
                w.setMaxVisibleLines(pressMaxLines + dLines);
            }
            case RESIZE_NE -> {
                int newW = pressBoxW + (mx - pressMx);
                w.setWidthFrac(newW / (float) gw);
                int dLines = Math.round((pressMy - my) / (float) ChatWindowGeometry.lineHeight());
                w.setMaxVisibleLines(pressMaxLines + dLines);
            }
            default -> {}
        }
    }

    private static void resetGuiCursor(Minecraft mc) {
        mc.getWindow().selectCursor(CursorType.DEFAULT);
    }

    private static void applyDragCursor(Minecraft mc, DragKind kind) {
        CursorType type = switch (kind) {
            case RESIZE_E  -> CursorTypes.RESIZE_EW;
            case RESIZE_N  -> CursorTypes.RESIZE_NS;
            case RESIZE_NE -> CursorTypes.RESIZE_ALL;
            case MOVE      -> CursorTypes.ARROW;
            default        -> CursorType.DEFAULT;
        };
        mc.getWindow().selectCursor(type);
    }

    private static void applyHoverCursor(Minecraft mc, ChatWindowGeometry.PositioningPointer ptr) {
        CursorType type = switch (ptr) {
            case RESIZE_E  -> CursorTypes.RESIZE_EW;
            case RESIZE_N  -> CursorTypes.RESIZE_NS;
            case RESIZE_NE -> CursorTypes.RESIZE_ALL;
            case MOVE      -> CursorTypes.ARROW;
            default        -> CursorType.DEFAULT;
        };
        mc.getWindow().selectCursor(type);
    }
}
