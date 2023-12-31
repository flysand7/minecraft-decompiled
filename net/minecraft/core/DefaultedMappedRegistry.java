package net.minecraft.core;

import com.mojang.serialization.Lifecycle;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

public class DefaultedMappedRegistry<T> extends MappedRegistry<T> implements DefaultedRegistry<T> {
   private final ResourceLocation defaultKey;
   private Holder.Reference<T> defaultValue;

   public DefaultedMappedRegistry(String var1, ResourceKey<? extends Registry<T>> var2, Lifecycle var3, boolean var4) {
      super(var2, var3, var4);
      this.defaultKey = new ResourceLocation(var1);
   }

   public Holder.Reference<T> registerMapping(int var1, ResourceKey<T> var2, T var3, Lifecycle var4) {
      Holder.Reference var5 = super.registerMapping(var1, var2, var3, var4);
      if (this.defaultKey.equals(var2.location())) {
         this.defaultValue = var5;
      }

      return var5;
   }

   public int getId(@Nullable T var1) {
      int var2 = super.getId(var1);
      return var2 == -1 ? super.getId(this.defaultValue.value()) : var2;
   }

   @Nonnull
   public ResourceLocation getKey(T var1) {
      ResourceLocation var2 = super.getKey(var1);
      return var2 == null ? this.defaultKey : var2;
   }

   @Nonnull
   public T get(@Nullable ResourceLocation var1) {
      Object var2 = super.get(var1);
      return var2 == null ? this.defaultValue.value() : var2;
   }

   public Optional<T> getOptional(@Nullable ResourceLocation var1) {
      return Optional.ofNullable(super.get(var1));
   }

   @Nonnull
   public T byId(int var1) {
      Object var2 = super.byId(var1);
      return var2 == null ? this.defaultValue.value() : var2;
   }

   public Optional<Holder.Reference<T>> getRandom(RandomSource var1) {
      return super.getRandom(var1).or(() -> {
         return Optional.of(this.defaultValue);
      });
   }

   public ResourceLocation getDefaultKey() {
      return this.defaultKey;
   }

   // $FF: synthetic method
   public Holder registerMapping(int var1, ResourceKey var2, Object var3, Lifecycle var4) {
      return this.registerMapping(var1, var2, var3, var4);
   }
}
