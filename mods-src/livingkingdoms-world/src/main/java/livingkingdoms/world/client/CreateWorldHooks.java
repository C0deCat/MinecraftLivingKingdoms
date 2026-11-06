package livingkingdoms.world.client;

import java.util.Optional;
import livingkingdoms.world.WorldSize;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Adapts the vanilla Create World screen (ADR-0004, D-18): preselects the default size, replaces the
 * World Type button with our "World Size" button, hides "Customize" and "Generate Structures"
 * (structures are always on).
 *
 * <p>The World tab adds its widgets only while it is shown, so they are found and hidden on every
 * render by type and translation key; our button follows the hidden World Type button.
 */
final class CreateWorldHooks {
    private static final int ROW_TOLERANCE = 10;
    private static Button sizeButton;

    private CreateWorldHooks() {}

    static void onInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof CreateWorldScreen screen)) return;
        WorldCreationUiState state = screen.getUiState();
        if (selectedSize(state).isEmpty()) select(state, WorldSize.DEFAULT);
        state.setGenerateStructures(true);
        sizeButton = Button.builder(Component.empty(), b -> Minecraft.getInstance().setScreen(new WorldSizeScreen(screen)))
                .bounds(0, 0, 310, 20)
                .build();
        sizeButton.visible = false;
        event.addListener(sizeButton);
    }

    static void onRender(ScreenEvent.Render.Pre event) {
        if (!(event.getScreen() instanceof CreateWorldScreen screen) || sizeButton == null) return;
        WorldCreationUiState state = screen.getUiState();
        AbstractWidget worldType = null;
        StringWidget structuresLabel = null;
        for (GuiEventListener child : screen.children()) {
            if (child instanceof CycleButton<?> cycle && cycle.getValue() instanceof WorldCreationUiState.WorldTypeEntry) {
                worldType = cycle;
            } else if (child instanceof Button button && hasKey(button.getMessage(), "selectWorld.customizeType")) {
                button.visible = false;
            } else if (child instanceof StringWidget label && hasKey(label.getMessage(), "selectWorld.mapFeatures")) {
                structuresLabel = label;
            }
        }
        if (structuresLabel != null) {
            structuresLabel.visible = false;
            for (GuiEventListener child : screen.children()) {
                if (child instanceof CycleButton<?> cycle && cycle.getValue() instanceof Boolean
                        && Math.abs(cycle.getY() - structuresLabel.getY()) < ROW_TOLERANCE) {
                    cycle.visible = false;
                }
            }
        }
        if (worldType != null) {
            worldType.visible = false;
            sizeButton.setPosition(worldType.getX(), worldType.getY());
            sizeButton.visible = true;
        } else {
            sizeButton.visible = false;
        }
        WorldSize size = selectedSize(state).orElse(WorldSize.DEFAULT);
        sizeButton.setMessage(Component.translatable("livingkingdoms_world.size_button",
                Component.translatable("livingkingdoms_world.size." + size.id())));
        if (!state.isGenerateStructures()) state.setGenerateStructures(true);
    }

    static Optional<WorldSize> selectedSize(WorldCreationUiState state) {
        Holder<WorldPreset> preset = state.getWorldType().preset();
        if (preset == null) return Optional.empty();
        return preset.unwrapKey().flatMap(key -> {
            for (WorldSize size : WorldSize.values()) {
                if (size.presetId().equals(key.location())) return Optional.of(size);
            }
            return Optional.empty();
        });
    }

    private static void select(WorldCreationUiState state, WorldSize size) {
        ResourceKey<WorldPreset> key = ResourceKey.create(Registries.WORLD_PRESET, size.presetId());
        state.getSettings().worldgenLoadContext().registryOrThrow(Registries.WORLD_PRESET).getHolder(key)
                .ifPresent(holder -> state.setWorldType(new WorldCreationUiState.WorldTypeEntry(holder)));
    }

    private static boolean hasKey(Component component, String key) {
        return component.getContents() instanceof TranslatableContents translatable && translatable.getKey().equals(key);
    }
}
