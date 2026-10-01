package com.twogether.core;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * WEDWULL Wings: the player may fly, creative style, while the effect lasts. Flight is granted
 * while the effect ticks and taken back by a player tick check rather than by removal events, so
 * it ends however the effect ends - expiry, milk, death, a command or a relog. A player caught in
 * the air gets a few seconds of slow falling. Creative and spectator flight is never touched.
 */
public class WingsEffect extends MobEffect {

    /** Set on players whose flight comes from this effect, so we only take back what we gave. */
    private static final String GRANTED_TAG = TwoGetherCoreMod.MODID + ".wings";
    private static final int LANDING_TICKS = 200;

    public WingsEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x3AA0F0);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // Often enough to restore flight after a respawn or a gamemode change, cheap all the same.
        return duration % 10 == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity instanceof Player player) grant(player);
        return true;
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        if (entity instanceof Player player) grant(player);
    }

    private static void grant(Player player) {
        if (player.level().isClientSide || player.getAbilities().mayfly) return;
        player.getAbilities().mayfly = true;
        player.getPersistentData().putBoolean(GRANTED_TAG, true);
        player.onUpdateAbilities();
    }

    static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || !player.getPersistentData().getBoolean(GRANTED_TAG)) return;
        if (player.hasEffect(TwoGetherCoreMod.WINGS)) return;
        player.getPersistentData().remove(GRANTED_TAG);
        if (player.isCreative() || player.isSpectator()) return;
        boolean wasFlying = player.getAbilities().flying;
        player.getAbilities().mayfly = false;
        player.getAbilities().flying = false;
        player.onUpdateAbilities();
        if (wasFlying || !player.onGround()) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, LANDING_TICKS, 0));
        }
    }
}
