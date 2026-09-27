package de.alek.netherportalhelper.hud;

import de.alek.netherportalhelper.Netherportalhelper;
import de.alek.netherportalhelper.config.ConfigManager;
import de.alek.netherportalhelper.config.ModConfig;
import de.alek.netherportalhelper.util.PortalTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.Locale;

public class HUDOverlay {

    public static boolean isVisible = true;
    private static boolean hasSmoothedDiff = false;
    private static float smoothedDiffDegrees = 0.0f;

    public static void resetCompassState() {
        hasSmoothedDiff = false;
        smoothedDiffDegrees = 0.0f;
    }

    public static void render(GuiGraphicsExtractor context) {
        if (!isVisible) return;

        ModConfig config = ConfigManager.get();
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        BlockPos playerBlockPos = client.player.blockPosition();
        ResourceKey<Level> currentDimension = client.level.dimension();
        if (!PortalTracker.isPortalDimension(currentDimension)) return;

        renderInfoPanel(context, client, playerBlockPos, config, currentDimension);

        if (PortalTracker.isActive() && PortalTracker.getTargetPos() != null && PortalTracker.getTargetDimension() != null) {
            if (currentDimension == PortalTracker.getTargetDimension()) {
                renderNavigationCompass(context, client, config);
            }
        }
    }

    private static void renderInfoPanel(
            GuiGraphicsExtractor context,
            Minecraft client,
            BlockPos pos,
            ModConfig config,
            ResourceKey<Level> currentDimension
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

        MutableComponent title = Component.translatable("hud.netherportalhelper.title").withStyle(ChatFormatting.GOLD);
        String bookmarkName = PortalTracker.getTargetName();
        if (bookmarkName != null) {
            title.append(Component.literal(" · " + bookmarkName).withStyle(ChatFormatting.GOLD));
        }
        boolean isLocked = PortalTracker.isActive();
        ResourceKey<Level> targetDimension = isLocked && PortalTracker.getTargetDimension() != null
                ? PortalTracker.getTargetDimension()
                : PortalTracker.getCounterpartDimension(currentDimension);

        Component line1 = Component.translatable(
                "hud.netherportalhelper.pos",
                Component.literal(Integer.toString(pos.getX())).withStyle(ChatFormatting.AQUA),
                Component.literal(Integer.toString(pos.getZ())).withStyle(ChatFormatting.AQUA)
        ).withStyle(ChatFormatting.WHITE);

        Component line2 = Component.translatable(
                "hud.netherportalhelper.target",
                Component.literal(Integer.toString(targetX)).withStyle(isLocked ? ChatFormatting.GREEN : ChatFormatting.GRAY),
                Component.literal(Integer.toString(targetZ)).withStyle(isLocked ? ChatFormatting.GREEN : ChatFormatting.GRAY)
        ).withStyle(ChatFormatting.WHITE);

        Component actionLine = Component.literal("[" + Netherportalhelper.getFreezeKeyName() + "] ")
                .withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.translatable(isLocked ? "hud.netherportalhelper.unlock_hint" : "hud.netherportalhelper.lock_hint").withStyle(ChatFormatting.WHITE));

