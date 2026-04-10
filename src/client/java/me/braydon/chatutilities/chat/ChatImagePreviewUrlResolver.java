package me.braydon.chatutilities.chat;

import me.braydon.chatutilities.client.ChatUtilitiesClientOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Resolves a previewable image URL under the cursor (vanilla chat + custom chat windows). */
public final class ChatImagePreviewUrlResolver {

    private static final Pattern URL_IN_PLAIN = Pattern.compile("https?://\\S+", Pattern.CASE_INSENSITIVE);

    private ChatImagePreviewUrlResolver() {}

    public static Optional<String> findPreviewUrl(Minecraft mc, int mouseX, int mouseY) {
        if (!ChatUtilitiesClientOptions.isImageChatPreviewEnabled()) {
            return Optional.empty();
        }
        if (!(mc.screen instanceof ChatScreen) || mc.mouseHandler.isMouseGrabbed()) {
            return Optional.empty();
        }
        if (ChatUtilitiesManager.get().isPositioning()) {
            return Optional.empty();
        }
        Optional<String> fromWin = tryCustomWindows(mc, mouseX, mouseY);
        if (fromWin.isPresent()) {
            return fromWin;
        }
        return tryVanillaChat(mc, mouseX, mouseY);
    }

    private static Optional<String> tryVanillaChat(Minecraft mc, int mouseX, int mouseY) {
        Optional<VanillaChatLinePicker.LineHit> hit = VanillaChatLinePicker.pickLineHitAt(mc, mouseX, mouseY);
        if (hit.isEmpty()) {
            return Optional.empty();
        }
        FormattedCharSequence row = hit.get().line().content();
        if (row == null || FormattedCharSequence.EMPTY.equals(row)) {
            return Optional.empty();
        }
        Style st = ChatWindowClickHandler.styleAtRelativeX(mc, row, hit.get().relXInContent());
        Optional<String> open = urlFromStyle(st);
        if (open.isPresent()) {
            return open;
        }
        return firstPreviewableUrlInPlain(row);
    }

    private static Optional<String> tryCustomWindows(Minecraft mc, int mouseX, int mouseY) {
        int gw = mc.getWindow().getGuiScaledWidth();
        int gh = mc.getWindow().getGuiScaledHeight();
        int guiTick = mc.gui.getGuiTicks();
        ChatUtilitiesManager mgr = ChatUtilitiesManager.get();
        List<ChatWindow> ordered = new ArrayList<>(mgr.getActiveProfileWindows());
        for (int i = ordered.size() - 1; i >= 0; i--) {
            ChatWindow window = ordered.get(i);
            if (!window.isVisible() || window.getLines().isEmpty()) {
                continue;
            }
            ChatWindowGeometry geo =
                    ChatWindowGeometry.compute(
                            window,
                            mc,
                            gw,
                            gh,
                            null,
                            guiTick,
                            false,
                            true,
                            mouseX,
                            mouseY,
                            false,
                            false);
            if (geo.rows.isEmpty()) {
                continue;
            }
            int tx = geo.x + ChatWindowGeometry.padding();
            int ty = geo.y + geo.contentRowOffsetY;
            int textRight = geo.x + geo.boxW - ChatWindowGeometry.padding();
            if (ChatUtilitiesClientOptions.isChatSearchBarEnabled() && ChatSearchState.isFiltering()) {
                textRight += 56;
            }
            int textBottom = geo.y + geo.boxH - ChatWindowGeometry.padding();
            if (mouseX < tx || mouseX >= textRight || mouseY < ty || mouseY >= textBottom) {
                continue;
            }
            int relY = mouseY - ty;
            int rowIndex = ChatWindowGeometry.rowIndexForContentRelY(geo, relY);
            if (rowIndex < 0) {
                continue;
            }
            int relX = mouseX - tx;
            FormattedCharSequence rowText = geo.rows.get(rowIndex).text;
            Style style = ChatWindowClickHandler.styleAtRelativeX(mc, rowText, relX);
            Optional<String> u = urlFromStyle(style);
            if (u.isPresent()) {
                return u;
            }
            Optional<String> plain = firstPreviewableUrlInPlain(rowText);
            if (plain.isPresent()) {
                return plain;
            }
        }
        return Optional.empty();
    }

    private static Optional<String> firstPreviewableUrlInPlain(FormattedCharSequence row) {
        String plain = ChatUtilitiesManager.plainTextForMatching(row);
        if (plain.isEmpty()) {
            return Optional.empty();
        }
        Matcher m = URL_IN_PLAIN.matcher(plain);
        while (m.find()) {
            String raw = trimUrlCandidate(m.group());
            Optional<String> norm = ChatImagePreviewUrls.normalizePreviewableHttpUrl(raw);
            if (norm.isPresent()) {
                return norm;
            }
        }
        return Optional.empty();
    }

    private static String trimUrlCandidate(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        int end = raw.length();
        while (end > 0) {
            char c = raw.charAt(end - 1);
            if (c == '.' || c == ',' || c == ';' || c == ':' || c == ')' || c == ']' || c == '"' || c == '\'') {
                end--;
                continue;
            }
            break;
        }
        return raw.substring(0, end);
    }

    private static Optional<String> urlFromStyle(Style style) {
        if (style == null) {
            return Optional.empty();
        }
        ClickEvent click = style.getClickEvent();
        if (!(click instanceof ClickEvent.OpenUrl open)) {
            return Optional.empty();
        }
        try {
            return ChatImagePreviewUrls.normalizePreviewableHttpUrl(open.uri().toString());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
