package net.sweenus.simplytooltips.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.sweenus.simplytooltips.SimplyTooltips;
import net.sweenus.simplytooltips.client.TooltipKeybinds;
import net.sweenus.simplytooltips.client.TooltipNavigationEvents;
import net.sweenus.simplytooltips.client.render.ItemThemeRegistry;
import net.sweenus.simplytooltips.client.render.ThemeRegistry;

public final class SimplyTooltipsFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES)
                .registerReloadListener(new SimpleSynchronousResourceReloadListener() {
                    @Override
                    public Identifier getFabricId() {
                        return Identifier.of(SimplyTooltips.MOD_ID, "themes");
                    }

                    @Override
                    public void reload(ResourceManager manager) {
                        ThemeRegistry.loadAll(manager);
                    }
                });

        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES)
                .registerReloadListener(new SimpleSynchronousResourceReloadListener() {
                    @Override
                    public Identifier getFabricId() {
                        return Identifier.of(SimplyTooltips.MOD_ID, "item_themes");
                    }

                    @Override
                    public void reload(ResourceManager manager) {
                        ItemThemeRegistry.loadAll(manager);
                    }
                });

        TooltipNavigationEvents.register();
        KeyBindingHelper.registerKeyBinding(TooltipKeybinds.CYCLE_TAB);
        KeyBindingHelper.registerKeyBinding(TooltipKeybinds.CAPTURE_GIF);
    }
}
