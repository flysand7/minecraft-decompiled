package net.minecraft.world.level.levelgen.structure.structures;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class RuinedPortalStructure extends Structure {
   private static final String[] STRUCTURE_LOCATION_PORTALS = new String[]{"ruined_portal/portal_1", "ruined_portal/portal_2", "ruined_portal/portal_3", "ruined_portal/portal_4", "ruined_portal/portal_5", "ruined_portal/portal_6", "ruined_portal/portal_7", "ruined_portal/portal_8", "ruined_portal/portal_9", "ruined_portal/portal_10"};
   private static final String[] STRUCTURE_LOCATION_GIANT_PORTALS = new String[]{"ruined_portal/giant_portal_1", "ruined_portal/giant_portal_2", "ruined_portal/giant_portal_3"};
   private static final float PROBABILITY_OF_GIANT_PORTAL = 0.05F;
   private static final int MIN_Y_INDEX = 15;
   private final List<RuinedPortalStructure.Setup> setups;
   public static final Codec<RuinedPortalStructure> CODEC = RecordCodecBuilder.create((var0) -> {
      return var0.group(settingsCodec(var0), ExtraCodecs.nonEmptyList(RuinedPortalStructure.Setup.CODEC.listOf()).fieldOf("setups").forGetter((var0x) -> {
         return var0x.setups;
      })).apply(var0, RuinedPortalStructure::new);
   });

   public RuinedPortalStructure(Structure.StructureSettings var1, List<RuinedPortalStructure.Setup> var2) {
      super(var1);
      this.setups = var2;
   }

   public RuinedPortalStructure(Structure.StructureSettings var1, RuinedPortalStructure.Setup var2) {
      this(var1, List.of(var2));
   }

   public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext var1) {
      RuinedPortalPiece.Properties var2 = new RuinedPortalPiece.Properties();
      WorldgenRandom var3 = var1.random();
      RuinedPortalStructure.Setup var4 = null;
      if (this.setups.size() > 1) {
         float var5 = 0.0F;

         RuinedPortalStructure.Setup var7;
         for(Iterator var6 = this.setups.iterator(); var6.hasNext(); var5 += var7.weight()) {
            var7 = (RuinedPortalStructure.Setup)var6.next();
         }

         float var20 = var3.nextFloat();
         Iterator var22 = this.setups.iterator();

         while(var22.hasNext()) {
            RuinedPortalStructure.Setup var8 = (RuinedPortalStructure.Setup)var22.next();
            var20 -= var8.weight() / var5;
            if (var20 < 0.0F) {
               var4 = var8;
               break;
            }
         }
      } else {
         var4 = (RuinedPortalStructure.Setup)this.setups.get(0);
      }

      if (var4 == null) {
         throw new IllegalStateException();
      } else {
         var2.airPocket = sample(var3, var4.airPocketProbability());
         var2.mossiness = var4.mossiness();
         var2.overgrown = var4.overgrown();
         var2.vines = var4.vines();
         var2.replaceWithBlackstone = var4.replaceWithBlackstone();
         ResourceLocation var21;
         if (var3.nextFloat() < 0.05F) {
            var21 = new ResourceLocation(STRUCTURE_LOCATION_GIANT_PORTALS[var3.nextInt(STRUCTURE_LOCATION_GIANT_PORTALS.length)]);
         } else {
            var21 = new ResourceLocation(STRUCTURE_LOCATION_PORTALS[var3.nextInt(STRUCTURE_LOCATION_PORTALS.length)]);
         }

         StructureTemplate var23 = var1.structureTemplateManager().getOrCreate(var21);
         Rotation var24 = (Rotation)Util.getRandom((Object[])Rotation.values(), var3);
         Mirror var9 = var3.nextFloat() < 0.5F ? Mirror.NONE : Mirror.FRONT_BACK;
         BlockPos var10 = new BlockPos(var23.getSize().getX() / 2, 0, var23.getSize().getZ() / 2);
         ChunkGenerator var11 = var1.chunkGenerator();
         LevelHeightAccessor var12 = var1.heightAccessor();
         RandomState var13 = var1.randomState();
         BlockPos var14 = var1.chunkPos().getWorldPosition();
         BoundingBox var15 = var23.getBoundingBox(var14, var24, var10, var9);
         BlockPos var16 = var15.getCenter();
         int var17 = var11.getBaseHeight(var16.getX(), var16.getZ(), RuinedPortalPiece.getHeightMapType(var4.placement()), var12, var13) - 1;
         int var18 = findSuitableY(var3, var11, var4.placement(), var2.airPocket, var17, var15.getYSpan(), var15, var12, var13);
         BlockPos var19 = new BlockPos(var14.getX(), var18, var14.getZ());
         return Optional.of(new Structure.GenerationStub(var19, (var10x) -> {
            if (var4.canBeCold()) {
               var2.cold = isCold(var19, var1.chunkGenerator().getBiomeSource().getNoiseBiome(QuartPos.fromBlock(var19.getX()), QuartPos.fromBlock(var19.getY()), QuartPos.fromBlock(var19.getZ()), var13.sampler()));
            }

            var10x.addPiece(new RuinedPortalPiece(var1.structureTemplateManager(), var19, var4.placement(), var2, var21, var23, var24, var9, var10));
         }));
      }
   }

   private static boolean sample(WorldgenRandom var0, float var1) {
      if (var1 == 0.0F) {
         return false;
      } else if (var1 == 1.0F) {
         return true;
      } else {
         return var0.nextFloat() < var1;
      }
   }

   private static boolean isCold(BlockPos var0, Holder<Biome> var1) {
      return ((Biome)var1.value()).coldEnoughToSnow(var0);
   }

   private static int findSuitableY(RandomSource var0, ChunkGenerator var1, RuinedPortalPiece.VerticalPlacement var2, boolean var3, int var4, int var5, BoundingBox var6, LevelHeightAccessor var7, RandomState var8) {
      int var10 = var7.getMinBuildHeight() + 15;
      int var9;
      if (var2 == RuinedPortalPiece.VerticalPlacement.IN_NETHER) {
         if (var3) {
            var9 = Mth.randomBetweenInclusive(var0, 32, 100);
         } else if (var0.nextFloat() < 0.5F) {
            var9 = Mth.randomBetweenInclusive(var0, 27, 29);
         } else {
            var9 = Mth.randomBetweenInclusive(var0, 29, 100);
         }
      } else {
         int var11;
         if (var2 == RuinedPortalPiece.VerticalPlacement.IN_MOUNTAIN) {
            var11 = var4 - var5;
            var9 = getRandomWithinInterval(var0, 70, var11);
         } else if (var2 == RuinedPortalPiece.VerticalPlacement.UNDERGROUND) {
            var11 = var4 - var5;
            var9 = getRandomWithinInterval(var0, var10, var11);
         } else if (var2 == RuinedPortalPiece.VerticalPlacement.PARTLY_BURIED) {
            var9 = var4 - var5 + Mth.randomBetweenInclusive(var0, 2, 8);
         } else {
            var9 = var4;
         }
      }

      ImmutableList var19 = ImmutableList.of(new BlockPos(var6.minX(), 0, var6.minZ()), new BlockPos(var6.maxX(), 0, var6.minZ()), new BlockPos(var6.minX(), 0, var6.maxZ()), new BlockPos(var6.maxX(), 0, var6.maxZ()));
      List var12 = (List)var19.stream().map((var3x) -> {
         return var1.getBaseColumn(var3x.getX(), var3x.getZ(), var7, var8);
      }).collect(Collectors.toList());
      Heightmap.Types var13 = var2 == RuinedPortalPiece.VerticalPlacement.ON_OCEAN_FLOOR ? Heightmap.Types.OCEAN_FLOOR_WG : Heightmap.Types.WORLD_SURFACE_WG;

      int var14;
      for(var14 = var9; var14 > var10; --var14) {
         int var15 = 0;
         Iterator var16 = var12.iterator();

         while(var16.hasNext()) {
            NoiseColumn var17 = (NoiseColumn)var16.next();
            BlockState var18 = var17.getBlock(var14);
            if (var13.isOpaque().test(var18)) {
               ++var15;
               if (var15 == 3) {
                  return var14;
               }
            }
         }
      }

      return var14;
   }

   private static int getRandomWithinInterval(RandomSource var0, int var1, int var2) {
      return var1 < var2 ? Mth.randomBetweenInclusive(var0, var1, var2) : var2;
   }

   public StructureType<?> type() {
      return StructureType.RUINED_PORTAL;
   }

   public static record Setup(RuinedPortalPiece.VerticalPlacement b, float c, float d, boolean e, boolean f, boolean g, boolean h, float i) {
      private final RuinedPortalPiece.VerticalPlacement placement;
      private final float airPocketProbability;
      private final float mossiness;
      private final boolean overgrown;
      private final boolean vines;
      private final boolean canBeCold;
      private final boolean replaceWithBlackstone;
      private final float weight;
      public static final Codec<RuinedPortalStructure.Setup> CODEC = RecordCodecBuilder.create((var0) -> {
         return var0.group(RuinedPortalPiece.VerticalPlacement.CODEC.fieldOf("placement").forGetter(RuinedPortalStructure.Setup::placement), Codec.floatRange(0.0F, 1.0F).fieldOf("air_pocket_probability").forGetter(RuinedPortalStructure.Setup::airPocketProbability), Codec.floatRange(0.0F, 1.0F).fieldOf("mossiness").forGetter(RuinedPortalStructure.Setup::mossiness), Codec.BOOL.fieldOf("overgrown").forGetter(RuinedPortalStructure.Setup::overgrown), Codec.BOOL.fieldOf("vines").forGetter(RuinedPortalStructure.Setup::vines), Codec.BOOL.fieldOf("can_be_cold").forGetter(RuinedPortalStructure.Setup::canBeCold), Codec.BOOL.fieldOf("replace_with_blackstone").forGetter(RuinedPortalStructure.Setup::replaceWithBlackstone), ExtraCodecs.POSITIVE_FLOAT.fieldOf("weight").forGetter(RuinedPortalStructure.Setup::weight)).apply(var0, RuinedPortalStructure.Setup::new);
      });

      public Setup(RuinedPortalPiece.VerticalPlacement var1, float var2, float var3, boolean var4, boolean var5, boolean var6, boolean var7, float var8) {
         this.placement = var1;
         this.airPocketProbability = var2;
         this.mossiness = var3;
         this.overgrown = var4;
         this.vines = var5;
         this.canBeCold = var6;
         this.replaceWithBlackstone = var7;
         this.weight = var8;
      }

      public RuinedPortalPiece.VerticalPlacement placement() {
         return this.placement;
      }

      public float airPocketProbability() {
         return this.airPocketProbability;
      }

      public float mossiness() {
         return this.mossiness;
      }

      public boolean overgrown() {
         return this.overgrown;
      }

      public boolean vines() {
         return this.vines;
      }

      public boolean canBeCold() {
         return this.canBeCold;
      }

      public boolean replaceWithBlackstone() {
         return this.replaceWithBlackstone;
      }

      public float weight() {
         return this.weight;
      }
   }
}
