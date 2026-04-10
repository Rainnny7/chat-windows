package me.braydon.chatutilities.client;

/**
 * Monotonic client-tick phase for chroma hue animation (used by {@link ModAccentAnimator} and the color picker
 * overlay) so hue advances reliably with the running game, not only with wall clock.
 */
public final class ModChromaClock {

    private static int ticks;

    private ModChromaClock() {}

    public static void onClientTick() {
        ticks++;
    }

    /** Seconds since client start, advancing at ~20 Hz. */
    public static float phaseSeconds() {
        return ticks / 20f;
    }
}
