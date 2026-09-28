package com.twogether.core;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client-only setup, kept apart so renderer classes are never loaded on a dedicated server. */
@EventBusSubscriber(modid = TwoGetherCoreMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class TwoGetherClient {

    private TwoGetherClient() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(TwoGetherCoreMod.MIXER_ROTOR_BE.get(), MixerRotorRenderer::new);
    }
}
