package de.alek.netherportalhelper.screen;

import de.alek.netherportalhelper.util.BookmarkStore;
import de.alek.netherportalhelper.util.PortalBookmark;
import de.alek.netherportalhelper.util.PortalTracker;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import java.util.List;

public final class PortalBookmarksScreen extends Screen {
    private final BookmarkStore store;
    private final String scope;
    private String selectedId;
    private Component statusMessage;
    private int statusColor = 0xFFFF7777;

    private EditBox nameInput;
    private BookmarkList bookmarkList;
    private Button savePositionButton;
    private Button saveTargetButton;
    private Button navigateButton;
    private Button renameButton;
    private Button deleteButton;

    public PortalBookmarksScreen(BookmarkStore store, String scope) {
        super(Component.translatable("screen.netherportalhelper.bookmark.title"));
        this.store = store;
        this.scope = scope;
        String error = store.errorKey(scope);
        this.statusMessage = error == null ? Component.empty() : Component.translatable(error);
    }

    @Override
    protected void init() {
        super.init();
        int contentWidth = Math.min(520, this.width - 24);
        int left = (this.width - contentWidth) / 2;
        int listWidth = Math.max(96, (contentWidth - 16) * 2 / 5);
        int detailsX = left + listWidth + 16;
        int detailsWidth = contentWidth - listWidth - 16;
        int listTop = 50;
        int listBottom = Math.max(listTop + 36, this.height - 43);

        bookmarkList = new BookmarkList(this.minecraft, listWidth, listTop, listBottom, left);
        this.addRenderableWidget(bookmarkList);
        refreshEntries();

        nameInput = new EditBox(this.font, detailsX, listTop, detailsWidth, 20,
                Component.translatable("screen.netherportalhelper.bookmark.name"));
        nameInput.setHint(Component.translatable("screen.netherportalhelper.bookmark.name_hint"));
        nameInput.setMaxLength(48);
        this.addRenderableWidget(nameInput);

        int buttonY = listTop + 29;
        savePositionButton = addButton(detailsX, buttonY, detailsWidth,
                "screen.netherportalhelper.bookmark.save_position", this::saveCurrentPosition);
        saveTargetButton = addButton(detailsX, buttonY + 24, detailsWidth,
                "screen.netherportalhelper.bookmark.save_target", this::saveLockedTarget);
        navigateButton = addButton(detailsX, buttonY + 54, detailsWidth,
                "screen.netherportalhelper.bookmark.navigate", this::navigate);
        renameButton = addButton(detailsX, buttonY + 78, detailsWidth,
                "screen.netherportalhelper.bookmark.rename", this::renameSelected);
        deleteButton = addButton(detailsX, buttonY + 102, detailsWidth,
                "screen.netherportalhelper.bookmark.delete", this::confirmDelete);

        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> this.onClose())
                .bounds(this.width / 2 - 50, this.height - 27, 100, 20).build());
        updateActions();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.text(this.font, this.title, (this.width - this.font.width(this.title)) / 2, 12, 0xFFFFFFFF, true);
        graphics.text(this.font, Component.translatable("screen.netherportalhelper.bookmark.list"),
                bookmarkList.getX() + 3, 35, 0xFFAAAAAA, true);
        if (bookmarkList.children().isEmpty()) {
            graphics.text(this.font, Component.translatable("screen.netherportalhelper.bookmark.empty"),
                    bookmarkList.getX() + 6, 58, 0xFFAAAAAA, true);
        }
        graphics.text(this.font, Component.translatable("screen.netherportalhelper.bookmark.name"),
                nameInput.getX(), 37, 0xFFAAAAAA, true);
        if (!statusMessage.getString().isEmpty()) {
            String status = this.font.plainSubstrByWidth(statusMessage.getString(), this.width - 24);
            graphics.text(this.font, status, (this.width - this.font.width(status)) / 2,
                    this.height - 39, statusColor, true);
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        String previousId = selectedId;
        boolean handled = super.mouseClicked(event, doubleClick);
        syncSelectedName(previousId);
        return handled;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        String previousId = selectedId;
        boolean handled = super.keyPressed(event);
        syncSelectedName(previousId);
        return handled;
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private Button addButton(int x, int y, int width, String translationKey, Runnable action) {
        Button button = Button.builder(Component.translatable(translationKey), ignored -> action.run())
                .bounds(x, y, width, 20).build();
        this.addRenderableWidget(button);
        return button;
    }

    private void saveCurrentPosition() {
        if (this.minecraft.player == null || this.minecraft.level == null) return;
        BlockPos position = this.minecraft.player.blockPosition();
        boolean saved = store.add(scope, nameInput.getValue().trim(), this.minecraft.level.dimension().identifier().toString(),
                position.getX(), position.getY(), position.getZ());
        if (saved) {
            selectBookmarkByName(nameInput.getValue().trim());
            statusMessage = Component.translatable("screen.netherportalhelper.bookmark.saved");
            statusColor = 0xFF55FF55;
            refreshEntries();
        } else {
            refreshStatus();
        }
        updateActions();
    }

    private void saveLockedTarget() {
        if (!PortalTracker.isActive()) return;
        BlockPos target = PortalTracker.getTargetPos();
        var dimension = PortalTracker.getTargetDimension();
        if (target == null || dimension == null) return;

        if (store.add(scope, nameInput.getValue().trim(), dimension.identifier().toString(),
                target.getX(), target.getY(), target.getZ())) {
            selectBookmarkByName(nameInput.getValue().trim());
            statusMessage = Component.translatable("screen.netherportalhelper.bookmark.saved");
            statusColor = 0xFF55FF55;
            refreshEntries();
        } else {
            refreshStatus();
        }
        updateActions();
    }

    private void navigate() {
        PortalBookmark bookmark = selectedBookmark();
        if (bookmark == null || this.minecraft.player == null) return;

        if (PortalTracker.lockTarget(bookmark.position(), bookmark.dimensionKey(), bookmark.name())) {
            statusMessage = Component.translatable("screen.netherportalhelper.bookmark.navigating", bookmark.name());
            statusColor = 0xFF55FF55;
            this.minecraft.player.sendOverlayMessage(statusMessage.copy().withStyle(ChatFormatting.GREEN));
            this.minecraft.gui.setScreen(null);
        }
    }

    private void renameSelected() {
        PortalBookmark selected = selectedBookmark();
        if (selected == null) return;

        if (store.rename(scope, selected.id(), nameInput.getValue().trim())) {
            statusMessage = Component.translatable("screen.netherportalhelper.bookmark.renamed");
            statusColor = 0xFF55FF55;
            refreshEntries();
        } else {
            refreshStatus();
        }
        updateActions();
    }

    private void confirmDelete() {
        PortalBookmark selected = selectedBookmark();
        if (selected == null) return;

        BooleanConsumer confirmed = answer -> {
            if (answer && store.remove(scope, selected.id())) {
                selectedId = null;
                statusMessage = Component.translatable("screen.netherportalhelper.bookmark.deleted");
                statusColor = 0xFF55FF55;
                refreshEntries();
            } else if (answer) {
                refreshStatus();
            }
            this.minecraft.gui.setScreen(this);
        };
        this.minecraft.gui.setScreen(new ConfirmScreen(confirmed,
                Component.translatable("screen.netherportalhelper.bookmark.confirm_delete"),
                Component.translatable("screen.netherportalhelper.bookmark.confirm_delete_message", selected.name())));
    }

    private void refreshEntries() {
        if (bookmarkList == null) return;
        String name = nameInput == null ? "" : nameInput.getValue();
        List<BookmarkEntry> entries = store.list(scope).stream().map(BookmarkEntry::new).toList();
        bookmarkList.replaceEntries(entries);
        BookmarkEntry selected = entries.stream().filter(entry -> entry.bookmark.id().equals(selectedId))
                .findFirst().orElse(null);
        bookmarkList.setSelected(selected);
        if (nameInput != null) {
            PortalBookmark bookmark = selectedBookmark();
            nameInput.setValue(bookmark == null ? name : bookmark.name());
        }
    }

    private void selectBookmarkByName(String name) {
        selectedId = store.list(scope).stream().filter(bookmark -> bookmark.name().equals(name))
                .map(PortalBookmark::id).findFirst().orElse(null);
    }

    private void syncSelectedName(String previousId) {
        if (bookmarkList == null || nameInput == null) return;
        BookmarkEntry selected = bookmarkList.getSelected();
        if (selected == null) return;

        selectedId = selected.bookmark.id();
        if (!selectedId.equals(previousId)) nameInput.setValue(selected.bookmark.name());
        updateActions();
    }

    private PortalBookmark selectedBookmark() {
        return store.find(scope, selectedId);
    }

    private void refreshStatus() {
        String error = store.errorKey(scope);
        statusMessage = error == null ? Component.empty() : Component.translatable(error);
        statusColor = 0xFFFF7777;
    }

    private void updateActions() {
        if (savePositionButton == null) return;
        boolean writable = store.canWrite(scope);
        PortalBookmark selected = selectedBookmark();
        savePositionButton.active = writable && this.minecraft.player != null && this.minecraft.level != null
                && PortalTracker.isPortalDimension(this.minecraft.level.dimension());
        saveTargetButton.active = writable && PortalTracker.isActive()
                && PortalTracker.isPortalDimension(PortalTracker.getTargetDimension());
        navigateButton.active = selected != null && this.minecraft.player != null && this.minecraft.level != null
                && PortalTracker.isPortalDimension(this.minecraft.level.dimension())
                && PortalTracker.isPortalDimension(selected.dimensionKey());
        renameButton.active = writable && selected != null;
        deleteButton.active = writable && selected != null;
    }

    private static String dimensionName(PortalBookmark bookmark) {
        return bookmark.dimensionKey() == Level.NETHER
                ? "hud.netherportalhelper.dim_nether"
                : "hud.netherportalhelper.dim_overworld";
    }

    private final class BookmarkList extends ObjectSelectionList<BookmarkEntry> {
        private BookmarkList(Minecraft minecraft, int width, int top, int bottom, int left) {
            super(minecraft, width, bottom - top, top, 36);
            this.setX(left);
        }
    }

    private final class BookmarkEntry extends ObjectSelectionList.Entry<BookmarkEntry> {
        private final PortalBookmark bookmark;

        private BookmarkEntry(PortalBookmark bookmark) {
            this.bookmark = bookmark;
        }

        @Override
        public Component getNarration() {
            return Component.translatable("screen.netherportalhelper.bookmark.entry_narration", bookmark.name(),
                    Component.translatable(dimensionName(bookmark)), bookmark.x(), bookmark.y(), bookmark.z());
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float delta) {
            int maxWidth = Math.max(32, this.getContentWidth());
            String name = font.plainSubstrByWidth(bookmark.name(), maxWidth);
            Component location = Component.translatable("screen.netherportalhelper.bookmark.entry_location",
                    Component.translatable(dimensionName(bookmark)), bookmark.x(), bookmark.y(), bookmark.z());
            String details = font.plainSubstrByWidth(location.getString(), maxWidth);
            graphics.text(font, name, this.getContentX(), this.getContentY() + 2, 0xFFFFFFFF, true);
            graphics.text(font, details, this.getContentX(), this.getContentY() + 17, 0xFFB5B5B5, true);
        }
    }
}
