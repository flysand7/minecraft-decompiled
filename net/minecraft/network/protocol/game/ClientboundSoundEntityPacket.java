package net.minecraft.network.protocol.game;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

public class ClientboundSoundEntityPacket implements Packet<ClientGamePacketListener> {
   private final Holder<SoundEvent> sound;
   private final SoundSource source;
   private final int id;
   private final float volume;
   private final float pitch;
   private final long seed;

   public ClientboundSoundEntityPacket(Holder<SoundEvent> var1, SoundSource var2, Entity var3, float var4, float var5, long var6) {
      this.sound = var1;
      this.source = var2;
      this.id = var3.getId();
      this.volume = var4;
      this.pitch = var5;
      this.seed = var6;
   }

   public ClientboundSoundEntityPacket(FriendlyByteBuf var1) {
      this.sound = var1.readById(BuiltInRegistries.SOUND_EVENT.asHolderIdMap(), SoundEvent::readFromNetwork);
      this.source = (SoundSource)var1.readEnum(SoundSource.class);
      this.id = var1.readVarInt();
      this.volume = var1.readFloat();
      this.pitch = var1.readFloat();
      this.seed = var1.readLong();
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeId(BuiltInRegistries.SOUND_EVENT.asHolderIdMap(), this.sound, (var0, var1x) -> {
         var1x.writeToNetwork(var0);
      });
      var1.writeEnum(this.source);
      var1.writeVarInt(this.id);
      var1.writeFloat(this.volume);
      var1.writeFloat(this.pitch);
      var1.writeLong(this.seed);
   }

   public Holder<SoundEvent> getSound() {
      return this.sound;
   }

   public SoundSource getSource() {
      return this.source;
   }

   public int getId() {
      return this.id;
   }

   public float getVolume() {
      return this.volume;
   }

   public float getPitch() {
      return this.pitch;
   }

   public long getSeed() {
      return this.seed;
   }

   public void handle(ClientGamePacketListener var1) {
      var1.handleSoundEntityEvent(this);
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void handle(PacketListener var1) {
      this.handle((ClientGamePacketListener)var1);
   }
}
