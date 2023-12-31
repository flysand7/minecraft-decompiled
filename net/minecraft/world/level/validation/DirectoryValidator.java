package net.minecraft.world.level.validation;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class DirectoryValidator {
   private final PathAllowList symlinkTargetAllowList;

   public DirectoryValidator(PathAllowList var1) {
      this.symlinkTargetAllowList = var1;
   }

   public void validateSymlink(Path var1, List<ForbiddenSymlinkInfo> var2) throws IOException {
      Path var3 = Files.readSymbolicLink(var1);
      if (!this.symlinkTargetAllowList.matches(var3)) {
         var2.add(new ForbiddenSymlinkInfo(var1, var3));
      }

   }

   public List<ForbiddenSymlinkInfo> validateSave(Path var1, boolean var2) throws IOException {
      final ArrayList var3 = new ArrayList();

      BasicFileAttributes var4;
      try {
         var4 = Files.readAttributes(var1, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
      } catch (NoSuchFileException var6) {
         return var3;
      }

      if (!var4.isRegularFile() && !var4.isOther()) {
         if (var4.isSymbolicLink()) {
            if (!var2) {
               this.validateSymlink(var1, var3);
               return var3;
            }

            var1 = Files.readSymbolicLink(var1);
         }

         Files.walkFileTree(var1, new SimpleFileVisitor<Path>() {
            private void validateSymlink(Path var1, BasicFileAttributes var2) throws IOException {
               if (var2.isSymbolicLink()) {
                  DirectoryValidator.this.validateSymlink(var1, var3);
               }

            }

            public FileVisitResult preVisitDirectory(Path var1, BasicFileAttributes var2) throws IOException {
               this.validateSymlink(var1, var2);
               return super.preVisitDirectory(var1, var2);
            }

            public FileVisitResult visitFile(Path var1, BasicFileAttributes var2) throws IOException {
               this.validateSymlink(var1, var2);
               return super.visitFile(var1, var2);
            }

            // $FF: synthetic method
            public FileVisitResult visitFile(Object var1, BasicFileAttributes var2) throws IOException {
               return this.visitFile((Path)var1, var2);
            }

            // $FF: synthetic method
            public FileVisitResult preVisitDirectory(Object var1, BasicFileAttributes var2) throws IOException {
               return this.preVisitDirectory((Path)var1, var2);
            }
         });
         return var3;
      } else {
         throw new IOException("Path " + var1 + " is not a directory");
      }
   }
}
