package de.alek.netherportalhelper.util;

import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

public final class BookmarkStoreRegressionCheck {
    private BookmarkStoreRegressionCheck() {
    }

    public static void main(String[] args) throws IOException {
        Path directory = Files.createTempDirectory("portal-bookmarks-check");
        try {
            checkPersistenceAndWorldScopes(directory);
            checkMalformedFilesArePreserved(directory);
            checkFailedWritesKeepTheirTarget(directory);
        } finally {
            try (var paths = Files.walk(directory)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException exception) {
                        throw new IllegalStateException("Could not clean up the bookmark test directory", exception);
                    }
                });
            }
        }
    }

    private static void checkPersistenceAndWorldScopes(Path directory) {
        Path file = directory.resolve("config/bookmarks.json");
        String firstWorld = BookmarkStore.hash("singleplayer:" + directory.resolve("world-a"));
        String secondWorld = BookmarkStore.hash("singleplayer:" + directory.resolve("world-b"));
        String server = BookmarkStore.hash("multiplayer:play.example.org:25565");

        BookmarkStore store = new BookmarkStore(LoggerFactory.getLogger(BookmarkStoreRegressionCheck.class), file);
        assert store.add(firstWorld, "Nether Hub", "minecraft:the_nether", 12, 64, -24);
        assert store.add(firstWorld, "Home", "minecraft:overworld", -95, 72, 64);
        assert store.add(secondWorld, "Elsewhere", "minecraft:overworld", 50, 70, 80);
        assert store.add(server, "Spawn", "minecraft:overworld", 0, 64, 0);
        assert store.list(firstWorld).stream().map(PortalBookmark::name).toList().equals(List.of("Home", "Nether Hub"));
        assert store.list(secondWorld).size() == 1;
        assert store.list(server).size() == 1;

        assert !store.add(firstWorld, "home", "minecraft:overworld", 0, 64, 0);
        assert !store.add(firstWorld, " ", "minecraft:overworld", 0, 64, 0);
        assert !store.add(firstWorld, "The End", "minecraft:the_end", 0, 64, 0);
        assert !store.add(firstWorld, "a".repeat(49), "minecraft:overworld", 0, 64, 0);

        PortalBookmark original = store.list(firstWorld).getFirst();
        assert store.rename(firstWorld, original.id(), "Village");
        assert store.find(firstWorld, original.id()).x() == -95;
        assert store.find(firstWorld, original.id()).name().equals("Village");

        BookmarkStore reloaded = new BookmarkStore(LoggerFactory.getLogger(BookmarkStoreRegressionCheck.class), file);
        assert reloaded.find(firstWorld, original.id()).id().equals(original.id());
        assert reloaded.find(firstWorld, original.id()).y() == 72;
        assert reloaded.list(secondWorld).size() == 1;
        assert reloaded.list(server).size() == 1;
        assert reloaded.remove(firstWorld, original.id());
        assert reloaded.find(secondWorld, store.list(secondWorld).getFirst().id()) != null;
    }

    private static void checkMalformedFilesArePreserved(Path directory) throws IOException {
        Path file = directory.resolve("corrupt/bookmarks.json");
        Files.createDirectories(file.getParent());
        byte[] corrupt = "{invalid json".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Files.write(file, corrupt);

        BookmarkStore store = new BookmarkStore(LoggerFactory.getLogger(BookmarkStoreRegressionCheck.class), file);
        assert !store.canWrite("test-scope");
        assert !store.add("test-scope", "Home", "minecraft:overworld", 0, 64, 0);
        assert java.util.Arrays.equals(corrupt, Files.readAllBytes(file));

        byte[] unknownVersion = "{\"schemaVersion\":2,\"scopes\":{}}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Files.write(file, unknownVersion);
        BookmarkStore newerFile = new BookmarkStore(LoggerFactory.getLogger(BookmarkStoreRegressionCheck.class), file);
        assert !newerFile.canWrite("test-scope");
        assert java.util.Arrays.equals(unknownVersion, Files.readAllBytes(file));
    }

    private static void checkFailedWritesKeepTheirTarget(Path directory) throws IOException {
        Path blocker = directory.resolve("regular-file");
        Files.writeString(blocker, "keep this file");
        Path bookmarkFile = blocker.resolve("bookmarks.json");
        BookmarkStore store = new BookmarkStore(LoggerFactory.getLogger(BookmarkStoreRegressionCheck.class), bookmarkFile);
        assert !store.add("test-scope", "Home", "minecraft:overworld", 0, 64, 0);
        assert Files.readString(blocker).equals("keep this file");
        assert store.errorKey("test-scope").equals("screen.netherportalhelper.bookmark.storage_error");
    }
}
