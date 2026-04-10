package me.braydon.chatutilities.gui;

import net.minecraft.util.Mth;

/** HSV / ARGB helpers for the mod primary color picker. */
public final class ModPrimaryColorUtils {

    private ModPrimaryColorUtils() {}

    public static int clamp255(int v) {
        return Mth.clamp(v, 0, 255);
    }

    public static String formatHexArgb(int argb) {
        return String.format("#%08X", argb);
    }

    public static int parseHexArgbOrDefault(String raw, int fallback) {
        if (raw == null) {
            return fallback;
        }
        String s = raw.strip();
        if (s.startsWith("#")) {
            s = s.substring(1);
        }
        if (s.length() != 8 && s.length() != 6) {
            return fallback;
        }
        try {
            long v = Long.parseLong(s, 16);
            if (s.length() == 6) {
                return (int) (0xFF000000L | v);
            }
            return (int) v;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** Returns {@code [h,s,v]} with h in [0,1), s and v in [0,1]. */
    public static float[] rgbToHsv(int r, int g, int b) {
        float rf = r / 255f;
        float gf = g / 255f;
        float bf = b / 255f;
        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float d = max - min;
        float h;
        if (d < 1e-5f) {
            h = 0f;
        } else if (max == rf) {
            h = ((gf - bf) / d + (gf < bf ? 6f : 0f)) / 6f;
        } else if (max == gf) {
            h = ((bf - rf) / d + 2f) / 6f;
        } else {
            h = ((rf - gf) / d + 4f) / 6f;
        }
        float s = max > 1e-5f ? d / max : 0f;
        return new float[] {h % 1f, s, max};
    }

    public static int hsvToArgb(float h, float s, float v, int alpha) {
        h = (h % 1f + 1f) % 1f;
        s = Mth.clamp(s, 0f, 1f);
        v = Mth.clamp(v, 0f, 1f);
        int i = (int) (h * 6f);
        float f = h * 6f - i;
        float p = v * (1f - s);
        float q = v * (1f - s * f);
        float t = v * (1f - s * (1f - f));
        float rf;
        float gf;
        float bf;
        switch (i % 6) {
            case 0 -> {
                rf = v;
                gf = t;
                bf = p;
            }
            case 1 -> {
                rf = q;
                gf = v;
                bf = p;
            }
            case 2 -> {
                rf = p;
                gf = v;
                bf = t;
            }
            case 3 -> {
                rf = p;
                gf = q;
                bf = v;
            }
            case 4 -> {
                rf = t;
                gf = p;
                bf = v;
            }
            default -> {
                rf = v;
                gf = p;
                bf = q;
            }
        }
        int a = clamp255(alpha);
        int r = clamp255(Math.round(rf * 255f));
        int g = clamp255(Math.round(gf * 255f));
        int b = clamp255(Math.round(bf * 255f));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int mixRgb(int rgb0, int rgb1, float t) {
        t = Mth.clamp(t, 0f, 1f);
        int r0 = (rgb0 >> 16) & 0xFF;
        int g0 = (rgb0 >> 8) & 0xFF;
        int b0 = rgb0 & 0xFF;
        int r1 = (rgb1 >> 16) & 0xFF;
        int g1 = (rgb1 >> 8) & 0xFF;
        int b1 = rgb1 & 0xFF;
        int r = Math.round(r0 + (r1 - r0) * t);
        int g = Math.round(g0 + (g1 - g0) * t);
        int b = Math.round(b0 + (b1 - b0) * t);
        return (r << 16) | (g << 8) | b;
    }

    /** Brighter RGB (0xRRGGBB) for hover accents. */
    public static int brightenRgb(int rgb, float factor) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        r = clamp255(Math.round(r * factor));
        g = clamp255(Math.round(g * factor));
        b = clamp255(Math.round(b * factor));
        return (r << 16) | (g << 8) | b;
    }
}
