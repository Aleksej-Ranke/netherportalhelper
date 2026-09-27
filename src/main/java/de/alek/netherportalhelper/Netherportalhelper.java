package de.alek.netherportalhelper;

import de.alek.netherportalhelper.config.ConfigManager;
import de.alek.netherportalhelper.hud.HUDOverlay;
import de.alek.netherportalhelper.screen.PortalBookmarksScreen;
import de.alek.netherportalhelper.util.BookmarkStore;
import de.alek.netherportalhelper.util.PortalTracker;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Netherportalhelper implements ClientModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("netherportalhelper");
    private static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("netherportalhelper", "controls")
    );

    private static KeyMapping freezeKey;
    private static KeyMapping toggleKey;
    private static KeyMapping bookmarksKey;
    private static BookmarkStore bookmarkStore;

    @Override
    public void onInitializeClient() {
        try {
            ConfigManager.load(LOGGER);
            bookmarkStore = new BookmarkStore(LOGGER);
            HUDOverlay.isVisible = ConfigManager.get().hudVisible;

            freezeKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                    "key.netherportalhelper.freeze",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_P,
                    KEY_CATEGORY
            ));
            toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                    "key.netherportalhelper.toggle",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_F6,
                    KEY_CATEGORY
            ));
            bookmarksKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                    "key.netherportalhelper.bookmarks",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_B,
                    KEY_CATEGORY
            ));

            HudElementRegistry.attachElementAfter(
                    VanillaHudElements.BOSS_BAR,
                    Identifier.fromNamespaceAndPath("netherportalhelper", "main_hud"),
                    (graphics, tickCounter) -> HUDOverlay.render(graphics)
            );

            ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
                PortalTracker.clear();
                HUDOverlay.resetCompassState();
            });
            ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
                PortalTracker.clear();
                HUDOverlay.resetCompassState();
            });

            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                while (bookmarksKey.consumeClick()) {
                    if (client.player != null && client.level != null && client.gui.screen() == null) {
                        client.gui.setScreen(new PortalBookmarksScreen(bookmarkStore, BookmarkStore.worldScope(client)));
                    }
                }

                if (PortalTracker.isActive() && (PortalTracker.getTargetPos() == null || PortalTracker.getTargetDimension() == null)) {
                    PortalTracker.clear();
                    HUDOverlay.resetCompassState();
                }

                while (toggleKey.consumeClick()) {
                    HUDOverlay.isVisible = !HUDOverlay.isVisible;
                    ConfigManager.get().hudVisible = HUDOverlay.isVisible;
                    ConfigManager.save(LOGGER);

                    if (client.player != null) {
                        client.player.sendOverlayMessage(
                                Component.translatable(
                                        HUDOverlay.isVisible ? "msg.netherportalhelper.enabled" : "msg.netherportalhelper.disabled"
                                ).withStyle(HUDOverlay.isVisible ? ChatFormatting.GREEN : ChatFormatting.RED)
                        );
                    }
                }

                while (freezeKey.consumeClick()) {
                    if (client.player == null || client.level == null) {
                        return;
                    }

                    if (PortalTracker.isActive()) {
                        PortalTracker.clear();
                        HUDOverlay.resetCompassState();
                        Component message = Component.literal("Portal Navigation: ")
                                .append(Component.translatable("hud.netherportalhelper.off").withStyle(ChatFormatting.RED));
                        client.player.sendOverlayMessage(message);
                        continue;
                    }

                    BlockPos currentPos = client.player.blockPosition();
                    ResourceKey<Level> currentDim = client.level.dimension();

                    if (!PortalTracker.isPortalDimension(currentDim)) {
                        client.player.sendOverlayMessage(Component.translatable("hud.netherportalhelper.not_possible").withStyle(ChatFormatting.RED));
                        continue;
                    }

                    BlockPos target = PortalTracker.calculateCounterpartPosition(currentPos, currentDim);
                    ResourceKey<Level> targetDim = PortalTracker.getCounterpartDimension(currentDim);

                    if (PortalTracker.lockTarget(target, targetDim)) {
                        HUDOverlay.resetCompassState();
                    }

                    Component message = Component.literal("Portal Navigation: ")
                            .append(Component.translatable("hud.netherportalhelper.locked").withStyle(ChatFormatting.GREEN));
                    client.player.sendOverlayMessage(message);
                }
            });
        } catch (Throwable t) {
            System.err.println("netherportalhelper: Fehler bei der Initialisierung:");
            t.printStackTrace();
            LOGGER.error("Initialisierung fehlgeschlagen", t);
        }
    }

    public static Logger getLogger() {
        return LOGGER;
    }

    public static String getFreezeKeyName() {
        if (freezeKey == null) return "?";
        return formatKeyName(freezeKey.getTranslatedKeyMessage().getString());
    }

    public static String getHideKeyName() {
        if (toggleKey == null) return "?";
        return formatKeyName(toggleKey.getTranslatedKeyMessage().getString());
    }

    private static String formatKeyName(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "?";
        }

        String normalized = raw
                .replace("Button ", "M")
                .replace("Mouse ", "M")
                .replace("Maustaste ", "M");

        if (normalized.length() > 12) {
            return normalized.substring(0, 12);
        }
        return normalized;
    }
}
