package com.twogether.core;

import com.mojang.serialization.Codec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

import java.util.List;

/**
 * Everything that can be poured into a stemmed glass: the spirits, which also come in bottles,
 * and the cocktails mixed from them. EMPTY is the empty glass. A sip from a bottle and a glass
 * give the same effects; the strength of the drink is in the nausea.
 */
public enum Drink implements StringRepresentable {
    EMPTY("empty", 0x00000000, 0),
    WHISKY("whisky", 0xFFB86A20, 200, effect(MobEffects.DAMAGE_RESISTANCE, 1200)),
    AGED_WHISKY("aged_whisky", 0xFF8A4A10, 100, effect(MobEffects.DAMAGE_RESISTANCE, 3600)),
    BRANDY("brandy", 0xFFC07830, 200, effect(MobEffects.REGENERATION, 400)),
    AGED_BRANDY("aged_brandy", 0xFF904818, 100, effect(MobEffects.REGENERATION, 900)),
    CALVADOS("calvados", 0xFFD09040, 160, effect(MobEffects.HEALTH_BOOST, 2400)),
    VODKA("vodka", 0xFFE8F0F4, 300, effect(MobEffects.FIRE_RESISTANCE, 1200)),
    RUM("rum", 0xFFD8A048, 200, effect(MobEffects.WATER_BREATHING, 2400)),
    DARK_RUM("dark_rum", 0xFF5A2A10, 100, effect(MobEffects.DOLPHINS_GRACE, 1800)),
    OLD_FASHIONED("old_fashioned", 0xFFB0581C, 60, effect(MobEffects.DAMAGE_RESISTANCE, 2400), effect(MobEffects.DAMAGE_BOOST, 1200)),
    HOT_TODDY("hot_toddy", 0xFFD89838, 60, effect(MobEffects.REGENERATION, 600), effect(MobEffects.FIRE_RESISTANCE, 1200)),
    BERRY_DAIQUIRI("berry_daiquiri", 0xFFD83A5A, 60, effect(MobEffects.MOVEMENT_SPEED, 2400), effect(MobEffects.JUMP, 1200)),
    RUM_PUNCH("rum_punch", 0xFFE0602A, 60, effect(MobEffects.HEALTH_BOOST, 2400), effect(MobEffects.SATURATION, 40)),
    BLACK_RUSSIAN("black_russian", 0xFF2A160C, 60, effect(MobEffects.NIGHT_VISION, 2400), effect(MobEffects.DIG_SPEED, 1200)),
    BRANDY_ALEXANDER("brandy_alexander", 0xFFE8D2B0, 60, effect(MobEffects.ABSORPTION, 1200), effect(MobEffects.REGENERATION, 400)),
    JACK_ROSE("jack_rose", 0xFFE84860, 60, effect(MobEffects.LUCK, 2400), effect(MobEffects.MOVEMENT_SPEED, 1200)),
    ENDER_MARTINI("ender_martini", 0xFFB070D8, 60, effect(MobEffects.SLOW_FALLING, 2400), effect(MobEffects.NIGHT_VISION, 1200));

    public static final Codec<Drink> CODEC = StringRepresentable.fromEnum(Drink::values);
    public static final StreamCodec<FriendlyByteBuf, Drink> STREAM_CODEC = NeoForgeStreamCodecs.enumCodec(Drink.class);

    private final String name;
    private final int color;
    private final int nauseaTicks;
    private final List<Effect> effects;

    Drink(String name, int color, int nauseaTicks, Effect... effects) {
        this.name = name;
        this.color = color;
        this.nauseaTicks = nauseaTicks;
        this.effects = List.of(effects);
    }

    private record Effect(Holder<MobEffect> effect, int ticks) {
    }

    private static Effect effect(Holder<MobEffect> effect, int ticks) {
        return new Effect(effect, ticks);
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public int color() {
        return color;
    }

    /** Spirits come in bottles; cocktails only exist in a glass. */
    public boolean isSpirit() {
        return ordinal() >= WHISKY.ordinal() && ordinal() <= DARK_RUM.ordinal();
    }

    public Component displayName() {
        return Component.translatable("drink.twogethercore." + name);
    }

    /** One sip from a bottle, or a whole glass. */
    public void drink(LivingEntity drinker) {
        if (drinker.level().isClientSide) return;
        for (Effect effect : effects) {
            drinker.addEffect(new MobEffectInstance(effect.effect(), effect.ticks(), 0));
        }
        if (nauseaTicks > 0) drinker.addEffect(new MobEffectInstance(MobEffects.CONFUSION, nauseaTicks, 0));
    }
}
