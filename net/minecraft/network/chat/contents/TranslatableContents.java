package net.minecraft.network.chat.contents;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.Entity;

public class TranslatableContents implements ComponentContents {
   public static final Object[] NO_ARGS = new Object[0];
   private static final FormattedText TEXT_PERCENT = FormattedText.of("%");
   private static final FormattedText TEXT_NULL = FormattedText.of("null");
   private final String key;
   @Nullable
   private final String fallback;
   private final Object[] args;
   @Nullable
   private Language decomposedWith;
   private List<FormattedText> decomposedParts = ImmutableList.of();
   private static final Pattern FORMAT_PATTERN = Pattern.compile("%(?:(\\d+)\\$)?([A-Za-z%]|$)");

   public TranslatableContents(String var1, @Nullable String var2, Object[] var3) {
      this.key = var1;
      this.fallback = var2;
      this.args = var3;
   }

   private void decompose() {
      Language var1 = Language.getInstance();
      if (var1 != this.decomposedWith) {
         this.decomposedWith = var1;
         String var2 = this.fallback != null ? var1.getOrDefault(this.key, this.fallback) : var1.getOrDefault(this.key);

         try {
            Builder var3 = ImmutableList.builder();
            Objects.requireNonNull(var3);
            this.decomposeTemplate(var2, var3::add);
            this.decomposedParts = var3.build();
         } catch (TranslatableFormatException var4) {
            this.decomposedParts = ImmutableList.of(FormattedText.of(var2));
         }

      }
   }

   private void decomposeTemplate(String var1, Consumer<FormattedText> var2) {
      Matcher var3 = FORMAT_PATTERN.matcher(var1);

      try {
         int var4 = 0;

         int var5;
         int var7;
         for(var5 = 0; var3.find(var5); var5 = var7) {
            int var6 = var3.start();
            var7 = var3.end();
            String var8;
            if (var6 > var5) {
               var8 = var1.substring(var5, var6);
               if (var8.indexOf(37) != -1) {
                  throw new IllegalArgumentException();
               }

               var2.accept(FormattedText.of(var8));
            }

            var8 = var3.group(2);
            String var9 = var1.substring(var6, var7);
            if ("%".equals(var8) && "%%".equals(var9)) {
               var2.accept(TEXT_PERCENT);
            } else {
               if (!"s".equals(var8)) {
                  throw new TranslatableFormatException(this, "Unsupported format: '" + var9 + "'");
               }

               String var10 = var3.group(1);
               int var11 = var10 != null ? Integer.parseInt(var10) - 1 : var4++;
               var2.accept(this.getArgument(var11));
            }
         }

         if (var5 < var1.length()) {
            String var13 = var1.substring(var5);
            if (var13.indexOf(37) != -1) {
               throw new IllegalArgumentException();
            }

            var2.accept(FormattedText.of(var13));
         }

      } catch (IllegalArgumentException var12) {
         throw new TranslatableFormatException(this, var12);
      }
   }

   private FormattedText getArgument(int var1) {
      if (var1 >= 0 && var1 < this.args.length) {
         Object var2 = this.args[var1];
         if (var2 instanceof Component) {
            return (Component)var2;
         } else {
            return var2 == null ? TEXT_NULL : FormattedText.of(var2.toString());
         }
      } else {
         throw new TranslatableFormatException(this, var1);
      }
   }

   public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> var1, Style var2) {
      this.decompose();
      Iterator var3 = this.decomposedParts.iterator();

      Optional var5;
      do {
         if (!var3.hasNext()) {
            return Optional.empty();
         }

         FormattedText var4 = (FormattedText)var3.next();
         var5 = var4.visit(var1, var2);
      } while(!var5.isPresent());

      return var5;
   }

   public <T> Optional<T> visit(FormattedText.ContentConsumer<T> var1) {
      this.decompose();
      Iterator var2 = this.decomposedParts.iterator();

      Optional var4;
      do {
         if (!var2.hasNext()) {
            return Optional.empty();
         }

         FormattedText var3 = (FormattedText)var2.next();
         var4 = var3.visit(var1);
      } while(!var4.isPresent());

      return var4;
   }

   public MutableComponent resolve(@Nullable CommandSourceStack var1, @Nullable Entity var2, int var3) throws CommandSyntaxException {
      Object[] var4 = new Object[this.args.length];

      for(int var5 = 0; var5 < var4.length; ++var5) {
         Object var6 = this.args[var5];
         if (var6 instanceof Component) {
            var4[var5] = ComponentUtils.updateForEntity(var1, (Component)var6, var2, var3);
         } else {
            var4[var5] = var6;
         }
      }

      return MutableComponent.create(new TranslatableContents(this.key, this.fallback, var4));
   }

   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      } else {
         boolean var10000;
         if (var1 instanceof TranslatableContents) {
            TranslatableContents var2 = (TranslatableContents)var1;
            if (Objects.equals(this.key, var2.key) && Objects.equals(this.fallback, var2.fallback) && Arrays.equals(this.args, var2.args)) {
               var10000 = true;
               return var10000;
            }
         }

         var10000 = false;
         return var10000;
      }
   }

   public int hashCode() {
      int var1 = Objects.hashCode(this.key);
      var1 = 31 * var1 + Objects.hashCode(this.fallback);
      var1 = 31 * var1 + Arrays.hashCode(this.args);
      return var1;
   }

   public String toString() {
      String var10000 = this.key;
      return "translation{key='" + var10000 + "'" + (this.fallback != null ? ", fallback='" + this.fallback + "'" : "") + ", args=" + Arrays.toString(this.args) + "}";
   }

   public String getKey() {
      return this.key;
   }

   @Nullable
   public String getFallback() {
      return this.fallback;
   }

   public Object[] getArgs() {
      return this.args;
   }
}
