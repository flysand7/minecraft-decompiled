package net.minecraft.network.chat;

import com.mojang.serialization.Codec;
import java.util.BitSet;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import org.apache.commons.lang3.StringUtils;

public class FilterMask {
   public static final Codec<FilterMask> CODEC = StringRepresentable.fromEnum(FilterMask.Type::values).dispatch(FilterMask::type, FilterMask.Type::codec);
   public static final FilterMask FULLY_FILTERED;
   public static final FilterMask PASS_THROUGH;
   public static final Style FILTERED_STYLE;
   static final Codec<FilterMask> PASS_THROUGH_CODEC;
   static final Codec<FilterMask> FULLY_FILTERED_CODEC;
   static final Codec<FilterMask> PARTIALLY_FILTERED_CODEC;
   private static final char HASH = '#';
   private final BitSet mask;
   private final FilterMask.Type type;

   private FilterMask(BitSet var1, FilterMask.Type var2) {
      this.mask = var1;
      this.type = var2;
   }

   private FilterMask(BitSet var1) {
      this.mask = var1;
      this.type = FilterMask.Type.PARTIALLY_FILTERED;
   }

   public FilterMask(int var1) {
      this(new BitSet(var1), FilterMask.Type.PARTIALLY_FILTERED);
   }

   private FilterMask.Type type() {
      return this.type;
   }

   private BitSet mask() {
      return this.mask;
   }

   public static FilterMask read(FriendlyByteBuf var0) {
      FilterMask.Type var1 = (FilterMask.Type)var0.readEnum(FilterMask.Type.class);
      FilterMask var10000;
      switch(var1) {
      case PASS_THROUGH:
         var10000 = PASS_THROUGH;
         break;
      case FULLY_FILTERED:
         var10000 = FULLY_FILTERED;
         break;
      case PARTIALLY_FILTERED:
         var10000 = new FilterMask(var0.readBitSet(), FilterMask.Type.PARTIALLY_FILTERED);
         break;
      default:
         throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   public static void write(FriendlyByteBuf var0, FilterMask var1) {
      var0.writeEnum(var1.type);
      if (var1.type == FilterMask.Type.PARTIALLY_FILTERED) {
         var0.writeBitSet(var1.mask);
      }

   }

   public void setFiltered(int var1) {
      this.mask.set(var1);
   }

   @Nullable
   public String apply(String var1) {
      String var10000;
      switch(this.type) {
      case PASS_THROUGH:
         var10000 = var1;
         break;
      case FULLY_FILTERED:
         var10000 = null;
         break;
      case PARTIALLY_FILTERED:
         char[] var2 = var1.toCharArray();

         for(int var3 = 0; var3 < var2.length && var3 < this.mask.length(); ++var3) {
            if (this.mask.get(var3)) {
               var2[var3] = '#';
            }
         }

         var10000 = new String(var2);
         break;
      default:
         throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   @Nullable
   public Component applyWithFormatting(String var1) {
      MutableComponent var10000;
      switch(this.type) {
      case PASS_THROUGH:
         var10000 = Component.literal(var1);
         break;
      case FULLY_FILTERED:
         var10000 = null;
         break;
      case PARTIALLY_FILTERED:
         MutableComponent var2 = Component.empty();
         int var3 = 0;
         boolean var4 = this.mask.get(0);

         while(true) {
            int var5 = var4 ? this.mask.nextClearBit(var3) : this.mask.nextSetBit(var3);
            var5 = var5 < 0 ? var1.length() : var5;
            if (var5 == var3) {
               var10000 = var2;
               return var10000;
            }

            if (var4) {
               var2.append((Component)Component.literal(StringUtils.repeat('#', var5 - var3)).withStyle(FILTERED_STYLE));
            } else {
               var2.append(var1.substring(var3, var5));
            }

            var4 = !var4;
            var3 = var5;
         }
      default:
         throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   public boolean isEmpty() {
      return this.type == FilterMask.Type.PASS_THROUGH;
   }

   public boolean isFullyFiltered() {
      return this.type == FilterMask.Type.FULLY_FILTERED;
   }

   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      } else if (var1 != null && this.getClass() == var1.getClass()) {
         FilterMask var2 = (FilterMask)var1;
         return this.mask.equals(var2.mask) && this.type == var2.type;
      } else {
         return false;
      }
   }

   public int hashCode() {
      int var1 = this.mask.hashCode();
      var1 = 31 * var1 + this.type.hashCode();
      return var1;
   }

   static {
      FULLY_FILTERED = new FilterMask(new BitSet(0), FilterMask.Type.FULLY_FILTERED);
      PASS_THROUGH = new FilterMask(new BitSet(0), FilterMask.Type.PASS_THROUGH);
      FILTERED_STYLE = Style.EMPTY.withColor(ChatFormatting.DARK_GRAY).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.filtered")));
      PASS_THROUGH_CODEC = Codec.unit(PASS_THROUGH);
      FULLY_FILTERED_CODEC = Codec.unit(FULLY_FILTERED);
      PARTIALLY_FILTERED_CODEC = ExtraCodecs.BIT_SET.xmap(FilterMask::new, FilterMask::mask);
   }

   private static enum Type implements StringRepresentable {
      PASS_THROUGH("pass_through", () -> {
         return FilterMask.PASS_THROUGH_CODEC;
      }),
      FULLY_FILTERED("fully_filtered", () -> {
         return FilterMask.FULLY_FILTERED_CODEC;
      }),
      PARTIALLY_FILTERED("partially_filtered", () -> {
         return FilterMask.PARTIALLY_FILTERED_CODEC;
      });

      private final String serializedName;
      private final Supplier<Codec<FilterMask>> codec;

      private Type(String var3, Supplier<Codec<FilterMask>> var4) {
         this.serializedName = var3;
         this.codec = var4;
      }

      public String getSerializedName() {
         return this.serializedName;
      }

      private Codec<FilterMask> codec() {
         return (Codec)this.codec.get();
      }

      // $FF: synthetic method
      private static FilterMask.Type[] $values() {
         return new FilterMask.Type[]{PASS_THROUGH, FULLY_FILTERED, PARTIALLY_FILTERED};
      }
   }
}
