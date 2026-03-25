package de.alek.netherportalhelper.hud;

import de.alek.netherportalhelper.Netherportalhelper;
import de.alek.netherportalhelper.config.ConfigManager;
import de.alek.netherportalhelper.config.ModConfig;
import de.alek.netherportalhelper.util.PortalTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.Locale;

public class HUDOverlay {

    public static boolean isVisible = true;
    private static boolean hasSmoothedDiff = false;
    private static float smoothedDiffDegrees = 0.0f;

    public static void resetCompassState() {
        hasSmoothedDiff = false;
        smoothedDiffDegrees = 0.0f;
    }

    public static void render(DrawContext context) {
        if (!isVisible) return;

        ModConfig config = ConfigManager.get();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        BlockPos playerBlockPos = client.player.getBlockPos();
        RegistryKey<World> currentDimension = client.world.getRegistryKey();
        if (!PortalTracker.isPortalDimension(currentDimension)) return;

        renderInfoPanel(context, client, playerBlockPos, config, currentDimension);

        if (PortalTracker.isActive() && PortalTracker.getTargetPos() != null && PortalTracker.getTargetDimension() != null) {
            if (currentDimension == PortalTracker.getTargetDimension()) {
                renderNavigationCompass(context, client, config);
            }
        }
    }

    private static void renderInfoPanel(
            DrawContext context,
            MinecraftClient client,
            BlockPos pos,
            ModConfig config,
            RegistryKey<World> currentDimension
    ) {
        int targetX;
        int targetZ;
        BlockPos trackedTarget = PortalTracker.getTargetPos();
        if (PortalTracker.isActive() && trackedTarget != null) {
            targetX = trackedTarget.getX();
            targetZ = trackedTarget.getZ();
        } else {
            BlockPos counterpart = PortalTracker.calculateCounterpartPosition(pos, currentDimension);
            if (counterpart == null) {
                return;
            }
            targetX = counterpart.getX();
            targetZ = counterpart.getZ();
        }

        Text title = Text.translatable("hud.netherportalhelper.title").formatted(Formatting.GOLD);
        boolean isLocked = PortalTracker.isActive();

        Text line1 = Text.translatable(
                "hud.netherportalhelper.pos",
                Text.literal(Integer.toString(pos.getX())).formatted(Formatting.AQUA),
                Text.literal(Integer.toString(pos.getZ())).formatted(Formatting.AQUA)
        ).formatted(Formatting.WHITE);

        Text line2 = Text.translatable(
                "hud.netherportalhelper.target",
                Text.literal(Integer.toString(targetX)).formatted(isLocked ? Formatting.GREEN : Formatting.GRAY),
                Text.literal(Integer.toString(targetZ)).formatted(isLocked ? Formatting.GREEN : Formatting.GRAY)
        ).formatted(Formatting.WHITE);

        Text actionLine = Text.literal("[" + Netherportalhelper.getFreezeKeyName() + "] ")
                .formatted(Formatting.DARK_GRAY)
                .append(Text.translatable(isLocked ? "hud.netherportalhelper.unlock_hint" : "hud.netherportalhelper.lock_hint").formatted(Formatting.WHITE));

        Text hintLine = Text.literal("[" + Netherportalhelper.getHideKeyName() + "] ")
                .formatted(Formatting.DARK_GRAY)
                .append(Text.translatable("hud.netherportalhelper.hide_hint").formatted(Formatting.GRAY));

        Text badgeText = Text.translatable(isLocked ? "hud.netherportalhelper.locked" : "hud.netherportalhelper.ready");
        int titleWidth = client.textRenderer.getWidth(title);
        int line1Width = client.textRenderer.getWidth(line1);
        int line2Width = client.textRenderer.getWidth(line2);
        int actionLineWidth = client.textRenderer.getWidth(actionLine);
        int hintLineWidth = Math.round(client.textRenderer.getWidth(hintLine) * 0.85f);
        int badgePaddingX = 4;
        int badgeWidth = client.textRenderer.getWidth(badgeText) + badgePaddingX * 2;

        int panelX = config.hudPanelX;
        int panelY = config.hudPanelY;
        int padding = 6;
        int headerHeight = 12;
        int headerGap = 6;
        int iconTextGap = 4;
        int iconColumnWidth = 20;
        int textColumnWidth = Math.max(Math.max(line1Width, line2Width), Math.max(actionLineWidth, hintLineWidth));
        int bodyContentWidth = textColumnWidth + iconTextGap + iconColumnWidth;
        int headerContentWidth = titleWidth + headerGap + badgeWidth;
        int contentWidth = Math.max(bodyContentWidth, headerContentWidth);
        int panelWidth = contentWidth + padding * 2;

        int textX = panelX + padding;
        int iconX = panelX + panelWidth - padding - 16;
        int icon1Y = panelY + headerHeight + 4;
        int icon2Y = icon1Y + 18;
        int line1Y = icon1Y + 4;
        int line2Y = icon2Y + 4;
        int separatorY = icon2Y + 18 + 1;
        int line3Y = separatorY + 3;
        int line4Y = line3Y + 11;
        int panelHeight = (line4Y - panelY) + 9;

        int outerColor = 0xA7000000;
        int innerTop = isLocked ? 0xB21B1B16 : 0xB21B1B1B;
        int innerBottom = 0xB0060608;
        int accent = isLocked ? 0xCC4CAF50 : 0xCC8A8A8A;

        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, outerColor);
        context.fillGradient(panelX + 1, panelY + 1, panelX + panelWidth - 1, panelY + panelHeight - 1, innerTop, innerBottom);
        // Vanilla-like frame edge (top/left brighter, bottom/right darker)
        context.fill(panelX, panelY, panelX + panelWidth, panelY + 1, 0x55FFFFFF);
        context.fill(panelX, panelY, panelX + 1, panelY + panelHeight, 0x55FFFFFF);
        context.fill(panelX, panelY + panelHeight - 1, panelX + panelWidth, panelY + panelHeight, 0x88000000);
        context.fill(panelX + panelWidth - 1, panelY, panelX + panelWidth, panelY + panelHeight, 0x88000000);

