package me.braydon.chatutilities.gui;

import me.braydon.chatutilities.chat.ChatUtilitiesManager;
import me.braydon.chatutilities.chat.MessageSoundRule;
import me.braydon.chatutilities.chat.ProfileFaviconCache;
import me.braydon.chatutilities.chat.ServerProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.PatternSyntaxException;

/**
 * Main UI screen. Sodium-style: a centered floating panel with a left sidebar (profiles + sections)
 * and an inline content area on the right. All panels are shown without navigating away.
 */
public class ChatUtilitiesRootScreen extends Screen implements ProfileWorkflowScreen {

    // ── Panel geometry ─────────────────────────────────────────────────────────
    /** How far the panel is inset from each screen edge. */
    private static final int MARGIN_X = 42;
    private static final int MARGIN_Y = 28;
    /** Width of the left sidebar within the panel. */
    private static final int SIDEBAR_W = 185;

    // ── Sidebar row heights ────────────────────────────────────────────────────
    private static final int PROFILE_ROW_H = 28;
    private static final int SUB_ROW_H     = 18;
    /** Left indent for sub-item text. */
    private static final int SUB_INDENT    = 28;

    // ── Content area insets ────────────────────────────────────────────────────
    private static final int CONTENT_PAD_X      = 18;
    /** Y offsets within the content area (relative to panelTop). */
    private static final int TITLE_Y_OFF  = 10;
    private static final int DESC_Y_OFF   = 22;
    private static final int BODY_Y_OFF   = 58;
    private static final int FOOTER_INSET = 26; // from panelBottom

    // ── Sidebar colors ─────────────────────────────────────────────────────────
    private static final int C_PANEL_BG       = 0xF0101012;
    private static final int C_SIDEBAR_BG     = 0xFF080810;
    private static final int C_SIDEBAR_SEP    = 0xFF1E1E28;
    private static final int C_PROFILE_SEL    = 0xFF12203A;
    private static final int C_ACTIVE_BG      = 0xFF0E1C36;
    private static final int C_ACCENT         = 0xFF3A9FE0;
    private static final int C_HOVER          = 0x18FFFFFF;
    private static final int C_PROFILE_NAME   = 0xFFEEEEEE;
    private static final int C_PROFILE_DETAIL = 0xFF888898;
    private static final int C_SUB_NORMAL     = 0xFF8A8A9A;
    private static final int C_SUB_ACTIVE     = 0xFF5BB8FF;
    private static final int C_NEW_PROFILE    = 0xFF6EBF6E;

    // ── Panels ─────────────────────────────────────────────────────────────────
    public enum Panel { NONE, EDIT_PROFILE, CHAT_WINDOWS, IGNORED_CHAT, CHAT_SOUNDS }

    // ── Sidebar hit-test entries (rebuilt each render cycle) ───────────────────
    private record SidebarEntry(int y, int h, boolean isHeader, String profileId, Panel panel) {}
    private final List<SidebarEntry> sidebarEntries = new ArrayList<>();

    // ── Screen state ───────────────────────────────────────────────────────────
    private final Screen parent;
    private String expandedProfileId;
    private Panel activePanel = Panel.NONE;
    private int sidebarScroll;

    // Per-panel scroll positions (survive resize-triggered init() calls)
    private int serverScroll, winScroll, ignScroll, ruleScroll;

    // Chat Sounds: stash field text while SoundPickerScreen is open
    private String stashPattern, stashSound;

    // Widget references rebuilt each init()
    private EditBox nameField, newServerField;
    private EditBox newWinIdField, newWinPatField;
    private EditBox newIgnoreField;
    private EditBox patternField, soundField;

    // Sound autocomplete popup
    private static final int SUGGEST_ROWS  = 8;
    private static final int SUGGEST_ROW_H = 12;
    private List<String> sugLines = List.of();
    private int sugLeft, sugTop, sugWidth;

    // ── Constructors ───────────────────────────────────────────────────────────

    public ChatUtilitiesRootScreen(Screen parent) {
        super(Component.literal("Chat Utilities"));
        this.parent = parent;
        // Auto-expand and pre-select the active server's profile on first open
        ServerProfile active = ChatUtilitiesManager.get().getActiveProfile();
        if (active != null) {
            this.expandedProfileId = active.getId();
        }
    }

    /** Restore to a specific state after returning from in-world position mode. */
    public ChatUtilitiesRootScreen(Screen parent, String profileId, Panel panel) {
        super(Component.literal("Chat Utilities"));
        this.parent = parent;
        this.expandedProfileId = profileId;
        this.activePanel = panel;
    }

