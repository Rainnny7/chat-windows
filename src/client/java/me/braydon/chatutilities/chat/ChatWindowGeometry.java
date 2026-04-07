package me.braydon.chatutilities.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ChatWindowGeometry {
    private static final int PADDING = 4;
    /**
     * Gap between the inner top padding and the first chat row — vanilla HUD leaves noticeable space above the first
     * line (in addition to side padding); 2px was too tight on common GUI scales.
     */
    public static final int CONTENT_TOP_INSET = 10;
    /**
     * In adjust-layout overlay only: small inset between the window outline and chat rows (not the HUD’s larger
     * {@link #padding()} + {@link #CONTENT_TOP_INSET}).
     */
    public static final int LAYOUT_MODE_CONTENT_INSET = 2;
    /** Pushes chat text slightly down within each row like vanilla (line strip vs baseline). */
    public static final int ROW_TEXT_TOP_NUDGE = 1;
    public static final int EDGE_PX = 6;
    /** Larger hit area for bottom-right corner (resize both width and height from bottom). */
    public static final int SE_CORNER_PX = 14;

    public enum PositioningPointer {
        NONE,
        MOVE,
        RESIZE_E,
        RESIZE_W,
        RESIZE_N,
        RESIZE_S,
        RESIZE_NE,
        RESIZE_NW,
        RESIZE_SE,
        RESIZE_SW
    }

    /**
     * Hit-test for layout mode: corners first (bottom-right uses a larger zone), then edges, then move.
     * Regions extend slightly outside the box so handles are easier to grab.
     */
    public static PositioningPointer positioningPointerAt(int mxGui, int myGui, ChatWindowGeometry geo) {
        int x = geo.x;
        int y = geo.y;
        int w = geo.boxW;
        int h = geo.boxH;
        int e = EDGE_PX;
        int se = SE_CORNER_PX;
        int oc = EDGE_PX;

        // Corners (outside padding allowed)
        if (mxGui >= x + w - se && mxGui <= x + w + e && myGui >= y + h - se && myGui <= y + h + e) {
            return PositioningPointer.RESIZE_SE;
        }
        if (mxGui >= x + w - oc && mxGui <= x + w + e && myGui >= y - e && myGui <= y + oc) {
            return PositioningPointer.RESIZE_NE;
        }
        if (mxGui >= x - e && mxGui <= x + oc && myGui >= y + h - oc && myGui <= y + h + e) {
            return PositioningPointer.RESIZE_SW;
        }
        if (mxGui >= x - e && mxGui <= x + oc && myGui >= y - e && myGui <= y + oc) {
            return PositioningPointer.RESIZE_NW;
        }
        // Edges (full length so narrow windows still get a bottom/top strip)
        if (myGui >= y + h - e && myGui <= y + h + e && mxGui >= x && mxGui <= x + w) {
            return PositioningPointer.RESIZE_S;
        }
        if (myGui >= y - e && myGui <= y + e && mxGui >= x && mxGui <= x + w) {
            return PositioningPointer.RESIZE_N;
        }
        if (mxGui >= x + w - e && mxGui <= x + w + e && myGui >= y && myGui <= y + h) {
            return PositioningPointer.RESIZE_E;
        }
        if (mxGui >= x - e && mxGui <= x + e && myGui >= y && myGui <= y + h) {
            return PositioningPointer.RESIZE_W;
        }
        if (mxGui >= x && mxGui <= x + w && myGui >= y && myGui <= y + h) {
            return PositioningPointer.MOVE;
        }
        return PositioningPointer.NONE;
    }

    public static final class RenderedRow {
        public final FormattedCharSequence text;
        public final float alpha;
        /** Extra Y (gui px) while smooth-chat slide-in runs; 0 when inactive. */
        public final int slideYOffset;

        public RenderedRow(FormattedCharSequence text, float alpha, int slideYOffset) {
            this.text = text;
            this.alpha = alpha;
            this.slideYOffset = slideYOffset;
        }
    }

    public final int x;
    public final int y;
    public final int boxW;
    public final int boxH;
    public final int anchorXGui;
    public final int anchorYGui;
    public final List<RenderedRow> rows;
    public final int maxHistoryScrollRows;
    public final int contentStartYOffset;
    /** Same row pitch as vanilla HUD chat ({@link net.minecraft.client.gui.components.ChatComponent#getLineHeight()}). */
    public final int linePx;

    private ChatWindowGeometry(
            int x,
            int y,
            int boxW,
            int boxH,
            int anchorXGui,
            int anchorYGui,
            List<RenderedRow> rows,
            int maxHistoryScrollRows,
            int contentStartYOffset,
            int linePx) {
        this.x = x;
        this.y = y;
        this.boxW = boxW;
        this.boxH = boxH;
        this.anchorXGui = anchorXGui;
        this.anchorYGui = anchorYGui;
        this.rows = rows;
        this.maxHistoryScrollRows = maxHistoryScrollRows;
        this.contentStartYOffset = contentStartYOffset;
        this.linePx = linePx;
    }

    /** Row height in gui pixels, matching vanilla chat for this client. */
    public int lineHeight() {
        return linePx;
    }

    private static int vanillaChatLineHeight(Minecraft mc) {
        return Math.max(1, mc.gui.getChat().getLineHeight());
    }

    /**
     * Layout hit-test / snap geometry: configured frame ({@code anchor}, {@code widthFrac}, {@code maxVisibleLines})
     * matching the layout overlay and HUD positioning mode.
     */
    public static ChatWindowGeometry compute(ChatWindow window, Minecraft mc, int gw, int gh, Component placeholderWhenNoLine) {
        return compute(window, mc, gw, gh, placeholderWhenNoLine, false);
    }

    /**
     * @param compactLayoutChrome when true (adjust-layout overlay), {@link #boxH} matches {@link #LAYOUT_MODE_CONTENT_INSET}
     *         margins only — not {@link #CONTENT_TOP_INSET} + {@link #padding()} — so the outline matches drawn chat rows.
     */
    public static ChatWindowGeometry compute(
            ChatWindow window, Minecraft mc, int gw, int gh, Component placeholderWhenNoLine, boolean compactLayoutChrome) {
        return compute(
                window,
                mc,
                gw,
                gh,
                placeholderWhenNoLine,
                mc.gui.getGuiTicks(),
                true,
                mc.screen instanceof ChatScreen,
                0,
                0,
                false,
                compactLayoutChrome);
    }

    public static ChatWindowGeometry compute(
            ChatWindow window,
            Minecraft mc,
            int gw,
            int gh,
            Component placeholderWhenNoLine,
            int guiTick,
            boolean forceOpaque,
            boolean chatScreenOpen,
            int mouseGuiX,
            int mouseGuiY,
            boolean tightLayoutChrome,
            boolean compactLayoutChrome) {
        int lh = vanillaChatLineHeight(mc);
        int maxLineWidth = Math.max(24, Math.round(window.getWidthFrac() * gw) - PADDING * 2);

        boolean useChatHistory =
                !forceOpaque && chatScreenOpen && mc.screen instanceof ChatScreen && !window.getLines().isEmpty();
        if (tightLayoutChrome) {
            useChatHistory = false;
        }

        List<RenderedRow> rows = new ArrayList<>();
        int maxHistoryScroll = 0;
        int contentStartYOffset = 0;
        int viewportRows = window.getMaxVisibleLines();

        if (useChatHistory) {
            List<RenderedRow> allHistory = new ArrayList<>();
            for (ChatWindowLine line : window.getLines()) {
                float a = ChatSmoothAppearance.fadeInMultiplier(line.addedGuiTick());
                int slide = ChatSmoothAppearance.fadeSlideOffsetYPixels(line.addedGuiTick());
                for (FormattedCharSequence row : mc.font.split(line.styled(), maxLineWidth)) {
                    allHistory.add(new RenderedRow(row, a, slide));
                }
            }
            int total = allHistory.size();
            maxHistoryScroll = Math.max(0, total - viewportRows);
            window.clampHistoryScroll(maxHistoryScroll);
            int scroll = window.getHistoryScrollRows();
            int start = Math.max(0, total - viewportRows - scroll);
            int end = Math.max(start, total - scroll);
            rows = new ArrayList<>(allHistory.subList(start, end));
        } else {
            List<ChatWindowLine> alive = new ArrayList<>();
            for (ChatWindowLine e : window.getLines()) {
                if (forceOpaque || ChatWindowFade.lineAlpha(e.addedGuiTick(), guiTick) > 0f) {
                    alive.add(e);
                }
            }
            List<RenderedRow> allVisualRows = new ArrayList<>();
            for (ChatWindowLine line : alive) {
                float a = forceOpaque ? 1f : ChatWindowFade.lineAlpha(line.addedGuiTick(), guiTick);
                int slide = forceOpaque ? 0 : ChatSmoothAppearance.fadeSlideOffsetYPixels(line.addedGuiTick());
                for (FormattedCharSequence row : mc.font.split(line.styled(), maxLineWidth)) {
                    allVisualRows.add(new RenderedRow(row, a, slide));
                }
            }
            if (allVisualRows.size() > viewportRows) {
                rows = new ArrayList<>(allVisualRows.subList(allVisualRows.size() - viewportRows, allVisualRows.size()));
            } else {
                rows = allVisualRows;
            }
        }

        if (rows.isEmpty() && placeholderWhenNoLine != null) {
            for (FormattedCharSequence row : mc.font.split(placeholderWhenNoLine, maxLineWidth)) {
                rows.add(new RenderedRow(row, 1f, 0));
            }
        }
        if (rows.isEmpty() && forceOpaque) {
            int splitW = Math.max(maxLineWidth, 200);
            for (FormattedCharSequence row : mc.font.split(Component.literal("[empty]"), splitW)) {
                rows.add(new RenderedRow(row, 1f, 0));
            }
        }

        int maxRowW = 0;
        for (RenderedRow rr : rows) {
            maxRowW = Math.max(maxRowW, mc.font.width(rr.text));
        }
        if (useChatHistory) {
            for (ChatWindowLine line : window.getLines()) {
                for (FormattedCharSequence row : mc.font.split(line.styled(), maxLineWidth)) {
                    maxRowW = Math.max(maxRowW, mc.font.width(row));
                }
            }
        }

        int configuredW = Math.round(window.getWidthFrac() * gw);
        int boxW;
        if (tightLayoutChrome) {
            int capW = Math.max(24, configuredW);
            int naturalW = rows.isEmpty() ? 56 : maxRowW + PADDING * 2;
            boxW = Math.min(gw, Math.max(48, Math.min(capW, naturalW)));
        } else {
            int innerMinW = rows.isEmpty() ? 40 : maxRowW + PADDING * 2;
            boxW = Math.min(gw, Math.max(innerMinW, configuredW));
        }

        int lineCount;
        if (useChatHistory) {
            // Shrink to actual visible rows (up to max visible lines); bottom stays anchored to anchorY.
            lineCount = Math.max(rows.isEmpty() ? 1 : rows.size(), 1);
        } else {
            int contentRows = Math.max(rows.isEmpty() ? 1 : rows.size(), 1);
            if (forceOpaque && !tightLayoutChrome) {
                lineCount = Math.max(contentRows, viewportRows);
            } else {
                lineCount = contentRows;
            }
        }

        int boxH;
        if (useChatHistory) {
            boxH = CONTENT_TOP_INSET + lineCount * lh + PADDING * 2;
        } else if (compactLayoutChrome) {
            boxH = LAYOUT_MODE_CONTENT_INSET * 2 + lineCount * lh;
        } else {
            boxH = CONTENT_TOP_INSET + lineCount * lh + PADDING * 2;
        }

        int anchorXGui = Math.round(window.getAnchorX() * gw);
        int anchorYGui = Math.round(window.getAnchorY() * gh);
        int x = Math.min(Math.max(0, anchorXGui), Math.max(0, gw - boxW));
        int y = Math.min(Math.max(0, anchorYGui - boxH), Math.max(0, gh - boxH));

        if (!tightLayoutChrome && !useChatHistory && forceOpaque && lineCount > rows.size()) {
            contentStartYOffset = (lineCount - rows.size()) * lh;
        }

        return new ChatWindowGeometry(
                x,
                y,
                boxW,
                boxH,
                anchorXGui,
                anchorYGui,
                rows,
                maxHistoryScroll,
                contentStartYOffset,
                lh);
    }

    /**
     * Which stored chat line produced {@code geo.rows.get(rowIndex)}; empty for placeholder-only rows or bad index.
     */
    public static Optional<ChatWindowLine> sourceLineForRow(
            ChatWindow window,
            Minecraft mc,
            int gw,
            ChatWindowGeometry geo,
            int guiTick,
            boolean forceOpaque,
            boolean chatScreenOpen,
            int rowIndex) {
        if (window.getLines().isEmpty() || rowIndex < 0 || rowIndex >= geo.rows.size()) {
            return Optional.empty();
        }
        int maxLineWidth = Math.max(24, Math.round(window.getWidthFrac() * gw) - PADDING * 2);
        boolean useChatHistory =
                !forceOpaque && chatScreenOpen && mc.screen instanceof ChatScreen && !window.getLines().isEmpty();

        List<ChatWindowLine> sources = new ArrayList<>();
        int viewportRows = window.getMaxVisibleLines();

        if (useChatHistory) {
            for (ChatWindowLine line : window.getLines()) {
                for (FormattedCharSequence ignored : mc.font.split(line.styled(), maxLineWidth)) {
                    sources.add(line);
                }
            }
            int total = sources.size();
            int maxHistoryScroll = Math.max(0, total - viewportRows);
            window.clampHistoryScroll(maxHistoryScroll);
            int scroll = window.getHistoryScrollRows();
            int start = Math.max(0, total - viewportRows - scroll);
            int end = Math.max(start, total - scroll);
            sources = new ArrayList<>(sources.subList(start, end));
        } else {
            List<ChatWindowLine> alive = new ArrayList<>();
            for (ChatWindowLine e : window.getLines()) {
                if (forceOpaque || ChatWindowFade.lineAlpha(e.addedGuiTick(), guiTick) > 0f) {
                    alive.add(e);
                }
            }
            List<ChatWindowLine> flat = new ArrayList<>();
            for (ChatWindowLine line : alive) {
                for (FormattedCharSequence ignored : mc.font.split(line.styled(), maxLineWidth)) {
                    flat.add(line);
                }
            }
            if (flat.size() > viewportRows) {
                sources = new ArrayList<>(flat.subList(flat.size() - viewportRows, flat.size()));
            } else {
                sources = flat;
            }
        }

        if (rowIndex >= sources.size()) {
            return Optional.empty();
        }
        return Optional.of(sources.get(rowIndex));
    }

    public static boolean historyHitTest(ChatWindow window, Minecraft mc, int gw, int mouseGuiX, int mouseGuiY) {
        if (!(mc.screen instanceof ChatScreen) || window.getLines().isEmpty()) {
            return false;
        }
        int gh = mc.getWindow().getGuiScaledHeight();
        ChatWindowGeometry geo =
                compute(
                        window,
                        mc,
                        gw,
                        gh,
                        null,
                        mc.gui.getGuiTicks(),
                        false,
                        true,
                        mouseGuiX,
                        mouseGuiY,
                        false,
                        false);
        return mouseGuiX >= geo.x
                && mouseGuiX < geo.x + geo.boxW
                && mouseGuiY >= geo.y
                && mouseGuiY < geo.y + geo.boxH;
    }

    public static int[] historyScrollMetrics(ChatWindow window, Minecraft mc, int gw) {
        int maxLineWidth = Math.max(24, Math.round(window.getWidthFrac() * gw) - PADDING * 2);
        int total = countWrappedRows(window, mc, maxLineWidth);
        int viewportRows = window.getMaxVisibleLines();
        return new int[] {total, viewportRows};
    }

    private static int countWrappedRows(ChatWindow window, Minecraft mc, int maxLineWidth) {
        int n = 0;
        for (ChatWindowLine line : window.getLines()) {
            for (FormattedCharSequence ignored : mc.font.split(line.styled(), maxLineWidth)) {
                n++;
            }
        }
        return n;
    }

    /**
     * Which visible row contains {@code relY} (pixels down from the top of the first text row), matching
     * {@link ChatUtilitiesHud} placement including {@link RenderedRow#slideYOffset}.
     */
    public static int rowIndexForContentRelY(ChatWindowGeometry geo, int relY) {
        if (relY < 0 || geo.rows.isEmpty()) {
            return -1;
        }
        int acc = 0;
        int rowH = geo.linePx;
        for (int i = 0; i < geo.rows.size(); i++) {
            RenderedRow r = geo.rows.get(i);
            int top = acc + r.slideYOffset;
            int bottom = top + rowH;
            if (relY >= top && relY < bottom) {
                return i;
            }
            acc += rowH;
        }
        return -1;
    }

    /** Vanilla chat row height when no {@link ChatWindowGeometry} is available yet. */
    public static int lineHeight(Minecraft mc) {
        return vanillaChatLineHeight(mc);
    }

    public static int padding() {
        return PADDING;
    }

    public static int argbText(float alpha, int rgb) {
        int a = Mth.clamp(Math.round(alpha * 255f), 0, 255);
        return (a << 24) | (rgb & 0xFFFFFF);
    }
}
