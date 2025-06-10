package io.github.linkfgfgui.mcllm.neoforge;

import io.github.linkfgfgui.mcllm.MCLLM;
import net.neoforged.fml.common.Mod;

@Mod(MCLLM.MOD_ID)
public final class McllmNeoForge {
    public McllmNeoForge() {
        // Run our common setup.
        MCLLM.init();
    }
}
