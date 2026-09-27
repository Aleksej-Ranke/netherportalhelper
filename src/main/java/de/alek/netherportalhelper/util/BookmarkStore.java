package de.alek.netherportalhelper.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class BookmarkStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Logger logger;
    private final Path file;
    private BookmarkFile data = new BookmarkFile();
    private String errorKey;
    private boolean readOnly;

    public BookmarkStore(Logger logger) {
        this(logger, FabricLoader.getInstance().getConfigDir().resolve("netherportalhelper-bookmarks.json"));
    }

    BookmarkStore(Logger logger, Path file) {
        this.logger = logger;
        this.file = file;
        load();
    }

    public static String worldScope(Minecraft client) {
        try {
            if (client.getSingleplayerServer() != null) {
                String worldPath = client.getSingleplayerServer().getWorldPath(LevelResource.ROOT)
                        .toRealPath().toString();
                return hash("singleplayer:" + worldPath);
            }

            ServerData server = client.getCurrentServer();
            if (server == null || server.ip == null || !ServerAddress.isValidAddress(server.ip)) {
                return null;
            }

            ServerAddress address = ServerAddress.parseString(server.ip);
            String normalizedAddress = address.getHost().toLowerCase(Locale.ROOT) + ":" + address.getPort();
            return hash("multiplayer:" + normalizedAddress);
        } catch (IOException exception) {
            return null;
        }
    }

    public static boolean isValidName(String name) {
        return name != null
                && name.equals(name.trim())
                && !name.isEmpty()
                && name.length() <= 48
                && name.codePoints().noneMatch(Character::isISOControl);
    }

    public List<PortalBookmark> list(String scope) {
        if (scope == null) return List.of();
        return data.scopes.getOrDefault(scope, List.of()).stream()
                .sorted(Comparator.comparing(PortalBookmark::name, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(PortalBookmark::id))
                .toList();
    }

    public PortalBookmark find(String scope, String id) {
        if (scope == null || id == null) return null;
        return data.scopes.getOrDefault(scope, List.of()).stream()
                .filter(bookmark -> bookmark.id().equals(id))
                .findFirst().orElse(null);
    }

    public String errorKey(String scope) {
        if (scope == null) return "screen.netherportalhelper.bookmark.no_world";
        if (errorKey != null) return errorKey;
        return readOnly ? "screen.netherportalhelper.bookmark.read_only" : null;
    }

    public boolean canWrite(String scope) {
        return scope != null && !readOnly;
    }

    public boolean add(String scope, String name, String dimension, int x, int y, int z) {
        if (!canWrite(scope)) return false;
        if (!isValidName(name)) return fail("screen.netherportalhelper.bookmark.invalid_name");
        if (!isValidDimension(dimension)) return fail("screen.netherportalhelper.bookmark.invalid_dimension");
        if (hasName(scope, name, null)) return fail("screen.netherportalhelper.bookmark.duplicate_name");

        PortalBookmark bookmark = new PortalBookmark(UUID.randomUUID().toString(), name, dimension, x, y, z);
        List<PortalBookmark> entries = new ArrayList<>(list(scope));
        entries.add(bookmark);
        return saveScope(scope, entries);
    }

    public boolean rename(String scope, String id, String name) {
        if (!canWrite(scope)) return false;
        if (!isValidName(name)) return fail("screen.netherportalhelper.bookmark.invalid_name");
        if (hasName(scope, name, id)) return fail("screen.netherportalhelper.bookmark.duplicate_name");

        List<PortalBookmark> entries = new ArrayList<>(list(scope));
        for (int index = 0; index < entries.size(); index++) {
            PortalBookmark bookmark = entries.get(index);
            if (bookmark.id().equals(id)) {
                entries.set(index, new PortalBookmark(id, name, bookmark.dimension(), bookmark.x(), bookmark.y(), bookmark.z()));
                return saveScope(scope, entries);
            }
        }
        return fail("screen.netherportalhelper.bookmark.missing");
    }

    public boolean remove(String scope, String id) {
        if (!canWrite(scope)) return false;
        List<PortalBookmark> entries = new ArrayList<>(list(scope));
        if (!entries.removeIf(bookmark -> bookmark.id().equals(id))) {
            return fail("screen.netherportalhelper.bookmark.missing");
        }
        return saveScope(scope, entries);
    }

    static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static boolean isValidDimension(String dimension) {
        return "minecraft:overworld".equals(dimension) || "minecraft:the_nether".equals(dimension);
    }

    private void load() {
        if (!Files.exists(file)) return;

        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            BookmarkFile parsed = GSON.fromJson(reader, BookmarkFile.class);
            if (parsed == null || parsed.schemaVersion != 1 || parsed.scopes == null) {
                throw new JsonParseException("Unsupported or incomplete bookmark file");
            }

            for (List<PortalBookmark> entries : parsed.scopes.values()) {
                validateEntries(entries);
            }
            data = parsed;
        } catch (IOException | JsonParseException | IllegalStateException exception) {
            logger.error("Portal bookmarks could not be loaded; leaving the existing file untouched", exception);
            errorKey = "screen.netherportalhelper.bookmark.storage_error";
            readOnly = true;
        }
    }

    private static void validateEntries(List<PortalBookmark> entries) {
        if (entries == null) throw new JsonParseException("Missing bookmark list");

        Set<String> ids = new HashSet<>();
        Set<String> names = new HashSet<>();
        for (PortalBookmark bookmark : entries) {
            if (bookmark == null || bookmark.id() == null || bookmark.name() == null
                    || !isValidName(bookmark.name()) || !isValidDimension(bookmark.dimension())) {
                throw new JsonParseException("Invalid portal bookmark");
            }
            try {
                UUID.fromString(bookmark.id());
            } catch (IllegalArgumentException exception) {
                throw new JsonParseException("Invalid bookmark id", exception);
            }
            if (!ids.add(bookmark.id()) || !names.add(bookmark.name().toLowerCase(Locale.ROOT))) {
                throw new JsonParseException("Duplicate portal bookmark");
            }
        }
    }

    private boolean hasName(String scope, String name, String excludedId) {
        String normalizedName = name.toLowerCase(Locale.ROOT);
        return list(scope).stream().anyMatch(bookmark -> !bookmark.id().equals(excludedId)
                && bookmark.name().toLowerCase(Locale.ROOT).equals(normalizedName));
    }

    private boolean saveScope(String scope, List<PortalBookmark> entries) {
        Map<String, List<PortalBookmark>> scopes = new HashMap<>(data.scopes);
        if (entries.isEmpty()) scopes.remove(scope);
        else scopes.put(scope, entries);

        BookmarkFile next = new BookmarkFile();
        next.schemaVersion = 1;
        next.scopes = scopes;

        Path temporaryFile = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(temporaryFile, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                GSON.toJson(next, writer);
            }
            try {
                Files.move(temporaryFile, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, file, StandardCopyOption.REPLACE_EXISTING);
            }
            data = next;
            errorKey = null;
            return true;
        } catch (IOException exception) {
            logger.error("Portal bookmarks could not be saved", exception);
            errorKey = "screen.netherportalhelper.bookmark.storage_error";
            try {
                Files.deleteIfExists(temporaryFile);
            } catch (IOException cleanupException) {
                logger.warn("Could not clean up the temporary bookmark file", cleanupException);
            }
            return false;
        }
    }

    private boolean fail(String key) {
        errorKey = key;
        return false;
    }

    private static final class BookmarkFile {
        private Integer schemaVersion;
        private Map<String, List<PortalBookmark>> scopes = new HashMap<>();
    }
}
