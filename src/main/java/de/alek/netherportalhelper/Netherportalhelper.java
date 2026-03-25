package de.alek.netherportalhelper;

import de.alek.netherportalhelper.config.ConfigManager;
import de.alek.netherportalhelper.hud.HUDOverlay;
import de.alek.netherportalhelper.util.PortalTracker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.KeyBinding.Category;
import net.minecraft.client.util.InputUtil;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Netherportalhelper implements ClientModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("netherportalhelper");

    private static KeyBinding freezeKey;
    private static KeyBinding toggleKey;

    @Override
    public void onInitializeClient() {
        try {
            ConfigManager.load(LOGGER);
            HUDOverlay.isVisible = ConfigManager.get().hudVisible;

            freezeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.netherportalhelper.freeze",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_P,
                    Category.MISC
            ));
            toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.netherportalhelper.toggle",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_F6,
                    Category.MISC
            ));

            HudElementRegistry.attachElementAfter(
                    VanillaHudElements.BOSS_BAR,
                    Identifier.of("netherportalhelper", "main_hud"),
                    (context, tickCounter) -> HUDOverlay.render(context)
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
                if (PortalTracker.isActive() && (PortalTracker.getTargetPos() == null || PortalTracker.getTargetDimension() == null)) {
                    PortalTracker.clear();
                    HUDOverlay.resetCompassState();
                }

                while (toggleKey.wasPressed()) {
                    HUDOverlay.isVisible = !HUDOverlay.isVisible;
                    ConfigManager.get().hudVisible = HUDOverlay.isVisible;
                    ConfigManager.save(LOGGER);

                    if (client.player != null) {
                        client.player.sendMessage(
                                Text.translatable(
                                        HUDOverlay.isVisible ? "msg.netherportalhelper.enabled" : "msg.netherportalhelper.disabled"
                                ).formatted(HUDOverlay.isVisible ? Formatting.GREEN : Formatting.RED),
                                true
                        );
                    }
                }

                while (freezeKey.wasPressed()) {
                    if (client.player == null || client.world == null) {
                        return;
                    }

                    if (PortalTracker.isActive()) {
                        PortalTracker.clear();
                        HUDOverlay.resetCompassState();
                        Text message = Text.literal("Portal Navigation: ")
                                .append(Text.translatable("hud.netherportalhelper.off").formatted(Formatting.RED));
                        client.player.sendMessage(message, true);
                        continue;
                    }

                    BlockPos currentPos = client.player.getBlockPos();
                    RegistryKey<World> currentDim = client.world.getRegistryKey();

                    if (!PortalTracker.isPortalDimension(currentDim)) {
                        client.player.sendMessage(Text.translatable("hud.netherportalhelper.not_possible").formatted(Formatting.RED), true);
                        continue;
                    }

                    BlockPos target = PortalTracker.calculateCounterpartPosition(currentPos, currentDim);
                    RegistryKey<World> targetDim = PortalTracker.getCounterpartDimension(currentDim);

                    if (PortalTracker.lockTarget(target, targetDim)) {
                        HUDOverlay.resetCompassState();
                    }

                    Text message = Text.literal("Portal Navigation: ")
                            .append(Text.translatable("hud.netherportalhelper.locked").formatted(Formatting.GREEN));
                    client.player.sendMessage(message, true);
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
        return formatKeyName(freezeKey.getBoundKeyLocalizedText().getString());
    }

    public static String getHideKeyName() {
        if (toggleKey == null) return "?";
        return formatKeyName(toggleKey.getBoundKeyLocalizedText().getString());
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
