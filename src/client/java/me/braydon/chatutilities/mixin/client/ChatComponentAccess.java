package me.braydon.chatutilities.mixin.client;

import java.util.List;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChatComponent.class)
public interface ChatComponentAccess {
    @Accessor("allMessages")
    List<GuiMessage> chatUtilities$getAllMessages();

    @Invoker("refreshTrimmedMessages")
    void chatUtilities$refreshTrimmedMessages();
}
