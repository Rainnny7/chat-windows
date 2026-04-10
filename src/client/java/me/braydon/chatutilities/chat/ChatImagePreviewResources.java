package me.braydon.chatutilities.chat;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

/**
 * Downloads image bytes off-thread, uploads {@link DynamicTexture} on the client thread, LRU-evicts with
 * {@link net.minecraft.client.renderer.texture.TextureManager#release}.
 */
public final class ChatImagePreviewResources {

    private static final int MAX_CACHE = 48;
    private static final ForkJoinPool POOL = ForkJoinPool.commonPool();
    private static final HttpClient HTTP =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();

    private static final Map<String, CachedTex> CACHE =
            new java.util.LinkedHashMap<>(32, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, CachedTex> eldest) {
                    if (size() <= MAX_CACHE) {
                        return false;
                    }
                    evictTexture(eldest.getValue());
                    return true;
                }
            };

    private ChatImagePreviewResources() {}

    public enum State {
        LOADING,
        READY,
        FAILED
    }

    public record CachedTex(State state, Identifier textureId, int width, int height) {}

    private static final CachedTex LOADING = new CachedTex(State.LOADING, null, 0, 0);

    public static CachedTex getOrStartLoading(String url) {
        synchronized (CACHE) {
            CachedTex existing = CACHE.get(url);
            if (existing != null) {
                return existing;
            }
            CACHE.put(url, LOADING);
        }
        CompletableFuture.runAsync(() -> downloadOffThread(url), POOL);
        return LOADING;
    }

    private static void downloadOffThread(String url) {
        byte[] body;
        try {
            HttpRequest req =
                    HttpRequest.newBuilder(URI.create(url))
                            .timeout(Duration.ofSeconds(15))
                            .GET()
                            .build();
            HttpResponse<InputStream> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofInputStream());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                markFailed(url);
                return;
            }
            try (InputStream in = resp.body()) {
                body = in.readAllBytes();
            }
        } catch (Exception e) {
            markFailed(url);
            return;
        }
        final byte[] data = body;
        Minecraft.getInstance()
                .execute(
                        () -> {
                            NativeImage img;
                            try {
                                img = NativeImage.read(data);
                            } catch (Exception ex) {
                                markFailed(url);
                                return;
                            }
                            if (img == null) {
                                markFailed(url);
                                return;
                            }
                            int w = img.getWidth();
                            int h = img.getHeight();
                            if (w < 1 || h < 1 || w > 8192 || h > 8192) {
                                img.close();
                                markFailed(url);
                                return;
                            }
                            String debugPath = "chatutilities/preview/" + Integer.toHexString(Objects.hashCode(url));
                            DynamicTexture tex = new DynamicTexture(() -> debugPath, img);
                            Identifier id = Identifier.fromNamespaceAndPath("chatutilities", "preview/" + Integer.toHexString(Objects.hashCode(url)));
                            Minecraft.getInstance().getTextureManager().register(id, tex);
                            synchronized (CACHE) {
                                CACHE.put(url, new CachedTex(State.READY, id, w, h));
                            }
                        });
    }

    private static void markFailed(String url) {
        Minecraft.getInstance()
                .execute(
                        () -> {
                            synchronized (CACHE) {
                                CACHE.put(url, new CachedTex(State.FAILED, null, 0, 0));
                            }
                        });
    }

    private static void evictTexture(CachedTex e) {
        if (e != null && e.textureId() != null) {
            Minecraft.getInstance().execute(() -> Minecraft.getInstance().getTextureManager().release(e.textureId()));
        }
    }

    public static void clearCache() {
        synchronized (CACHE) {
            for (CachedTex e : CACHE.values()) {
                evictTexture(e);
            }
            CACHE.clear();
        }
    }
}
