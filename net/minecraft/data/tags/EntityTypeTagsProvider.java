package net.minecraft.data.tags;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;

public class EntityTypeTagsProvider extends IntrinsicHolderTagsProvider<EntityType<?>> {
   public EntityTypeTagsProvider(PackOutput var1, CompletableFuture<HolderLookup.Provider> var2) {
      super(var1, Registries.ENTITY_TYPE, var2, (var0) -> {
         return var0.builtInRegistryHolder().key();
      });
   }

   protected void addTags(HolderLookup.Provider var1) {
      this.tag(EntityTypeTags.SKELETONS).add((Object[])(EntityType.SKELETON, EntityType.STRAY, EntityType.WITHER_SKELETON));
      this.tag(EntityTypeTags.RAIDERS).add((Object[])(EntityType.EVOKER, EntityType.PILLAGER, EntityType.RAVAGER, EntityType.VINDICATOR, EntityType.ILLUSIONER, EntityType.WITCH));
      this.tag(EntityTypeTags.BEEHIVE_INHABITORS).add((Object)EntityType.BEE);
      this.tag(EntityTypeTags.ARROWS).add((Object[])(EntityType.ARROW, EntityType.SPECTRAL_ARROW));
      this.tag(EntityTypeTags.IMPACT_PROJECTILES).addTag(EntityTypeTags.ARROWS).add((Object[])(EntityType.SNOWBALL, EntityType.FIREBALL, EntityType.SMALL_FIREBALL, EntityType.EGG, EntityType.TRIDENT, EntityType.DRAGON_FIREBALL, EntityType.WITHER_SKULL));
      this.tag(EntityTypeTags.POWDER_SNOW_WALKABLE_MOBS).add((Object[])(EntityType.RABBIT, EntityType.ENDERMITE, EntityType.SILVERFISH, EntityType.FOX));
      this.tag(EntityTypeTags.AXOLOTL_HUNT_TARGETS).add((Object[])(EntityType.TROPICAL_FISH, EntityType.PUFFERFISH, EntityType.SALMON, EntityType.COD, EntityType.SQUID, EntityType.GLOW_SQUID, EntityType.TADPOLE));
      this.tag(EntityTypeTags.AXOLOTL_ALWAYS_HOSTILES).add((Object[])(EntityType.DROWNED, EntityType.GUARDIAN, EntityType.ELDER_GUARDIAN));
      this.tag(EntityTypeTags.FREEZE_IMMUNE_ENTITY_TYPES).add((Object[])(EntityType.STRAY, EntityType.POLAR_BEAR, EntityType.SNOW_GOLEM, EntityType.WITHER));
      this.tag(EntityTypeTags.FREEZE_HURTS_EXTRA_TYPES).add((Object[])(EntityType.STRIDER, EntityType.BLAZE, EntityType.MAGMA_CUBE));
      this.tag(EntityTypeTags.FROG_FOOD).add((Object[])(EntityType.SLIME, EntityType.MAGMA_CUBE));
      this.tag(EntityTypeTags.FALL_DAMAGE_IMMUNE).add((Object[])(EntityType.IRON_GOLEM, EntityType.SNOW_GOLEM, EntityType.SHULKER, EntityType.ALLAY, EntityType.BAT, EntityType.BEE, EntityType.BLAZE, EntityType.CAT, EntityType.CHICKEN, EntityType.GHAST, EntityType.PHANTOM, EntityType.MAGMA_CUBE, EntityType.OCELOT, EntityType.PARROT, EntityType.WITHER));
      this.tag(EntityTypeTags.DISMOUNTS_UNDERWATER).add((Object[])(EntityType.CAMEL, EntityType.CHICKEN, EntityType.DONKEY, EntityType.HORSE, EntityType.LLAMA, EntityType.MULE, EntityType.PIG, EntityType.RAVAGER, EntityType.SPIDER, EntityType.STRIDER, EntityType.TRADER_LLAMA, EntityType.ZOMBIE_HORSE));
   }
}
