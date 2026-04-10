package me.braydon.chatutilities.chat;

/**
 * Guards against chat actions being re-run during internal chat list rebuilds (e.g. refreshTrimmedMessages).
 * Some vanilla rebuild paths may re-queue existing messages, which should not be treated as new incoming chat.
 */
public final class ChatMessageRebuildGuard {
    private static final ThreadLocal<Integer> depth = ThreadLocal.withInitial(() -> 0);

    private ChatMessageRebuildGuard() {}

    public static void enter() {
        depth.set(depth.get() + 1);
    }

    public static void exit() {
        int d = depth.get() - 1;
        if (d <= 0) {
            depth.remove();
        } else {
            depth.set(d);
        }
    }

    public static boolean isActive() {
        return depth.get() > 0;
    }
}
