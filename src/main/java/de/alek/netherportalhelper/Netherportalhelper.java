package de.alek.netherportalhelper;

import de.alek.netherportalhelper.hud.HUDOverlay;
import de.alek.netherportalhelper.util.PortalTracker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;

public class Netherportalhelper implements ClientModInitializer {

    private static KeyBinding freezeKey;
    private static KeyBinding toggleKey;

    @Override
    public void onInitializeClient() {
        // 1. Keybind Registrierung
        freezeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.netherportalhelper.freeze",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                "category.netherportalhelper"
        ));
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.netherportalhelper.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F6,
                "category.netherportalhelper"
        ));

        // 2. HUD Registrierung
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            HUDOverlay.render(drawContext);
        });

        // 3. Tasten-Logik
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                HUDOverlay.isVisible = !HUDOverlay.isVisible;

                if (client.player != null) {
                    if (HUDOverlay.isVisible) {
                        client.player.sendMessage(Text.translatable("msg.netherportalhelper.enabled").formatted(Formatting.GREEN), true);
                    } else {
                        client.player.sendMessage(Text.translatable("msg.netherportalhelper.disabled").formatted(Formatting.RED), true);
                    }
                }
            }

            while (freezeKey.wasPressed() && HUDOverlay.isVisible) {
                if (client.player == null || client.world == null) return;

                if (PortalTracker.active) {
                    // Deaktivieren
                    PortalTracker.toggleFreeze(null);
                    // Nachricht bauen: "Portal Navigation: AUS"
                    Text message = Text.literal("Portal Navigation: ")
                            .append(Text.translatable("hud.netherportalhelper.off").formatted(Formatting.RED));
                    client.player.sendMessage(message, true);
                } else {
                    // Aktivieren
                    BlockPos currentPos = client.player.getBlockPos();
                    boolean inNether = client.world.getRegistryKey() == World.NETHER;
                    boolean inOverworld = client.world.getRegistryKey() == World.OVERWORLD;

                    if (inNether || inOverworld) {
                        int targetX = inNether ? currentPos.getX() * 8 : Math.floorDiv(currentPos.getX(), 8);
                        int targetZ = inNether ? currentPos.getZ() * 8 : Math.floorDiv(currentPos.getZ(), 8);
                        BlockPos target = new BlockPos(targetX, currentPos.getY(), targetZ);

                        PortalTracker.toggleFreeze(target);

                        // Nachricht bauen: "Portal Navigation: LOCKED"
                        Text message = Text.literal("Portal Navigation: ")
                                .append(Text.translatable("hud.netherportalhelper.locked").formatted(Formatting.GREEN));
                        client.player.sendMessage(message, true);
                    } else {
                        client.player.sendMessage(Text.translatable("hud.netherportalhelper.not_possible").formatted(Formatting.RED), true);
                    }
                }
            }
        });
    }

    public static String getFreezeKeyName() {
        return freezeKey.getBoundKeyLocalizedText().getString();
    }
}