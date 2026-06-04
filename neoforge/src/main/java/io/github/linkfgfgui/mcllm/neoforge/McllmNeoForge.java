package io.github.linkfgfgui.mcllm.neoforge;

import io.github.linkfgfgui.mcllm.MCLLM;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

@Mod(value = MCLLM.MOD_ID, dist= Dist.CLIENT)
public final class McllmNeoForge {
    public McllmNeoForge() {
        // Run our common setup.
        MCLLM.init();
    }
}
