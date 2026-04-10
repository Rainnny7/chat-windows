package me.braydon.chatutilities.mixin.client;

import me.braydon.chatutilities.ChatUtilitiesModClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Treat chat as focused while Chat Peek is held so vanilla HUD chat expands like pressing T,
 * but without opening the chat input screen.
 */
@Mixin(ChatComponent.class)
public class ChatComponentFocusPeekMixin {

    @Inject(method = "isChatFocused()Z", at = @At("HEAD"), cancellable = true)
    private void chatUtilities$peekForcesChatFocused(CallbackInfoReturnable<Boolean> cir) {
        if (ChatUtilitiesModClient.CHAT_PEEK_KEY != null && ChatUtilitiesModClient.CHAT_PEEK_KEY.isDown()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && !(mc.screen instanceof net.minecraft.client.gui.screens.ChatScreen)) {
                cir.setReturnValue(true);
            }
        }
    }
}

