package de.alek.netherportalhelper.hud;

import de.alek.netherportalhelper.Netherportalhelper;
import de.alek.netherportalhelper.util.PortalTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class HUDOverlay {

    public static boolean isVisible = true;

    public static void render(DrawContext context) {
        if (!isVisible) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        BlockPos playerBlockPos = client.player.getBlockPos();
        boolean inNether = client.world.getRegistryKey() == World.NETHER;
        boolean inOverworld = client.world.getRegistryKey() == World.OVERWORLD;

        if (!inNether && !inOverworld) return;

        renderInfoPanel(context, client, playerBlockPos, inNether);

        // Nur rendern wenn aktiv UND wir ein Ziel haben
        if (PortalTracker.active && PortalTracker.targetPos != null && inNether) {
            renderNavigationCompass(context, client);
        }
    }

    private static void renderInfoPanel(DrawContext context, MinecraftClient client, BlockPos pos, boolean inNether) {
        int targetX, targetZ;

        if (PortalTracker.active && PortalTracker.targetPos != null) {
            targetX = PortalTracker.targetPos.getX();
            targetZ = PortalTracker.targetPos.getZ();
        } else {
            targetX = inNether ? pos.getX() * 8 : pos.getX() / 8;
            targetZ = inNether ? pos.getZ() * 8 : pos.getZ() / 8;
        }

        String currentDim = inNether ? "Nether" : "Overworld";
        String targetDim = inNether ? "Overworld" : "Nether";

        // Formatierung: Wir übergeben die Farben als Argumente an die Translation
        String targetColor = PortalTracker.active ? "§a" : "§7";

        // Zeile 1: Pos
        Text line1 = Text.translatable("hud.netherportalhelper.pos",
                "§b" + pos.getX(),
                pos.getZ(),
                "§7" + currentDim
        ).formatted(Formatting.WHITE);

        // Zeile 2: Target
        Text line2 = Text.translatable("hud.netherportalhelper.target",
                targetColor,
                targetX,
                targetZ,
                "§7" + targetDim
        ).formatted(Formatting.WHITE);

        // Zeile 3: Status
        Text line3;
        if (PortalTracker.active) {
            line3 = Text.translatable("hud.netherportalhelper.locked").formatted(Formatting.GREEN);
        } else {
            // Wir bauen den Satz: "[Taste] to lock"
            line3 = Text.literal("§8[" + Netherportalhelper.getFreezeKeyName() + "] ")
                    .append(Text.translatable("hud.netherportalhelper.lock_hint"));
        }

        int maxWidth = Math.max(client.textRenderer.getWidth(line1), client.textRenderer.getWidth(line2)) + 10;

        context.fill(5, 5, 5 + maxWidth, 5 + 35, 0x90000000);

        context.drawText(client.textRenderer, line1, 10, 10, 0xFFFFFF, false);
        context.drawText(client.textRenderer, line2, 10, 20, 0xFFFFFF, false);
        context.drawText(client.textRenderer, line3, 10, 30, 0xFFFFFF, false);
    }

    private static void renderNavigationCompass(DrawContext context, MinecraftClient client) {
        BlockPos target = PortalTracker.targetPos;

        double px = client.player.getX();
        double pz = client.player.getZ();

        double tx = target.getX() + 0.5;
        double tz = target.getZ() + 0.5;

        double distSq = client.player.squaredDistanceTo(tx, client.player.getY(), tz);
        double distance = Math.sqrt(distSq);

        boolean onTargetX = client.player.getBlockX() == target.getX();
        boolean onTargetZ = client.player.getBlockZ() == target.getZ();

        int screenW = client.getWindow().getScaledWidth();
        int centerY = 60;
        int centerX = screenW / 2;

        if (onTargetX && onTargetZ) {
            drawCenteredScaleText(context, client.textRenderer, Text.literal("✔"), centerX, centerY, 0x55FF55, 3.0f);
            drawCenteredScaleText(context, client.textRenderer, Text.translatable("hud.netherportalhelper.build_here"), centerX, centerY + 25, 0xFFFFFF, 1.0f);
            return;
        }

        // Winkel Berechnung
        double angleToTargetRad = Math.atan2(tz - pz, tx - px);
        double angleToTargetDeg = Math.toDegrees(angleToTargetRad) - 90;
        float playerYaw = client.player.getYaw();
        double diff = MathHelper.wrapDegrees(angleToTargetDeg - playerYaw);

        String symbol;
        int color = 0xFFA500;

        // Richtungspfeile
        if (Math.abs(diff) < 20.0) {
            symbol = "⬆";
            color = 0x55FF55;
        } else if (Math.abs(diff) > 175.0) {
            symbol = "⬇";
            color = 0xFF5555;
        } else if (diff > 0) {
            symbol = "➡";
        } else {
            symbol = "⬅";
        }

        // Pfeil rendern
        drawCenteredScaleText(context, client.textRenderer, Text.literal(symbol), centerX, centerY, color, 3.0f);

        // Distanz rendern
        String distString = String.format("%.1fm", distance);
        drawCenteredScaleText(context, client.textRenderer, Text.literal(distString), centerX, centerY + 25, 0xFFFFFF, 1.0f);

        // Y-Level Warnung
        int dy = target.getY() - client.player.getBlockY();
        if (Math.abs(dy) > 3) {
            String yArrow = dy > 0 ? "⇧" : "⇩";
            drawCenteredScaleText(context, client.textRenderer, Text.literal(yArrow + " " + Math.abs(dy)), centerX, centerY + 35, 0xAAAAAA, 0.8f);
        }
    }

    private static void drawCenteredScaleText(DrawContext context, TextRenderer textRenderer, Text text, int x, int y, int color, float scale) {
        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(scale, scale, 1.0f);

        context.drawTextWithShadow(textRenderer, text, -textRenderer.getWidth(text) / 2, 0, color);
        context.getMatrices().pop();
    }
}