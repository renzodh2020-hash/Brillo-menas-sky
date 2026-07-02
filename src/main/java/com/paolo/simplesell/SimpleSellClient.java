package com.paolo.simplesell;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

public class SimpleSellClient implements ClientModInitializer {
    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static final Random random = new Random();

    private static KeyBinding toggleKey;

    private static boolean enabled = false;

    // Evita repetir /sellall mientras el mismo item sigue en la segunda mano
    private static boolean alreadySold = false;

    // Controla si ya se programo un /sellall pendiente
    private static boolean sellScheduled = false;
    private static int sellDelayTicks = 0;

    // Contador de /sellall
    private static int sellCount = 0;

    // Meta aleatoria para ejecutar /home up entre 9 y 14 sellall
    private static int nextHomeTarget = randomHomeTarget();

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

                if (!enabled) {
                    sellScheduled = false;
                    sellDelayTicks = 0;
                }
            }

            tickSell();
        });
    }

    private static void tickSell() {
        if (!enabled) return;
        if (client.player == null || client.world == null) return;

        boolean offhandHasItem = !client.player.getOffHandStack().isEmpty();

        // Si la segunda mano esta vacia, se reinicia el detector
        if (!offhandHasItem) {
            alreadySold = false;
            sellScheduled = false;
            sellDelayTicks = 0;
            return;
        }

        // Si ya se vendio por este item, no repetir
        if (alreadySold) {
            return;
        }

        // Si hay item y aun no hay venta programada, programa el tiempo aleatorio
        if (!sellScheduled) {
            scheduleRandomSellDelay();
            sellScheduled = true;
            return;
        }

        // Cuenta regresiva para ejecutar /sellall
        if (sellDelayTicks > 0) {
            sellDelayTicks--;
            return;
        }

        // Antes de ejecutar, verifica otra vez que aun haya item en la segunda mano
        if (!client.player.getOffHandStack().isEmpty()) {
            runCommand("sellall");

            sellCount++;

            if (sellCount >= nextHomeTarget) {
                sellCount = 0;
                runCommand("home up");

                // Despues de ejecutar /home up, elige una nueva meta aleatoria
                nextHomeTarget = randomHomeTarget();
            }

            alreadySold = true;
            sellScheduled = false;
        }
    }

    private static void scheduleRandomSellDelay() {
        // Tiempo aleatorio entre 0.500 y 1.500 segundos, con 3 decimales
        int milliseconds = 500 + random.nextInt(1001);

        // Minecraft corre a 20 ticks por segundo: 1 tick = 50 ms
        sellDelayTicks = Math.max(1, milliseconds / 50);
    }

    private static int randomHomeTarget() {
        // Numero aleatorio entre 9 y 14
        return 9 + random.nextInt(6);
    }

    private static void runCommand(String command) {
        if (client.player != null && client.player.networkHandler != null) {
            client.player.networkHandler.sendChatCommand(command);
        }
    }
}
