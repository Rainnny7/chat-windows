package me.braydon.chatutilities.chat;

import java.util.Locale;

/** One effect inside a {@link ChatActionGroup} (same pattern can have several). */
public final class ChatActionEffect {

    public static final int DEFAULT_HIGHLIGHT_RGB = 0xFFFF55;

    public enum Type {
        IGNORE,
        PLAY_SOUND,
        COLOR_HIGHLIGHT,
        TEXT_REPLACEMENT,
        AUTO_RESPONSE;

        public String persistKey() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Type fromPersistKey(String raw) {
            if (raw == null || raw.isBlank()) {
                return IGNORE;
            }
            String s = raw.strip().toUpperCase(Locale.ROOT);
            if ("PLAY_SOUND".equals(s) || "SOUND".equals(s)) {
                return PLAY_SOUND;
            }
            if ("COLOR_HIGHLIGHT".equals(s) || "HIGHLIGHT".equals(s) || "COLOR".equals(s)) {
                return COLOR_HIGHLIGHT;
            }
            if ("TEXT_REPLACEMENT".equals(s) || "REPLACE".equals(s) || "REPLACEMENT".equals(s)) {
                return TEXT_REPLACEMENT;
            }
            if ("AUTO_RESPONSE".equals(s) || "AUTORESPONSE".equals(s) || "RESPONSE".equals(s)) {
                return AUTO_RESPONSE;
            }
            return IGNORE;
        }
    }

    private Type type;
    private String soundId;
    /** Used by {@link Type#TEXT_REPLACEMENT} and {@link Type#AUTO_RESPONSE}. */
    private String targetText;
    /** RGB 0xRRGGBB for {@link Type#COLOR_HIGHLIGHT}. */
    private int highlightColorRgb;
    private boolean highlightBold;
    private boolean highlightItalic;
    private boolean highlightUnderlined;
    private boolean highlightStrikethrough;
    private boolean highlightObfuscated;

    public ChatActionEffect(Type type, String soundId) {
        this(type, soundId, "", DEFAULT_HIGHLIGHT_RGB);
    }

    public ChatActionEffect(Type type, String soundId, int highlightColorRgb) {
        this(type, soundId, "", highlightColorRgb, false, false, false, false, false);
    }

    public ChatActionEffect(Type type, String soundId, String targetText, int highlightColorRgb) {
        this(type, soundId, targetText, highlightColorRgb, false, false, false, false, false);
    }

    public ChatActionEffect(
            Type type,
            String soundId,
            String targetText,
            int highlightColorRgb,
            boolean highlightBold,
            boolean highlightItalic,
            boolean highlightUnderlined,
            boolean highlightStrikethrough,
            boolean highlightObfuscated) {
        this.type = type == null ? Type.IGNORE : type;
        this.soundId = soundId == null ? "" : soundId;
        this.targetText = targetText == null ? "" : targetText;
        this.highlightColorRgb = highlightColorRgb & 0xFFFFFF;
        this.highlightBold = highlightBold;
        this.highlightItalic = highlightItalic;
        this.highlightUnderlined = highlightUnderlined;
        this.highlightStrikethrough = highlightStrikethrough;
        this.highlightObfuscated = highlightObfuscated;
    }

    /** Copy for {@link ChatActionGroup} clone. */
    public ChatActionEffect copy() {
        return new ChatActionEffect(
                type,
                soundId,
                targetText,
                highlightColorRgb,
                highlightBold,
                highlightItalic,
                highlightUnderlined,
                highlightStrikethrough,
                highlightObfuscated);
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type == null ? Type.IGNORE : type;
    }

    public String getSoundId() {
        return soundId;
    }

    public void setSoundId(String soundId) {
        this.soundId = soundId == null ? "" : soundId;
    }

    public String getTargetText() {
        return targetText;
    }

    public void setTargetText(String targetText) {
        this.targetText = targetText == null ? "" : targetText;
    }

    public int getHighlightColorRgb() {
        return highlightColorRgb & 0xFFFFFF;
    }

    public void setHighlightColorRgb(int highlightColorRgb) {
        this.highlightColorRgb = highlightColorRgb & 0xFFFFFF;
    }

    public boolean isHighlightBold() {
        return highlightBold;
    }

    public void setHighlightBold(boolean highlightBold) {
        this.highlightBold = highlightBold;
    }

    public boolean isHighlightItalic() {
        return highlightItalic;
    }

    public void setHighlightItalic(boolean highlightItalic) {
        this.highlightItalic = highlightItalic;
    }

    public boolean isHighlightUnderlined() {
        return highlightUnderlined;
    }

    public void setHighlightUnderlined(boolean highlightUnderlined) {
        this.highlightUnderlined = highlightUnderlined;
    }

    public boolean isHighlightStrikethrough() {
        return highlightStrikethrough;
    }

    public void setHighlightStrikethrough(boolean highlightStrikethrough) {
        this.highlightStrikethrough = highlightStrikethrough;
    }

    public boolean isHighlightObfuscated() {
        return highlightObfuscated;
    }

    public void setHighlightObfuscated(boolean highlightObfuscated) {
        this.highlightObfuscated = highlightObfuscated;
    }
}
