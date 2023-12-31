package net.minecraft.data;

import java.nio.file.Path;
import net.minecraft.resources.ResourceLocation;

public class PackOutput {
   private final Path outputFolder;

   public PackOutput(Path var1) {
      this.outputFolder = var1;
   }

   public Path getOutputFolder() {
      return this.outputFolder;
   }

   public Path getOutputFolder(PackOutput.Target var1) {
      return this.getOutputFolder().resolve(var1.directory);
   }

   public PackOutput.PathProvider createPathProvider(PackOutput.Target var1, String var2) {
      return new PackOutput.PathProvider(this, var1, var2);
   }

   public static enum Target {
      DATA_PACK("data"),
      RESOURCE_PACK("assets"),
      REPORTS("reports");

      final String directory;

      private Target(String var3) {
         this.directory = var3;
      }

      // $FF: synthetic method
      private static PackOutput.Target[] $values() {
         return new PackOutput.Target[]{DATA_PACK, RESOURCE_PACK, REPORTS};
      }
   }

   public static class PathProvider {
      private final Path root;
      private final String kind;

      PathProvider(PackOutput var1, PackOutput.Target var2, String var3) {
         this.root = var1.getOutputFolder(var2);
         this.kind = var3;
      }

      public Path file(ResourceLocation var1, String var2) {
         Path var10000 = this.root.resolve(var1.getNamespace()).resolve(this.kind);
         String var10001 = var1.getPath();
         return var10000.resolve(var10001 + "." + var2);
      }

      public Path json(ResourceLocation var1) {
         return this.root.resolve(var1.getNamespace()).resolve(this.kind).resolve(var1.getPath() + ".json");
      }
   }
}
