package net.minecraft.world.level;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.StructureAccess;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheck;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;

public class StructureManager {
   private final LevelAccessor level;
   private final WorldOptions worldOptions;
   private final StructureCheck structureCheck;

   public StructureManager(LevelAccessor var1, WorldOptions var2, StructureCheck var3) {
      this.level = var1;
      this.worldOptions = var2;
      this.structureCheck = var3;
   }

   public StructureManager forWorldGenRegion(WorldGenRegion var1) {
      if (var1.getLevel() != this.level) {
         ServerLevel var10002 = var1.getLevel();
         throw new IllegalStateException("Using invalid structure manager (source level: " + var10002 + ", region: " + var1);
      } else {
         return new StructureManager(var1, this.worldOptions, this.structureCheck);
      }
   }

   public List<StructureStart> startsForStructure(ChunkPos var1, Predicate<Structure> var2) {
      Map var3 = this.level.getChunk(var1.x, var1.z, ChunkStatus.STRUCTURE_REFERENCES).getAllReferences();
      Builder var4 = ImmutableList.builder();
      Iterator var5 = var3.entrySet().iterator();

      while(var5.hasNext()) {
         Entry var6 = (Entry)var5.next();
         Structure var7 = (Structure)var6.getKey();
         if (var2.test(var7)) {
            LongSet var10002 = (LongSet)var6.getValue();
            Objects.requireNonNull(var4);
            this.fillStartsForStructure(var7, var10002, var4::add);
         }
      }

      return var4.build();
   }

   public List<StructureStart> startsForStructure(SectionPos var1, Structure var2) {
      LongSet var3 = this.level.getChunk(var1.x(), var1.z(), ChunkStatus.STRUCTURE_REFERENCES).getReferencesForStructure(var2);
      Builder var4 = ImmutableList.builder();
      Objects.requireNonNull(var4);
      this.fillStartsForStructure(var2, var3, var4::add);
      return var4.build();
   }

   public void fillStartsForStructure(Structure var1, LongSet var2, Consumer<StructureStart> var3) {
      LongIterator var4 = var2.iterator();

      while(var4.hasNext()) {
         long var5 = (Long)var4.next();
         SectionPos var7 = SectionPos.of(new ChunkPos(var5), this.level.getMinSection());
         StructureStart var8 = this.getStartForStructure(var7, var1, this.level.getChunk(var7.x(), var7.z(), ChunkStatus.STRUCTURE_STARTS));
         if (var8 != null && var8.isValid()) {
            var3.accept(var8);
         }
      }

   }

   @Nullable
   public StructureStart getStartForStructure(SectionPos var1, Structure var2, StructureAccess var3) {
      return var3.getStartForStructure(var2);
   }

   public void setStartForStructure(SectionPos var1, Structure var2, StructureStart var3, StructureAccess var4) {
      var4.setStartForStructure(var2, var3);
   }

   public void addReferenceForStructure(SectionPos var1, Structure var2, long var3, StructureAccess var5) {
      var5.addReferenceForStructure(var2, var3);
   }

   public boolean shouldGenerateStructures() {
      return this.worldOptions.generateStructures();
   }

   public StructureStart getStructureAt(BlockPos var1, Structure var2) {
      Iterator var3 = this.startsForStructure(SectionPos.of(var1), var2).iterator();

      StructureStart var4;
      do {
         if (!var3.hasNext()) {
            return StructureStart.INVALID_START;
         }

         var4 = (StructureStart)var3.next();
      } while(!var4.getBoundingBox().isInside(var1));

      return var4;
   }

   public StructureStart getStructureWithPieceAt(BlockPos var1, ResourceKey<Structure> var2) {
      Structure var3 = (Structure)this.registryAccess().registryOrThrow(Registries.STRUCTURE).get(var2);
      return var3 == null ? StructureStart.INVALID_START : this.getStructureWithPieceAt(var1, var3);
   }

   public StructureStart getStructureWithPieceAt(BlockPos var1, TagKey<Structure> var2) {
      Registry var3 = this.registryAccess().registryOrThrow(Registries.STRUCTURE);
      Iterator var4 = this.startsForStructure(new ChunkPos(var1), (var2x) -> {
         return (Boolean)var3.getHolder(var3.getId(var2x)).map((var1) -> {
            return var1.is(var2);
         }).orElse(false);
      }).iterator();

      StructureStart var5;
      do {
         if (!var4.hasNext()) {
            return StructureStart.INVALID_START;
         }

         var5 = (StructureStart)var4.next();
      } while(!this.structureHasPieceAt(var1, var5));

      return var5;
   }

   public StructureStart getStructureWithPieceAt(BlockPos var1, Structure var2) {
      Iterator var3 = this.startsForStructure(SectionPos.of(var1), var2).iterator();

      StructureStart var4;
      do {
         if (!var3.hasNext()) {
            return StructureStart.INVALID_START;
         }

         var4 = (StructureStart)var3.next();
      } while(!this.structureHasPieceAt(var1, var4));

      return var4;
   }

   public boolean structureHasPieceAt(BlockPos var1, StructureStart var2) {
      Iterator var3 = var2.getPieces().iterator();

      StructurePiece var4;
      do {
         if (!var3.hasNext()) {
            return false;
         }

         var4 = (StructurePiece)var3.next();
      } while(!var4.getBoundingBox().isInside(var1));

      return true;
   }

   public boolean hasAnyStructureAt(BlockPos var1) {
      SectionPos var2 = SectionPos.of(var1);
      return this.level.getChunk(var2.x(), var2.z(), ChunkStatus.STRUCTURE_REFERENCES).hasAnyStructureReferences();
   }

   public Map<Structure, LongSet> getAllStructuresAt(BlockPos var1) {
      SectionPos var2 = SectionPos.of(var1);
      return this.level.getChunk(var2.x(), var2.z(), ChunkStatus.STRUCTURE_REFERENCES).getAllReferences();
   }

   public StructureCheckResult checkStructurePresence(ChunkPos var1, Structure var2, boolean var3) {
      return this.structureCheck.checkStart(var1, var2, var3);
   }

   public void addReference(StructureStart var1) {
      var1.addReference();
      this.structureCheck.incrementReference(var1.getChunkPos(), var1.getStructure());
   }

   public RegistryAccess registryAccess() {
      return this.level.registryAccess();
   }
}
