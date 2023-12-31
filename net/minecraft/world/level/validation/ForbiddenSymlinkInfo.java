package net.minecraft.world.level.validation;

import java.nio.file.Path;

public record ForbiddenSymlinkInfo(Path a, Path b) {
   private final Path link;
   private final Path target;

   public ForbiddenSymlinkInfo(Path var1, Path var2) {
      this.link = var1;
      this.target = var2;
   }

   public Path link() {
      return this.link;
   }

   public Path target() {
      return this.target;
   }
}
