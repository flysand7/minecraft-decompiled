package net.minecraft.client.multiplayer.chat;

import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.minecraft.util.StringRepresentable;

public interface LoggedChatEvent {
   Codec<LoggedChatEvent> CODEC = StringRepresentable.fromEnum(LoggedChatEvent.Type::values).dispatch(LoggedChatEvent::type, LoggedChatEvent.Type::codec);

   LoggedChatEvent.Type type();

   public static enum Type implements StringRepresentable {
      PLAYER("player", () -> {
         return LoggedChatMessage.Player.CODEC;
      }),
      SYSTEM("system", () -> {
         return LoggedChatMessage.System.CODEC;
      });

      private final String serializedName;
      private final Supplier<Codec<? extends LoggedChatEvent>> codec;

      private Type(String var3, Supplier<Codec<? extends LoggedChatEvent>> var4) {
         this.serializedName = var3;
         this.codec = var4;
      }

      private Codec<? extends LoggedChatEvent> codec() {
         return (Codec)this.codec.get();
      }

      public String getSerializedName() {
         return this.serializedName;
      }

      // $FF: synthetic method
      private static LoggedChatEvent.Type[] $values() {
         return new LoggedChatEvent.Type[]{PLAYER, SYSTEM};
      }
   }
}
