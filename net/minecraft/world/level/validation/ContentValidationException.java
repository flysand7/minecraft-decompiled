package net.minecraft.world.level.validation;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class ContentValidationException extends Exception {
   private final Path directory;
   private final List<ForbiddenSymlinkInfo> entries;

   public ContentValidationException(Path var1, List<ForbiddenSymlinkInfo> var2) {
      this.directory = var1;
      this.entries = var2;
   }

   public String getMessage() {
      return getMessage(this.directory, this.entries);
   }

   public static String getMessage(Path var0, List<ForbiddenSymlinkInfo> var1) {
      return "Failed to validate '" + var0 + "'. Found forbidden symlinks: " + (String)var1.stream().map((var0x) -> {
         Path var10000 = var0x.link();
         return var10000 + "->" + var0x.target();
      }).collect(Collectors.joining(", "));
   }
}
