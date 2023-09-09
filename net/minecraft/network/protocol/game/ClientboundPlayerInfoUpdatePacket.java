package net.minecraft.network.protocol.game;

import com.google.common.base.MoreObjects;
import com.mojang.authlib.GameProfile;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.Optionull;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.RemoteChatSession;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

public class ClientboundPlayerInfoUpdatePacket implements Packet<ClientGamePacketListener> {
   private final EnumSet<ClientboundPlayerInfoUpdatePacket.Action> actions;
   private final List<ClientboundPlayerInfoUpdatePacket.Entry> entries;

   public ClientboundPlayerInfoUpdatePacket(EnumSet<ClientboundPlayerInfoUpdatePacket.Action> var1, Collection<ServerPlayer> var2) {
      this.actions = var1;
      this.entries = var2.stream().map(ClientboundPlayerInfoUpdatePacket.Entry::new).toList();
   }

   public ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action var1, ServerPlayer var2) {
      this.actions = EnumSet.of(var1);
      this.entries = List.of(new ClientboundPlayerInfoUpdatePacket.Entry(var2));
   }

   public static ClientboundPlayerInfoUpdatePacket createPlayerInitializing(Collection<ServerPlayer> var0) {
      EnumSet var1 = EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, ClientboundPlayerInfoUpdatePacket.Action.INITIALIZE_CHAT, ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE, ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED, ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY, ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME);
      return new ClientboundPlayerInfoUpdatePacket(var1, var0);
   }

   public ClientboundPlayerInfoUpdatePacket(FriendlyByteBuf var1) {
      this.actions = var1.readEnumSet(ClientboundPlayerInfoUpdatePacket.Action.class);
      this.entries = var1.readList((var1x) -> {
         ClientboundPlayerInfoUpdatePacket.EntryBuilder var2 = new ClientboundPlayerInfoUpdatePacket.EntryBuilder(var1x.readUUID());
         Iterator var3 = this.actions.iterator();

         while(var3.hasNext()) {
            ClientboundPlayerInfoUpdatePacket.Action var4 = (ClientboundPlayerInfoUpdatePacket.Action)var3.next();
            var4.reader.read(var2, var1x);
         }

         return var2.build();
      });
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeEnumSet(this.actions, ClientboundPlayerInfoUpdatePacket.Action.class);
      var1.writeCollection(this.entries, (var1x, var2) -> {
         var1x.writeUUID(var2.profileId());
         Iterator var3 = this.actions.iterator();

         while(var3.hasNext()) {
            ClientboundPlayerInfoUpdatePacket.Action var4 = (ClientboundPlayerInfoUpdatePacket.Action)var3.next();
            var4.writer.write(var1x, var2);
         }

      });
   }

   public void handle(ClientGamePacketListener var1) {
      var1.handlePlayerInfoUpdate(this);
   }

   public EnumSet<ClientboundPlayerInfoUpdatePacket.Action> actions() {
      return this.actions;
   }

   public List<ClientboundPlayerInfoUpdatePacket.Entry> entries() {
      return this.entries;
   }

   public List<ClientboundPlayerInfoUpdatePacket.Entry> newEntries() {
      return this.actions.contains(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER) ? this.entries : List.of();
   }

   public String toString() {
      return MoreObjects.toStringHelper(this).add("actions", this.actions).add("entries", this.entries).toString();
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void handle(PacketListener var1) {
      this.handle((ClientGamePacketListener)var1);
   }

   public static record Entry(UUID a, GameProfile b, boolean c, int d, GameType e, @Nullable Component f, @Nullable RemoteChatSession.Data g) {
      private final UUID profileId;
      private final GameProfile profile;
      private final boolean listed;
      private final int latency;
      private final GameType gameMode;
      @Nullable
      private final Component displayName;
      @Nullable
      final RemoteChatSession.Data chatSession;

      Entry(ServerPlayer var1) {
         this(var1.getUUID(), var1.getGameProfile(), true, var1.latency, var1.gameMode.getGameModeForPlayer(), var1.getTabListDisplayName(), (RemoteChatSession.Data)Optionull.map(var1.getChatSession(), RemoteChatSession::asData));
      }

      public Entry(UUID var1, GameProfile var2, boolean var3, int var4, GameType var5, @Nullable Component var6, @Nullable RemoteChatSession.Data var7) {
         this.profileId = var1;
         this.profile = var2;
         this.listed = var3;
         this.latency = var4;
         this.gameMode = var5;
         this.displayName = var6;
         this.chatSession = var7;
      }

      public UUID profileId() {
         return this.profileId;
      }

      public GameProfile profile() {
         return this.profile;
      }

      public boolean listed() {
         return this.listed;
      }

      public int latency() {
         return this.latency;
      }

      public GameType gameMode() {
         return this.gameMode;
      }

      @Nullable
      public Component displayName() {
         return this.displayName;
      }

      @Nullable
      public RemoteChatSession.Data chatSession() {
         return this.chatSession;
      }
   }

   public static enum Action {
      ADD_PLAYER((var0, var1) -> {
         GameProfile var2 = new GameProfile(var0.profileId, var1.readUtf(16));
         var2.getProperties().putAll(var1.readGameProfileProperties());
         var0.profile = var2;
      }, (var0, var1) -> {
         var0.writeUtf(var1.profile().getName(), 16);
         var0.writeGameProfileProperties(var1.profile().getProperties());
      }),
      INITIALIZE_CHAT((var0, var1) -> {
         var0.chatSession = (RemoteChatSession.Data)var1.readNullable(RemoteChatSession.Data::read);
      }, (var0, var1) -> {
         var0.writeNullable(var1.chatSession, RemoteChatSession.Data::write);
      }),
      UPDATE_GAME_MODE((var0, var1) -> {
         var0.gameMode = GameType.byId(var1.readVarInt());
      }, (var0, var1) -> {
         var0.writeVarInt(var1.gameMode().getId());
      }),
      UPDATE_LISTED((var0, var1) -> {
         var0.listed = var1.readBoolean();
      }, (var0, var1) -> {
         var0.writeBoolean(var1.listed());
      }),
      UPDATE_LATENCY((var0, var1) -> {
         var0.latency = var1.readVarInt();
      }, (var0, var1) -> {
         var0.writeVarInt(var1.latency());
      }),
      UPDATE_DISPLAY_NAME((var0, var1) -> {
         var0.displayName = (Component)var1.readNullable(FriendlyByteBuf::readComponent);
      }, (var0, var1) -> {
         var0.writeNullable(var1.displayName(), FriendlyByteBuf::writeComponent);
      });

      final ClientboundPlayerInfoUpdatePacket.Action.Reader reader;
      final ClientboundPlayerInfoUpdatePacket.Action.Writer writer;

      private Action(ClientboundPlayerInfoUpdatePacket.Action.Reader var3, ClientboundPlayerInfoUpdatePacket.Action.Writer var4) {
         this.reader = var3;
         this.writer = var4;
      }

      // $FF: synthetic method
      private static ClientboundPlayerInfoUpdatePacket.Action[] $values() {
         return new ClientboundPlayerInfoUpdatePacket.Action[]{ADD_PLAYER, INITIALIZE_CHAT, UPDATE_GAME_MODE, UPDATE_LISTED, UPDATE_LATENCY, UPDATE_DISPLAY_NAME};
      }

      public interface Reader {
         void read(ClientboundPlayerInfoUpdatePacket.EntryBuilder var1, FriendlyByteBuf var2);
      }

      public interface Writer {
         void write(FriendlyByteBuf var1, ClientboundPlayerInfoUpdatePacket.Entry var2);
      }
   }

   private static class EntryBuilder {
      final UUID profileId;
      GameProfile profile;
      boolean listed;
      int latency;
      GameType gameMode;
      @Nullable
      Component displayName;
      @Nullable
      RemoteChatSession.Data chatSession;

      EntryBuilder(UUID var1) {
         this.gameMode = GameType.DEFAULT_MODE;
         this.profileId = var1;
         this.profile = new GameProfile(var1, (String)null);
      }

      ClientboundPlayerInfoUpdatePacket.Entry build() {
         return new ClientboundPlayerInfoUpdatePacket.Entry(this.profileId, this.profile, this.listed, this.latency, this.gameMode, this.displayName, this.chatSession);
      }
   }
}
