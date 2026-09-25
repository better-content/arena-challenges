package com.bettercontent.arenachallenges.client;

import com.bettercontent.arenachallenges.menu.ArenaMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class ArenaScreen extends AbstractContainerScreen<ArenaMenu> {
    private boolean replayView;
    public ArenaScreen(ArenaMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 284;
        imageHeight = 210;
        inventoryLabelY = imageHeight + 10_000;
        titleLabelY = 18;
    }

    @Override
    protected void init() {
        super.init();
        buildWidgets();
    }

    private void buildWidgets() {
        clearWidgets();
        int left = leftPos;
        int top = topPos;
        if (replayView) {
            addRenderableWidget(Button.builder(Component.literal("Back"), b -> { replayView = false; buildWidgets(); })
                    .bounds(left + 16, top + 153, 70, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Older"), b -> sendButton(20))
                    .bounds(left + 96, top + 153, 84, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Newer"), b -> sendButton(21))
                    .bounds(left + 188, top + 153, 80, 20).build());
            int count = menu.recordCount();
            int page = menu.replayPage();
            for (int row = 0; row < 3; row++) {
                int index = count - 1 - page * 3 - row;
                if (index < 0) continue;
                final int button = row;
                addRenderableWidget(Button.builder(Component.literal("Play recording " + (index + 1)), b -> sendButton(22 + button))
                        .bounds(left + 16, top + 60 + row * 25, 252, 20).build());
            }
            return;
        }
        addRenderableWidget(Button.builder(Component.literal("Challenge a player"), b -> sendButton(0))
                .bounds(left + 16, top + 54, 122, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Accept duel"), b -> sendButton(4))
                .bounds(left + 146, top + 54, 122, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Trial I"), b -> sendButton(1))
                .bounds(left + 16, top + 82, 80, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Trial II"), b -> sendButton(2))
                .bounds(left + 102, top + 82, 80, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Trial III"), b -> sendButton(3))
                .bounds(left + 188, top + 82, 80, 20).build());
        addRenderableWidget(Button.builder(Component.literal(rewardLabel("Blade", 0)), b -> sendButton(6))
                .bounds(left + 16, top + 125, 80, 20).build());
        addRenderableWidget(Button.builder(Component.literal(rewardLabel("Axe", 1)), b -> sendButton(7))
                .bounds(left + 102, top + 125, 80, 20).build());
        addRenderableWidget(Button.builder(Component.literal(rewardLabel("Sigil", 2)), b -> sendButton(8))
                .bounds(left + 188, top + 125, 80, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Recorded duels (" + menu.recordCount() + ")"), b -> { replayView = true; buildWidgets(); })
                .bounds(left + 16, top + 153, 122, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose())
                .bounds(left + 146, top + 153, 122, 20).build());
    }

    private String rewardLabel(String name, int index) {
        return menu.rewardAvailable(index) ? name + " · ready" : name + " · claimed";
    }

    private void sendButton(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xEE17151B);
        graphics.fill(leftPos + 5, topPos + 5, leftPos + imageWidth - 5, topPos + 45, 0xFF29222F);
        graphics.fill(leftPos + 5, topPos + 49, leftPos + imageWidth - 5, topPos + 102, 0xFF211E25);
        graphics.fill(leftPos + 5, topPos + 107, leftPos + imageWidth - 5, topPos + 145, 0xFF211E25);
        graphics.fill(leftPos + 10, topPos + 174, leftPos + imageWidth - 10, topPos + 178, 0xFF7D4FA0);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, Component.literal("ARENA CHALLENGE"), imageWidth / 2, 11, 0xFFF2DCA8);
        if (replayView) {
            graphics.drawString(font, Component.literal("Archived duels · newest first"), 16, 34, 0xFFD9CFDF);
            graphics.drawString(font, Component.literal(menu.recordCount() + " recordings · page " + (menu.replayPage() + 1)), 16, 47, 0xFFFFE6A3);
            if (menu.recordCount() == 0) graphics.drawString(font, Component.literal("No duels have been recorded here yet."), 16, 70, 0xFFD9CFDF);
        } else {
            graphics.drawString(font, Component.literal("Choose a duel or a solo trial"), 16, 34, 0xFFD9CFDF);
            graphics.drawString(font, Component.literal("Unique rewards remaining: " + menu.rewardCount()), 16, 110, 0xFFFFE6A3);
            graphics.drawString(font, Component.literal("Claim after a victory"), 16, 148, 0xFFD9CFDF);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
