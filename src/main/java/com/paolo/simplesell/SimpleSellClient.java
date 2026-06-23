package com.paolo.simplesell;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class SimpleSellClient implements ClientModInitializer {
    private static final MinecraftClient client = MinecraftClient.getInstance();

    private static KeyBinding toggleKey;

    private static boolean enabled = false;
    private static boolean alreadySold = false;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.simple_sell.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                "category.simple_sell"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(minecraftClient -> {
            while (toggleKey.wasPressed()) {
                enabled = !enabled;
            }

            tickSell();
        });
    }

    private static void tickSell() {
        if (!enabled) return;
        if (client.player == null || client.world == null) return;

        boolean offhandHasItem = !client.player.getOffHandStack().isEmpty();

        if (!offhandHasItem) {
            alreadySold = false;
            return;
        }

        if (!alreadySold) {
            runCommand("sellall");
            alreadySold = true;
        }
    }

    private static void runCommand(String command) {
        if (client.player != null && client.player.networkHandler != null) {
            client.player.networkHandler.sendChatCommand(command);
        }
    }
}
