package net.minecraft.server.packs.repository;

import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;

public enum PackCompatibility {
   TOO_OLD("old"),
   TOO_NEW("new"),
   COMPATIBLE("compatible");

   private final Component description;
   private final Component confirmation;

   private PackCompatibility(String var3) {
      this.description = Component.translatable("pack.incompatible." + var3).withStyle(ChatFormatting.GRAY);
      this.confirmation = Component.translatable("pack.incompatible.confirm." + var3);
   }

   public boolean isCompatible() {
      return this == COMPATIBLE;
   }

   public static PackCompatibility forFormat(int var0, PackType var1) {
      int var2 = SharedConstants.getCurrentVersion().getPackVersion(var1);
      if (var0 < var2) {
         return TOO_OLD;
      } else {
         return var0 > var2 ? TOO_NEW : COMPATIBLE;
      }
   }

   public Component getDescription() {
      return this.description;
   }

   public Component getConfirmation() {
      return this.confirmation;
   }

   // $FF: synthetic method
   private static PackCompatibility[] $values() {
      return new PackCompatibility[]{TOO_OLD, TOO_NEW, COMPATIBLE};
   }
}
