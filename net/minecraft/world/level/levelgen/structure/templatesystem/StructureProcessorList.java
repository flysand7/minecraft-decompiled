package net.minecraft.world.level.levelgen.structure.templatesystem;

import java.util.List;

public class StructureProcessorList {
   private final List<StructureProcessor> list;

   public StructureProcessorList(List<StructureProcessor> var1) {
      this.list = var1;
   }

   public List<StructureProcessor> list() {
      return this.list;
   }

   public String toString() {
      return "ProcessorList[" + this.list + "]";
   }
}
