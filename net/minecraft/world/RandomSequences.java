package net.minecraft.world;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.Logger;

public class RandomSequences extends SavedData {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final long seed;
   private final Map<ResourceLocation, RandomSequence> sequences = new Object2ObjectOpenHashMap();

   public RandomSequences(long var1) {
      this.seed = var1;
   }

   public RandomSource get(ResourceLocation var1) {
      final RandomSource var2 = ((RandomSequence)this.sequences.computeIfAbsent(var1, (var1x) -> {
         return new RandomSequence(this.seed, var1x);
      })).random();
      return new RandomSource() {
         public RandomSource fork() {
            RandomSequences.this.setDirty();
            return var2.fork();
         }

         public PositionalRandomFactory forkPositional() {
            RandomSequences.this.setDirty();
            return var2.forkPositional();
         }

         public void setSeed(long var1) {
            RandomSequences.this.setDirty();
            var2.setSeed(var1);
         }

         public int nextInt() {
            RandomSequences.this.setDirty();
            return var2.nextInt();
         }

         public int nextInt(int var1) {
            RandomSequences.this.setDirty();
            return var2.nextInt(var1);
         }

         public long nextLong() {
            RandomSequences.this.setDirty();
            return var2.nextLong();
         }

         public boolean nextBoolean() {
            RandomSequences.this.setDirty();
            return var2.nextBoolean();
         }

         public float nextFloat() {
            RandomSequences.this.setDirty();
            return var2.nextFloat();
         }

         public double nextDouble() {
            RandomSequences.this.setDirty();
            return var2.nextDouble();
         }

         public double nextGaussian() {
            RandomSequences.this.setDirty();
            return var2.nextGaussian();
         }
      };
   }

   public CompoundTag save(CompoundTag var1) {
      this.sequences.forEach((var1x, var2) -> {
         var1.put(var1x.toString(), (Tag)RandomSequence.CODEC.encodeStart(NbtOps.INSTANCE, var2).result().orElseThrow());
      });
      return var1;
   }

   public static RandomSequences load(long var0, CompoundTag var2) {
      RandomSequences var3 = new RandomSequences(var0);
      Set var4 = var2.getAllKeys();
      Iterator var5 = var4.iterator();

      while(var5.hasNext()) {
         String var6 = (String)var5.next();

         try {
            RandomSequence var7 = (RandomSequence)((Pair)RandomSequence.CODEC.decode(NbtOps.INSTANCE, var2.get(var6)).result().get()).getFirst();
            var3.sequences.put(new ResourceLocation(var6), var7);
         } catch (Exception var8) {
            LOGGER.error("Failed to load random sequence {}", var6, var8);
         }
      }

      return var3;
   }
}
