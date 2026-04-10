package me.braydon.chatutilities.client;

import me.braydon.chatutilities.gui.ModPrimaryColorUtils;
import net.minecraft.util.Mth;

/** Resolves the accent ARGB for UI, including optional chroma animation. */
public final class ModAccentAnimator {

    private ModAccentAnimator() {}

    public static int currentArgb() {
        int base = ChatUtilitiesClientOptions.getModPrimaryArgb();
        if (!ChatUtilitiesClientOptions.isModPrimaryChroma()) {
            return base;
        }
        float t = ModChromaClock.phaseSeconds();
        float speed = ChatUtilitiesClientOptions.getModPrimaryChromaSpeed();
        int a = (base >>> 24) & 0xFF;
        int r = (base >> 16) & 0xFF;
        int g = (base >> 8) & 0xFF;
        int b = base & 0xFF;
        float[] hsv = ModPrimaryColorUtils.rgbToHsv(r, g, b);
        float hue = chromaSmoothHue(t, speed, hsv[0]);
        // Full saturation/value so the accent reads as a vivid rainbow, not a muted tint of the saved RGB.
        return ModPrimaryColorUtils.hsvToArgb(hue, 1f, 1f, a);
    }

    /** Smooth oscillation around the saved hue (single chroma mode). */
    private static float chromaSmoothHue(float timeSec, float speed, float baseHue) {
        float w = Mth.sin(timeSec * speed * 0.12f) * 0.5f + 0.5f;
        return (baseHue + w * 0.35f) % 1f;
    }
}
