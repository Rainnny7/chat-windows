package me.braydon.chatutilities.mixin.client;

import me.braydon.chatutilities.chat.CommandOutgoingAliases;
import me.braydon.chatutilities.chat.ChatUtilitiesManager;
import me.braydon.chatutilities.chat.ServerProfile;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CommandSuggestions.class)
public class CommandSuggestionsMixin {

    @Unique
    private static final ThreadLocal<String> chatUtilities$resolvedSuggestionsInput = new ThreadLocal<>();

    @Unique
    private static final ThreadLocal<Integer> chatUtilities$adjustedCursor = new ThreadLocal<>();

    @Redirect(
            method = "updateCommandInfo()V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/EditBox;getValue()Ljava/lang/String;"))
    private String chatUtilities$aliasExpandedSuggestionsInput(EditBox box) {
        String before = box.getValue();
        if (before == null || before.isBlank() || !before.stripLeading().startsWith("/")) {
            chatUtilities$resolvedSuggestionsInput.set(before);
            chatUtilities$adjustedCursor.set(box.getCursorPosition());
            return before;
        }
        ServerProfile p = ChatUtilitiesManager.get().getEffectiveProfileForOutgoingCommands();
        if (p == null || p.getCommandAliases().isEmpty()) {
            chatUtilities$resolvedSuggestionsInput.set(before);
            chatUtilities$adjustedCursor.set(box.getCursorPosition());
            return before;
        }
        String after = CommandOutgoingAliases.modifySlashChatMessage(before, p);
        String resolved = after != null ? after : before;
        int cursor = box.getCursorPosition();
        int adjusted = adjustCursorForAliasExpansion(before, resolved, cursor);
        chatUtilities$resolvedSuggestionsInput.set(resolved);
        chatUtilities$adjustedCursor.set(adjusted);
        return resolved;
    }

    @Redirect(
            method = "updateCommandInfo()V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/EditBox;getCursorPosition()I"))
    private int chatUtilities$clampCursorForAliasExpandedSuggestions(EditBox box) {
        String value = chatUtilities$resolvedSuggestionsInput.get();
        if (value == null) {
            value = box.getValue();
        }
        Integer c = chatUtilities$adjustedCursor.get();
        int cursor = c != null ? c : box.getCursorPosition();
        int len = value != null ? value.length() : 0;
        return Mth.clamp(cursor, 0, len);
    }

    @Unique
    private static int adjustCursorForAliasExpansion(String before, String after, int cursor) {
        if (before == null || after == null) {
            return cursor;
        }
        String b = before.stripLeading();
        String a = after.stripLeading();
        if (!b.startsWith("/") || !a.startsWith("/")) {
            return cursor;
        }
        int bSpace = b.indexOf(' ');
        int aSpace = a.indexOf(' ');
        int bCmdEnd = bSpace < 0 ? b.length() : bSpace;
        int aCmdEnd = aSpace < 0 ? a.length() : aSpace;
        // If cursor is inside the first token, clamp to end of the new first token.
        if (cursor <= bCmdEnd) {
            return Math.min(cursor, aCmdEnd);
        }
        // If cursor is after the first token, shift by the command-length delta.
        int delta = aCmdEnd - bCmdEnd;
        return cursor + delta;
    }

    @Inject(method = "updateCommandInfo()V", at = @At("RETURN"))
    private void chatUtilities$clearSuggestionsThreadLocal(CallbackInfo ci) {
        chatUtilities$resolvedSuggestionsInput.remove();
        chatUtilities$adjustedCursor.remove();
    }
}