        context.fill(panelX + 1, panelY + headerHeight, panelX + panelWidth - 1, panelY + headerHeight + 1, accent);
        context.drawTextWithShadow(client.textRenderer, title, panelX + padding, panelY + 2, 0xFFFFD95F);

        int badgeX = panelX + panelWidth - padding - badgeWidth;
        int badgeY = panelY + 1;
        int badgeColor = isLocked ? 0xAA1F3D22 : 0xAA303030;
        int badgeOutline = isLocked ? 0xCC4CAF50 : 0xCC8A8A8A;
        context.fill(badgeX, badgeY, badgeX + badgeWidth, badgeY + 10, badgeColor);
        context.fill(badgeX, badgeY, badgeX + badgeWidth, badgeY + 1, badgeOutline);
        context.fill(badgeX, badgeY + 9, badgeX + badgeWidth, badgeY + 10, 0x99000000);
        context.drawTextWithShadow(client.textRenderer, badgeText, badgeX + badgePaddingX, badgeY + 1, 0xFFFFFFFF);

        context.fill(panelX + padding, separatorY, panelX + panelWidth - padding, separatorY + 1, 0x55777777);

        context.drawTextWithShadow(client.textRenderer, line1, textX, line1Y, 0xFFFFFFFF);
        context.drawTextWithShadow(client.textRenderer, line2, textX, line2Y, 0xFFFFFFFF);
        context.drawTextWithShadow(client.textRenderer, actionLine, textX, line3Y, 0xFFFFFFFF);
        drawScaledText(context, client.textRenderer, hintLine, textX, line4Y, 0xFFB5B5B5, 0.85f);

