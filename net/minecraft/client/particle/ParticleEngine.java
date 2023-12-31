package net.minecraft.client.particle;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleGroup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.slf4j.Logger;

public class ParticleEngine implements PreparableReloadListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final FileToIdConverter PARTICLE_LISTER = FileToIdConverter.json("particles");
   private static final ResourceLocation PARTICLES_ATLAS_INFO = new ResourceLocation("particles");
   private static final int MAX_PARTICLES_PER_LAYER = 16384;
   private static final List<ParticleRenderType> RENDER_ORDER;
   protected ClientLevel level;
   private final Map<ParticleRenderType, Queue<Particle>> particles = Maps.newIdentityHashMap();
   private final Queue<TrackingEmitter> trackingEmitters = Queues.newArrayDeque();
   private final TextureManager textureManager;
   private final RandomSource random = RandomSource.create();
   private final Int2ObjectMap<ParticleProvider<?>> providers = new Int2ObjectOpenHashMap();
   private final Queue<Particle> particlesToAdd = Queues.newArrayDeque();
   private final Map<ResourceLocation, ParticleEngine.MutableSpriteSet> spriteSets = Maps.newHashMap();
   private final TextureAtlas textureAtlas;
   private final Object2IntOpenHashMap<ParticleGroup> trackedParticleCounts = new Object2IntOpenHashMap();

   public ParticleEngine(ClientLevel var1, TextureManager var2) {
      this.textureAtlas = new TextureAtlas(TextureAtlas.LOCATION_PARTICLES);
      var2.register((ResourceLocation)this.textureAtlas.location(), (AbstractTexture)this.textureAtlas);
      this.level = var1;
      this.textureManager = var2;
      this.registerProviders();
   }

   private void registerProviders() {
      this.register(ParticleTypes.AMBIENT_ENTITY_EFFECT, (ParticleEngine.SpriteParticleRegistration)(SpellParticle.AmbientMobProvider::new));
      this.register(ParticleTypes.ANGRY_VILLAGER, (ParticleEngine.SpriteParticleRegistration)(HeartParticle.AngryVillagerProvider::new));
      this.register(ParticleTypes.BLOCK_MARKER, (ParticleProvider)(new BlockMarker.Provider()));
      this.register(ParticleTypes.BLOCK, (ParticleProvider)(new TerrainParticle.Provider()));
      this.register(ParticleTypes.BUBBLE, (ParticleEngine.SpriteParticleRegistration)(BubbleParticle.Provider::new));
      this.register(ParticleTypes.BUBBLE_COLUMN_UP, (ParticleEngine.SpriteParticleRegistration)(BubbleColumnUpParticle.Provider::new));
      this.register(ParticleTypes.BUBBLE_POP, (ParticleEngine.SpriteParticleRegistration)(BubblePopParticle.Provider::new));
      this.register(ParticleTypes.CAMPFIRE_COSY_SMOKE, (ParticleEngine.SpriteParticleRegistration)(CampfireSmokeParticle.CosyProvider::new));
      this.register(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, (ParticleEngine.SpriteParticleRegistration)(CampfireSmokeParticle.SignalProvider::new));
      this.register(ParticleTypes.CLOUD, (ParticleEngine.SpriteParticleRegistration)(PlayerCloudParticle.Provider::new));
      this.register(ParticleTypes.COMPOSTER, (ParticleEngine.SpriteParticleRegistration)(SuspendedTownParticle.ComposterFillProvider::new));
      this.register(ParticleTypes.CRIT, (ParticleEngine.SpriteParticleRegistration)(CritParticle.Provider::new));
      this.register(ParticleTypes.CURRENT_DOWN, (ParticleEngine.SpriteParticleRegistration)(WaterCurrentDownParticle.Provider::new));
      this.register(ParticleTypes.DAMAGE_INDICATOR, (ParticleEngine.SpriteParticleRegistration)(CritParticle.DamageIndicatorProvider::new));
      this.register(ParticleTypes.DRAGON_BREATH, (ParticleEngine.SpriteParticleRegistration)(DragonBreathParticle.Provider::new));
      this.register(ParticleTypes.DOLPHIN, (ParticleEngine.SpriteParticleRegistration)(SuspendedTownParticle.DolphinSpeedProvider::new));
      this.register(ParticleTypes.DRIPPING_LAVA, (ParticleProvider.Sprite)(DripParticle::createLavaHangParticle));
      this.register(ParticleTypes.FALLING_LAVA, (ParticleProvider.Sprite)(DripParticle::createLavaFallParticle));
      this.register(ParticleTypes.LANDING_LAVA, (ParticleProvider.Sprite)(DripParticle::createLavaLandParticle));
      this.register(ParticleTypes.DRIPPING_WATER, (ParticleProvider.Sprite)(DripParticle::createWaterHangParticle));
      this.register(ParticleTypes.FALLING_WATER, (ParticleProvider.Sprite)(DripParticle::createWaterFallParticle));
      this.register(ParticleTypes.DUST, DustParticle.Provider::new);
      this.register(ParticleTypes.DUST_COLOR_TRANSITION, DustColorTransitionParticle.Provider::new);
      this.register(ParticleTypes.EFFECT, (ParticleEngine.SpriteParticleRegistration)(SpellParticle.Provider::new));
      this.register(ParticleTypes.ELDER_GUARDIAN, (ParticleProvider)(new MobAppearanceParticle.Provider()));
      this.register(ParticleTypes.ENCHANTED_HIT, (ParticleEngine.SpriteParticleRegistration)(CritParticle.MagicProvider::new));
      this.register(ParticleTypes.ENCHANT, (ParticleEngine.SpriteParticleRegistration)(EnchantmentTableParticle.Provider::new));
      this.register(ParticleTypes.END_ROD, (ParticleEngine.SpriteParticleRegistration)(EndRodParticle.Provider::new));
      this.register(ParticleTypes.ENTITY_EFFECT, (ParticleEngine.SpriteParticleRegistration)(SpellParticle.MobProvider::new));
      this.register(ParticleTypes.EXPLOSION_EMITTER, (ParticleProvider)(new HugeExplosionSeedParticle.Provider()));
      this.register(ParticleTypes.EXPLOSION, (ParticleEngine.SpriteParticleRegistration)(HugeExplosionParticle.Provider::new));
      this.register(ParticleTypes.SONIC_BOOM, (ParticleEngine.SpriteParticleRegistration)(SonicBoomParticle.Provider::new));
      this.register(ParticleTypes.FALLING_DUST, FallingDustParticle.Provider::new);
      this.register(ParticleTypes.FIREWORK, (ParticleEngine.SpriteParticleRegistration)(FireworkParticles.SparkProvider::new));
      this.register(ParticleTypes.FISHING, (ParticleEngine.SpriteParticleRegistration)(WakeParticle.Provider::new));
      this.register(ParticleTypes.FLAME, (ParticleEngine.SpriteParticleRegistration)(FlameParticle.Provider::new));
      this.register(ParticleTypes.SCULK_SOUL, (ParticleEngine.SpriteParticleRegistration)(SoulParticle.EmissiveProvider::new));
      this.register(ParticleTypes.SCULK_CHARGE, SculkChargeParticle.Provider::new);
      this.register(ParticleTypes.SCULK_CHARGE_POP, (ParticleEngine.SpriteParticleRegistration)(SculkChargePopParticle.Provider::new));
      this.register(ParticleTypes.SOUL, (ParticleEngine.SpriteParticleRegistration)(SoulParticle.Provider::new));
      this.register(ParticleTypes.SOUL_FIRE_FLAME, (ParticleEngine.SpriteParticleRegistration)(FlameParticle.Provider::new));
      this.register(ParticleTypes.FLASH, (ParticleEngine.SpriteParticleRegistration)(FireworkParticles.FlashProvider::new));
      this.register(ParticleTypes.HAPPY_VILLAGER, (ParticleEngine.SpriteParticleRegistration)(SuspendedTownParticle.HappyVillagerProvider::new));
      this.register(ParticleTypes.HEART, (ParticleEngine.SpriteParticleRegistration)(HeartParticle.Provider::new));
      this.register(ParticleTypes.INSTANT_EFFECT, (ParticleEngine.SpriteParticleRegistration)(SpellParticle.InstantProvider::new));
      this.register(ParticleTypes.ITEM, (ParticleProvider)(new BreakingItemParticle.Provider()));
      this.register(ParticleTypes.ITEM_SLIME, (ParticleProvider)(new BreakingItemParticle.SlimeProvider()));
      this.register(ParticleTypes.ITEM_SNOWBALL, (ParticleProvider)(new BreakingItemParticle.SnowballProvider()));
      this.register(ParticleTypes.LARGE_SMOKE, (ParticleEngine.SpriteParticleRegistration)(LargeSmokeParticle.Provider::new));
      this.register(ParticleTypes.LAVA, (ParticleEngine.SpriteParticleRegistration)(LavaParticle.Provider::new));
      this.register(ParticleTypes.MYCELIUM, (ParticleEngine.SpriteParticleRegistration)(SuspendedTownParticle.Provider::new));
      this.register(ParticleTypes.NAUTILUS, (ParticleEngine.SpriteParticleRegistration)(EnchantmentTableParticle.NautilusProvider::new));
      this.register(ParticleTypes.NOTE, (ParticleEngine.SpriteParticleRegistration)(NoteParticle.Provider::new));
      this.register(ParticleTypes.POOF, (ParticleEngine.SpriteParticleRegistration)(ExplodeParticle.Provider::new));
      this.register(ParticleTypes.PORTAL, (ParticleEngine.SpriteParticleRegistration)(PortalParticle.Provider::new));
      this.register(ParticleTypes.RAIN, (ParticleEngine.SpriteParticleRegistration)(WaterDropParticle.Provider::new));
      this.register(ParticleTypes.SMOKE, (ParticleEngine.SpriteParticleRegistration)(SmokeParticle.Provider::new));
      this.register(ParticleTypes.SNEEZE, (ParticleEngine.SpriteParticleRegistration)(PlayerCloudParticle.SneezeProvider::new));
      this.register(ParticleTypes.SNOWFLAKE, (ParticleEngine.SpriteParticleRegistration)(SnowflakeParticle.Provider::new));
      this.register(ParticleTypes.SPIT, (ParticleEngine.SpriteParticleRegistration)(SpitParticle.Provider::new));
      this.register(ParticleTypes.SWEEP_ATTACK, (ParticleEngine.SpriteParticleRegistration)(AttackSweepParticle.Provider::new));
      this.register(ParticleTypes.TOTEM_OF_UNDYING, (ParticleEngine.SpriteParticleRegistration)(TotemParticle.Provider::new));
      this.register(ParticleTypes.SQUID_INK, (ParticleEngine.SpriteParticleRegistration)(SquidInkParticle.Provider::new));
      this.register(ParticleTypes.UNDERWATER, (ParticleEngine.SpriteParticleRegistration)(SuspendedParticle.UnderwaterProvider::new));
      this.register(ParticleTypes.SPLASH, (ParticleEngine.SpriteParticleRegistration)(SplashParticle.Provider::new));
      this.register(ParticleTypes.WITCH, (ParticleEngine.SpriteParticleRegistration)(SpellParticle.WitchProvider::new));
      this.register(ParticleTypes.DRIPPING_HONEY, (ParticleProvider.Sprite)(DripParticle::createHoneyHangParticle));
      this.register(ParticleTypes.FALLING_HONEY, (ParticleProvider.Sprite)(DripParticle::createHoneyFallParticle));
      this.register(ParticleTypes.LANDING_HONEY, (ParticleProvider.Sprite)(DripParticle::createHoneyLandParticle));
      this.register(ParticleTypes.FALLING_NECTAR, (ParticleProvider.Sprite)(DripParticle::createNectarFallParticle));
      this.register(ParticleTypes.FALLING_SPORE_BLOSSOM, (ParticleProvider.Sprite)(DripParticle::createSporeBlossomFallParticle));
      this.register(ParticleTypes.SPORE_BLOSSOM_AIR, (ParticleEngine.SpriteParticleRegistration)(SuspendedParticle.SporeBlossomAirProvider::new));
      this.register(ParticleTypes.ASH, (ParticleEngine.SpriteParticleRegistration)(AshParticle.Provider::new));
      this.register(ParticleTypes.CRIMSON_SPORE, (ParticleEngine.SpriteParticleRegistration)(SuspendedParticle.CrimsonSporeProvider::new));
      this.register(ParticleTypes.WARPED_SPORE, (ParticleEngine.SpriteParticleRegistration)(SuspendedParticle.WarpedSporeProvider::new));
      this.register(ParticleTypes.DRIPPING_OBSIDIAN_TEAR, (ParticleProvider.Sprite)(DripParticle::createObsidianTearHangParticle));
      this.register(ParticleTypes.FALLING_OBSIDIAN_TEAR, (ParticleProvider.Sprite)(DripParticle::createObsidianTearFallParticle));
      this.register(ParticleTypes.LANDING_OBSIDIAN_TEAR, (ParticleProvider.Sprite)(DripParticle::createObsidianTearLandParticle));
      this.register(ParticleTypes.REVERSE_PORTAL, (ParticleEngine.SpriteParticleRegistration)(ReversePortalParticle.ReversePortalProvider::new));
      this.register(ParticleTypes.WHITE_ASH, (ParticleEngine.SpriteParticleRegistration)(WhiteAshParticle.Provider::new));
      this.register(ParticleTypes.SMALL_FLAME, (ParticleEngine.SpriteParticleRegistration)(FlameParticle.SmallFlameProvider::new));
      this.register(ParticleTypes.DRIPPING_DRIPSTONE_WATER, (ParticleProvider.Sprite)(DripParticle::createDripstoneWaterHangParticle));
      this.register(ParticleTypes.FALLING_DRIPSTONE_WATER, (ParticleProvider.Sprite)(DripParticle::createDripstoneWaterFallParticle));
      this.register(ParticleTypes.CHERRY_LEAVES, (ParticleEngine.SpriteParticleRegistration)((var0) -> {
         return (var1, var2, var3, var5, var7, var9, var11, var13) -> {
            return new CherryParticle(var2, var3, var5, var7, var0);
         };
      }));
      this.register(ParticleTypes.DRIPPING_DRIPSTONE_LAVA, (ParticleProvider.Sprite)(DripParticle::createDripstoneLavaHangParticle));
      this.register(ParticleTypes.FALLING_DRIPSTONE_LAVA, (ParticleProvider.Sprite)(DripParticle::createDripstoneLavaFallParticle));
      this.register(ParticleTypes.VIBRATION, VibrationSignalParticle.Provider::new);
      this.register(ParticleTypes.GLOW_SQUID_INK, (ParticleEngine.SpriteParticleRegistration)(SquidInkParticle.GlowInkProvider::new));
      this.register(ParticleTypes.GLOW, (ParticleEngine.SpriteParticleRegistration)(GlowParticle.GlowSquidProvider::new));
      this.register(ParticleTypes.WAX_ON, (ParticleEngine.SpriteParticleRegistration)(GlowParticle.WaxOnProvider::new));
      this.register(ParticleTypes.WAX_OFF, (ParticleEngine.SpriteParticleRegistration)(GlowParticle.WaxOffProvider::new));
      this.register(ParticleTypes.ELECTRIC_SPARK, (ParticleEngine.SpriteParticleRegistration)(GlowParticle.ElectricSparkProvider::new));
      this.register(ParticleTypes.SCRAPE, (ParticleEngine.SpriteParticleRegistration)(GlowParticle.ScrapeProvider::new));
      this.register(ParticleTypes.SHRIEK, ShriekParticle.Provider::new);
      this.register(ParticleTypes.EGG_CRACK, (ParticleEngine.SpriteParticleRegistration)(SuspendedTownParticle.EggCrackProvider::new));
   }

   private <T extends ParticleOptions> void register(ParticleType<T> var1, ParticleProvider<T> var2) {
      this.providers.put(BuiltInRegistries.PARTICLE_TYPE.getId(var1), var2);
   }

   private <T extends ParticleOptions> void register(ParticleType<T> var1, ParticleProvider.Sprite<T> var2) {
      this.register(var1, (var1x) -> {
         return (var2x, var3, var4, var6, var8, var10, var12, var14) -> {
            TextureSheetParticle var16 = var2.createParticle(var2x, var3, var4, var6, var8, var10, var12, var14);
            if (var16 != null) {
               var16.pickSprite(var1x);
            }

            return var16;
         };
      });
   }

   private <T extends ParticleOptions> void register(ParticleType<T> var1, ParticleEngine.SpriteParticleRegistration<T> var2) {
      ParticleEngine.MutableSpriteSet var3 = new ParticleEngine.MutableSpriteSet();
      this.spriteSets.put(BuiltInRegistries.PARTICLE_TYPE.getKey(var1), var3);
      this.providers.put(BuiltInRegistries.PARTICLE_TYPE.getId(var1), var2.create(var3));
   }

   public CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier var1, ResourceManager var2, ProfilerFiller var3, ProfilerFiller var4, Executor var5, Executor var6) {
      CompletableFuture var7 = CompletableFuture.supplyAsync(() -> {
         return PARTICLE_LISTER.listMatchingResources(var2);
      }, var5).thenCompose((var2x) -> {
         ArrayList var3 = new ArrayList(var2x.size());
         var2x.forEach((var3x, var4) -> {
            ResourceLocation var5x = PARTICLE_LISTER.fileToId(var3x);
            var3.add(CompletableFuture.supplyAsync(() -> {
               record ParticleDefinition(ResourceLocation a, Optional<List<ResourceLocation>> b) {
                  private final ResourceLocation id;
                  private final Optional<List<ResourceLocation>> sprites;

                  ParticleDefinition(ResourceLocation var1, Optional<List<ResourceLocation>> var2) {
                     this.id = var1;
                     this.sprites = var2;
                  }

                  public ResourceLocation id() {
                     return this.id;
                  }

                  public Optional<List<ResourceLocation>> sprites() {
                     return this.sprites;
                  }
               }

               return new ParticleDefinition(var5x, this.loadParticleDescription(var5x, var4));
            }, var5));
         });
         return Util.sequence(var3);
      });
      CompletableFuture var8 = SpriteLoader.create(this.textureAtlas).loadAndStitch(var2, PARTICLES_ATLAS_INFO, 0, var5).thenCompose(SpriteLoader.Preparations::waitForUpload);
      CompletableFuture var10000 = CompletableFuture.allOf(var8, var7);
      Objects.requireNonNull(var1);
      return var10000.thenCompose(var1::wait).thenAcceptAsync((var4x) -> {
         this.clearParticles();
         var4.startTick();
         var4.push("upload");
         SpriteLoader.Preparations var5 = (SpriteLoader.Preparations)var8.join();
         this.textureAtlas.upload(var5);
         var4.popPush("bindSpriteSets");
         HashSet var6 = new HashSet();
         TextureAtlasSprite var7x = var5.missing();
         ((List)var7.join()).forEach((var4xx) -> {
            Optional var5x = var4xx.sprites();
            if (!var5x.isEmpty()) {
               ArrayList var6x = new ArrayList();
               Iterator var7 = ((List)var5x.get()).iterator();

               while(var7.hasNext()) {
                  ResourceLocation var8 = (ResourceLocation)var7.next();
                  TextureAtlasSprite var9 = (TextureAtlasSprite)var5.regions().get(var8);
                  if (var9 == null) {
                     var6.add(var8);
                     var6x.add(var7x);
                  } else {
                     var6x.add(var9);
                  }
               }

               if (var6x.isEmpty()) {
                  var6x.add(var7x);
               }

               ((ParticleEngine.MutableSpriteSet)this.spriteSets.get(var4xx.id())).rebind(var6x);
            }
         });
         if (!var6.isEmpty()) {
            LOGGER.warn("Missing particle sprites: {}", var6.stream().sorted().map(ResourceLocation::toString).collect(Collectors.joining(",")));
         }

         var4.pop();
         var4.endTick();
      }, var6);
   }

   public void close() {
      this.textureAtlas.clearTextureData();
   }

   private Optional<List<ResourceLocation>> loadParticleDescription(ResourceLocation var1, Resource var2) {
      if (!this.spriteSets.containsKey(var1)) {
         LOGGER.debug("Redundant texture list for particle: {}", var1);
         return Optional.empty();
      } else {
         try {
            BufferedReader var3 = var2.openAsReader();

            Optional var5;
            try {
               ParticleDescription var4 = ParticleDescription.fromJson(GsonHelper.parse((Reader)var3));
               var5 = Optional.of(var4.getTextures());
            } catch (Throwable var7) {
               if (var3 != null) {
                  try {
                     var3.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }
               }

               throw var7;
            }

            if (var3 != null) {
               var3.close();
            }

            return var5;
         } catch (IOException var8) {
            throw new IllegalStateException("Failed to load description for particle " + var1, var8);
         }
      }
   }

   public void createTrackingEmitter(Entity var1, ParticleOptions var2) {
      this.trackingEmitters.add(new TrackingEmitter(this.level, var1, var2));
   }

   public void createTrackingEmitter(Entity var1, ParticleOptions var2, int var3) {
      this.trackingEmitters.add(new TrackingEmitter(this.level, var1, var2, var3));
   }

   @Nullable
   public Particle createParticle(ParticleOptions var1, double var2, double var4, double var6, double var8, double var10, double var12) {
      Particle var14 = this.makeParticle(var1, var2, var4, var6, var8, var10, var12);
      if (var14 != null) {
         this.add(var14);
         return var14;
      } else {
         return null;
      }
   }

   @Nullable
   private <T extends ParticleOptions> Particle makeParticle(T var1, double var2, double var4, double var6, double var8, double var10, double var12) {
      ParticleProvider var14 = (ParticleProvider)this.providers.get(BuiltInRegistries.PARTICLE_TYPE.getId(var1.getType()));
      return var14 == null ? null : var14.createParticle(var1, this.level, var2, var4, var6, var8, var10, var12);
   }

   public void add(Particle var1) {
      Optional var2 = var1.getParticleGroup();
      if (var2.isPresent()) {
         if (this.hasSpaceInParticleLimit((ParticleGroup)var2.get())) {
            this.particlesToAdd.add(var1);
            this.updateCount((ParticleGroup)var2.get(), 1);
         }
      } else {
         this.particlesToAdd.add(var1);
      }

   }

   public void tick() {
      this.particles.forEach((var1x, var2x) -> {
         this.level.getProfiler().push(var1x.toString());
         this.tickParticleList(var2x);
         this.level.getProfiler().pop();
      });
      if (!this.trackingEmitters.isEmpty()) {
         ArrayList var1 = Lists.newArrayList();
         Iterator var2 = this.trackingEmitters.iterator();

         while(var2.hasNext()) {
            TrackingEmitter var3 = (TrackingEmitter)var2.next();
            var3.tick();
            if (!var3.isAlive()) {
               var1.add(var3);
            }
         }

         this.trackingEmitters.removeAll(var1);
      }

      Particle var4;
      if (!this.particlesToAdd.isEmpty()) {
         while((var4 = (Particle)this.particlesToAdd.poll()) != null) {
            ((Queue)this.particles.computeIfAbsent(var4.getRenderType(), (var0) -> {
               return EvictingQueue.create(16384);
            })).add(var4);
         }
      }

   }

   private void tickParticleList(Collection<Particle> var1) {
      if (!var1.isEmpty()) {
         Iterator var2 = var1.iterator();

         while(var2.hasNext()) {
            Particle var3 = (Particle)var2.next();
            this.tickParticle(var3);
            if (!var3.isAlive()) {
               var3.getParticleGroup().ifPresent((var1x) -> {
                  this.updateCount(var1x, -1);
               });
               var2.remove();
            }
         }
      }

   }

   private void updateCount(ParticleGroup var1, int var2) {
      this.trackedParticleCounts.addTo(var1, var2);
   }

   private void tickParticle(Particle var1) {
      try {
         var1.tick();
      } catch (Throwable var5) {
         CrashReport var3 = CrashReport.forThrowable(var5, "Ticking Particle");
         CrashReportCategory var4 = var3.addCategory("Particle being ticked");
         Objects.requireNonNull(var1);
         var4.setDetail("Particle", var1::toString);
         ParticleRenderType var10002 = var1.getRenderType();
         Objects.requireNonNull(var10002);
         var4.setDetail("Particle Type", var10002::toString);
         throw new ReportedException(var3);
      }
   }

   public void render(PoseStack var1, MultiBufferSource.BufferSource var2, LightTexture var3, Camera var4, float var5) {
      var3.turnOnLightLayer();
      RenderSystem.enableDepthTest();
      PoseStack var6 = RenderSystem.getModelViewStack();
      var6.pushPose();
      var6.mulPoseMatrix(var1.last().pose());
      RenderSystem.applyModelViewMatrix();
      Iterator var7 = RENDER_ORDER.iterator();

      while(true) {
         ParticleRenderType var8;
         Iterable var9;
         do {
            if (!var7.hasNext()) {
               var6.popPose();
               RenderSystem.applyModelViewMatrix();
               RenderSystem.depthMask(true);
               RenderSystem.disableBlend();
               var3.turnOffLightLayer();
               return;
            }

            var8 = (ParticleRenderType)var7.next();
            var9 = (Iterable)this.particles.get(var8);
         } while(var9 == null);

         RenderSystem.setShader(GameRenderer::getParticleShader);
         Tesselator var10 = Tesselator.getInstance();
         BufferBuilder var11 = var10.getBuilder();
         var8.begin(var11, this.textureManager);
         Iterator var12 = var9.iterator();

         while(var12.hasNext()) {
            Particle var13 = (Particle)var12.next();

            try {
               var13.render(var11, var4, var5);
            } catch (Throwable var17) {
               CrashReport var15 = CrashReport.forThrowable(var17, "Rendering Particle");
               CrashReportCategory var16 = var15.addCategory("Particle being rendered");
               Objects.requireNonNull(var13);
               var16.setDetail("Particle", var13::toString);
               Objects.requireNonNull(var8);
               var16.setDetail("Particle Type", var8::toString);
               throw new ReportedException(var15);
            }
         }

         var8.end(var10);
      }
   }

   public void setLevel(@Nullable ClientLevel var1) {
      this.level = var1;
      this.clearParticles();
      this.trackingEmitters.clear();
   }

   public void destroy(BlockPos var1, BlockState var2) {
      if (!var2.isAir() && var2.shouldSpawnParticlesOnBreak()) {
         VoxelShape var3 = var2.getShape(this.level, var1);
         double var4 = 0.25D;
         var3.forAllBoxes((var3x, var5, var7, var9, var11, var13) -> {
            double var15 = Math.min(1.0D, var9 - var3x);
            double var17 = Math.min(1.0D, var11 - var5);
            double var19 = Math.min(1.0D, var13 - var7);
            int var21 = Math.max(2, Mth.ceil(var15 / 0.25D));
            int var22 = Math.max(2, Mth.ceil(var17 / 0.25D));
            int var23 = Math.max(2, Mth.ceil(var19 / 0.25D));

            for(int var24 = 0; var24 < var21; ++var24) {
               for(int var25 = 0; var25 < var22; ++var25) {
                  for(int var26 = 0; var26 < var23; ++var26) {
                     double var27 = ((double)var24 + 0.5D) / (double)var21;
                     double var29 = ((double)var25 + 0.5D) / (double)var22;
                     double var31 = ((double)var26 + 0.5D) / (double)var23;
                     double var33 = var27 * var15 + var3x;
                     double var35 = var29 * var17 + var5;
                     double var37 = var31 * var19 + var7;
                     this.add(new TerrainParticle(this.level, (double)var1.getX() + var33, (double)var1.getY() + var35, (double)var1.getZ() + var37, var27 - 0.5D, var29 - 0.5D, var31 - 0.5D, var2, var1));
                  }
               }
            }

         });
      }
   }

   public void crack(BlockPos var1, Direction var2) {
      BlockState var3 = this.level.getBlockState(var1);
      if (var3.getRenderShape() != RenderShape.INVISIBLE) {
         int var4 = var1.getX();
         int var5 = var1.getY();
         int var6 = var1.getZ();
         float var7 = 0.1F;
         AABB var8 = var3.getShape(this.level, var1).bounds();
         double var9 = (double)var4 + this.random.nextDouble() * (var8.maxX - var8.minX - 0.20000000298023224D) + 0.10000000149011612D + var8.minX;
         double var11 = (double)var5 + this.random.nextDouble() * (var8.maxY - var8.minY - 0.20000000298023224D) + 0.10000000149011612D + var8.minY;
         double var13 = (double)var6 + this.random.nextDouble() * (var8.maxZ - var8.minZ - 0.20000000298023224D) + 0.10000000149011612D + var8.minZ;
         if (var2 == Direction.DOWN) {
            var11 = (double)var5 + var8.minY - 0.10000000149011612D;
         }

         if (var2 == Direction.UP) {
            var11 = (double)var5 + var8.maxY + 0.10000000149011612D;
         }

         if (var2 == Direction.NORTH) {
            var13 = (double)var6 + var8.minZ - 0.10000000149011612D;
         }

         if (var2 == Direction.SOUTH) {
            var13 = (double)var6 + var8.maxZ + 0.10000000149011612D;
         }

         if (var2 == Direction.WEST) {
            var9 = (double)var4 + var8.minX - 0.10000000149011612D;
         }

         if (var2 == Direction.EAST) {
            var9 = (double)var4 + var8.maxX + 0.10000000149011612D;
         }

         this.add((new TerrainParticle(this.level, var9, var11, var13, 0.0D, 0.0D, 0.0D, var3, var1)).setPower(0.2F).scale(0.6F));
      }
   }

   public String countParticles() {
      return String.valueOf(this.particles.values().stream().mapToInt(Collection::size).sum());
   }

   private boolean hasSpaceInParticleLimit(ParticleGroup var1) {
      return this.trackedParticleCounts.getInt(var1) < var1.getLimit();
   }

   private void clearParticles() {
      this.particles.clear();
      this.particlesToAdd.clear();
      this.trackingEmitters.clear();
      this.trackedParticleCounts.clear();
   }

   static {
      RENDER_ORDER = ImmutableList.of(ParticleRenderType.TERRAIN_SHEET, ParticleRenderType.PARTICLE_SHEET_OPAQUE, ParticleRenderType.PARTICLE_SHEET_LIT, ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT, ParticleRenderType.CUSTOM);
   }

   @FunctionalInterface
   private interface SpriteParticleRegistration<T extends ParticleOptions> {
      ParticleProvider<T> create(SpriteSet var1);
   }

   private static class MutableSpriteSet implements SpriteSet {
      private List<TextureAtlasSprite> sprites;

      MutableSpriteSet() {
      }

      public TextureAtlasSprite get(int var1, int var2) {
         return (TextureAtlasSprite)this.sprites.get(var1 * (this.sprites.size() - 1) / var2);
      }

      public TextureAtlasSprite get(RandomSource var1) {
         return (TextureAtlasSprite)this.sprites.get(var1.nextInt(this.sprites.size()));
      }

      public void rebind(List<TextureAtlasSprite> var1) {
         this.sprites = ImmutableList.copyOf(var1);
      }
   }
}
