package com.twogether.core;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Consumer;

/**
 * Mod fourre-tout pour TWHOGETHER. Premier morceau : un fluide "biere" simple
 * (texture eau vanilla teintee ambre - pas de texture custom pour l'instant)
 * pour pouvoir la transporter dans les tuyaux/trains Create et la servir via
 * un Spout Create + recette KubeJS. Pas de bloc tireuse custom : decision
 * prise en session, le Spout suffit.
 */
@Mod(TwoGetherCoreMod.MODID)
public class TwoGetherCoreMod {
    public static final String MODID = "twogethercore";

    private static final ResourceLocation WATER_STILL_TEXTURE = ResourceLocation.withDefaultNamespace("block/water_still");
    private static final ResourceLocation WATER_FLOW_TEXTURE = ResourceLocation.withDefaultNamespace("block/water_flow");
    private static final int BEER_TINT = 0xFFFFCC1A; // jaune biere
    private static final int WINE_TINT = 0xFF7A1030; // rouge vin

    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(BuiltInRegistries.FLUID, MODID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    public static final DeferredHolder<FluidType, FluidType> BEER_FLUID_TYPE = FLUID_TYPES.register("beer",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid.twogethercore.beer")
                    .viscosity(1200)
                    .density(1050)
                    .temperature(300)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)) {
                @Override
                public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                    consumer.accept(new IClientFluidTypeExtensions() {
                        @Override
                        public ResourceLocation getStillTexture() {
                            return WATER_STILL_TEXTURE;
                        }

                        @Override
                        public ResourceLocation getFlowingTexture() {
                            return WATER_FLOW_TEXTURE;
                        }

                        @Override
                        public int getTintColor() {
                            return BEER_TINT;
                        }
                    });
                }
            });

    public static final DeferredHolder<FluidType, FluidType> WINE_FLUID_TYPE = FLUID_TYPES.register("wine",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid.twogethercore.wine")
                    .viscosity(1200)
                    .density(1050)
                    .temperature(300)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)) {
                @Override
                public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                    consumer.accept(new IClientFluidTypeExtensions() {
                        @Override
                        public ResourceLocation getStillTexture() {
                            return WATER_STILL_TEXTURE;
                        }

                        @Override
                        public ResourceLocation getFlowingTexture() {
                            return WATER_FLOW_TEXTURE;
                        }

                        @Override
                        public int getTintColor() {
                            return WINE_TINT;
                        }
                    });
                }
            });

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> BEER_STILL = FLUIDS.register("beer",
            () -> new BaseFlowingFluid.Source(beerProperties()));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> BEER_FLOWING = FLUIDS.register("flowing_beer",
            () -> new BaseFlowingFluid.Flowing(beerProperties()));

    public static final DeferredBlock<LiquidBlock> BEER_BLOCK = BLOCKS.registerBlock("beer",
            props -> new LiquidBlock(BEER_STILL.get(), props),
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD)
                    .replaceable()
                    .noCollission()
                    .strength(100.0F)
                    .pushReaction(PushReaction.DESTROY)
                    .liquid()
                    .sound(SoundType.EMPTY)
                    .noLootTable());

    public static final DeferredItem<Item> BEER_BUCKET = ITEMS.registerItem("beer_bucket",
            props -> new BucketItem(BEER_STILL.get(), props.craftRemainder(Items.BUCKET).stacksTo(1)));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> WINE_STILL = FLUIDS.register("wine",
            () -> new BaseFlowingFluid.Source(wineProperties()));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> WINE_FLOWING = FLUIDS.register("flowing_wine",
            () -> new BaseFlowingFluid.Flowing(wineProperties()));

    public static final DeferredBlock<LiquidBlock> WINE_BLOCK = BLOCKS.registerBlock("wine",
            props -> new LiquidBlock(WINE_STILL.get(), props),
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .replaceable()
                    .noCollission()
                    .strength(100.0F)
                    .pushReaction(PushReaction.DESTROY)
                    .liquid()
                    .sound(SoundType.EMPTY)
                    .noLootTable());

    public static final DeferredItem<Item> WINE_BUCKET = ITEMS.registerItem("wine_bucket",
            props -> new BucketItem(WINE_STILL.get(), props.craftRemainder(Items.BUCKET).stacksTo(1)));

    // ---- Chaine de brassage : houblonnage -> empatage -> ensemencement -> fermentation ----
    // Comme beer/wine, chaque fluide de process a aussi son bloc placable + son seau.
    private static final int HOP_TINT = 0xFF8FA83C; // vert houblon dilue
    private static final int WORT_TINT = 0xFFC98A2B; // ambre mout
    private static final int CONCENTRATED_WORT_TINT = 0xFF8A4A12; // ambre fonce, mout concentre
    private static final int PITCHED_WORT_TINT = 0xFFB06A1E; // ambre plus fonce (levure ajoutee)
    private static final int GRAPE_MUST_TINT = 0xFF6E1B3A; // rouge violace, raisin presse
    private static final int WEDWULL_TINT = 0xFFE8C020; // ambre dore fluo, boisson energisante

    public static final DeferredHolder<FluidType, FluidType> HOPPED_WATER_FLUID_TYPE = FLUID_TYPES.register("hopped_water",
            () -> processFluidType("hopped_water", HOP_TINT));
    public static final DeferredHolder<FluidType, FluidType> WORT_FLUID_TYPE = FLUID_TYPES.register("wort",
            () -> processFluidType("wort", WORT_TINT));
    public static final DeferredHolder<FluidType, FluidType> CONCENTRATED_WORT_FLUID_TYPE = FLUID_TYPES.register("concentrated_wort",
            () -> processFluidType("concentrated_wort", CONCENTRATED_WORT_TINT));
    public static final DeferredHolder<FluidType, FluidType> PITCHED_WORT_FLUID_TYPE = FLUID_TYPES.register("pitched_wort",
            () -> processFluidType("pitched_wort", PITCHED_WORT_TINT));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> HOPPED_WATER_STILL = FLUIDS.register("hopped_water",
            () -> new BaseFlowingFluid.Source(hoppedWaterProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> HOPPED_WATER_FLOWING = FLUIDS.register("flowing_hopped_water",
            () -> new BaseFlowingFluid.Flowing(hoppedWaterProperties()));
    public static final DeferredBlock<LiquidBlock> HOPPED_WATER_BLOCK = BLOCKS.registerBlock("hopped_water",
            props -> new LiquidBlock(HOPPED_WATER_STILL.get(), props), processBlockProperties(MapColor.COLOR_GREEN));
    public static final DeferredItem<Item> HOPPED_WATER_BUCKET = ITEMS.registerItem("hopped_water_bucket",
            props -> new BucketItem(HOPPED_WATER_STILL.get(), props.craftRemainder(Items.BUCKET).stacksTo(1)));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> WORT_STILL = FLUIDS.register("wort",
            () -> new BaseFlowingFluid.Source(wortProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> WORT_FLOWING = FLUIDS.register("flowing_wort",
            () -> new BaseFlowingFluid.Flowing(wortProperties()));
    public static final DeferredBlock<LiquidBlock> WORT_BLOCK = BLOCKS.registerBlock("wort",
            props -> new LiquidBlock(WORT_STILL.get(), props), processBlockProperties(MapColor.COLOR_ORANGE));
    public static final DeferredItem<Item> WORT_BUCKET = ITEMS.registerItem("wort_bucket",
            props -> new BucketItem(WORT_STILL.get(), props.craftRemainder(Items.BUCKET).stacksTo(1)));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> CONCENTRATED_WORT_STILL = FLUIDS.register("concentrated_wort",
            () -> new BaseFlowingFluid.Source(concentratedWortProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> CONCENTRATED_WORT_FLOWING = FLUIDS.register("flowing_concentrated_wort",
            () -> new BaseFlowingFluid.Flowing(concentratedWortProperties()));
    public static final DeferredBlock<LiquidBlock> CONCENTRATED_WORT_BLOCK = BLOCKS.registerBlock("concentrated_wort",
            props -> new LiquidBlock(CONCENTRATED_WORT_STILL.get(), props), processBlockProperties(MapColor.COLOR_BROWN));
    public static final DeferredItem<Item> CONCENTRATED_WORT_BUCKET = ITEMS.registerItem("concentrated_wort_bucket",
            props -> new BucketItem(CONCENTRATED_WORT_STILL.get(), props.craftRemainder(Items.BUCKET).stacksTo(1)));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> PITCHED_WORT_STILL = FLUIDS.register("pitched_wort",
            () -> new BaseFlowingFluid.Source(pitchedWortProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> PITCHED_WORT_FLOWING = FLUIDS.register("flowing_pitched_wort",
            () -> new BaseFlowingFluid.Flowing(pitchedWortProperties()));
    public static final DeferredBlock<LiquidBlock> PITCHED_WORT_BLOCK = BLOCKS.registerBlock("pitched_wort",
            props -> new LiquidBlock(PITCHED_WORT_STILL.get(), props), processBlockProperties(MapColor.COLOR_ORANGE));
    public static final DeferredItem<Item> PITCHED_WORT_BUCKET = ITEMS.registerItem("pitched_wort_bucket",
            props -> new BucketItem(PITCHED_WORT_STILL.get(), props.craftRemainder(Items.BUCKET).stacksTo(1)));

    // Grape must: pressed grapes, the wine chain's equivalent of pitched wort.
    public static final DeferredHolder<FluidType, FluidType> GRAPE_MUST_FLUID_TYPE = FLUID_TYPES.register("grape_must",
            () -> processFluidType("grape_must", GRAPE_MUST_TINT));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> GRAPE_MUST_STILL = FLUIDS.register("grape_must",
            () -> new BaseFlowingFluid.Source(grapeMustProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> GRAPE_MUST_FLOWING = FLUIDS.register("flowing_grape_must",
            () -> new BaseFlowingFluid.Flowing(grapeMustProperties()));
    public static final DeferredBlock<LiquidBlock> GRAPE_MUST_BLOCK = BLOCKS.registerBlock("grape_must",
            props -> new LiquidBlock(GRAPE_MUST_STILL.get(), props), processBlockProperties(MapColor.COLOR_PURPLE));
    public static final DeferredItem<Item> GRAPE_MUST_BUCKET = ITEMS.registerItem("grape_must_bucket",
            props -> new BucketItem(GRAPE_MUST_STILL.get(), props.craftRemainder(Items.BUCKET).stacksTo(1)));

    // WEDWULL: energy drink, not fermented - mixed cold and canned.
    public static final DeferredHolder<FluidType, FluidType> WEDWULL_FLUID_TYPE = FLUID_TYPES.register("wedwull",
            () -> processFluidType("wedwull", WEDWULL_TINT));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> WEDWULL_STILL = FLUIDS.register("wedwull",
            () -> new BaseFlowingFluid.Source(wedwullProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> WEDWULL_FLOWING = FLUIDS.register("flowing_wedwull",
            () -> new BaseFlowingFluid.Flowing(wedwullProperties()));
    public static final DeferredBlock<LiquidBlock> WEDWULL_BLOCK = BLOCKS.registerBlock("wedwull",
            props -> new LiquidBlock(WEDWULL_STILL.get(), props), processBlockProperties(MapColor.COLOR_YELLOW));
    public static final DeferredItem<Item> WEDWULL_BUCKET = ITEMS.registerItem("wedwull_bucket",
            props -> new BucketItem(WEDWULL_STILL.get(), props.craftRemainder(Items.BUCKET).stacksTo(1)));

    public static final DeferredItem<Item> EMPTY_CAN = ITEMS.registerSimpleItem("empty_can");

    public static final DeferredItem<Item> WEDWULL_CAN = ITEMS.registerItem("wedwull_can",
            props -> new CanItem(props.stacksTo(16).food(new FoodProperties.Builder()
                    .nutrition(1)
                    .saturationModifier(0.1F)
                    .alwaysEdible()
                    .fast()
                    .usingConvertsTo(EMPTY_CAN.get())
                    .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1800, 0), 1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.DIG_SPEED, 1800, 0), 1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.JUMP, 600, 0), 1.0F)
                    .build())));

    // Optional last step of the beer chain: plain beer mixed with an adjunct becomes the matching
    // Brewery variant. Without it the chain still ends on plain beer, served as barley beer.
    public static final ProcessFluid BEER_HALEY = new ProcessFluid("beer_haley", 0xFFB0709A, MapColor.COLOR_PINK);
    public static final ProcessFluid BEER_HOPS = new ProcessFluid("beer_hops", 0xFFB8B02A, MapColor.COLOR_YELLOW);
    public static final ProcessFluid BEER_NETTLE = new ProcessFluid("beer_nettle", 0xFF7F9A3A, MapColor.COLOR_GREEN);
    public static final ProcessFluid BEER_OAT = new ProcessFluid("beer_oat", 0xFFD9B060, MapColor.COLOR_ORANGE);
    public static final ProcessFluid BEER_WHEAT = new ProcessFluid("beer_wheat", 0xFFF0D060, MapColor.COLOR_YELLOW);
    public static final java.util.List<ProcessFluid> BEER_VARIANTS =
            java.util.List.of(BEER_HALEY, BEER_HOPS, BEER_NETTLE, BEER_OAT, BEER_WHEAT);

    // One must and one base wine per Vinery juice type, so each Vinery wine can be bottled from
    // the juice it asks for. Plain red grapes keep the original grape_must and wine.
    public static final ProcessFluid RED_JUNGLE_MUST = new ProcessFluid("red_jungle_grape_must", 0xFF5A1848, MapColor.COLOR_PURPLE);
    public static final ProcessFluid RED_JUNGLE_WINE = new ProcessFluid("red_jungle_wine", 0xFF631040, MapColor.COLOR_PURPLE);
    public static final ProcessFluid RED_SAVANNA_MUST = new ProcessFluid("red_savanna_grape_must", 0xFF8A2A2A, MapColor.COLOR_RED);
    public static final ProcessFluid RED_SAVANNA_WINE = new ProcessFluid("red_savanna_wine", 0xFF8E1A20, MapColor.COLOR_RED);
    public static final ProcessFluid RED_TAIGA_MUST = new ProcessFluid("red_taiga_grape_must", 0xFF4A1230, MapColor.TERRACOTTA_PURPLE);
    public static final ProcessFluid RED_TAIGA_WINE = new ProcessFluid("red_taiga_wine", 0xFF4E0A22, MapColor.TERRACOTTA_PURPLE);
    public static final ProcessFluid WHITE_MUST = new ProcessFluid("white_grape_must", 0xFFC8C060, MapColor.COLOR_YELLOW);
    public static final ProcessFluid WHITE_WINE = new ProcessFluid("white_wine", 0xFFE0D070, MapColor.COLOR_YELLOW);
    public static final ProcessFluid WHITE_JUNGLE_MUST = new ProcessFluid("white_jungle_grape_must", 0xFFA8C050, MapColor.COLOR_LIGHT_GREEN);
    public static final ProcessFluid WHITE_JUNGLE_WINE = new ProcessFluid("white_jungle_wine", 0xFFC8D860, MapColor.COLOR_LIGHT_GREEN);
    public static final ProcessFluid WHITE_SAVANNA_MUST = new ProcessFluid("white_savanna_grape_must", 0xFFD0A850, MapColor.GOLD);
    public static final ProcessFluid WHITE_SAVANNA_WINE = new ProcessFluid("white_savanna_wine", 0xFFE8C060, MapColor.GOLD);
    public static final ProcessFluid WHITE_TAIGA_MUST = new ProcessFluid("white_taiga_grape_must", 0xFFB8C8A0, MapColor.SAND);
    public static final ProcessFluid WHITE_TAIGA_WINE = new ProcessFluid("white_taiga_wine", 0xFFD8E0B8, MapColor.SAND);
    public static final ProcessFluid APPLE_MUST = new ProcessFluid("apple_must", 0xFFC89A40, MapColor.COLOR_ORANGE);
    public static final ProcessFluid APPLE_WINE = new ProcessFluid("hard_cider", 0xFFD8A030, MapColor.COLOR_ORANGE);
    public static final java.util.List<ProcessFluid> WINE_FLUIDS = java.util.List.of(
            RED_JUNGLE_MUST, RED_JUNGLE_WINE,
            RED_SAVANNA_MUST, RED_SAVANNA_WINE,
            RED_TAIGA_MUST, RED_TAIGA_WINE,
            WHITE_MUST, WHITE_WINE,
            WHITE_JUNGLE_MUST, WHITE_JUNGLE_WINE,
            WHITE_SAVANNA_MUST, WHITE_SAVANNA_WINE,
            WHITE_TAIGA_MUST, WHITE_TAIGA_WINE,
            APPLE_MUST, APPLE_WINE);

    // Spirits, none of which needs a Let's Do mod: mashes made in the Mixer, washes fermented and
    // spirits distilled in the tower, and aged spirits from the Aging Cask.
    public static final ProcessFluid GRAIN_MASH = new ProcessFluid("grain_mash", 0xFFC8A868, MapColor.SAND);
    public static final ProcessFluid GRAIN_WASH = new ProcessFluid("grain_wash", 0xFFB89850, MapColor.SAND);
    public static final ProcessFluid POTATO_MASH = new ProcessFluid("potato_mash", 0xFFD8C8A0, MapColor.SAND);
    public static final ProcessFluid POTATO_WASH = new ProcessFluid("potato_wash", 0xFFC8B888, MapColor.SAND);
    public static final ProcessFluid CANE_JUICE = new ProcessFluid("cane_juice", 0xFFB8C870, MapColor.COLOR_LIGHT_GREEN);
    public static final ProcessFluid SUGAR_WASH = new ProcessFluid("sugar_wash", 0xFFC0B060, MapColor.COLOR_YELLOW);
    public static final ProcessFluid WHISKY = new ProcessFluid("whisky", 0xFFB86A20, MapColor.COLOR_ORANGE);
    public static final ProcessFluid AGED_WHISKY = new ProcessFluid("aged_whisky", 0xFF8A4A10, MapColor.COLOR_BROWN);
    public static final ProcessFluid BRANDY = new ProcessFluid("brandy", 0xFFC07830, MapColor.COLOR_ORANGE);
    public static final ProcessFluid AGED_BRANDY = new ProcessFluid("aged_brandy", 0xFF904818, MapColor.COLOR_BROWN);
    public static final ProcessFluid CALVADOS = new ProcessFluid("calvados", 0xFFD09040, MapColor.GOLD);
    public static final ProcessFluid VODKA = new ProcessFluid("vodka", 0xFFE8F0F4, MapColor.SNOW);
    public static final ProcessFluid RUM = new ProcessFluid("rum", 0xFFD8A048, MapColor.GOLD);
    public static final ProcessFluid DARK_RUM = new ProcessFluid("dark_rum", 0xFF5A2A10, MapColor.COLOR_BROWN);
    public static final java.util.List<ProcessFluid> SPIRIT_FLUIDS = java.util.List.of(
            GRAIN_MASH, GRAIN_WASH, POTATO_MASH, POTATO_WASH,
            CANE_JUICE, SUGAR_WASH, WHISKY, AGED_WHISKY,
            BRANDY, AGED_BRANDY, CALVADOS, VODKA,
            RUM, DARK_RUM);

    /** Left over from mashing grain: compost, animal feed or Mekanism bio fuel. */
    public static final DeferredItem<Item> SPENT_GRAIN = ITEMS.registerSimpleItem("spent_grain");
    /** Our own yeast, so the brewing chain does not depend on Farm & Charm's. */
    public static final DeferredItem<Item> YEAST = ITEMS.registerSimpleItem("yeast");

    public static final DeferredItem<Item> WHISKY_BOTTLE = spiritBottle("whisky_bottle", () -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1200, 0), 200);
    public static final DeferredItem<Item> AGED_WHISKY_BOTTLE = spiritBottle("aged_whisky_bottle", () -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 3600, 0), 100);
    public static final DeferredItem<Item> BRANDY_BOTTLE = spiritBottle("brandy_bottle", () -> new MobEffectInstance(MobEffects.REGENERATION, 400, 0), 200);
    public static final DeferredItem<Item> AGED_BRANDY_BOTTLE = spiritBottle("aged_brandy_bottle", () -> new MobEffectInstance(MobEffects.REGENERATION, 900, 0), 100);
    public static final DeferredItem<Item> CALVADOS_BOTTLE = spiritBottle("calvados_bottle", () -> new MobEffectInstance(MobEffects.HEALTH_BOOST, 2400, 0), 160);
    public static final DeferredItem<Item> VODKA_BOTTLE = spiritBottle("vodka_bottle", () -> new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1200, 0), 300);
    public static final DeferredItem<Item> RUM_BOTTLE = spiritBottle("rum_bottle", () -> new MobEffectInstance(MobEffects.WATER_BREATHING, 2400, 0), 200);
    public static final DeferredItem<Item> DARK_RUM_BOTTLE = spiritBottle("dark_rum_bottle", () -> new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 1800, 0), 100);
    public static final java.util.List<DeferredItem<Item>> SPIRIT_BOTTLES = java.util.List.of(
            WHISKY_BOTTLE, AGED_WHISKY_BOTTLE, BRANDY_BOTTLE, AGED_BRANDY_BOTTLE,
            CALVADOS_BOTTLE, VODKA_BOTTLE, RUM_BOTTLE, DARK_RUM_BOTTLE);

    /** A drinkable spirit: one effect for the spirit's character, and some nausea for the strength. */
    private static DeferredItem<Item> spiritBottle(String name, java.util.function.Supplier<MobEffectInstance> effect, int nauseaTicks) {
        return ITEMS.registerItem(name, props -> new CanItem(props.stacksTo(16).food(new FoodProperties.Builder()
                .nutrition(2)
                .saturationModifier(0.2F)
                .alwaysEdible()
                .usingConvertsTo(Items.GLASS_BOTTLE)
                .effect(effect, 1.0F)
                .effect(() -> new MobEffectInstance(MobEffects.CONFUSION, nauseaTicks, 0), 1.0F)
                .build())));
    }

    static BlockBehaviour.Properties processBlockProperties(MapColor mapColor) {
        return BlockBehaviour.Properties.of()
                .mapColor(mapColor)
                .replaceable()
                .noCollission()
                .strength(100.0F)
                .pushReaction(PushReaction.DESTROY)
                .liquid()
                .sound(SoundType.EMPTY)
                .noLootTable();
    }

    private static BaseFlowingFluid.Properties hoppedWaterProperties() {
        return processProperties(HOPPED_WATER_FLUID_TYPE, HOPPED_WATER_STILL, HOPPED_WATER_FLOWING)
                .bucket(HOPPED_WATER_BUCKET)
                .block(HOPPED_WATER_BLOCK);
    }

    private static BaseFlowingFluid.Properties wortProperties() {
        return processProperties(WORT_FLUID_TYPE, WORT_STILL, WORT_FLOWING)
                .bucket(WORT_BUCKET)
                .block(WORT_BLOCK);
    }

    private static BaseFlowingFluid.Properties concentratedWortProperties() {
        return processProperties(CONCENTRATED_WORT_FLUID_TYPE, CONCENTRATED_WORT_STILL, CONCENTRATED_WORT_FLOWING)
                .bucket(CONCENTRATED_WORT_BUCKET)
                .block(CONCENTRATED_WORT_BLOCK);
    }

    private static BaseFlowingFluid.Properties grapeMustProperties() {
        return processProperties(GRAPE_MUST_FLUID_TYPE, GRAPE_MUST_STILL, GRAPE_MUST_FLOWING)
                .bucket(GRAPE_MUST_BUCKET)
                .block(GRAPE_MUST_BLOCK);
    }

    private static BaseFlowingFluid.Properties wedwullProperties() {
        return processProperties(WEDWULL_FLUID_TYPE, WEDWULL_STILL, WEDWULL_FLOWING)
                .bucket(WEDWULL_BUCKET)
                .block(WEDWULL_BLOCK);
    }

    private static BaseFlowingFluid.Properties pitchedWortProperties() {
        return processProperties(PITCHED_WORT_FLUID_TYPE, PITCHED_WORT_STILL, PITCHED_WORT_FLOWING)
                .bucket(PITCHED_WORT_BUCKET)
                .block(PITCHED_WORT_BLOCK);
    }

    static FluidType processFluidType(String descriptionId, int tint) {
        return new FluidType(FluidType.Properties.create()
                .descriptionId("fluid.twogethercore." + descriptionId)
                .viscosity(1000)
                .density(1000)
                .temperature(300)
                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)) {
            @Override
            public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                consumer.accept(new IClientFluidTypeExtensions() {
                    @Override
                    public ResourceLocation getStillTexture() {
                        return WATER_STILL_TEXTURE;
                    }

                    @Override
                    public ResourceLocation getFlowingTexture() {
                        return WATER_FLOW_TEXTURE;
                    }

                    @Override
                    public int getTintColor() {
                        return tint;
                    }
                });
            }
        };
    }

    static BaseFlowingFluid.Properties processProperties(
            DeferredHolder<FluidType, FluidType> type,
            java.util.function.Supplier<? extends Fluid> still,
            java.util.function.Supplier<? extends Fluid> flowing) {
        return new BaseFlowingFluid.Properties(type, still, flowing)
                .slopeFindDistance(4)
                .levelDecreasePerBlock(1)
                .explosionResistance(100.0F)
                .tickRate(5);
    }

    // ---- Distillation Tower : multiblock maison (pas d'API Mekanism publique pour ca,
    // voir DistillationTowerControllerBlockEntity), forme fixee par le prototype du joueur.
    // 2 tiers cosmetiques/perf : cuivre (base) et acier inox (plus efficace, via le controleur). ----

    private static BlockBehaviour.Properties towerBlockProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(4.0f)
                .requiresCorrectToolForDrops();
    }

    public static final DeferredBlock<DistillationTowerCasingBlock> DISTILLATION_CASING_COPPER = BLOCKS.register("distillation_tower_casing_copper",
            () -> new DistillationTowerCasingBlock(towerBlockProperties()));
    public static final DeferredItem<BlockItem> DISTILLATION_CASING_COPPER_ITEM =
            ITEMS.registerSimpleBlockItem("distillation_tower_casing_copper", DISTILLATION_CASING_COPPER);

    public static final DeferredBlock<DistillationTowerCasingBlock> DISTILLATION_CASING_STEEL = BLOCKS.register("distillation_tower_casing_steel",
            () -> new DistillationTowerCasingBlock(towerBlockProperties()));
    public static final DeferredItem<BlockItem> DISTILLATION_CASING_STEEL_ITEM =
            ITEMS.registerSimpleBlockItem("distillation_tower_casing_steel", DISTILLATION_CASING_STEEL);

    public static final DeferredBlock<DistillationTowerValveBlock> DISTILLATION_VALVE_COPPER = BLOCKS.register("distillation_tower_valve_copper",
            () -> new DistillationTowerValveBlock(towerBlockProperties()));
    public static final DeferredItem<BlockItem> DISTILLATION_VALVE_COPPER_ITEM =
            ITEMS.registerSimpleBlockItem("distillation_tower_valve_copper", DISTILLATION_VALVE_COPPER);

    public static final DeferredBlock<DistillationTowerValveBlock> DISTILLATION_VALVE_STEEL = BLOCKS.register("distillation_tower_valve_steel",
            () -> new DistillationTowerValveBlock(towerBlockProperties()));
    public static final DeferredItem<BlockItem> DISTILLATION_VALVE_STEEL_ITEM =
            ITEMS.registerSimpleBlockItem("distillation_tower_valve_steel", DISTILLATION_VALVE_STEEL);

    public static final DeferredBlock<DistillationTowerControllerBlock> DISTILLATION_CONTROLLER_COPPER = BLOCKS.register("distillation_tower_controller_copper",
            () -> new DistillationTowerControllerBlock(towerBlockProperties()));
    public static final DeferredItem<BlockItem> DISTILLATION_CONTROLLER_COPPER_ITEM =
            ITEMS.registerSimpleBlockItem("distillation_tower_controller_copper", DISTILLATION_CONTROLLER_COPPER);

    public static final DeferredBlock<DistillationTowerControllerBlock> DISTILLATION_CONTROLLER_STEEL = BLOCKS.register("distillation_tower_controller_steel",
            () -> new DistillationTowerControllerBlock(towerBlockProperties()));
    public static final DeferredItem<BlockItem> DISTILLATION_CONTROLLER_STEEL_ITEM =
            ITEMS.registerSimpleBlockItem("distillation_tower_controller_steel", DISTILLATION_CONTROLLER_STEEL);

    // ---- Mixer : cuve octogonale en acier, colonne de rotors a pales au centre ----

    // Coque en laiton, propre au mixer : il ne partage plus les blocs acier de la tour.
    public static final DeferredBlock<DistillationTowerCasingBlock> MIXER_CASING = BLOCKS.register("mixer_casing",
            () -> new DistillationTowerCasingBlock(towerBlockProperties()));
    public static final DeferredItem<BlockItem> MIXER_CASING_ITEM =
            ITEMS.registerSimpleBlockItem("mixer_casing", MIXER_CASING);

    public static final DeferredBlock<DistillationTowerValveBlock> MIXER_VALVE = BLOCKS.register("mixer_valve",
            () -> new DistillationTowerValveBlock(towerBlockProperties()));
    public static final DeferredItem<BlockItem> MIXER_VALVE_ITEM =
            ITEMS.registerSimpleBlockItem("mixer_valve", MIXER_VALVE);

    public static final DeferredBlock<MixerControllerBlock> MIXER_CONTROLLER = BLOCKS.register("mixer_controller",
            () -> new MixerControllerBlock(towerBlockProperties()));
    public static final DeferredItem<BlockItem> MIXER_CONTROLLER_ITEM =
            ITEMS.registerSimpleBlockItem("mixer_controller", MIXER_CONTROLLER);

    public static final DeferredBlock<MixerRotorBlock> MIXER_ROTOR = BLOCKS.register("mixer_rotor",
            () -> new MixerRotorBlock(towerBlockProperties().noOcclusion()));
    public static final DeferredItem<BlockItem> MIXER_ROTOR_ITEM =
            ITEMS.registerSimpleBlockItem("mixer_rotor", MIXER_ROTOR);

    public static final DeferredBlock<Block> MIXER_DRIVE = BLOCKS.registerSimpleBlock("mixer_drive", towerBlockProperties());
    public static final DeferredBlock<AgingCaskBlock> AGING_CASK = BLOCKS.register("aging_cask",
            () -> new AgingCaskBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD)));
    public static final DeferredItem<BlockItem> AGING_CASK_ITEM = ITEMS.registerSimpleBlockItem("aging_cask", AGING_CASK);
    public static final DeferredItem<BlockItem> MIXER_DRIVE_ITEM =
            ITEMS.registerSimpleBlockItem("mixer_drive", MIXER_DRIVE);

    public static final DeferredItem<Item> MIXER_BLADE = ITEMS.registerSimpleItem("mixer_blade");

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CREATIVE_TAB = CREATIVE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.twogethercore"))
                    .icon(() -> BEER_BUCKET.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        output.accept(BEER_BUCKET.get());
                        output.accept(WINE_BUCKET.get());
                        output.accept(HOPPED_WATER_BUCKET.get());
                        output.accept(WORT_BUCKET.get());
                        output.accept(CONCENTRATED_WORT_BUCKET.get());
                        output.accept(PITCHED_WORT_BUCKET.get());
                        output.accept(GRAPE_MUST_BUCKET.get());
                        output.accept(WEDWULL_BUCKET.get());
                        output.accept(EMPTY_CAN.get());
                        output.accept(WEDWULL_CAN.get());
                        BEER_VARIANTS.forEach(variant -> output.accept(variant.bucket.get()));
                        WINE_FLUIDS.forEach(fluid -> output.accept(fluid.bucket.get()));
                        output.accept(DISTILLATION_CASING_COPPER_ITEM.get());
                        output.accept(DISTILLATION_CASING_STEEL_ITEM.get());
                        output.accept(DISTILLATION_VALVE_COPPER_ITEM.get());
                        output.accept(DISTILLATION_VALVE_STEEL_ITEM.get());
                        output.accept(DISTILLATION_CONTROLLER_COPPER_ITEM.get());
                        output.accept(DISTILLATION_CONTROLLER_STEEL_ITEM.get());
                        output.accept(MIXER_CASING_ITEM.get());
                        output.accept(MIXER_VALVE_ITEM.get());
                        output.accept(MIXER_CONTROLLER_ITEM.get());
                        output.accept(MIXER_ROTOR_ITEM.get());
                        output.accept(MIXER_DRIVE_ITEM.get());
                        output.accept(MIXER_BLADE.get());
                        output.accept(AGING_CASK_ITEM.get());
                        output.accept(SPENT_GRAIN.get());
                        output.accept(YEAST.get());
                        SPIRIT_FLUIDS.forEach(fluid -> output.accept(fluid.bucket.get()));
                        SPIRIT_BOTTLES.forEach(bottle -> output.accept(bottle.get()));
                    })
                    .build());

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DistillationTowerValveBlockEntity>> DISTILLATION_VALVE_BE =
            BLOCK_ENTITIES.register("distillation_tower_valve", () -> BlockEntityType.Builder.of(
                    DistillationTowerValveBlockEntity::new, DISTILLATION_VALVE_COPPER.get(), DISTILLATION_VALVE_STEEL.get(),
                    MIXER_VALVE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DistillationTowerControllerBlockEntity>> DISTILLATION_CONTROLLER_BE =
            BLOCK_ENTITIES.register("distillation_tower_controller", () -> BlockEntityType.Builder.of(
                    DistillationTowerControllerBlockEntity::new, DISTILLATION_CONTROLLER_COPPER.get(), DISTILLATION_CONTROLLER_STEEL.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MixerControllerBlockEntity>> MIXER_CONTROLLER_BE =
            BLOCK_ENTITIES.register("mixer_controller", () -> BlockEntityType.Builder.of(
                    MixerControllerBlockEntity::new, MIXER_CONTROLLER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AgingCaskBlockEntity>> AGING_CASK_BE =
            BLOCK_ENTITIES.register("aging_cask", () -> BlockEntityType.Builder.of(
                    AgingCaskBlockEntity::new, AGING_CASK.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MixerRotorBlockEntity>> MIXER_ROTOR_BE =
            BLOCK_ENTITIES.register("mixer_rotor", () -> BlockEntityType.Builder.of(
                    MixerRotorBlockEntity::new, MIXER_ROTOR.get()).build(null));

    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<DistillationTowerControllerMenu>> DISTILLATION_CONTROLLER_MENU =
            MENU_TYPES.register("distillation_tower_controller", () -> IMenuTypeExtension.create(DistillationTowerControllerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<MixerMenu>> MIXER_MENU =
            MENU_TYPES.register("mixer", () -> IMenuTypeExtension.create(MixerMenu::new));

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<FermentingRecipe>> FERMENTING_TYPE =
            RECIPE_TYPES.register("fermenting", () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MODID, "fermenting")));

    public static final DeferredHolder<RecipeSerializer<?>, FermentingRecipe.Serializer> FERMENTING_SERIALIZER =
            RECIPE_SERIALIZERS.register("fermenting", FermentingRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<DistillingRecipe>> DISTILLING_TYPE =
            RECIPE_TYPES.register("distilling", () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MODID, "distilling")));

    public static final DeferredHolder<RecipeSerializer<?>, DistillingRecipe.Serializer> DISTILLING_SERIALIZER =
            RECIPE_SERIALIZERS.register("distilling", DistillingRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<AgingRecipe>> AGING_TYPE =
            RECIPE_TYPES.register("aging", () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MODID, "aging")));

    public static final DeferredHolder<RecipeSerializer<?>, AgingRecipe.Serializer> AGING_SERIALIZER =
            RECIPE_SERIALIZERS.register("aging", AgingRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<MixingRecipe>> MIXING_TYPE =
            RECIPE_TYPES.register("mixing", () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MODID, "mixing")));

    public static final DeferredHolder<RecipeSerializer<?>, MixingRecipe.Serializer> MIXING_SERIALIZER =
            RECIPE_SERIALIZERS.register("mixing", MixingRecipe.Serializer::new);

    private static BaseFlowingFluid.Properties beerProperties() {
        return new BaseFlowingFluid.Properties(BEER_FLUID_TYPE, BEER_STILL, BEER_FLOWING)
                .bucket(BEER_BUCKET)
                .block(BEER_BLOCK)
                .slopeFindDistance(4)
                .levelDecreasePerBlock(1)
                .explosionResistance(100.0F)
                .tickRate(5);
    }

    private static BaseFlowingFluid.Properties wineProperties() {
        return new BaseFlowingFluid.Properties(WINE_FLUID_TYPE, WINE_STILL, WINE_FLOWING)
                .bucket(WINE_BUCKET)
                .block(WINE_BLOCK)
                .slopeFindDistance(4)
                .levelDecreasePerBlock(1)
                .explosionResistance(100.0F)
                .tickRate(5);
    }

    public TwoGetherCoreMod(IEventBus modEventBus) {
        FLUID_TYPES.register(modEventBus);
        FLUIDS.register(modEventBus);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        MENU_TYPES.register(modEventBus);
        RECIPE_TYPES.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
        modEventBus.addListener(this::registerCapabilities);
        modEventBus.addListener(this::registerScreens);
        // Only touch CC classes when it is installed: it is an optional dependency.
        if (net.neoforged.fml.ModList.get().isLoaded("computercraft")) ComputerCraftCompat.register();
    }

    private void registerScreens(RegisterMenuScreensEvent event) {
        event.register(DISTILLATION_CONTROLLER_MENU.get(), DistillationTowerControllerScreen::new);
        event.register(MIXER_MENU.get(), MixerScreen::new);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, DISTILLATION_VALVE_BE.get(),
                (be, side) -> be.getFluidCapability());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DISTILLATION_VALVE_BE.get(),
                (be, side) -> be.getItemCapability());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, DISTILLATION_VALVE_BE.get(),
                (be, side) -> be.getEnergyCapability());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, AGING_CASK_BE.get(),
                (be, side) -> be.getTank());
        // Heat is taken on the valves, not the controller: conductors hook into the tower wall.
        event.registerBlockEntity(mekanism.common.capabilities.Capabilities.HEAT, DISTILLATION_VALVE_BE.get(),
                (be, side) -> be);
    }
}
