package me.braydon.chatutilities.chat;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.braydon.chatutilities.client.ChatUtilitiesClientOptions;
import me.braydon.chatutilities.mixin.client.ChatComponentAccess;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;

/**
 * Merges consecutive duplicate chat lines in the vanilla HUD into one line with a gray {@code (xN)} suffix.
 */
public final class VanillaChatRepeatStacker {
    /** Plain text after formatting strip; matches a trailing repeat suffix from this mod. */
    private static final Pattern PLAIN_STACK_SUFFIX = Pattern.compile(" \\(x(\\d+)\\)$");

    private VanillaChatRepeatStacker() {}

    public static void afterAddMessage(ChatComponent chat, Component latestDuplicate) {
        if (!ChatUtilitiesClientOptions.isStackRepeatedMessages()) {
            return;
        }
        ChatComponentAccess access = (ChatComponentAccess) chat;
        List<GuiMessage> msgs = access.chatUtilities$getAllMessages();
        if (msgs.size() < 2) {
            return;
        }
        GuiMessage newest = msgs.get(0);
        GuiMessage older = msgs.get(1);
        if (!plainStackKeysEqual(newest.content(), older.content())) {
            return;
        }
        int next = parseDisplayedCount(older.content()) + 1;
        GuiMessage merged =
                new GuiMessage(
                        older.addedTime(),
                        withStackSuffix(latestDuplicate, next),
                        older.signature(),
                        older.tag());
        msgs.remove(0);
        msgs.remove(0);
        msgs.add(0, merged);
        access.chatUtilities$refreshTrimmedMessages();
    }

    private static boolean plainStackKeysEqual(Component a, Component b) {
        return plainStackKey(a).equals(plainStackKey(b));
    }

    private static String plainStackKey(Component c) {
        String p = ChatUtilitiesManager.plainTextForMatching(c);
        Matcher m = PLAIN_STACK_SUFFIX.matcher(p);
        if (m.find()) {
            return p.substring(0, m.start()).strip();
        }
        return p;
    }

    private static int parseDisplayedCount(Component c) {
        String p = ChatUtilitiesManager.plainTextForMatching(c);
        Matcher m = PLAIN_STACK_SUFFIX.matcher(p);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return 1;
    }

    private static Component withStackSuffix(Component base, int count) {
        return Component.empty()
                .append(base)
                .append(Component.literal(" (x" + count + ")").withStyle(ChatFormatting.GRAY));
    }
}
