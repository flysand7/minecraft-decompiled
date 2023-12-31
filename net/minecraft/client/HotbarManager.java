package net.minecraft.client;

import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import java.io.File;
import net.minecraft.client.player.inventory.Hotbar;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.datafix.DataFixTypes;
import org.slf4j.Logger;

public class HotbarManager {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final int NUM_HOTBAR_GROUPS = 9;
   private final File optionsFile;
   private final DataFixer fixerUpper;
   private final Hotbar[] hotbars = new Hotbar[9];
   private boolean loaded;

   public HotbarManager(File var1, DataFixer var2) {
      this.optionsFile = new File(var1, "hotbar.nbt");
      this.fixerUpper = var2;

      for(int var3 = 0; var3 < 9; ++var3) {
         this.hotbars[var3] = new Hotbar();
      }

   }

   private void load() {
      try {
         CompoundTag var1 = NbtIo.read(this.optionsFile);
         if (var1 == null) {
            return;
         }

         int var2 = NbtUtils.getDataVersion(var1, 1343);
         var1 = DataFixTypes.HOTBAR.updateToCurrentVersion(this.fixerUpper, var1, var2);

         for(int var3 = 0; var3 < 9; ++var3) {
            this.hotbars[var3].fromTag(var1.getList(String.valueOf(var3), 10));
         }
      } catch (Exception var4) {
         LOGGER.error("Failed to load creative mode options", var4);
      }

   }

   public void save() {
      try {
         CompoundTag var1 = NbtUtils.addCurrentDataVersion(new CompoundTag());

         for(int var2 = 0; var2 < 9; ++var2) {
            var1.put(String.valueOf(var2), this.get(var2).createTag());
         }

         NbtIo.write(var1, this.optionsFile);
      } catch (Exception var3) {
         LOGGER.error("Failed to save creative mode options", var3);
      }

   }

   public Hotbar get(int var1) {
      if (!this.loaded) {
         this.load();
         this.loaded = true;
      }

      return this.hotbars[var1];
   }
}