    // ── ProfileWorkflowScreen ──────────────────────────────────────────────────

    @Override public ChatUtilitiesRootScreen getChatRoot() { return this; }

    @Override
    public Screen recreateForProfile() {
        return new ChatUtilitiesRootScreen(parent, expandedProfileId, activePanel);
    }

    public Screen getParentScreen() { return parent; }

    // ── Panel / content coordinates ────────────────────────────────────────────

    private int panelLeft()   { return MARGIN_X; }
    private int panelRight()  { return this.width  - MARGIN_X; }
    private int panelTop()    { return MARGIN_Y; }
    private int panelBottom() { return this.height - MARGIN_Y; }
    private int panelW()      { return panelRight()  - panelLeft(); }
    private int panelH()      { return panelBottom() - panelTop(); }

    private int sidebarLeft()   { return panelLeft(); }
    private int sidebarRight()  { return panelLeft() + SIDEBAR_W; }
    private int sidebarTop()    { return panelTop(); }
    private int sidebarBottom() { return panelBottom(); }

    private int contentLeft()  { return sidebarRight() + CONTENT_PAD_X; }
    private int contentRight() { return panelRight()   - CONTENT_PAD_X; }
    private int contentCX()    { return sidebarRight() + (panelRight() - sidebarRight()) / 2; }
    private int contentW()     { return contentRight() - contentLeft(); }
    private int bodyY()        { return panelTop()    + BODY_Y_OFF; }
    private int footerY()      { return panelBottom() - FOOTER_INSET; }

