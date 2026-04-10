# Chat Utilities

A [Fabric](https://fabricmc.net/) client mod that upgrades your chat into a configurable toolkit: **chat windows with tabs**, a **GUI settings menu**, **chat search**, **click-to-copy**, **image link previews**, **timestamps**, and more.

[View a demo on YouTube](https://youtu.be/lMcVxxYGzR0)

## Key features

- **In-game GUI**: open the Chat Utilities menu via the keybind (**Open Chat Utilities**) or `/chatutils` (`/chatutilities`).
- **Chat windows + tabs**: create extra HUD panes, split them into tabs, and **drag/resize** them with an “Adjust Layout” mode.
- **Window filtering**: route matching messages into windows/tabs using **plain text** patterns or `regex:` Java regex.
- **Unread tab badges**: optional red unread indicators on background tabs.
- **Chat search bar**: optional filter row when chat is open, with a **Jump** action to snap to a matched line.
- **Click to copy**: copy plain or formatted chat with configurable mouse binds; choose output style (**Vanilla `&`**, **Section `§`**, or **MiniMessage**).
- **Chat symbol selector**: quick picker for inserting symbols/colors/styles with your chosen code style.
- **Image link previews**: hover whitelisted image URLs for thumbnails; open fullscreen via keybind or Shift+click (configurable).
- **Timestamps**: prepend timestamps using `DateTimeFormatter` patterns and a configurable color.
- **Smooth chat + visuals**: fade/slide-in animations, chat background opacity controls, optional chat text shadow.
- **Bigger history + stacking repeats**: optional longer chat history and repeated-message stacking with a `(xN)` counter.
- **Command aliases**: map `/short` → `/long ...` while preserving arguments (longest match wins).
- **Profiles + actions**: per-server profiles with configurable chat actions (ignore, play sounds, highlight, text replacement, auto responses) and import/export.

## How to open it

- **Keybind**: Controls → Chat Utilities → **Open Chat Utilities**
- **Commands** (client-side):
  - `/chatutils`
  - `/chatutilities`

## Patterns (windows + search)

- **Plain text (default)**: matched as a literal substring on chat with formatting stripped. Characters like `.`, `*`, `+`, and brackets are not special.
- **`regex:` prefix**: everything after `regex:` (any casing) is compiled as a Java `Pattern`. Example: `regex:.*joined the lobby.*`

## Requirements

- Minecraft **1.21.11**
- [Fabric Loader](https://fabricmc.net/use/installer/) **0.18.4** or newer
- [Fabric API](https://modrinth.com/mod/fabric-api)
- **Java 21+**

## Download

Download the mod jar from **[GitHub Releases](https://github.com/Rainnny7/chat-windows/releases)** and place it in your `mods` folder together with [Fabric API](https://modrinth.com/mod/fabric-api).

## Installation

Add the mod jar (and Fabric API) to your client’s `mods` folder. The mod is **client-only** and does not need to be installed on a dedicated server.

## Config & migration

- **Config file**: `.minecraft/config/chat-utilities.json`
- **Legacy migration**: older `.minecraft/config/chat-windows.json` configs are migrated on load.