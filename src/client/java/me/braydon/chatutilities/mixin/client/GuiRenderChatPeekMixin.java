package me.braydon.chatutilities.mixin.client;

import me.braydon.chatutilities.ChatUtilitiesModClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Ensures vanilla HUD chat still renders while Chat Peek is held.
 *
 * <p>In 1.21.11 {@code Gui.renderChat()} is structured as:
 * <pre>
 *   if (!this.chat.isChatFocused()) {
 *       this.chat.render(..., false, false);
 *   }
 * </pre>
 * {@link ChatComponentFocusPeekMixin} makes {@code ChatComponent.isChatFocused()} return
 * {@code true} during peek so that {@code getLinesPerPage()} (via {@code getHeight()}) returns
 * the expanded focused count. But that same {@code true} causes the {@code renderChat()} guard to
 * evaluate as {@code false}, skipping the render call entirely.
 *
 * <p>This mixin {@code @Redirect}s specifically the {@code isChatFocused()} call that acts as the
 * guard inside {@code renderChat()} so it returns {@code false} while peek is held — allowing
 * {@code renderChat()} to proceed. The expanded render booleans are then supplied by the
 * {@code @ModifyVariable} injections in {@link ChatComponentMixin}.
 */
@Mixin(Gui.class)
public class GuiRenderChatPeekMixin {

    /**
     * During peek: returns {@code false} so the {@code if (!isChatFocused())} guard passes and
     * {@code chat.render()} is actually called.  All other callers of {@code isChatFocused()} are
     * unaffected (only this specific call site in {@code renderChat()} is redirected).
     */
    @Redirect(
            method = "renderChat",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/ChatComponent;isChatFocused()Z"),
            require = 0)
    private boolean chatUtilities$peekKeepRenderChatActive(ChatComponent chatComponent) {
        if (ChatUtilitiesModClient.CHAT_PEEK_KEY != null && ChatUtilitiesModClient.CHAT_PEEK_KEY.isDown()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && !(mc.screen instanceof ChatScreen)) {
                // Lie to renderChat: claim chat is not focused so it proceeds to call
                // chat.render(). getLinesPerPage() will still use the expanded height
                // because ChatComponentFocusPeekMixin makes isChatFocused() return true
                // in that call path.
                return false;
            }
        }
        return chatComponent.isChatFocused();
    }
}
