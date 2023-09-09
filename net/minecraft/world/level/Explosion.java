package net.minecraft.world.level;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ProtectionEnchantment;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class Explosion {
   private static final ExplosionDamageCalculator EXPLOSION_DAMAGE_CALCULATOR = new ExplosionDamageCalculator();
   private static final int MAX_DROPS_PER_COMBINED_STACK = 16;
   private final boolean fire;
   private final Explosion.BlockInteraction blockInteraction;
   private final RandomSource random;
   private final Level level;
   private final double x;
   private final double y;
   private final double z;
   @Nullable
   private final Entity source;
   private final float radius;
   private final DamageSource damageSource;
   private final ExplosionDamageCalculator damageCalculator;
   private final ObjectArrayList<BlockPos> toBlow;
   private final Map<Player, Vec3> hitPlayers;

   public Explosion(Level var1, @Nullable Entity var2, double var3, double var5, double var7, float var9, List<BlockPos> var10) {
      this(var1, var2, var3, var5, var7, var9, false, Explosion.BlockInteraction.DESTROY_WITH_DECAY, var10);
   }

   public Explosion(Level var1, @Nullable Entity var2, double var3, double var5, double var7, float var9, boolean var10, Explosion.BlockInteraction var11, List<BlockPos> var12) {
      this(var1, var2, var3, var5, var7, var9, var10, var11);
      this.toBlow.addAll(var12);
   }

   public Explosion(Level var1, @Nullable Entity var2, double var3, double var5, double var7, float var9, boolean var10, Explosion.BlockInteraction var11) {
      this(var1, var2, (DamageSource)null, (ExplosionDamageCalculator)null, var3, var5, var7, var9, var10, var11);
   }

   public Explosion(Level var1, @Nullable Entity var2, @Nullable DamageSource var3, @Nullable ExplosionDamageCalculator var4, double var5, double var7, double var9, float var11, boolean var12, Explosion.BlockInteraction var13) {
      this.random = RandomSource.create();
      this.toBlow = new ObjectArrayList();
      this.hitPlayers = Maps.newHashMap();
      this.level = var1;
      this.source = var2;
      this.radius = var11;
      this.x = var5;
      this.y = var7;
      this.z = var9;
      this.fire = var12;
      this.blockInteraction = var13;
      this.damageSource = var3 == null ? var1.damageSources().explosion(this) : var3;
      this.damageCalculator = var4 == null ? this.makeDamageCalculator(var2) : var4;
   }

   private ExplosionDamageCalculator makeDamageCalculator(@Nullable Entity var1) {
      return (ExplosionDamageCalculator)(var1 == null ? EXPLOSION_DAMAGE_CALCULATOR : new EntityBasedExplosionDamageCalculator(var1));
   }

   public static float getSeenPercent(Vec3 var0, Entity var1) {
      AABB var2 = var1.getBoundingBox();
      double var3 = 1.0D / ((var2.maxX - var2.minX) * 2.0D + 1.0D);
      double var5 = 1.0D / ((var2.maxY - var2.minY) * 2.0D + 1.0D);
      double var7 = 1.0D / ((var2.maxZ - var2.minZ) * 2.0D + 1.0D);
      double var9 = (1.0D - Math.floor(1.0D / var3) * var3) / 2.0D;
      double var11 = (1.0D - Math.floor(1.0D / var7) * var7) / 2.0D;
      if (!(var3 < 0.0D) && !(var5 < 0.0D) && !(var7 < 0.0D)) {
         int var13 = 0;
         int var14 = 0;

         for(double var15 = 0.0D; var15 <= 1.0D; var15 += var3) {
            for(double var17 = 0.0D; var17 <= 1.0D; var17 += var5) {
               for(double var19 = 0.0D; var19 <= 1.0D; var19 += var7) {
                  double var21 = Mth.lerp(var15, var2.minX, var2.maxX);
                  double var23 = Mth.lerp(var17, var2.minY, var2.maxY);
                  double var25 = Mth.lerp(var19, var2.minZ, var2.maxZ);
                  Vec3 var27 = new Vec3(var21 + var9, var23, var25 + var11);
                  if (var1.level().clip(new ClipContext(var27, var0, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, var1)).getType() == HitResult.Type.MISS) {
                     ++var13;
                  }

                  ++var14;
               }
            }
         }

         return (float)var13 / (float)var14;
      } else {
         return 0.0F;
      }
   }

   public void explode() {
      this.level.gameEvent(this.source, GameEvent.EXPLODE, new Vec3(this.x, this.y, this.z));
      HashSet var1 = Sets.newHashSet();
      boolean var2 = true;

      int var4;
      int var5;
      for(int var3 = 0; var3 < 16; ++var3) {
         for(var4 = 0; var4 < 16; ++var4) {
            for(var5 = 0; var5 < 16; ++var5) {
               if (var3 == 0 || var3 == 15 || var4 == 0 || var4 == 15 || var5 == 0 || var5 == 15) {
                  double var6 = (double)((float)var3 / 15.0F * 2.0F - 1.0F);
                  double var8 = (double)((float)var4 / 15.0F * 2.0F - 1.0F);
                  double var10 = (double)((float)var5 / 15.0F * 2.0F - 1.0F);
                  double var12 = Math.sqrt(var6 * var6 + var8 * var8 + var10 * var10);
                  var6 /= var12;
                  var8 /= var12;
                  var10 /= var12;
                  float var14 = this.radius * (0.7F + this.level.random.nextFloat() * 0.6F);
                  double var15 = this.x;
                  double var17 = this.y;
                  double var19 = this.z;

                  for(float var21 = 0.3F; var14 > 0.0F; var14 -= 0.22500001F) {
                     BlockPos var22 = BlockPos.containing(var15, var17, var19);
                     BlockState var23 = this.level.getBlockState(var22);
                     FluidState var24 = this.level.getFluidState(var22);
                     if (!this.level.isInWorldBounds(var22)) {
                        break;
                     }

                     Optional var25 = this.damageCalculator.getBlockExplosionResistance(this, this.level, var22, var23, var24);
                     if (var25.isPresent()) {
                        var14 -= ((Float)var25.get() + 0.3F) * 0.3F;
                     }

                     if (var14 > 0.0F && this.damageCalculator.shouldBlockExplode(this, this.level, var22, var23, var14)) {
                        var1.add(var22);
                     }

                     var15 += var6 * 0.30000001192092896D;
                     var17 += var8 * 0.30000001192092896D;
                     var19 += var10 * 0.30000001192092896D;
                  }
               }
            }
         }
      }

      this.toBlow.addAll(var1);
      float var32 = this.radius * 2.0F;
      var4 = Mth.floor(this.x - (double)var32 - 1.0D);
      var5 = Mth.floor(this.x + (double)var32 + 1.0D);
      int var33 = Mth.floor(this.y - (double)var32 - 1.0D);
      int var7 = Mth.floor(this.y + (double)var32 + 1.0D);
      int var34 = Mth.floor(this.z - (double)var32 - 1.0D);
      int var9 = Mth.floor(this.z + (double)var32 + 1.0D);
      List var35 = this.level.getEntities(this.source, new AABB((double)var4, (double)var33, (double)var34, (double)var5, (double)var7, (double)var9));
      Vec3 var11 = new Vec3(this.x, this.y, this.z);

      for(int var36 = 0; var36 < var35.size(); ++var36) {
         Entity var13 = (Entity)var35.get(var36);
         if (!var13.ignoreExplosion()) {
            double var37 = Math.sqrt(var13.distanceToSqr(var11)) / (double)var32;
            if (var37 <= 1.0D) {
               double var16 = var13.getX() - this.x;
               double var18 = (var13 instanceof PrimedTnt ? var13.getY() : var13.getEyeY()) - this.y;
               double var20 = var13.getZ() - this.z;
               double var38 = Math.sqrt(var16 * var16 + var18 * var18 + var20 * var20);
               if (var38 != 0.0D) {
                  var16 /= var38;
                  var18 /= var38;
                  var20 /= var38;
                  double var39 = (double)getSeenPercent(var11, var13);
                  double var26 = (1.0D - var37) * var39;
                  var13.hurt(this.getDamageSource(), (float)((int)((var26 * var26 + var26) / 2.0D * 7.0D * (double)var32 + 1.0D)));
                  double var28;
                  if (var13 instanceof LivingEntity) {
                     LivingEntity var30 = (LivingEntity)var13;
                     var28 = ProtectionEnchantment.getExplosionKnockbackAfterDampener(var30, var26);
                  } else {
                     var28 = var26;
                  }

                  var16 *= var28;
                  var18 *= var28;
                  var20 *= var28;
                  Vec3 var40 = new Vec3(var16, var18, var20);
                  var13.setDeltaMovement(var13.getDeltaMovement().add(var40));
                  if (var13 instanceof Player) {
                     Player var31 = (Player)var13;
                     if (!var31.isSpectator() && (!var31.isCreative() || !var31.getAbilities().flying)) {
                        this.hitPlayers.put(var31, var40);
                     }
                  }
               }
            }
         }
      }

   }

   public void finalizeExplosion(boolean var1) {
      if (this.level.isClientSide) {
         this.level.playLocalSound(this.x, this.y, this.z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4.0F, (1.0F + (this.level.random.nextFloat() - this.level.random.nextFloat()) * 0.2F) * 0.7F, false);
      }

      boolean var2 = this.interactsWithBlocks();
      if (var1) {
         if (!(this.radius < 2.0F) && var2) {
            this.level.addParticle(ParticleTypes.EXPLOSION_EMITTER, this.x, this.y, this.z, 1.0D, 0.0D, 0.0D);
         } else {
            this.level.addParticle(ParticleTypes.EXPLOSION, this.x, this.y, this.z, 1.0D, 0.0D, 0.0D);
         }
      }

      if (var2) {
         ObjectArrayList var3 = new ObjectArrayList();
         boolean var4 = this.getIndirectSourceEntity() instanceof Player;
         Util.shuffle(this.toBlow, this.level.random);
         ObjectListIterator var5 = this.toBlow.iterator();

         while(var5.hasNext()) {
            BlockPos var6 = (BlockPos)var5.next();
            BlockState var7 = this.level.getBlockState(var6);
            Block var8 = var7.getBlock();
            if (!var7.isAir()) {
               BlockPos var9 = var6.immutable();
               this.level.getProfiler().push("explosion_blocks");
               if (var8.dropFromExplosion(this)) {
                  Level var11 = this.level;
                  if (var11 instanceof ServerLevel) {
                     ServerLevel var10 = (ServerLevel)var11;
                     BlockEntity var16 = var7.hasBlockEntity() ? this.level.getBlockEntity(var6) : null;
                     LootParams.Builder var12 = (new LootParams.Builder(var10)).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(var6)).withParameter(LootContextParams.TOOL, ItemStack.EMPTY).withOptionalParameter(LootContextParams.BLOCK_ENTITY, var16).withOptionalParameter(LootContextParams.THIS_ENTITY, this.source);
                     if (this.blockInteraction == Explosion.BlockInteraction.DESTROY_WITH_DECAY) {
                        var12.withParameter(LootContextParams.EXPLOSION_RADIUS, this.radius);
                     }

                     var7.spawnAfterBreak(var10, var6, ItemStack.EMPTY, var4);
                     var7.getDrops(var12).forEach((var2x) -> {
                        addBlockDrops(var3, var2x, var9);
                     });
                  }
               }

               this.level.setBlock(var6, Blocks.AIR.defaultBlockState(), 3);
               var8.wasExploded(this.level, var6, this);
               this.level.getProfiler().pop();
            }
         }

         var5 = var3.iterator();

         while(var5.hasNext()) {
            Pair var15 = (Pair)var5.next();
            Block.popResource(this.level, (BlockPos)var15.getSecond(), (ItemStack)var15.getFirst());
         }
      }

      if (this.fire) {
         ObjectListIterator var13 = this.toBlow.iterator();

         while(var13.hasNext()) {
            BlockPos var14 = (BlockPos)var13.next();
            if (this.random.nextInt(3) == 0 && this.level.getBlockState(var14).isAir() && this.level.getBlockState(var14.below()).isSolidRender(this.level, var14.below())) {
               this.level.setBlockAndUpdate(var14, BaseFireBlock.getState(this.level, var14));
            }
         }
      }

   }

   public boolean interactsWithBlocks() {
      return this.blockInteraction != Explosion.BlockInteraction.KEEP;
   }

   private static void addBlockDrops(ObjectArrayList<Pair<ItemStack, BlockPos>> var0, ItemStack var1, BlockPos var2) {
      int var3 = var0.size();

      for(int var4 = 0; var4 < var3; ++var4) {
         Pair var5 = (Pair)var0.get(var4);
         ItemStack var6 = (ItemStack)var5.getFirst();
         if (ItemEntity.areMergable(var6, var1)) {
            ItemStack var7 = ItemEntity.merge(var6, var1, 16);
            var0.set(var4, Pair.of(var7, (BlockPos)var5.getSecond()));
            if (var1.isEmpty()) {
               return;
            }
         }
      }

      var0.add(Pair.of(var1, var2));
   }

   public DamageSource getDamageSource() {
      return this.damageSource;
   }

   public Map<Player, Vec3> getHitPlayers() {
      return this.hitPlayers;
   }

   @Nullable
   public LivingEntity getIndirectSourceEntity() {
      if (this.source == null) {
         return null;
      } else {
         Entity var2 = this.source;
         if (var2 instanceof PrimedTnt) {
            PrimedTnt var5 = (PrimedTnt)var2;
            return var5.getOwner();
         } else {
            var2 = this.source;
            if (var2 instanceof LivingEntity) {
               LivingEntity var4 = (LivingEntity)var2;
               return var4;
            } else {
               var2 = this.source;
               if (var2 instanceof Projectile) {
                  Projectile var1 = (Projectile)var2;
                  var2 = var1.getOwner();
                  if (var2 instanceof LivingEntity) {
                     LivingEntity var3 = (LivingEntity)var2;
                     return var3;
                  }
               }

               return null;
            }
         }
      }
   }

   @Nullable
   public Entity getDirectSourceEntity() {
      return this.source;
   }

   public void clearToBlow() {
      this.toBlow.clear();
   }

   public List<BlockPos> getToBlow() {
      return this.toBlow;
   }

   public static enum BlockInteraction {
      KEEP,
      DESTROY,
      DESTROY_WITH_DECAY;

      private BlockInteraction() {
      }

      // $FF: synthetic method
      private static Explosion.BlockInteraction[] $values() {
         return new Explosion.BlockInteraction[]{KEEP, DESTROY, DESTROY_WITH_DECAY};
      }
   }
}
