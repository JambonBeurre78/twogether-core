package com.twogether.core;

import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
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
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.attachment.AttachmentType;
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

import java.util.UUID;
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

    public static final DeferredBlock<FermenterBlock> FERMENTER = BLOCKS.register("fermenter",
            () -> new FermenterBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(4.0f)
                    .requiresCorrectToolForDrops()));

    public static final DeferredItem<BlockItem> FERMENTER_ITEM = ITEMS.registerSimpleBlockItem("fermenter", FERMENTER);

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

    public static final DeferredItem<Item> MAGIC_WAND = ITEMS.registerItem("magic_wand",
            props -> new MagicWandItem(props.stacksTo(1)));

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
                        output.accept(FERMENTER_ITEM.get());
                        output.accept(DISTILLATION_CASING_COPPER_ITEM.get());
                        output.accept(DISTILLATION_CASING_STEEL_ITEM.get());
                        output.accept(DISTILLATION_VALVE_COPPER_ITEM.get());
                        output.accept(DISTILLATION_VALVE_STEEL_ITEM.get());
                        output.accept(DISTILLATION_CONTROLLER_COPPER_ITEM.get());
                        output.accept(DISTILLATION_CONTROLLER_STEEL_ITEM.get());
                        output.accept(MAGIC_WAND.get());
                    })
                    .build());

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FermenterBlockEntity>> FERMENTER_BE =
            BLOCK_ENTITIES.register("fermenter", () -> BlockEntityType.Builder.of(
                    FermenterBlockEntity::new, FERMENTER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DistillationTowerValveBlockEntity>> DISTILLATION_VALVE_BE =
            BLOCK_ENTITIES.register("distillation_tower_valve", () -> BlockEntityType.Builder.of(
                    DistillationTowerValveBlockEntity::new, DISTILLATION_VALVE_COPPER.get(), DISTILLATION_VALVE_STEEL.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DistillationTowerControllerBlockEntity>> DISTILLATION_CONTROLLER_BE =
            BLOCK_ENTITIES.register("distillation_tower_controller", () -> BlockEntityType.Builder.of(
                    DistillationTowerControllerBlockEntity::new, DISTILLATION_CONTROLLER_COPPER.get(), DISTILLATION_CONTROLLER_STEEL.get()).build(null));

    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<DistillationTowerControllerMenu>> DISTILLATION_CONTROLLER_MENU =
            MENU_TYPES.register("distillation_tower_controller", () -> IMenuTypeExtension.create(DistillationTowerControllerMenu::new));

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<FermentingRecipe>> FERMENTING_TYPE =
            RECIPE_TYPES.register("fermenting", () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(MODID, "fermenting")));

    public static final DeferredHolder<RecipeSerializer<?>, FermentingRecipe.Serializer> FERMENTING_SERIALIZER =
            RECIPE_SERIALIZERS.register("fermenting", FermentingRecipe.Serializer::new);

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MODID);

    /** The villager the magic wand currently has selected. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> WAND_VILLAGER =
            DATA_COMPONENTS.register("wand_villager", () -> DataComponentType.<UUID>builder()
                    .persistent(UUIDUtil.CODEC)
                    .networkSynchronized(UUIDUtil.STREAM_CODEC)
                    .build());

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<VillagerAssignment>> VILLAGER_ASSIGNMENT =
            ATTACHMENT_TYPES.register("villager_assignment", () -> AttachmentType
                    .builder(() -> VillagerAssignment.EMPTY)
                    .serialize(VillagerAssignment.CODEC)
                    .build());

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
        DATA_COMPONENTS.register(modEventBus);
        ATTACHMENT_TYPES.register(modEventBus);
        modEventBus.addListener(this::registerCapabilities);
        modEventBus.addListener(this::registerScreens);
    }

    private void registerScreens(RegisterMenuScreensEvent event) {
        event.register(DISTILLATION_CONTROLLER_MENU.get(), DistillationTowerControllerScreen::new);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, FERMENTER_BE.get(),
                (be, side) -> be.getInputSlot());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, FERMENTER_BE.get(),
                (be, side) -> be.getFluidCapability());
        event.registerBlockEntity(mekanism.common.capabilities.Capabilities.HEAT, FERMENTER_BE.get(),
                (be, side) -> be);

        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, DISTILLATION_VALVE_BE.get(),
                (be, side) -> be.getFluidCapability());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DISTILLATION_VALVE_BE.get(),
                (be, side) -> be.getItemCapability());
        // Heat is taken on the valves, not the controller: conductors hook into the tower wall.
        event.registerBlockEntity(mekanism.common.capabilities.Capabilities.HEAT, DISTILLATION_VALVE_BE.get(),
                (be, side) -> be);
    }
}
