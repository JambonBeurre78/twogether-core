package com.twogether.core;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client-only setup, kept apart so renderer classes are never loaded on a dedicated server. */
@EventBusSubscriber(modid = TwoGetherCoreMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class TwoGetherClient {

    private TwoGetherClient() {
    }

    /** The glass item's model picks its liquid from the drink it holds (ordinal / 100). */
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(TwoGetherCoreMod.STEMMED_GLASS.get(),
                ResourceLocation.fromNamespaceAndPath(TwoGetherCoreMod.MODID, "drink"),
                (stack, level, entity, seed) -> StemmedGlassItem.contents(stack).ordinal() / 100.0F));
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(TwoGetherCoreMod.MIXER_ROTOR_BE.get(), MixerRotorRenderer::new);
        event.registerBlockEntityRenderer(TwoGetherCoreMod.MIXER_CONTROLLER_BE.get(), MixerFluidRenderer::new);
    }
}
