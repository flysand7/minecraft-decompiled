package net.minecraft.core;

import com.mojang.datafixers.util.Either;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

public interface Holder<T> {
   T value();

   boolean isBound();

   boolean is(ResourceLocation var1);

   boolean is(ResourceKey<T> var1);

   boolean is(Predicate<ResourceKey<T>> var1);

   boolean is(TagKey<T> var1);

   Stream<TagKey<T>> tags();

   Either<ResourceKey<T>, T> unwrap();

   Optional<ResourceKey<T>> unwrapKey();

   Holder.Kind kind();

   boolean canSerializeIn(HolderOwner<T> var1);

   static <T> Holder<T> direct(T var0) {
      return new Holder.Direct(var0);
   }

   public static record Direct<T>(T a) implements Holder<T> {
      private final T value;

      public Direct(T var1) {
         this.value = var1;
      }

      public boolean isBound() {
         return true;
      }

      public boolean is(ResourceLocation var1) {
         return false;
      }

      public boolean is(ResourceKey<T> var1) {
         return false;
      }

      public boolean is(TagKey<T> var1) {
         return false;
      }

      public boolean is(Predicate<ResourceKey<T>> var1) {
         return false;
      }

      public Either<ResourceKey<T>, T> unwrap() {
         return Either.right(this.value);
      }

      public Optional<ResourceKey<T>> unwrapKey() {
         return Optional.empty();
      }

      public Holder.Kind kind() {
         return Holder.Kind.DIRECT;
      }

      public String toString() {
         return "Direct{" + this.value + "}";
      }

      public boolean canSerializeIn(HolderOwner<T> var1) {
         return true;
      }

      public Stream<TagKey<T>> tags() {
         return Stream.of();
      }

      public T value() {
         return this.value;
      }
   }

   public static class Reference<T> implements Holder<T> {
      private final HolderOwner<T> owner;
      private Set<TagKey<T>> tags = Set.of();
      private final Holder.Reference.Type type;
      @Nullable
      private ResourceKey<T> key;
      @Nullable
      private T value;

      private Reference(Holder.Reference.Type var1, HolderOwner<T> var2, @Nullable ResourceKey<T> var3, @Nullable T var4) {
         this.owner = var2;
         this.type = var1;
         this.key = var3;
         this.value = var4;
      }

      public static <T> Holder.Reference<T> createStandAlone(HolderOwner<T> var0, ResourceKey<T> var1) {
         return new Holder.Reference(Holder.Reference.Type.STAND_ALONE, var0, var1, (Object)null);
      }

      /** @deprecated */
      @Deprecated
      public static <T> Holder.Reference<T> createIntrusive(HolderOwner<T> var0, @Nullable T var1) {
         return new Holder.Reference(Holder.Reference.Type.INTRUSIVE, var0, (ResourceKey)null, var1);
      }

      public ResourceKey<T> key() {
         if (this.key == null) {
            throw new IllegalStateException("Trying to access unbound value '" + this.value + "' from registry " + this.owner);
         } else {
            return this.key;
         }
      }

      public T value() {
         if (this.value == null) {
            throw new IllegalStateException("Trying to access unbound value '" + this.key + "' from registry " + this.owner);
         } else {
            return this.value;
         }
      }

      public boolean is(ResourceLocation var1) {
         return this.key().location().equals(var1);
      }

      public boolean is(ResourceKey<T> var1) {
         return this.key() == var1;
      }

      public boolean is(TagKey<T> var1) {
         return this.tags.contains(var1);
      }

      public boolean is(Predicate<ResourceKey<T>> var1) {
         return var1.test(this.key());
      }

      public boolean canSerializeIn(HolderOwner<T> var1) {
         return this.owner.canSerializeIn(var1);
      }

      public Either<ResourceKey<T>, T> unwrap() {
         return Either.left(this.key());
      }

      public Optional<ResourceKey<T>> unwrapKey() {
         return Optional.of(this.key());
      }

      public Holder.Kind kind() {
         return Holder.Kind.REFERENCE;
      }

      public boolean isBound() {
         return this.key != null && this.value != null;
      }

      void bindKey(ResourceKey<T> var1) {
         if (this.key != null && var1 != this.key) {
            throw new IllegalStateException("Can't change holder key: existing=" + this.key + ", new=" + var1);
         } else {
            this.key = var1;
         }
      }

      void bindValue(T var1) {
         if (this.type == Holder.Reference.Type.INTRUSIVE && this.value != var1) {
            throw new IllegalStateException("Can't change holder " + this.key + " value: existing=" + this.value + ", new=" + var1);
         } else {
            this.value = var1;
         }
      }

      void bindTags(Collection<TagKey<T>> var1) {
         this.tags = Set.copyOf(var1);
      }

      public Stream<TagKey<T>> tags() {
         return this.tags.stream();
      }

      public String toString() {
         return "Reference{" + this.key + "=" + this.value + "}";
      }

      static enum Type {
         STAND_ALONE,
         INTRUSIVE;

         private Type() {
         }

         // $FF: synthetic method
         private static Holder.Reference.Type[] $values() {
            return new Holder.Reference.Type[]{STAND_ALONE, INTRUSIVE};
         }
      }
   }

   public static enum Kind {
      REFERENCE,
      DIRECT;

      private Kind() {
      }

      // $FF: synthetic method
      private static Holder.Kind[] $values() {
         return new Holder.Kind[]{REFERENCE, DIRECT};
      }
   }
}