        Component hintLine = Component.literal("[" + Netherportalhelper.getHideKeyName() + "] ")
                .withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.translatable("hud.netherportalhelper.hide_hint").withStyle(ChatFormatting.GRAY));

        Component badgeText = Component.translatable(isLocked ? "hud.netherportalhelper.locked" : "hud.netherportalhelper.ready");
        int titleWidth = client.font.width(title);
        int line1Width = client.font.width(line1);
        int line2Width = client.font.width(line2);
        int actionLineWidth = client.font.width(actionLine);
        int hintLineWidth = Math.round(client.font.width(hintLine) * 0.85f);
        int badgePaddingX = 4;
        int badgeWidth = client.font.width(badgeText) + badgePaddingX * 2;

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
        context.text(client.font, title, panelX + padding, panelY + 2, 0xFFFFD95F, true);

        int badgeX = panelX + panelWidth - padding - badgeWidth;
        int badgeY = panelY + 1;
        int badgeColor = isLocked ? 0xAA1F3D22 : 0xAA303030;
        int badgeOutline = isLocked ? 0xCC4CAF50 : 0xCC8A8A8A;
        context.fill(badgeX, badgeY, badgeX + badgeWidth, badgeY + 10, badgeColor);
        context.fill(badgeX, badgeY, badgeX + badgeWidth, badgeY + 1, badgeOutline);
        context.fill(badgeX, badgeY + 9, badgeX + badgeWidth, badgeY + 10, 0x99000000);
        context.text(client.font, badgeText, badgeX + badgePaddingX, badgeY + 1, 0xFFFFFFFF, true);

        context.fill(panelX + padding, separatorY, panelX + panelWidth - padding, separatorY + 1, 0x55777777);

        context.text(client.font, line1, textX, line1Y, 0xFFFFFFFF, true);
        context.text(client.font, line2, textX, line2Y, 0xFFFFFFFF, true);
        context.text(client.font, actionLine, textX, line3Y, 0xFFFFFFFF, true);
        drawScaledText(context, client.font, hintLine, textX, line4Y, 0xFFB5B5B5, 0.85f);

        context.item(getDimensionIcon(currentDimension), iconX, icon1Y);
        context.item(getDimensionIcon(targetDimension), iconX, icon2Y);
    }

    private static void renderNavigationCompass(GuiGraphicsExtractor context, Minecraft client, ModConfig config) {
        BlockPos target = PortalTracker.getTargetPos();
        if (target == null) {
            return;
        }

        double px = client.player.getX();
        double pz = client.player.getZ();
        double tx = target.getX() + 0.5;
        double tz = target.getZ() + 0.5;

        double distance = Math.sqrt(client.player.distanceToSqr(tx, client.player.getY(), tz));

        boolean onTargetX = client.player.getBlockX() == target.getX();
        boolean onTargetZ = client.player.getBlockZ() == target.getZ();

        int centerX = client.getWindow().getGuiScaledWidth() / 2;
        int centerY = config.compassCenterY;

        if (onTargetX && onTargetZ) {
            drawCenteredScaleText(context, client.font, Component.literal("\u2714"), centerX, centerY - 4, 0xFF55FF55, config.compassArrowScale);
            String status = PortalTracker.getTargetName() == null
                    ? "hud.netherportalhelper.build_here"
                    : "hud.netherportalhelper.bookmark_arrived";
            drawCenteredScaleText(context, client.font, Component.translatable(status), centerX, centerY + 20, 0xFFFFFFFF, config.compassTextScale);
            return;
        }

        double angleToTargetRad = Math.atan2(tz - pz, tx - px);
        double angleToTargetDeg = Math.toDegrees(angleToTargetRad) - 90.0;
        float rawDiff = (float) Mth.wrapDegrees(angleToTargetDeg - client.player.getYRot());
        float smoothedDiff = smoothDiff(rawDiff, config.compassSmoothing);

        int arrowColor = blendArrowColor(Math.abs(smoothedDiff) / 180.0f);
        drawCenteredRotatedScaleText(
                context,
                client.font,
                Component.literal("\u2B06"),
                centerX,
                centerY,
                arrowColor,
                config.compassArrowScale,
                smoothedDiff
        );

        String distString = String.format(Locale.ROOT, "%.1fm", distance);
        drawCenteredScaleText(context, client.font, Component.literal(distString), centerX, centerY + 24, 0xFFFFFFFF, config.compassTextScale);

        if (config.showTurnAroundHint && Math.abs(smoothedDiff) >= config.compassBackzoneDegrees) {
            drawCenteredScaleText(
                    context,
                    client.font,
                    Component.translatable("hud.netherportalhelper.turn_around"),
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
                drawCenteredScaleText(context, client.font, Component.literal(yArrow + " " + Math.abs(dy)), centerX, centerY + 46, 0xFFAAAAAA, 0.8f);
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

        float delta = Mth.wrapDegrees(rawDiff - smoothedDiffDegrees);
        smoothedDiffDegrees = Mth.wrapDegrees(smoothedDiffDegrees + delta * alpha);
        return smoothedDiffDegrees;
    }

    private static void drawCenteredScaleText(GuiGraphicsExtractor context, Font textRenderer, Component text, int x, int y, int color, float scale) {
        int textWidth = textRenderer.width(text);
        context.pose().pushMatrix();
        context.pose().translate((float) x, (float) y);
        context.pose().scale(scale, scale);
        context.text(textRenderer, text, -textWidth / 2, 0, color, true);
        context.pose().popMatrix();
    }

    private static void drawCenteredRotatedScaleText(
            GuiGraphicsExtractor context,
            Font textRenderer,
            Component text,
            int x,
            int y,
            int color,
            float scale,
            float rotationDegrees
    ) {
        int textWidth = textRenderer.width(text);
        int textHeight = textRenderer.lineHeight;
        context.pose().pushMatrix();
        context.pose().translate((float) x, (float) y);
        context.pose().rotate((float) Math.toRadians(rotationDegrees));
        context.pose().scale(scale, scale);
        context.text(textRenderer, text, -textWidth / 2, -textHeight / 2, color, true);
        context.pose().popMatrix();
    }

    private static void drawScaledText(
            GuiGraphicsExtractor context,
            Font textRenderer,
            Component text,
            int x,
            int y,
            int color,
            float scale
    ) {
        context.pose().pushMatrix();
        context.pose().translate((float) x, (float) y);
        context.pose().scale(scale, scale);
        context.text(textRenderer, text, 0, 0, color, true);
        context.pose().popMatrix();
    }

    private static ItemStack getDimensionIcon(ResourceKey<Level> dimension) {
        if (dimension == Level.NETHER) {
            return new ItemStack(Items.NETHERRACK);
        }
        if (dimension == Level.OVERWORLD) {
            return new ItemStack(Items.GRASS_BLOCK);
        }
        return new ItemStack(Items.COMPASS);
    }
}

