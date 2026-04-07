package me.braydon.chatutilities.chat;

import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Resolves which visible vanilla chat line is under the cursor using the same layout as {@link
 * ChatComponent#forEachLine} (private API; reached via reflection). The {@code int} passed to
 * {@link ChatComponent.LineConsumer#accept} is a <em>line index</em> into the visible window, not a pixel Y;
 * vanilla maps it to screen space in {@code ChatComponent}'s render consumer. Mouse coordinates are converted
 * to chat-local space (inverse of the chat pose: scale + 4px X translate) to match expanded-chat hit tests.
 */
public final class VanillaChatLinePicker {
    /**
     * While non-null, {@link me.braydon.chatutilities.mixin.client.ChatComponentMixin} forwards each
     * {@code LineConsumer.accept} from {@link ChatComponent#forEachLine} here (Proxy dispatch is unreliable for
     * package-private functional interfaces on some runtimes).
     */
    private static final AtomicReference<PickerState> PICK_CAPTURE = new AtomicReference<>();

    private static final Method FOR_EACH_LINE;

    static {
        try {
            // Do not use getDeclaredMethod("forEachLine", ...): in production the JVM uses intermediary
            // names (e.g. method_71990), not Mojmap "forEachLine". Match by parameter types instead.
            FOR_EACH_LINE = resolveForEachLine();
            FOR_EACH_LINE.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static Method resolveForEachLine() throws NoSuchMethodException {
        Class<?> alpha = ChatComponent.AlphaCalculator.class;
        Class<?> consumer = ChatComponent.LineConsumer.class;
        for (Method m : ChatComponent.class.getDeclaredMethods()) {
            if (m.getReturnType() != int.class || m.getParameterCount() != 2) {
                continue;
            }
            Class<?>[] p = m.getParameterTypes();
            if (p[0].equals(alpha) && p[1].equals(consumer)) {
                return m;
            }
        }
        throw new NoSuchMethodException(
                "ChatComponent line iterator (int, AlphaCalculator, LineConsumer) not found");
    }

    private VanillaChatLinePicker() {}

    /**
     * While true, {@link me.braydon.chatutilities.mixin.client.ChatComponentMixin} skips smooth-chat fade on line
     * opacity so hit-testing still sees full alpha (picker uses {@code AlphaCalculator.FULLY_VISIBLE}).
     */
    public static boolean isPickCaptureActive() {
        return PICK_CAPTURE.get() != null;
    }

    /** Called from {@link me.braydon.chatutilities.mixin.client.ChatComponentMixin} around each chat line. */
    public static void notifyLineDuringPick(GuiMessage.Line line, int lineIndex, float opacity) {
        PickerState state = PICK_CAPTURE.get();
        if (state != null) {
            state.onLine(line, lineIndex, opacity);
        }
    }

    public static Optional<Component> pickLineAt(Minecraft mc, int mouseX, int mouseY) {
        ChatComponent chat = mc.gui.getChat();
        if (chat.isChatHidden()) {
            return Optional.empty();
        }
        // Expanded chat (ChatScreen) draws lines fully opaque; timeBased applies HUD fade and yields ~0 for most lines.
        ChatComponent.AlphaCalculator alpha = ChatComponent.AlphaCalculator.FULLY_VISIBLE;
        PickerState state = new PickerState(mc, chat, mouseX, mouseY, guiScaledHeight(mc));
        Object consumer =
                Proxy.newProxyInstance(
                        ChatComponent.LineConsumer.class.getClassLoader(),
                        new Class<?>[] {ChatComponent.LineConsumer.class},
                        new LineConsumerHandler());
        PICK_CAPTURE.set(state);
        try {
            FOR_EACH_LINE.invoke(chat, alpha, consumer);
        } catch (ReflectiveOperationException e) {
            return Optional.empty();
        } finally {
            PICK_CAPTURE.set(null);
        }
        return entryComponentForLine(chat, state.pickedLine);
    }

    /** No-op consumer; real work happens in {@link #notifyLineDuringPick} via mixin redirect. */
    private static final class LineConsumerHandler implements InvocationHandler {
        @Override
        public @Nullable Object invoke(Object proxy, Method method, @Nullable Object[] args) {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "equals" ->
                            args != null && args.length == 1 && proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> "VanillaChatLinePicker$LineConsumer";
                    default -> null;
                };
            }
            return null;
        }
    }

    private static final class PickerState {
        /** Mouse in chat-local space (matches {@link ChatComponent} pose after updatePose). */
        private final float localMouseX;
        private final float localMouseY;
        /** Same as {@code ChatComponent} render: {@code floor((guiHeight - 40) / scale)}. */
        private final int chatBottom;
        /** Row height; same formula as {@link ChatComponent#getLineHeight()}. */
        private final int entryHeight;
        /** {@code ceil(getWidth() / scale)}; background fill uses x in {@code [-4, this+8)}. */
        private final int rowInnerWidth;
        private GuiMessage.@Nullable Line pickedLine;

        PickerState(Minecraft mc, ChatComponent chat, int mouseX, int mouseY, int guiHeight) {
            double scale = chat.getScale();
            float sf = (float) scale;
            var pose = new Matrix3x2f();
            pose.scale(sf, sf);
            pose.translate(4.0f, 0.0f);
            var inv = new Matrix3x2f(pose);
            inv.invert();
            Vector2f local = inv.transformPosition((float) mouseX, (float) mouseY, new Vector2f());
            this.localMouseX = local.x;
            this.localMouseY = local.y;
            this.chatBottom = Mth.floor((guiHeight - 40) / sf);
            this.entryHeight = Math.max(1, chat.getLineHeight());
            this.rowInnerWidth = Mth.ceil(chat.getWidth() / sf);
        }

        void onLine(GuiMessage.Line line, int lineIndex, float opacity) {
            if (opacity <= 0.01f) {
                return;
            }
            FormattedCharSequence seq = line.content();
            if (seq == null || FormattedCharSequence.EMPTY.equals(seq)) {
                return;
            }
            int slide = ChatSmoothAppearance.fadeSlideOffsetYPixels(line.addedTime());
            int rowBottom = this.chatBottom - lineIndex * this.entryHeight + slide;
            int rowTop = rowBottom - this.entryHeight;
            if (localMouseX < -4 || localMouseX >= rowInnerWidth + 8) {
                return;
            }
            if (localMouseY < rowTop || localMouseY >= rowBottom) {
                return;
            }
            pickedLine = line;
        }
    }

    /**
     * One logical message becomes several {@link GuiMessage.Line}s when wrapped. Vanilla pushes each wrapped row
     * with {@code addFirst}, so {@code trimmedMessages} runs <strong>newest-first</strong>: within one message, the
     * row with {@link GuiMessage.Line#endOfEntry()} {@code true} is the <em>last</em> wrapped row when splitting but
     * sits at the <em>lowest</em> index of that message's contiguous run. Copy joins the whole run in on-screen
     * order (top line first).
     */
    private static Optional<Component> entryComponentForLine(ChatComponent chat, GuiMessage.@Nullable Line hit) {
        if (hit == null) {
            return Optional.empty();
        }
        @SuppressWarnings("unchecked")
        List<GuiMessage.Line> lines = (List<GuiMessage.Line>) chat.trimmedMessages;
        int i = indexOfLine(lines, hit);
        if (i < 0) {
            return Optional.of(formattedSequenceToComponent(hit.content()));
        }
        int blockStart = i;
        while (blockStart > 0 && !lines.get(blockStart).endOfEntry()) {
            blockStart--;
        }
        int blockEnd = i;
        while (blockEnd < lines.size() - 1 && !lines.get(blockEnd + 1).endOfEntry()) {
            blockEnd++;
        }
        MutableComponent out = Component.empty();
        for (int k = blockEnd; k >= blockStart; k--) {
            if (k < blockEnd) {
                out.append(Component.literal("\n"));
            }
            FormattedCharSequence seq = lines.get(k).content();
            if (seq != null && !FormattedCharSequence.EMPTY.equals(seq)) {
                out.append(formattedSequenceToComponent(seq));
            }
        }
        return Optional.of(out);
    }

    private static int indexOfLine(List<GuiMessage.Line> lines, GuiMessage.Line hit) {
        for (int j = 0; j < lines.size(); j++) {
            if (lines.get(j) == hit) {
                return j;
            }
        }
        int tick = hit.addedTime();
        for (int j = 0; j < lines.size(); j++) {
            GuiMessage.Line line = lines.get(j);
            if (line.addedTime() == tick && line.equals(hit)) {
                return j;
            }
        }
        return -1;
    }

    private static int guiScaledHeight(Minecraft mc) {
        return mc.getWindow().getGuiScaledHeight();
    }

    private static Component formattedSequenceToComponent(FormattedCharSequence seq) {
        MutableComponent out = Component.empty();
        seq.accept(
                (index, style, codePoint) -> {
                    out.append(
                            Component.literal(new String(Character.toChars(codePoint)))
                                    .withStyle(style));
                    return true;
                });
        return out;
    }

}
