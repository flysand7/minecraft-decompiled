package net.minecraft.client.renderer.texture;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class Stitcher<T extends Stitcher.Entry> {
   private static final Comparator<Stitcher.Holder<?>> HOLDER_COMPARATOR = Comparator.comparing((var0) -> {
      return -var0.height;
   }).thenComparing((var0) -> {
      return -var0.width;
   }).thenComparing((var0) -> {
      return var0.entry.name();
   });
   private final int mipLevel;
   private final List<Stitcher.Holder<T>> texturesToBeStitched = new ArrayList();
   private final List<Stitcher.Region<T>> storage = new ArrayList();
   private int storageX;
   private int storageY;
   private final int maxWidth;
   private final int maxHeight;

   public Stitcher(int var1, int var2, int var3) {
      this.mipLevel = var3;
      this.maxWidth = var1;
      this.maxHeight = var2;
   }

   public int getWidth() {
      return this.storageX;
   }

   public int getHeight() {
      return this.storageY;
   }

   public void registerSprite(T var1) {
      Stitcher.Holder var2 = new Stitcher.Holder(var1, this.mipLevel);
      this.texturesToBeStitched.add(var2);
   }

   public void stitch() {
      ArrayList var1 = new ArrayList(this.texturesToBeStitched);
      var1.sort(HOLDER_COMPARATOR);
      Iterator var2 = var1.iterator();

      Stitcher.Holder var3;
      do {
         if (!var2.hasNext()) {
            return;
         }

         var3 = (Stitcher.Holder)var2.next();
      } while(this.addToStorage(var3));

      throw new StitcherException(var3.entry, (Collection)var1.stream().map((var0) -> {
         return var0.entry;
      }).collect(ImmutableList.toImmutableList()));
   }

   public void gatherSprites(Stitcher.SpriteLoader<T> var1) {
      Iterator var2 = this.storage.iterator();

      while(var2.hasNext()) {
         Stitcher.Region var3 = (Stitcher.Region)var2.next();
         var3.walk(var1);
      }

   }

   static int smallestFittingMinTexel(int var0, int var1) {
      return (var0 >> var1) + ((var0 & (1 << var1) - 1) == 0 ? 0 : 1) << var1;
   }

   private boolean addToStorage(Stitcher.Holder<T> var1) {
      Iterator var2 = this.storage.iterator();

      Stitcher.Region var3;
      do {
         if (!var2.hasNext()) {
            return this.expand(var1);
         }

         var3 = (Stitcher.Region)var2.next();
      } while(!var3.add(var1));

      return true;
   }

   private boolean expand(Stitcher.Holder<T> var1) {
      int var3 = Mth.smallestEncompassingPowerOfTwo(this.storageX);
      int var4 = Mth.smallestEncompassingPowerOfTwo(this.storageY);
      int var5 = Mth.smallestEncompassingPowerOfTwo(this.storageX + var1.width);
      int var6 = Mth.smallestEncompassingPowerOfTwo(this.storageY + var1.height);
      boolean var7 = var5 <= this.maxWidth;
      boolean var8 = var6 <= this.maxHeight;
      if (!var7 && !var8) {
         return false;
      } else {
         boolean var9 = var7 && var3 != var5;
         boolean var10 = var8 && var4 != var6;
         boolean var2;
         if (var9 ^ var10) {
            var2 = var9;
         } else {
            var2 = var7 && var3 <= var4;
         }

         Stitcher.Region var11;
         if (var2) {
            if (this.storageY == 0) {
               this.storageY = var6;
            }

            var11 = new Stitcher.Region(this.storageX, 0, var5 - this.storageX, this.storageY);
            this.storageX = var5;
         } else {
            var11 = new Stitcher.Region(0, this.storageY, this.storageX, var6 - this.storageY);
            this.storageY = var6;
         }

         var11.add(var1);
         this.storage.add(var11);
         return true;
      }
   }

   private static record Holder<T extends Stitcher.Entry>(T a, int b, int c) {
      final T entry;
      final int width;
      final int height;

      public Holder(T var1, int var2) {
         this(var1, Stitcher.smallestFittingMinTexel(var1.width(), var2), Stitcher.smallestFittingMinTexel(var1.height(), var2));
      }

      private Holder(T var1, int var2, int var3) {
         this.entry = var1;
         this.width = var2;
         this.height = var3;
      }

      public T entry() {
         return this.entry;
      }

      public int width() {
         return this.width;
      }

      public int height() {
         return this.height;
      }
   }

   public interface Entry {
      int width();

      int height();

      ResourceLocation name();
   }

   public static class Region<T extends Stitcher.Entry> {
      private final int originX;
      private final int originY;
      private final int width;
      private final int height;
      @Nullable
      private List<Stitcher.Region<T>> subSlots;
      @Nullable
      private Stitcher.Holder<T> holder;

      public Region(int var1, int var2, int var3, int var4) {
         this.originX = var1;
         this.originY = var2;
         this.width = var3;
         this.height = var4;
      }

      public int getX() {
         return this.originX;
      }

      public int getY() {
         return this.originY;
      }

      public boolean add(Stitcher.Holder<T> var1) {
         if (this.holder != null) {
            return false;
         } else {
            int var2 = var1.width;
            int var3 = var1.height;
            if (var2 <= this.width && var3 <= this.height) {
               if (var2 == this.width && var3 == this.height) {
                  this.holder = var1;
                  return true;
               } else {
                  if (this.subSlots == null) {
                     this.subSlots = new ArrayList(1);
                     this.subSlots.add(new Stitcher.Region(this.originX, this.originY, var2, var3));
                     int var4 = this.width - var2;
                     int var5 = this.height - var3;
                     if (var5 > 0 && var4 > 0) {
                        int var6 = Math.max(this.height, var4);
                        int var7 = Math.max(this.width, var5);
                        if (var6 >= var7) {
                           this.subSlots.add(new Stitcher.Region(this.originX, this.originY + var3, var2, var5));
                           this.subSlots.add(new Stitcher.Region(this.originX + var2, this.originY, var4, this.height));
                        } else {
                           this.subSlots.add(new Stitcher.Region(this.originX + var2, this.originY, var4, var3));
                           this.subSlots.add(new Stitcher.Region(this.originX, this.originY + var3, this.width, var5));
                        }
                     } else if (var4 == 0) {
                        this.subSlots.add(new Stitcher.Region(this.originX, this.originY + var3, var2, var5));
                     } else if (var5 == 0) {
                        this.subSlots.add(new Stitcher.Region(this.originX + var2, this.originY, var4, var3));
                     }
                  }

                  Iterator var8 = this.subSlots.iterator();

                  Stitcher.Region var9;
                  do {
                     if (!var8.hasNext()) {
                        return false;
                     }

                     var9 = (Stitcher.Region)var8.next();
                  } while(!var9.add(var1));

                  return true;
               }
            } else {
               return false;
            }
         }
      }

      public void walk(Stitcher.SpriteLoader<T> var1) {
         if (this.holder != null) {
            var1.load(this.holder.entry, this.getX(), this.getY());
         } else if (this.subSlots != null) {
            Iterator var2 = this.subSlots.iterator();

            while(var2.hasNext()) {
               Stitcher.Region var3 = (Stitcher.Region)var2.next();
               var3.walk(var1);
            }
         }

      }

      public String toString() {
         return "Slot{originX=" + this.originX + ", originY=" + this.originY + ", width=" + this.width + ", height=" + this.height + ", texture=" + this.holder + ", subSlots=" + this.subSlots + "}";
      }
   }

   public interface SpriteLoader<T extends Stitcher.Entry> {
      void load(T var1, int var2, int var3);
   }
}