        context.drawItem(getDimensionIcon(currentDimension), iconX, icon1Y);
        context.drawItem(getDimensionIcon(PortalTracker.getCounterpartDimension(currentDimension)), iconX, icon2Y);
    }

    private static void renderNavigationCompass(DrawContext context, MinecraftClient client, ModConfig config) {
        BlockPos target = PortalTracker.getTargetPos();
        if (target == null) {
            return;
        }

        double px = client.player.getX();
        double pz = client.player.getZ();
        double tx = target.getX() + 0.5;
        double tz = target.getZ() + 0.5;

        double distance = Math.sqrt(client.player.squaredDistanceTo(tx, client.player.getY(), tz));

        boolean onTargetX = client.player.getBlockX() == target.getX();
        boolean onTargetZ = client.player.getBlockZ() == target.getZ();

        int centerX = client.getWindow().getScaledWidth() / 2;
        int centerY = config.compassCenterY;

        if (onTargetX && onTargetZ) {
            drawCenteredScaleText(context, client.textRenderer, Text.literal("\u2714"), centerX, centerY - 4, 0xFF55FF55, config.compassArrowScale);
            drawCenteredScaleText(context, client.textRenderer, Text.translatable("hud.netherportalhelper.build_here"), centerX, centerY + 20, 0xFFFFFFFF, config.compassTextScale);
            return;
        }

        double angleToTargetRad = Math.atan2(tz - pz, tx - px);
        double angleToTargetDeg = Math.toDegrees(angleToTargetRad) - 90.0;
        float rawDiff = (float) MathHelper.wrapDegrees(angleToTargetDeg - client.player.getYaw());
        float smoothedDiff = smoothDiff(rawDiff, config.compassSmoothing);

        int arrowColor = blendArrowColor(Math.abs(smoothedDiff) / 180.0f);
        drawCenteredRotatedScaleText(
                context,
                client.textRenderer,
                Text.literal("\u2B06"),
                centerX,
                centerY,
                arrowColor,
                config.compassArrowScale,
                smoothedDiff
        );

        String distString = String.format(Locale.ROOT, "%.1fm", distance);
        drawCenteredScaleText(context, client.textRenderer, Text.literal(distString), centerX, centerY + 24, 0xFFFFFFFF, config.compassTextScale);

        if (config.showTurnAroundHint && Math.abs(smoothedDiff) >= config.compassBackzoneDegrees) {
            drawCenteredScaleText(
                    context,
                    client.textRenderer,
                    Text.translatable("hud.netherportalhelper.turn_around"),
                    centerX,
                    centerY + 35,
                    0xFFFF6666,
                    0.85f
            );
        }

        if (config.showYLevelIndicator) {
            int dy = target.getY() - client.player.getBlockY();
            if (Math.abs(dy) > 3) {
                String yArrow = dy > 0 ? "\u21E7" : "\u21E9";
                drawCenteredScaleText(context, client.textRenderer, Text.literal(yArrow + " " + Math.abs(dy)), centerX, centerY + 46, 0xFFAAAAAA, 0.8f);
            }
        }
    }

    private static int blendArrowColor(float normalized) {
        float t = Math.max(0.0f, Math.min(1.0f, normalized));
        int startR = 0x55;
        int startG = 0xFF;
        int startB = 0x55;
        int endR = 0xFF;
        int endG = 0x55;
        int endB = 0x55;

        int r = (int) (startR + (endR - startR) * t);
        int g = (int) (startG + (endG - startG) * t);
        int b = (int) (startB + (endB - startB) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static float smoothDiff(float rawDiff, float smoothingFactor) {
        float alpha = Math.max(0.0f, Math.min(1.0f, smoothingFactor));
        if (!hasSmoothedDiff) {
            hasSmoothedDiff = true;
            smoothedDiffDegrees = rawDiff;
            return smoothedDiffDegrees;
        }

        float delta = MathHelper.wrapDegrees(rawDiff - smoothedDiffDegrees);
        smoothedDiffDegrees = MathHelper.wrapDegrees(smoothedDiffDegrees + delta * alpha);
        return smoothedDiffDegrees;
    }

    private static void drawCenteredScaleText(DrawContext context, TextRenderer textRenderer, Text text, int x, int y, int color, float scale) {
        int textWidth = textRenderer.getWidth(text);
        context.getMatrices().pushMatrix();
        context.getMatrices().scaleLocal(scale, scale);
        context.getMatrices().translateLocal((float) x, (float) y);
        context.drawTextWithShadow(textRenderer, text, -textWidth / 2, 0, color);
        context.getMatrices().popMatrix();
    }

    private static void drawCenteredRotatedScaleText(
            DrawContext context,
            TextRenderer textRenderer,
            Text text,
            int x,
            int y,
            int color,
            float scale,
            float rotationDegrees
    ) {
        int textWidth = textRenderer.getWidth(text);
        int textHeight = textRenderer.fontHeight;
        context.getMatrices().pushMatrix();
        context.getMatrices().scaleLocal(scale, scale);
        context.getMatrices().rotateLocal((float) Math.toRadians(rotationDegrees));
        context.getMatrices().translateLocal((float) x, (float) y);
        context.drawTextWithShadow(textRenderer, text, -textWidth / 2, -textHeight / 2, color);
        context.getMatrices().popMatrix();
    }

    private static void drawScaledText(
            DrawContext context,
            TextRenderer textRenderer,
            Text text,
            int x,
            int y,
            int color,
            float scale
    ) {
        context.getMatrices().pushMatrix();
        context.getMatrices().scaleLocal(scale, scale);
        context.getMatrices().translateLocal((float) x, (float) y);
        context.drawTextWithShadow(textRenderer, text, 0, 0, color);
        context.getMatrices().popMatrix();
    }

    private static ItemStack getDimensionIcon(RegistryKey<World> dimension) {
        if (dimension == World.NETHER) {
            return new ItemStack(Items.NETHERRACK);
        }
        if (dimension == World.OVERWORLD) {
            return new ItemStack(Items.GRASS_BLOCK);
        }
        return new ItemStack(Items.COMPASS);
    }
}

