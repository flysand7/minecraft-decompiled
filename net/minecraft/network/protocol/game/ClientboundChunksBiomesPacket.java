package net.minecraft.network.protocol.game;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

public record ClientboundChunksBiomesPacket(List<ClientboundChunksBiomesPacket.ChunkBiomeData> a) implements Packet<ClientGamePacketListener> {
   private final List<ClientboundChunksBiomesPacket.ChunkBiomeData> chunkBiomeData;
   private static final int TWO_MEGABYTES = 2097152;

   public ClientboundChunksBiomesPacket(FriendlyByteBuf var1) {
      this(var1.readList(ClientboundChunksBiomesPacket.ChunkBiomeData::new));
   }

   public ClientboundChunksBiomesPacket(List<ClientboundChunksBiomesPacket.ChunkBiomeData> var1) {
      this.chunkBiomeData = var1;
   }

   public static ClientboundChunksBiomesPacket forChunks(List<LevelChunk> var0) {
      return new ClientboundChunksBiomesPacket(var0.stream().map(ClientboundChunksBiomesPacket.ChunkBiomeData::new).toList());
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeCollection(this.chunkBiomeData, (var0, var1x) -> {
         var1x.write(var0);
      });
   }

   public void handle(ClientGamePacketListener var1) {
      var1.handleChunksBiomes(this);
   }

   public List<ClientboundChunksBiomesPacket.ChunkBiomeData> chunkBiomeData() {
      return this.chunkBiomeData;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void handle(PacketListener var1) {
      this.handle((ClientGamePacketListener)var1);
   }

   public static record ChunkBiomeData(ChunkPos a, byte[] b) {
      private final ChunkPos pos;
      private final byte[] buffer;

      public ChunkBiomeData(LevelChunk var1) {
         this(var1.getPos(), new byte[calculateChunkSize(var1)]);
         extractChunkData(new FriendlyByteBuf(this.getWriteBuffer()), var1);
      }

      public ChunkBiomeData(FriendlyByteBuf var1) {
         this(var1.readChunkPos(), var1.readByteArray(2097152));
      }

      public ChunkBiomeData(ChunkPos var1, byte[] var2) {
         this.pos = var1;
         this.buffer = var2;
      }

      private static int calculateChunkSize(LevelChunk var0) {
         int var1 = 0;
         LevelChunkSection[] var2 = var0.getSections();
         int var3 = var2.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            LevelChunkSection var5 = var2[var4];
            var1 += var5.getBiomes().getSerializedSize();
         }

         return var1;
      }

      public FriendlyByteBuf getReadBuffer() {
         return new FriendlyByteBuf(Unpooled.wrappedBuffer(this.buffer));
      }

      private ByteBuf getWriteBuffer() {
         ByteBuf var1 = Unpooled.wrappedBuffer(this.buffer);
         var1.writerIndex(0);
         return var1;
      }

      public static void extractChunkData(FriendlyByteBuf var0, LevelChunk var1) {
         LevelChunkSection[] var2 = var1.getSections();
         int var3 = var2.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            LevelChunkSection var5 = var2[var4];
            var5.getBiomes().write(var0);
         }

      }

      public void write(FriendlyByteBuf var1) {
         var1.writeChunkPos(this.pos);
         var1.writeByteArray(this.buffer);
      }

      public ChunkPos pos() {
         return this.pos;
      }

      public byte[] buffer() {
         return this.buffer;
      }
   }
}
