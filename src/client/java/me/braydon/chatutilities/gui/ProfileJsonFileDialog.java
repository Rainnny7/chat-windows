package me.braydon.chatutilities.gui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.PointerBuffer;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

/** Native file picker for profile JSON export/import (AWT; no extra dependencies). */
public final class ProfileJsonFileDialog {

    private ProfileJsonFileDialog() {}

    public static Optional<Path> pickSaveJson(Path defaultDirectory, String defaultFileName) {
        try {
            String base =
                    defaultDirectory != null
                            ? defaultDirectory.toAbsolutePath().toString()
                            : Path.of(".").toAbsolutePath().normalize().toString();
            if (defaultDirectory != null) {
                try {
                    Files.createDirectories(defaultDirectory);
                } catch (Exception ignored) {
                }
            }
            String file =
                    defaultFileName != null && !defaultFileName.isBlank()
                            ? (defaultFileName.endsWith(".json") ? defaultFileName : defaultFileName + ".json")
                            : "profiles-export.json";
            String defaultPathAndFile = Path.of(base, file).toString();
            String chosen;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer filterPatterns = stack.pointers(stack.UTF8("*.json"));
                chosen =
                        TinyFileDialogs.tinyfd_saveFileDialog(
                                "Export Chat Utilities profiles",
                                defaultPathAndFile,
                                filterPatterns,
                                "JSON (*.json)");
            }
            if (chosen == null || chosen.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(Path.of(chosen));
        } catch (Throwable t) {
            return Optional.empty();
        }
    }

    public static Optional<Path> pickOpenJson(Path defaultDirectory) {
        try {
            String base =
                    defaultDirectory != null
                            ? defaultDirectory.toAbsolutePath().toString()
                            : Path.of(".").toAbsolutePath().normalize().toString();
            String defaultPathAndFile = base;
            String chosen;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer filterPatterns = stack.pointers(stack.UTF8("*.json"));
                chosen =
                        TinyFileDialogs.tinyfd_openFileDialog(
                                "Import Chat Utilities profiles",
                                defaultPathAndFile,
                                filterPatterns,
                                "JSON (*.json)",
                                false);
            }
            if (chosen == null || chosen.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(Path.of(chosen));
        } catch (Throwable t) {
            return Optional.empty();
        }
    }
}
