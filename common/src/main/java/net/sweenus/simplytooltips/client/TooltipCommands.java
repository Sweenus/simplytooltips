package net.sweenus.simplytooltips.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.architectury.event.events.client.ClientCommandRegistrationEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.sweenus.simplytooltips.SimplyTooltips;
import net.sweenus.simplytooltips.client.render.ItemThemeRegistry;
import net.sweenus.simplytooltips.client.render.ThemeRegistry;
import net.sweenus.simplytooltips.client.studio.ThemeStudioScreen;
import net.sweenus.simplytooltips.client.studio.UserDataStore;

/**
 * Client-side commands. Registered through Architectury so one definition serves both loaders.
 *
 * <ul>
 *   <li>{@code /simplytooltips} - opens the Theme Studio
 *   <li>{@code /simplytooltips <item_id>} - opens it previewing that item
 *   <li>{@code /simplytooltips reload} - re-reads {@code config/simplytooltips/}
 * </ul>
 */
public final class TooltipCommands {

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;

        ClientCommandRegistrationEvent.EVENT.register((dispatcher, registry) -> dispatcher.register(
                ClientCommandRegistrationEvent.literal(SimplyTooltips.MOD_ID)
                        .executes(context -> openStudio(null))
                        .then(ClientCommandRegistrationEvent.literal("reload")
                                .executes(context -> reload()))
                        .then(ClientCommandRegistrationEvent.argument("item", StringArgumentType.greedyString())
                                .executes(context -> openStudio(StringArgumentType.getString(context, "item"))))
        ));
    }

    private static int openStudio(String itemId) {
        MinecraftClient client = MinecraftClient.getInstance();
        // The chat screen is still closing while the command executes, so opening the Studio has to
        // wait for the next client task or it is immediately replaced by null.
        client.send(() -> client.setScreen(new ThemeStudioScreen(itemId)));
        return 1;
    }

    private static int reload() {
        MinecraftClient client = MinecraftClient.getInstance();
        ThemeRegistry.reloadUser();
        ItemThemeRegistry.reloadUser();

        Text path = Text.literal(UserDataStore.root().toString())
                .formatted(Formatting.AQUA);
        client.inGameHud.getChatHud().addMessage(
                Text.translatable("simplytooltips.command.reloaded", path));
        return 1;
    }

    private TooltipCommands() {}
}
