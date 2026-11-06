package livingkingdoms.world.client;

import java.util.Optional;
import livingkingdoms.world.WorldSize;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.presets.WorldPreset;

/** Our world-size picker (ADR-0004): one card per fixed size, opened from the Create World screen. */
final class WorldSizeScreen extends Screen {
    /** Sprinting speed in blocks per second, used for the "time to cross" hint. */
    private static final double SPRINT_SPEED = 5.6;
    private static final int CARD_WIDTH = 300;
    private static final int CARD_HEIGHT = 44;

    private final CreateWorldScreen parent;

    WorldSizeScreen(CreateWorldScreen parent) {
        super(Component.translatable("livingkingdoms_world.size_screen.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        WorldSize selected = CreateWorldHooks.selectedSize(parent.getUiState()).orElse(WorldSize.DEFAULT);
        int left = (width - CARD_WIDTH) / 2;
        int top = Math.max(32, (height - CARD_HEIGHT * WorldSize.values().length - 32) / 2);
        WorldSize[] order = {WorldSize.LARGE, WorldSize.MEDIUM, WorldSize.SMALL, WorldSize.TEST};
        for (int i = 0; i < order.length; i++) {
            WorldSize size = order[i];
            int y = top + i * CARD_HEIGHT;
            Component name = Component.translatable("livingkingdoms_world.size." + size.id());
            Component label = size == selected
                    ? Component.translatable("livingkingdoms_world.size_screen.selected", name)
                    : name;
            addRenderableWidget(Button.builder(label, b -> choose(size)).bounds(left, y, CARD_WIDTH, 20).build());
            Component details = Component.translatable(
                    "livingkingdoms_world.size_screen.details",
                    String.format(java.util.Locale.ROOT, "%,d", size.side()),
                    String.format(java.util.Locale.ROOT, "%,d", size.netherSide()),
                    size.netherRatio(),
                    Math.round(size.side() / SPRINT_SPEED / 60.0));
            StringWidget text = new StringWidget(left, y + 22, CARD_WIDTH, 12, details, font);
            addRenderableWidget(text.alignCenter());
        }
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose())
                .bounds((width - 150) / 2, top + order.length * CARD_HEIGHT + 4, 150, 20).build());
    }

    private void choose(WorldSize size) {
        WorldCreationUiState state = parent.getUiState();
        ResourceKey<WorldPreset> key = ResourceKey.create(Registries.WORLD_PRESET, size.presetId());
        Optional<? extends Holder<WorldPreset>> preset =
                state.getSettings().worldgenLoadContext().registryOrThrow(Registries.WORLD_PRESET).getHolder(key);
        preset.ifPresent(holder -> state.setWorldType(new WorldCreationUiState.WorldTypeEntry(holder)));
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 14, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }
}
