package me.braydon.chatutilities.chat;

import me.braydon.chatutilities.client.ChatUtilitiesClientOptions;

import java.net.URI;
import java.util.Locale;
import java.util.Optional;

/** URL to image preview eligibility (extension + domain whitelist). */
public final class ChatImagePreviewUrls {

    private ChatImagePreviewUrls() {}

    public static Optional<String> normalizePreviewableHttpUrl(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        URI uri;
        try {
            uri = URI.create(raw.strip());
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        if (!uri.isAbsolute()) {
            return Optional.empty();
        }
        String scheme = uri.getScheme();
        if (scheme == null) {
            return Optional.empty();
        }
        String sch = scheme.toLowerCase(Locale.ROOT);
        if (!"https".equals(sch) && !"http".equals(sch)) {
            return Optional.empty();
        }
        String host = uri.getHost();
        if (host == null || host.isEmpty()) {
            return Optional.empty();
        }
        if (!hostAllowedForPreview(host)) {
            return Optional.empty();
        }
        String path = uri.getPath();
        if (path == null || path.isEmpty()) {
            return Optional.empty();
        }
        if (!pathLooksLikeImageFile(path)) {
            return Optional.empty();
        }
        return Optional.of(uri.toString());
    }

    static boolean hostAllowedForPreview(String host) {
        if (ChatUtilitiesClientOptions.isAllowUntrustedImagePreviewDomains()) {
            return true;
        }
        String h = host.toLowerCase(Locale.ROOT);
        for (String entry : ChatUtilitiesClientOptions.getImagePreviewWhitelistHosts()) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            String e = entry.strip().toLowerCase(Locale.ROOT);
            if (e.isEmpty()) {
                continue;
            }
            if (h.equals(e) || h.endsWith("." + e)) {
                return true;
            }
        }
        return false;
    }

    static boolean pathLooksLikeImageFile(String path) {
        int q = path.indexOf('?');
        String p = q >= 0 ? path.substring(0, q) : path;
        int slash = p.lastIndexOf('/');
        String file = slash >= 0 ? p.substring(slash + 1) : p;
        int dot = file.lastIndexOf('.');
        if (dot < 0 || dot >= file.length() - 1) {
            return false;
        }
        String ext = file.substring(dot + 1).toLowerCase(Locale.ROOT);
        return switch (ext) {
            case "png", "jpg", "jpeg", "gif", "webp", "bmp" -> true;
            default -> false;
        };
    }
}
