package net.minecraft.world.entity.animal.horse;

import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class ZombieHorse extends AbstractHorse {
   public ZombieHorse(EntityType<? extends ZombieHorse> var1, Level var2) {
      super(var1, var2);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return createBaseHorseAttributes().add(Attributes.MAX_HEALTH, 15.0D).add(Attributes.MOVEMENT_SPEED, 0.20000000298023224D);
   }

   protected void randomizeAttributes(RandomSource var1) {
      AttributeInstance var10000 = this.getAttribute(Attributes.JUMP_STRENGTH);
      Objects.requireNonNull(var1);
      var10000.setBaseValue(generateJumpStrength(var1::nextDouble));
   }

   public MobType getMobType() {
      return MobType.UNDEAD;
   }

   protected SoundEvent getAmbientSound() {
      return SoundEvents.ZOMBIE_HORSE_AMBIENT;
   }

   protected SoundEvent getDeathSound() {
      return SoundEvents.ZOMBIE_HORSE_DEATH;
   }

   protected SoundEvent getHurtSound(DamageSource var1) {
      return SoundEvents.ZOMBIE_HORSE_HURT;
   }

   @Nullable
   public AgeableMob getBreedOffspring(ServerLevel var1, AgeableMob var2) {
      return (AgeableMob)EntityType.ZOMBIE_HORSE.create(var1);
   }

   public InteractionResult mobInteract(Player var1, InteractionHand var2) {
      return !this.isTamed() ? InteractionResult.PASS : super.mobInteract(var1, var2);
   }

   protected void addBehaviourGoals() {
   }
}