    // ── Background override (panel only, game world visible at margins) ─────────

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Subtle global dim so the game world at the margins is not distracting
        g.fill(0, 0, this.width, this.height, 0x55000000);
        // Opaque panel background
        g.fill(panelLeft(), panelTop(), panelRight(), panelBottom(), C_PANEL_BG);
    }

    // ── init() ────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        clearWidgets();
        sugLines = List.of();

        if (expandedProfileId != null
                && ChatUtilitiesManager.get().getProfile(expandedProfileId) == null) {
            expandedProfileId = null;
            activePanel = Panel.NONE;
        }

        // Done button — always at bottom-right of content area
        addRenderableWidget(primaryButton(
                Component.literal("Done"), () -> onClose(),
                contentRight() - 80, footerY(), 80, 20));

        switch (activePanel) {
            case EDIT_PROFILE -> buildEditProfileWidgets();
            case CHAT_WINDOWS -> buildChatWindowsWidgets();
            case IGNORED_CHAT -> buildIgnoredChatWidgets();
            case CHAT_SOUNDS  -> buildChatSoundsWidgets();
            default -> {}
        }
    }

    // ── Flat button helpers ────────────────────────────────────────────────────

    /** Standard flat button used for most actions. */
    private AbstractWidget flatButton(Component label, Runnable press, int x, int y, int w, int h) {
        return new AbstractWidget(x, y, w, h, label) {
            @Override public void onClick(net.minecraft.client.input.MouseButtonEvent event, boolean dbl) { press.run(); }
            @Override protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                boolean hov = this.isHovered();
                boolean act = this.active;
                int bg      = !act ? 0x25000000 : hov ? 0x55FFFFFF : 0x35000000;
                int outline = !act ? 0x25FFFFFF : hov ? 0x70FFFFFF : 0x40FFFFFF;
                int tc      = !act ? 0xFF555565 : hov ? 0xFFFFFFFF : 0xFFBBBBCC;
                g.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bg);
                g.renderOutline(getX(), getY(), getWidth(), getHeight(), outline);
                g.drawCenteredString(Minecraft.getInstance().font, getMessage(),
                        getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, tc);
            }
            @Override public void updateWidgetNarration(NarrationElementOutput n) {
                defaultButtonNarrationText(n);
            }
        };
    }

    /** Accent (blue) button used for primary actions like Done. */
    private AbstractWidget primaryButton(Component label, Runnable press, int x, int y, int w, int h) {
        return new AbstractWidget(x, y, w, h, label) {
            @Override public void onClick(net.minecraft.client.input.MouseButtonEvent event, boolean dbl) { press.run(); }
            @Override protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                boolean hov = this.isHovered();
                int bg      = hov ? 0xCC2060A8 : 0xCC1A4A8A;
                int outline = hov ? 0xFF5BAAFF : 0xFF3A80CC;
                g.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bg);
                g.renderOutline(getX(), getY(), getWidth(), getHeight(), outline);
                g.drawCenteredString(Minecraft.getInstance().font, getMessage(),
                        getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, 0xFFFFFFFF);
            }
            @Override public void updateWidgetNarration(NarrationElementOutput n) {
                defaultButtonNarrationText(n);
            }
        };
    }

    // ── Edit Profile panel ────────────────────────────────────────────────────

    private static final int SERVER_ROWS = 6;

    private void buildEditProfileWidgets() {
        ServerProfile p = profile();
        if (p == null) return;
        ChatUtilitiesManager mgr = ChatUtilitiesManager.get();

        int fx = contentLeft();
        int fW = Math.min(240, contentW());
        int y  = bodyY();

        nameField = new EditBox(this.font, fx, y, fW, 20, Component.literal("name"));
        nameField.setValue(p.getDisplayName());
        nameField.setMaxLength(128);
        nameField.setHint(Component.literal("Profile name"));
        addRenderableWidget(nameField);
        y += 26;

        int addW = 70;
        newServerField = new EditBox(this.font, fx, y, fW - addW - 4, 20, Component.literal("host"));
        newServerField.setMaxLength(255);
        newServerField.setHint(Component.literal("e.g. play.example.net"));
        addRenderableWidget(newServerField);
        addRenderableWidget(flatButton(Component.literal("Add Server"), () -> {
            applyName(mgr, p);
            String h = newServerField.getValue().strip().toLowerCase(Locale.ROOT);
            if (!h.isEmpty() && !p.getServers().contains(h)) {
                p.getServers().add(h);
                mgr.markProfileServersDirty();
                newServerField.setValue("");
            }
            init();
        }, fx + fW - addW, y, addW, 20));
        y += 26;

        addRenderableWidget(flatButton(Component.literal("Add Current Server"), () -> {
            applyName(mgr, p);
            String h = ChatUtilitiesManager.currentConnectionHostNormalized();
            if (h != null && !h.isEmpty() && !p.getServers().contains(h)) {
                p.getServers().add(h);
                mgr.markProfileServersDirty();
            }
            init();
        }, fx, y, Math.min(160, fW), 18));
        y += 24;

        List<String> servers = p.getServers();
        int srvMax = Math.max(0, servers.size() - SERVER_ROWS);
        serverScroll = Math.min(serverScroll, srvMax);
        int srvEnd = Math.min(serverScroll + SERVER_ROWS, servers.size());
        for (int i = serverScroll; i < srvEnd; i++) {
            String s = servers.get(i);
            String label = s.length() > 32 ? s.substring(0, 29) + "..." : s;
            int idx = i;
            addRenderableWidget(flatButton(Component.literal("✕  " + label), () -> {
                applyName(mgr, p);
                p.getServers().remove(idx);
                mgr.markProfileServersDirty();
                serverScroll = Math.min(serverScroll, Math.max(0, p.getServers().size() - SERVER_ROWS));
                init();
            }, fx, y, Math.min(200, fW), 18));
            y += 20;
        }
        if (srvMax > 0) {
            if (serverScroll > 0)
                addRenderableWidget(flatButton(Component.literal("▲"),
                        () -> { serverScroll = Math.max(0, serverScroll - 1); init(); },
                        fx, y, 20, 14));
            if (serverScroll < srvMax)
                addRenderableWidget(flatButton(Component.literal("▼"),
                        () -> { serverScroll = Math.min(srvMax, serverScroll + 1); init(); },
                        fx + 24, y, 20, 14));
        }

        addRenderableWidget(flatButton(Component.literal("Delete Profile"), () ->
                Minecraft.getInstance().setScreen(new DeleteProfileConfirmScreen(
                        this, new ChatUtilitiesRootScreen(parent), expandedProfileId))
        , contentLeft(), footerY(), 110, 20));
    }

    private void applyName(ChatUtilitiesManager mgr, ServerProfile p) {
        if (nameField == null) return;
        String n = nameField.getValue().strip();
        if (!n.isEmpty()) {
            p.setDisplayName(n);
            mgr.markProfileServersDirty();
        }
    }

    // ── Chat Windows panel ────────────────────────────────────────────────────

    private static final int WIN_ROWS = 8;

    private void buildChatWindowsWidgets() {
        ServerProfile p = profile();
        if (p == null) return;
        ChatUtilitiesManager mgr = ChatUtilitiesManager.get();

        int fx = contentLeft();
        int fW = Math.min(240, contentW());
        int y  = bodyY();

        int idW  = Math.min(86, fW / 2 - 2);
        int patW = fW - idW - 4;
        newWinIdField = new EditBox(this.font, fx, y, idW, 20, Component.literal("id"));
        newWinIdField.setMaxLength(64);
        newWinIdField.setHint(Component.literal("Window id"));
        addRenderableWidget(newWinIdField);
        newWinPatField = new EditBox(this.font, fx + idW + 4, y, patW, 20, Component.literal("pat"));
        newWinPatField.setMaxLength(2048);
        newWinPatField.setHint(Component.literal("regex: or plain text…"));
        addRenderableWidget(newWinPatField);
        y += 24;

        addRenderableWidget(flatButton(Component.literal("Add Window"), () -> {
            String id = newWinIdField.getValue().strip();
            if (id.isEmpty()) return;
            try {
                if (mgr.createWindow(p, id, newWinPatField.getValue())) {
                    newWinIdField.setValue("");
                    newWinPatField.setValue("");
                }
            } catch (PatternSyntaxException ignored) {}
            init();
        }, fx, y, fW, 18));
        y += 24;

        // Adjust Layout button — enables positioning for all visible windows
        final String pid = expandedProfileId;
        addRenderableWidget(flatButton(Component.literal("Adjust Layout"), () -> {
            if (pid == null || profile() == null) return;
            mgr.enableAllWindowsPositioning(pid);
            mgr.setRestoreScreenAfterPosition(
                    () -> new ChatUtilitiesRootScreen(parent, pid, Panel.CHAT_WINDOWS));
            Minecraft.getInstance().setScreen(null);
        }, fx, y, fW, 18));
        y += 24;

        List<String> winIds = p.getWindowIds();
        int wMax = Math.max(0, winIds.size() - WIN_ROWS);
        winScroll = Math.min(winScroll, wMax);
        int wEnd = Math.min(winScroll + WIN_ROWS, winIds.size());
        for (int i = winScroll; i < wEnd; i++) {
            String wid = winIds.get(i);
            addRenderableWidget(flatButton(Component.literal("⚙  " + wid),
                    () -> Minecraft.getInstance().setScreen(
                            new WindowManageScreen(expandedProfileId, wid, this)),
                    fx, y, Math.min(210, fW), 18));
            y += 20;
        }
        if (wMax > 0) {
            if (winScroll > 0)
                addRenderableWidget(flatButton(Component.literal("▲"),
                        () -> { winScroll = Math.max(0, winScroll - 1); init(); },
                        fx, y, 20, 14));
            if (winScroll < wMax)
                addRenderableWidget(flatButton(Component.literal("▼"),
                        () -> { winScroll = Math.min(wMax, winScroll + 1); init(); },
                        fx + 24, y, 20, 14));
        }
    }

    // ── Ignored Chat panel ────────────────────────────────────────────────────

    private static final int IGN_ROWS = 9;

    private void buildIgnoredChatWidgets() {
        ServerProfile p = profile();
        if (p == null) return;
        ChatUtilitiesManager mgr = ChatUtilitiesManager.get();

        int fx  = contentLeft();
        int fW  = Math.min(240, contentW());
        int addW = 72;
        int y   = bodyY();

        newIgnoreField = new EditBox(this.font, fx, y, fW - addW - 4, 20, Component.literal("ign"));
        newIgnoreField.setMaxLength(2048);
        newIgnoreField.setHint(Component.literal("Hide matching chat…"));
        addRenderableWidget(newIgnoreField);
        addRenderableWidget(flatButton(Component.literal("Add Ignore"), () -> {
            try {
                mgr.addIgnorePattern(p, newIgnoreField.getValue());
                newIgnoreField.setValue("");
            } catch (PatternSyntaxException ignored) {}
            init();
        }, fx + fW - addW, y, addW, 20));
        y += 26;

        List<String> ignores = p.getIgnorePatternSources();
        int igMax = Math.max(0, ignores.size() - IGN_ROWS);
        ignScroll = Math.min(ignScroll, igMax);
        int igEnd = Math.min(ignScroll + IGN_ROWS, ignores.size());
        for (int i = ignScroll; i < igEnd; i++) {
            String pat = ignores.get(i);
            String label = pat.length() > 32 ? pat.substring(0, 29) + "..." : pat;
            int idx = i;
            addRenderableWidget(flatButton(Component.literal("✕  " + label), () -> {
                mgr.removeIgnorePattern(p, idx);
                init();
            }, fx, y, Math.min(210, fW), 18));
            y += 20;
        }
        if (igMax > 0) {
            if (ignScroll > 0)
                addRenderableWidget(flatButton(Component.literal("▲"),
                        () -> { ignScroll = Math.max(0, ignScroll - 1); init(); },
                        fx, y, 20, 14));
            if (ignScroll < igMax)
                addRenderableWidget(flatButton(Component.literal("▼"),
                        () -> { ignScroll = Math.min(igMax, ignScroll + 1); init(); },
                        fx + 24, y, 20, 14));
        }
    }

    // ── Chat Sounds panel ─────────────────────────────────────────────────────

    private static final int RULE_ROWS = 7;

    private void buildChatSoundsWidgets() {
        ServerProfile p = profile();
        if (p == null) return;
        ChatUtilitiesManager mgr = ChatUtilitiesManager.get();

        int fx = contentLeft();
        int fW = Math.min(240, contentW());
        int y  = bodyY();

        String restorePattern = stashPattern; stashPattern = null;
        String restoreSound   = stashSound;   stashSound   = null;

        patternField = new EditBox(this.font, fx, y, fW, 20, Component.literal("pat"));
        patternField.setMaxLength(2048);
        patternField.setHint(Component.literal("Match chat (or regex:…)"));
        if (restorePattern != null) patternField.setValue(restorePattern);
        addRenderableWidget(patternField);
        y += 24;

        int pickW = 44;
        soundField = new EditBox(this.font, fx, y, fW - pickW - 4, 20, Component.literal("snd"));
        soundField.setMaxLength(256);
        soundField.setHint(Component.literal("Sound id…"));
        if (restoreSound != null) soundField.setValue(restoreSound);
        addRenderableWidget(soundField);
        addRenderableWidget(flatButton(Component.literal("Pick"), () -> {
            stashPattern = patternField.getValue();
            stashSound   = soundField.getValue();
            Minecraft.getInstance().setScreen(
                    new SoundPickerScreen(this, id -> stashSound = id));
        }, fx + fW - pickW, y, pickW, 20));
        y += 24;

        int testW = 52;
        addRenderableWidget(flatButton(Component.literal("Test"), () ->
                ChatUtilitiesManager.parseSoundId(soundField.getValue())
                        .filter(ChatUtilitiesManager::isRegisteredSound)
                        .ifPresent(id -> ChatUtilitiesManager.playSoundPreview(id))
        , fx, y, testW, 18));
        addRenderableWidget(flatButton(Component.literal("Add Rule"), () -> {
            try {
                mgr.addMessageSound(p, patternField.getValue(), soundField.getValue());
                patternField.setValue("");
            } catch (IllegalArgumentException ignored) {}
            init();
        }, fx + testW + 4, y, fW - testW - 4, 18));
        y += 24;

        List<me.braydon.chatutilities.chat.MessageSoundRule> rules = p.getMessageSounds();
        int rMax = Math.max(0, rules.size() - RULE_ROWS);
        ruleScroll = Math.min(ruleScroll, rMax);
        int rEnd = Math.min(ruleScroll + RULE_ROWS, rules.size());
        for (int i = ruleScroll; i < rEnd; i++) {
            MessageSoundRule rule = rules.get(i);
            String pat = rule.getPatternSource();
            if (pat.length() > 14) pat = pat.substring(0, 11) + "...";
            Identifier sid = ChatUtilitiesManager.parseSoundId(rule.getSoundId()).orElse(null);
            String snd = sid != null ? sid.toString() : rule.getSoundId();
            if (snd.length() > 20) snd = snd.substring(0, 17) + "...";
            String label = pat + " → " + snd;
            int idx = i;
            addRenderableWidget(flatButton(Component.literal("✕  " + label), () -> {
                mgr.removeMessageSound(p, idx);
                init();
            }, fx, y, Math.min(210, fW), 18));
            y += 20;
        }
        if (rMax > 0) {
            if (ruleScroll > 0)
                addRenderableWidget(flatButton(Component.literal("▲"),
                        () -> { ruleScroll = Math.max(0, ruleScroll - 1); init(); },
                        fx, y, 20, 14));
            if (ruleScroll < rMax)
                addRenderableWidget(flatButton(Component.literal("▼"),
                        () -> { ruleScroll = Math.min(rMax, ruleScroll + 1); init(); },
                        fx + 24, y, 20, 14));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ServerProfile profile() {
        if (expandedProfileId == null) return null;
        return ChatUtilitiesManager.get().getProfile(expandedProfileId);
    }

    private void resetScrolls() {
        serverScroll = 0; winScroll = 0; ignScroll = 0; ruleScroll = 0;
    }

    /** Returns the profile list sorted so the currently active server's profile comes first. */
    private List<ServerProfile> sortedProfiles() {
        List<ServerProfile> all = new ArrayList<>(ChatUtilitiesManager.get().getProfiles());
        ServerProfile active = ChatUtilitiesManager.get().getActiveProfile();
        if (active != null && all.remove(active)) {
            all.add(0, active);
        }
        return all;
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        sugLines = List.of();
        super.render(graphics, mouseX, mouseY, partialTick); // renderBackground() + widgets
        renderSidebar(graphics, mouseX, mouseY);
        renderContentHeader(graphics);
        if (activePanel == Panel.CHAT_SOUNDS && soundField != null && soundField.isFocused()) {
            renderSoundSuggestions(graphics, mouseX, mouseY);
        }
    }

    private void renderSidebar(GuiGraphics g, int mouseX, int mouseY) {
        int sl = sidebarLeft(), sr = sidebarRight();
        int st = sidebarTop(),  sb = sidebarBottom();

        // Sidebar background (slightly darker than panel)
        g.fill(sl, st, sr, sb, C_SIDEBAR_BG);
        // Right separator line
        g.fill(sr, st, sr + 1, sb, C_SIDEBAR_SEP);

        // "Chat Utilities" header strip
        int titleH = 30;
        g.fill(sl, st, sr, st + titleH, 0xFF040408);
        g.fill(sl, st + titleH, sr, st + titleH + 1, C_SIDEBAR_SEP);
        g.drawCenteredString(this.font, "Chat Utilities", sl + SIDEBAR_W / 2,
                st + (titleH - 8) / 2, 0xFFE0E0F0);

        // "+ New Profile" footer strip
        int footerH = 26;
        int footerTop = sb - footerH;
        g.fill(sl, footerTop - 1, sr, footerTop, C_SIDEBAR_SEP);
        g.fill(sl, footerTop, sr, sb, 0xFF040408);
        boolean cpHov = mouseX >= sl && mouseX < sr && mouseY >= footerTop && mouseY < sb;
        if (cpHov) g.fill(sl, footerTop, sr, sb, C_HOVER);
        g.drawString(this.font, "+ New Profile",
                sl + SUB_INDENT, footerTop + (footerH - 8) / 2, C_NEW_PROFILE, false);

        // Scrollable profile list
        sidebarEntries.clear();
        List<ServerProfile> profiles = sortedProfiles();
        int listTop    = st + titleH + 1;
        int listBottom = footerTop - 1;
        int listAreaH  = listBottom - listTop;
        int totalH     = computeSidebarTotalH(profiles);
        int maxScroll  = Math.max(0, totalH - listAreaH);
        sidebarScroll  = Mth.clamp(sidebarScroll, 0, maxScroll);

        g.enableScissor(sl, listTop, sr, listBottom);
        try {
            int iy = listTop - sidebarScroll;

            if (profiles.isEmpty()) {
                int ey = listTop + listAreaH / 2 - 4;
                g.drawCenteredString(this.font, "No profiles yet.", sl + SIDEBAR_W / 2, ey, 0xFF505060);
            }

            for (ServerProfile p : profiles) {
                boolean expanded = p.getId().equals(expandedProfileId);

                // ── Profile header ──────────────────────────────────────────
                int rowY = iy;
                if (rowY + PROFILE_ROW_H > listTop && rowY < listBottom) {
                    boolean hov = mouseX >= sl && mouseX < sr
                            && mouseY >= rowY && mouseY < rowY + PROFILE_ROW_H;
                    if (expanded) g.fill(sl, rowY, sr, rowY + PROFILE_ROW_H, C_PROFILE_SEL);
                    if (hov)      g.fill(sl, rowY, sr, rowY + PROFILE_ROW_H, C_HOVER);
                    // Bottom hairline
                    g.fill(sl, rowY + PROFILE_ROW_H - 1, sr, rowY + PROFILE_ROW_H, 0x18FFFFFF);

                    // Favicon / item icon (16x16)
                    int iconX = sl + 6;
                    int iconY = rowY + (PROFILE_ROW_H - 16) / 2;
                    String firstHost = p.getServers().isEmpty() ? null : p.getServers().getFirst();
                    Identifier favicon = ProfileFaviconCache.getIcon(this.minecraft, firstHost);
                    if (favicon != null) {
                        g.blit(RenderPipelines.GUI_TEXTURED, favicon,
                                iconX, iconY, 0f, 0f, 16, 16, 16, 16);
                    } else {
                        g.renderItem(new ItemStack(Items.COMPASS), iconX, iconY);
                    }

                    // Profile name (truncated)
                    String name = p.getDisplayName();
                    int maxNW = SIDEBAR_W - 30 - 14;
                    boolean trunc = false;
                    while (name.length() > 1 && this.font.width(name + "…") > maxNW) {
                        name = name.substring(0, name.length() - 1);
                        trunc = true;
                    }
                    if (trunc) name += "…";
                    g.drawString(this.font, name,
                            iconX + 20, rowY + (PROFILE_ROW_H - 8) / 2, C_PROFILE_NAME, false);

                    // Expand arrow
                    g.drawString(this.font, expanded ? "▾" : "▸",
                            sr - 12, rowY + (PROFILE_ROW_H - 8) / 2, 0xFF606070, false);

                    sidebarEntries.add(new SidebarEntry(rowY, PROFILE_ROW_H, true, p.getId(), null));
                }
                iy += PROFILE_ROW_H;

                // ── Sub-items ───────────────────────────────────────────────
                if (expanded) {
                    Panel[]  panels = {Panel.CHAT_WINDOWS, Panel.IGNORED_CHAT, Panel.CHAT_SOUNDS, Panel.EDIT_PROFILE};
                    String[] labels = {"Chat Windows",     "Ignored Chat",     "Chat Sounds",     "Edit Profile"};
                    for (int si = 0; si < panels.length; si++) {
                        Panel  sp = panels[si];
                        String sl2 = labels[si];
                        int    subY = iy;
                        if (subY + SUB_ROW_H > listTop && subY < listBottom) {
                            boolean active  = sp == activePanel;
                            boolean subHov  = mouseX >= sl && mouseX < sr
                                    && mouseY >= subY && mouseY < subY + SUB_ROW_H;
                            if (active)            g.fill(sl, subY, sr, subY + SUB_ROW_H, C_ACTIVE_BG);
                            if (subHov && !active) g.fill(sl, subY, sr, subY + SUB_ROW_H, C_HOVER);
                            // Left accent bar
                            if (active) g.fill(sl, subY, sl + 2, subY + SUB_ROW_H, C_ACCENT);
                            g.drawString(this.font, sl2,
                                    sl + SUB_INDENT, subY + (SUB_ROW_H - 8) / 2,
                                    active ? C_SUB_ACTIVE : C_SUB_NORMAL, false);
                            sidebarEntries.add(new SidebarEntry(subY, SUB_ROW_H, false, p.getId(), sp));
                        }
                        iy += SUB_ROW_H;
                    }
                    // Hairline separator after expanded group
                    if (iy > listTop && iy < listBottom) {
                        g.fill(sl, iy, sr, iy + 1, 0x20FFFFFF);
                    }
                }
            }
        } finally {
            g.disableScissor();
        }
    }

    private int computeSidebarTotalH(List<ServerProfile> profiles) {
        int h = 0;
        for (ServerProfile p : profiles) {
            h += PROFILE_ROW_H;
            if (p.getId().equals(expandedProfileId)) h += SUB_ROW_H * 4;
        }
        return h;
    }

    private void renderContentHeader(GuiGraphics g) {
        int cx   = contentCX();
        int descW = Math.min(contentW() - CONTENT_PAD_X, 420);

        String title = switch (activePanel) {
            case EDIT_PROFILE -> "Edit Profile";
            case CHAT_WINDOWS -> "Chat Windows";
            case IGNORED_CHAT -> "Ignored Chat";
            case CHAT_SOUNDS  -> "Chat Sounds";
            default           -> "Server Profiles";
        };
        String desc = switch (activePanel) {
            case EDIT_PROFILE ->
                "Give this profile a name and list the servers it should activate on. "
                + "Partial domain matching works — adding 'hypixel.net' also covers 'mc.hypixel.net'.";
            case CHAT_WINDOWS ->
                "Windows capture chat lines matching your patterns and show them in a separate HUD panel. "
                + "Great for isolating party chat, server events, or anything you want to track. "
                + "Use 'Adjust Layout' to drag and resize all windows at once.";
            case IGNORED_CHAT ->
                "Messages matching any of these patterns are silently removed before reaching your chat. "
                + "Handy for hiding spam or system noise. Prefix a pattern with regex: for a regular expression.";
            case CHAT_SOUNDS  ->
                "Plays a sound whenever a chat message matches a rule. "
                + "Ignored messages never trigger sounds. Prefix a pattern with regex: for a regular expression.";
            default ->
                "Pick a profile from the sidebar to manage its chat windows, filters, and sounds.";
        };

        g.drawCenteredString(this.font, title, cx, panelTop() + TITLE_Y_OFF, 0xFFFFFFFF);
        // Thin separator under title
        g.fill(sidebarRight() + CONTENT_PAD_X, panelTop() + TITLE_Y_OFF + 12,
                panelRight() - CONTENT_PAD_X, panelTop() + TITLE_Y_OFF + 13, 0x30FFFFFF);
        ChatUtilitiesScreenLayout.drawCenteredWrapped(
                this.font, g, Component.literal(desc),
                cx, panelTop() + DESC_Y_OFF, descW, ChatUtilitiesScreenLayout.TEXT_GRAY, 10);
    }

    private void renderSoundSuggestions(GuiGraphics g, int mouseX, int mouseY) {
        sugLines = SoundRegistryList.filterContains(soundField.getValue(), SUGGEST_ROWS);
        if (sugLines.isEmpty()) return;
        sugLeft  = soundField.getX();
        sugTop   = soundField.getY() + soundField.getHeight() + 1;
        sugWidth = Math.max(200, soundField.getWidth() + 48);
        int sugBottom = sugTop + sugLines.size() * SUGGEST_ROW_H;
        g.fill(sugLeft - 1, sugTop - 1, sugLeft + sugWidth + 1, sugBottom + 1, 0xFF000000);
        g.fill(sugLeft,     sugTop,     sugLeft + sugWidth,     sugBottom,     C_SIDEBAR_BG);
        for (int i = 0; i < sugLines.size(); i++) {
            String line = sugLines.get(i);
            if (line.length() > 48) line = line.substring(0, 45) + "...";
            int ry = sugTop + i * SUGGEST_ROW_H;
            boolean hov = mouseX >= sugLeft && mouseX < sugLeft + sugWidth
                    && mouseY >= ry && mouseY < ry + SUGGEST_ROW_H;
            if (hov) g.fill(sugLeft, ry, sugLeft + sugWidth, ry + SUGGEST_ROW_H, 0x336688FF);
            g.drawString(this.font, line, sugLeft + 3, ry + 2, 0xFFFFFFFF, false);
        }
    }

    // ── Mouse input ───────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            double mx = event.x();
            double my = event.y();

            // Sound suggestion click
            if (activePanel == Panel.CHAT_SOUNDS && soundField != null
                    && soundField.isFocused() && !sugLines.isEmpty()) {
                if (mx >= sugLeft && mx < sugLeft + sugWidth
                        && my >= sugTop && my < sugTop + sugLines.size() * SUGGEST_ROW_H) {
                    int row = (int) ((my - sugTop) / SUGGEST_ROW_H);
                    if (row >= 0 && row < sugLines.size()) {
                        soundField.setValue(sugLines.get(row));
                        soundField.moveCursorToEnd(false);
                        return true;
                    }
                }
            }

            // Sidebar click (including its header/footer strips)
            if (mx >= sidebarLeft() && mx < sidebarRight()) {
                int footerTop = sidebarBottom() - 26;
                // "+ New Profile"
                if (my >= footerTop && my < sidebarBottom()) {
                    ServerProfile np = ChatUtilitiesManager.get()
                            .createProfileForCurrentServer("New Profile");
                    expandedProfileId = np.getId();
                    activePanel = Panel.EDIT_PROFILE;
                    serverScroll = 0;
                    init();
                    return true;
                }
                // Profile headers and sub-items
                for (SidebarEntry entry : sidebarEntries) {
                    if (my >= entry.y() && my < entry.y() + entry.h()) {
                        if (entry.isHeader()) {
                            if (entry.profileId().equals(expandedProfileId)) {
                                expandedProfileId = null;
                                activePanel = Panel.NONE;
                            } else {
                                expandedProfileId = entry.profileId();
                                activePanel = Panel.NONE;
                            }
                        } else {
                            expandedProfileId = entry.profileId();
                            activePanel = entry.panel();
                            resetScrolls();
                        }
                        init();
                        return true;
                    }
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(
            double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX >= sidebarLeft() && mouseX < sidebarRight()) {
            List<ServerProfile> profiles = sortedProfiles();
            int titleH = 30, footerH = 26;
            int listAreaH = panelH() - titleH - 1 - footerH - 2;
            int maxScroll = Math.max(0, computeSidebarTotalH(profiles) - listAreaH);
            int delta = verticalAmount > 0 ? -PROFILE_ROW_H : PROFILE_ROW_H;
            sidebarScroll = Mth.clamp(sidebarScroll + delta, 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }
}
