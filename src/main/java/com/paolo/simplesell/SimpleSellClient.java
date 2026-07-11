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
    private static final MinecraftClient CLIENT = MinecraftClient.getInstance();
    private static final Random RANDOM = new Random();

    private static KeyBinding toggleSellKey;
    private static KeyBinding toggleAutoMineKey;

    private static boolean sellEnabled = false;
    private static boolean autoMineEnabled = false;

    private static boolean alreadySold = false;
    private static boolean sellScheduled = false;

    private static int sellDelayTicks = 0;
    private static int sellCount = 0;

    /*
     * Después de esta cantidad aleatoria de /sellall,
     * se ejecuta /home up.
     */
    private static int nextHomeTarget = randomHomeTarget();

    @Override
    public void onInitializeClient() {
        toggleSellKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.simple_sell.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                "category.simple_sell"
        ));

        toggleAutoMineKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.simple_sell.automine",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                "category.simple_sell"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleSellKey.wasPressed()) {
                sellEnabled = !sellEnabled;

                if (!sellEnabled) {
                    sellScheduled = false;
                    sellDelayTicks = 0;
                }
            }

            while (toggleAutoMineKey.wasPressed()) {
                autoMineEnabled = !autoMineEnabled;

                if (!autoMineEnabled) {
                    releaseAutoMineKeys();
                }
            }

            tickSell();
            tickAutoMine();
        });
    }

    private static void tickSell() {
        if (!sellEnabled) {
            return;
        }

        if (CLIENT.player == null || CLIENT.world == null) {
            return;
        }

        boolean offhandHasItem =
                !CLIENT.player.getOffHandStack().isEmpty();

        /*
         * Si la segunda mano queda vacía,
         * se reinicia el detector.
         */
        if (!offhandHasItem) {
            alreadySold = false;
            sellScheduled = false;
            sellDelayTicks = 0;
            return;
        }

        /*
         * Si ya se ejecutó /sellall por este ítem,
         * no vuelve a repetirlo hasta que la mano se vacíe.
         */
        if (alreadySold) {
            return;
        }

        /*
         * Cuando aparece un ítem, programa una espera
         * aleatoria entre 0.500 y 1.500 segundos.
         */
        if (!sellScheduled) {
            scheduleRandomSellDelay();
            sellScheduled = true;
            return;
        }

        if (sellDelayTicks > 0) {
            sellDelayTicks--;
            return;
        }

        /*
         * Comprueba otra vez que el ítem siga en la segunda mano.
         */
        if (!CLIENT.player.getOffHandStack().isEmpty()) {
            runCommand("sellall");

            sellCount++;

            /*
             * Cada 9 a 14 ejecuciones aleatorias,
             * manda /home up.
             */
            if (sellCount >= nextHomeTarget) {
                runCommand("home up");

                sellCount = 0;
                nextHomeTarget = randomHomeTarget();
            }

            alreadySold = true;
            sellScheduled = false;
        }
    }

    private static void tickAutoMine() {
        if (!autoMineEnabled) {
            return;
        }

        if (CLIENT.player == null || CLIENT.world == null) {
            releaseAutoMineKeys();
            return;
        }

        /*
         * Mantiene presionados:
         * - clic izquierdo para picar
         * - Shift para agacharse
         */
        CLIENT.options.attackKey.setPressed(true);
        CLIENT.options.sneakKey.setPressed(true);
    }

    private static void releaseAutoMineKeys() {
        CLIENT.options.attackKey.setPressed(false);
        CLIENT.options.sneakKey.setPressed(false);
    }

    private static void scheduleRandomSellDelay() {
        int milliseconds =
                500 + RANDOM.nextInt(1001);

        sellDelayTicks =
                Math.max(1, milliseconds / 50);
    }

    private static int randomHomeTarget() {
        return 9 + RANDOM.nextInt(6);
    }

    private static void runCommand(String command) {
        if (CLIENT.player != null
                && CLIENT.player.networkHandler != null) {
            CLIENT.player.networkHandler.sendChatCommand(command);
        }
    }
}
