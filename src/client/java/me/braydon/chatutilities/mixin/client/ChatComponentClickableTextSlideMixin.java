package me.braydon.chatutilities.mixin.client;

import me.braydon.chatutilities.chat.ChatSmoothAppearance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Keeps clickable-text capture aligned with smooth-chat vertical slide. */
@Mixin(targets = "net.minecraft.client.gui.components.ChatComponent$ClickableTextOnlyGraphicsAccess")
public class ChatComponentClickableTextSlideMixin {

    @ModifyArg(
            method = "handleMessage",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/gui/ActiveTextCollector;accept(Lnet/minecraft/client/gui/TextAlignment;IILnet/minecraft/util/FormattedCharSequence;)V"),
            index = 2)
    private int chatUtilities$slideClickableCaptureY(int y) {
        return y + ChatSmoothAppearance.vanillaChatLineSlideYPixels();
    }
}
