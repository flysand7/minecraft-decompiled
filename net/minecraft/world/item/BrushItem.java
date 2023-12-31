package net.minecraft.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class BrushItem extends Item {
   public static final int ANIMATION_DURATION = 10;
   private static final int USE_DURATION = 200;
   private static final double MAX_BRUSH_DISTANCE;

   public BrushItem(Item.Properties var1) {
      super(var1);
   }

   public InteractionResult useOn(UseOnContext var1) {
      Player var2 = var1.getPlayer();
      if (var2 != null && this.calculateHitResult(var2).getType() == HitResult.Type.BLOCK) {
         var2.startUsingItem(var1.getHand());
      }

      return InteractionResult.CONSUME;
   }

   public UseAnim getUseAnimation(ItemStack var1) {
      return UseAnim.BRUSH;
   }

   public int getUseDuration(ItemStack var1) {
      return 200;
   }

   public void onUseTick(Level var1, LivingEntity var2, ItemStack var3, int var4) {
      if (var4 >= 0 && var2 instanceof Player) {
         Player var5 = (Player)var2;
         HitResult var6 = this.calculateHitResult(var2);
         if (var6 instanceof BlockHitResult) {
            BlockHitResult var7 = (BlockHitResult)var6;
            if (var6.getType() == HitResult.Type.BLOCK) {
               int var8 = this.getUseDuration(var3) - var4 + 1;
               boolean var9 = var8 % 10 == 5;
               if (var9) {
                  BlockPos var10 = var7.getBlockPos();
                  BlockState var11 = var1.getBlockState(var10);
                  HumanoidArm var12 = var2.getUsedItemHand() == InteractionHand.MAIN_HAND ? var5.getMainArm() : var5.getMainArm().getOpposite();
                  this.spawnDustParticles(var1, var7, var11, var2.getViewVector(0.0F), var12);
                  Block var15 = var11.getBlock();
                  SoundEvent var13;
                  if (var15 instanceof BrushableBlock) {
                     BrushableBlock var14 = (BrushableBlock)var15;
                     var13 = var14.getBrushSound();
                  } else {
                     var13 = SoundEvents.BRUSH_GENERIC;
                  }

                  var1.playSound(var5, var10, var13, SoundSource.BLOCKS);
                  if (!var1.isClientSide()) {
                     BlockEntity var18 = var1.getBlockEntity(var10);
                     if (var18 instanceof BrushableBlockEntity) {
                        BrushableBlockEntity var17 = (BrushableBlockEntity)var18;
                        boolean var19 = var17.brush(var1.getGameTime(), var5, var7.getDirection());
                        if (var19) {
                           EquipmentSlot var16 = var3.equals(var5.getItemBySlot(EquipmentSlot.OFFHAND)) ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
                           var3.hurtAndBreak(1, var2, (var1x) -> {
                              var1x.broadcastBreakEvent(var16);
                           });
                        }
                     }
                  }
               }

               return;
            }
         }

         var2.releaseUsingItem();
      } else {
         var2.releaseUsingItem();
      }
   }

   private HitResult calculateHitResult(LivingEntity var1) {
      return ProjectileUtil.getHitResultOnViewVector(var1, (var0) -> {
         return !var0.isSpectator() && var0.isPickable();
      }, MAX_BRUSH_DISTANCE);
   }

   public void spawnDustParticles(Level var1, BlockHitResult var2, BlockState var3, Vec3 var4, HumanoidArm var5) {
      double var6 = 3.0D;
      int var8 = var5 == HumanoidArm.RIGHT ? 1 : -1;
      int var9 = var1.getRandom().nextInt(7, 12);
      BlockParticleOption var10 = new BlockParticleOption(ParticleTypes.BLOCK, var3);
      Direction var11 = var2.getDirection();
      BrushItem.DustParticlesDelta var12 = BrushItem.DustParticlesDelta.fromDirection(var4, var11);
      Vec3 var13 = var2.getLocation();

      for(int var14 = 0; var14 < var9; ++var14) {
         var1.addParticle(var10, var13.x - (double)(var11 == Direction.WEST ? 1.0E-6F : 0.0F), var13.y, var13.z - (double)(var11 == Direction.NORTH ? 1.0E-6F : 0.0F), var12.xd() * (double)var8 * 3.0D * var1.getRandom().nextDouble(), 0.0D, var12.zd() * (double)var8 * 3.0D * var1.getRandom().nextDouble());
      }

   }

   static {
      MAX_BRUSH_DISTANCE = Math.sqrt(ServerGamePacketListenerImpl.MAX_INTERACTION_DISTANCE) - 1.0D;
   }

   static record DustParticlesDelta(double a, double b, double c) {
      private final double xd;
      private final double yd;
      private final double zd;
      private static final double ALONG_SIDE_DELTA = 1.0D;
      private static final double OUT_FROM_SIDE_DELTA = 0.1D;

      private DustParticlesDelta(double var1, double var3, double var5) {
         this.xd = var1;
         this.yd = var3;
         this.zd = var5;
      }

      public static BrushItem.DustParticlesDelta fromDirection(Vec3 var0, Direction var1) {
         double var2 = 0.0D;
         BrushItem.DustParticlesDelta var10000;
         switch(var1) {
         case DOWN:
         case UP:
            var10000 = new BrushItem.DustParticlesDelta(var0.z(), 0.0D, -var0.x());
            break;
         case NORTH:
            var10000 = new BrushItem.DustParticlesDelta(1.0D, 0.0D, -0.1D);
            break;
         case SOUTH:
            var10000 = new BrushItem.DustParticlesDelta(-1.0D, 0.0D, 0.1D);
            break;
         case WEST:
            var10000 = new BrushItem.DustParticlesDelta(-0.1D, 0.0D, -1.0D);
            break;
         case EAST:
            var10000 = new BrushItem.DustParticlesDelta(0.1D, 0.0D, 1.0D);
            break;
         default:
            throw new IncompatibleClassChangeError();
         }

         return var10000;
      }

      public double xd() {
         return this.xd;
      }

      public double yd() {
         return this.yd;
      }

      public double zd() {
         return this.zd;
      }
   }
}
