package net.minecraft.world.entity.monster;

import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RangedCrossbowAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;

public class Pillager extends AbstractIllager implements CrossbowAttackMob, InventoryCarrier {
   private static final EntityDataAccessor<Boolean> IS_CHARGING_CROSSBOW;
   private static final int INVENTORY_SIZE = 5;
   private static final int SLOT_OFFSET = 300;
   private static final float CROSSBOW_POWER = 1.6F;
   private final SimpleContainer inventory = new SimpleContainer(5);

   public Pillager(EntityType<? extends Pillager> var1, Level var2) {
      super(var1, var2);
   }

   protected void registerGoals() {
      super.registerGoals();
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(2, new Raider.HoldGroundAttackGoal(this, 10.0F));
      this.goalSelector.addGoal(3, new RangedCrossbowAttackGoal(this, 1.0D, 8.0F));
      this.goalSelector.addGoal(8, new RandomStrollGoal(this, 0.6D));
      this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 15.0F, 1.0F));
      this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 15.0F));
      this.targetSelector.addGoal(1, (new HurtByTargetGoal(this, new Class[]{Raider.class})).setAlertOthers());
      this.targetSelector.addGoal(2, new NearestAttackableTargetGoal(this, Player.class, true));
      this.targetSelector.addGoal(3, new NearestAttackableTargetGoal(this, AbstractVillager.class, false));
      this.targetSelector.addGoal(3, new NearestAttackableTargetGoal(this, IronGolem.class, true));
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes().add(Attributes.MOVEMENT_SPEED, 0.3499999940395355D).add(Attributes.MAX_HEALTH, 24.0D).add(Attributes.ATTACK_DAMAGE, 5.0D).add(Attributes.FOLLOW_RANGE, 32.0D);
   }

   protected void defineSynchedData() {
      super.defineSynchedData();
      this.entityData.define(IS_CHARGING_CROSSBOW, false);
   }

   public boolean canFireProjectileWeapon(ProjectileWeaponItem var1) {
      return var1 == Items.CROSSBOW;
   }

   public boolean isChargingCrossbow() {
      return (Boolean)this.entityData.get(IS_CHARGING_CROSSBOW);
   }

   public void setChargingCrossbow(boolean var1) {
      this.entityData.set(IS_CHARGING_CROSSBOW, var1);
   }

   public void onCrossbowAttackPerformed() {
      this.noActionTime = 0;
   }

   public void addAdditionalSaveData(CompoundTag var1) {
      super.addAdditionalSaveData(var1);
      this.writeInventoryToTag(var1);
   }

   public AbstractIllager.IllagerArmPose getArmPose() {
      if (this.isChargingCrossbow()) {
         return AbstractIllager.IllagerArmPose.CROSSBOW_CHARGE;
      } else if (this.isHolding(Items.CROSSBOW)) {
         return AbstractIllager.IllagerArmPose.CROSSBOW_HOLD;
      } else {
         return this.isAggressive() ? AbstractIllager.IllagerArmPose.ATTACKING : AbstractIllager.IllagerArmPose.NEUTRAL;
      }
   }

   public void readAdditionalSaveData(CompoundTag var1) {
      super.readAdditionalSaveData(var1);
      this.readInventoryFromTag(var1);
      this.setCanPickUpLoot(true);
   }

   public float getWalkTargetValue(BlockPos var1, LevelReader var2) {
      return 0.0F;
   }

   public int getMaxSpawnClusterSize() {
      return 1;
   }

   @Nullable
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor var1, DifficultyInstance var2, MobSpawnType var3, @Nullable SpawnGroupData var4, @Nullable CompoundTag var5) {
      RandomSource var6 = var1.getRandom();
      this.populateDefaultEquipmentSlots(var6, var2);
      this.populateDefaultEquipmentEnchantments(var6, var2);
      return super.finalizeSpawn(var1, var2, var3, var4, var5);
   }

   protected void populateDefaultEquipmentSlots(RandomSource var1, DifficultyInstance var2) {
      this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
   }

   protected void enchantSpawnedWeapon(RandomSource var1, float var2) {
      super.enchantSpawnedWeapon(var1, var2);
      if (var1.nextInt(300) == 0) {
         ItemStack var3 = this.getMainHandItem();
         if (var3.is(Items.CROSSBOW)) {
            Map var4 = EnchantmentHelper.getEnchantments(var3);
            var4.putIfAbsent(Enchantments.PIERCING, 1);
            EnchantmentHelper.setEnchantments(var4, var3);
            this.setItemSlot(EquipmentSlot.MAINHAND, var3);
         }
      }

   }

   public boolean isAlliedTo(Entity var1) {
      if (super.isAlliedTo(var1)) {
         return true;
      } else if (var1 instanceof LivingEntity && ((LivingEntity)var1).getMobType() == MobType.ILLAGER) {
         return this.getTeam() == null && var1.getTeam() == null;
      } else {
         return false;
      }
   }

   protected SoundEvent getAmbientSound() {
      return SoundEvents.PILLAGER_AMBIENT;
   }

   protected SoundEvent getDeathSound() {
      return SoundEvents.PILLAGER_DEATH;
   }

   protected SoundEvent getHurtSound(DamageSource var1) {
      return SoundEvents.PILLAGER_HURT;
   }

   public void performRangedAttack(LivingEntity var1, float var2) {
      this.performCrossbowAttack(this, 1.6F);
   }

   public void shootCrossbowProjectile(LivingEntity var1, ItemStack var2, Projectile var3, float var4) {
      this.shootCrossbowProjectile(this, var1, var3, var4, 1.6F);
   }

   public SimpleContainer getInventory() {
      return this.inventory;
   }

   protected void pickUpItem(ItemEntity var1) {
      ItemStack var2 = var1.getItem();
      if (var2.getItem() instanceof BannerItem) {
         super.pickUpItem(var1);
      } else if (this.wantsItem(var2)) {
         this.onItemPickup(var1);
         ItemStack var3 = this.inventory.addItem(var2);
         if (var3.isEmpty()) {
            var1.discard();
         } else {
            var2.setCount(var3.getCount());
         }
      }

   }

   private boolean wantsItem(ItemStack var1) {
      return this.hasActiveRaid() && var1.is(Items.WHITE_BANNER);
   }

   public SlotAccess getSlot(int var1) {
      int var2 = var1 - 300;
      return var2 >= 0 && var2 < this.inventory.getContainerSize() ? SlotAccess.forContainer(this.inventory, var2) : super.getSlot(var1);
   }

   public void applyRaidBuffs(int var1, boolean var2) {
      Raid var3 = this.getCurrentRaid();
      boolean var4 = this.random.nextFloat() <= var3.getEnchantOdds();
      if (var4) {
         ItemStack var5 = new ItemStack(Items.CROSSBOW);
         HashMap var6 = Maps.newHashMap();
         if (var1 > var3.getNumGroups(Difficulty.NORMAL)) {
            var6.put(Enchantments.QUICK_CHARGE, 2);
         } else if (var1 > var3.getNumGroups(Difficulty.EASY)) {
            var6.put(Enchantments.QUICK_CHARGE, 1);
         }

         var6.put(Enchantments.MULTISHOT, 1);
         EnchantmentHelper.setEnchantments(var6, var5);
         this.setItemSlot(EquipmentSlot.MAINHAND, var5);
      }

   }

   public SoundEvent getCelebrateSound() {
      return SoundEvents.PILLAGER_CELEBRATE;
   }

   static {
      IS_CHARGING_CROSSBOW = SynchedEntityData.defineId(Pillager.class, EntityDataSerializers.BOOLEAN);
   }
}
