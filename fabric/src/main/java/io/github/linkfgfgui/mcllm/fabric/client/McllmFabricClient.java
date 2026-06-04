package io.github.linkfgfgui.mcllm.fabric.client;

import io.github.linkfgfgui.mcllm.MCLLM;
import net.fabricmc.api.ClientModInitializer;

public final class McllmFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // This entrypoint is suitable for setting up client-specific logic, such as rendering.
        MCLLM.init();
    }
}
