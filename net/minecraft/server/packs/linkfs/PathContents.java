package net.minecraft.server.packs.linkfs;

import java.nio.file.Path;
import java.util.Map;

interface PathContents {
   PathContents MISSING = new PathContents() {
      public String toString() {
         return "empty";
      }
   };
   PathContents RELATIVE = new PathContents() {
      public String toString() {
         return "relative";
      }
   };

   public static record DirectoryContents(Map<String, LinkFSPath> c) implements PathContents {
      private final Map<String, LinkFSPath> children;

      public DirectoryContents(Map<String, LinkFSPath> var1) {
         this.children = var1;
      }

      public Map<String, LinkFSPath> children() {
         return this.children;
      }
   }

   public static record FileContents(Path c) implements PathContents {
      private final Path contents;

      public FileContents(Path var1) {
         this.contents = var1;
      }

      public Path contents() {
         return this.contents;
      }
   }
}
