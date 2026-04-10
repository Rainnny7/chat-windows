package me.braydon.chatutilities.chat;

import org.jspecify.annotations.Nullable;

/** Last previewable image URL under the cursor (same chat session). */
public final class ChatImagePreviewState {

    private static volatile @Nullable String lastHoveredPreviewUrl;

    private ChatImagePreviewState() {}

    public static void setLastHoveredPreviewUrl(@Nullable String url) {
        lastHoveredPreviewUrl = url;
    }

    public static @Nullable String getLastHoveredPreviewUrl() {
        return lastHoveredPreviewUrl;
    }
}
