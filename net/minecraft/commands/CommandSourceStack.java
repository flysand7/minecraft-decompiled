package net.minecraft.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.ResultConsumer;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.TaskChainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class CommandSourceStack implements SharedSuggestionProvider {
   public static final SimpleCommandExceptionType ERROR_NOT_PLAYER = new SimpleCommandExceptionType(Component.translatable("permissions.requires.player"));
   public static final SimpleCommandExceptionType ERROR_NOT_ENTITY = new SimpleCommandExceptionType(Component.translatable("permissions.requires.entity"));
   private final CommandSource source;
   private final Vec3 worldPosition;
   private final ServerLevel level;
   private final int permissionLevel;
   private final String textName;
   private final Component displayName;
   private final MinecraftServer server;
   private final boolean silent;
   @Nullable
   private final Entity entity;
   @Nullable
   private final ResultConsumer<CommandSourceStack> consumer;
   private final EntityAnchorArgument.Anchor anchor;
   private final Vec2 rotation;
   private final CommandSigningContext signingContext;
   private final TaskChainer chatMessageChainer;
   private final IntConsumer returnValueConsumer;

   public CommandSourceStack(CommandSource var1, Vec3 var2, Vec2 var3, ServerLevel var4, int var5, String var6, Component var7, MinecraftServer var8, @Nullable Entity var9) {
      this(var1, var2, var3, var4, var5, var6, var7, var8, var9, false, (var0, var1x, var2x) -> {
      }, EntityAnchorArgument.Anchor.FEET, CommandSigningContext.ANONYMOUS, TaskChainer.immediate(var8), (var0) -> {
      });
   }

   protected CommandSourceStack(CommandSource var1, Vec3 var2, Vec2 var3, ServerLevel var4, int var5, String var6, Component var7, MinecraftServer var8, @Nullable Entity var9, boolean var10, @Nullable ResultConsumer<CommandSourceStack> var11, EntityAnchorArgument.Anchor var12, CommandSigningContext var13, TaskChainer var14, IntConsumer var15) {
      this.source = var1;
      this.worldPosition = var2;
      this.level = var4;
      this.silent = var10;
      this.entity = var9;
      this.permissionLevel = var5;
      this.textName = var6;
      this.displayName = var7;
      this.server = var8;
      this.consumer = var11;
      this.anchor = var12;
      this.rotation = var3;
      this.signingContext = var13;
      this.chatMessageChainer = var14;
      this.returnValueConsumer = var15;
   }

   public CommandSourceStack withSource(CommandSource var1) {
      return this.source == var1 ? this : new CommandSourceStack(var1, this.worldPosition, this.rotation, this.level, this.permissionLevel, this.textName, this.displayName, this.server, this.entity, this.silent, this.consumer, this.anchor, this.signingContext, this.chatMessageChainer, this.returnValueConsumer);
   }

   public CommandSourceStack withEntity(Entity var1) {
      return this.entity == var1 ? this : new CommandSourceStack(this.source, this.worldPosition, this.rotation, this.level, this.permissionLevel, var1.getName().getString(), var1.getDisplayName(), this.server, var1, this.silent, this.consumer, this.anchor, this.signingContext, this.chatMessageChainer, this.returnValueConsumer);
   }

   public CommandSourceStack withPosition(Vec3 var1) {
      return this.worldPosition.equals(var1) ? this : new CommandSourceStack(this.source, var1, this.rotation, this.level, this.permissionLevel, this.textName, this.displayName, this.server, this.entity, this.silent, this.consumer, this.anchor, this.signingContext, this.chatMessageChainer, this.returnValueConsumer);
   }

   public CommandSourceStack withRotation(Vec2 var1) {
      return this.rotation.equals(var1) ? this : new CommandSourceStack(this.source, this.worldPosition, var1, this.level, this.permissionLevel, this.textName, this.displayName, this.server, this.entity, this.silent, this.consumer, this.anchor, this.signingContext, this.chatMessageChainer, this.returnValueConsumer);
   }

   public CommandSourceStack withCallback(ResultConsumer<CommandSourceStack> var1) {
      return Objects.equals(this.consumer, var1) ? this : new CommandSourceStack(this.source, this.worldPosition, this.rotation, this.level, this.permissionLevel, this.textName, this.displayName, this.server, this.entity, this.silent, var1, this.anchor, this.signingContext, this.chatMessageChainer, this.returnValueConsumer);
   }

   public CommandSourceStack withCallback(ResultConsumer<CommandSourceStack> var1, BinaryOperator<ResultConsumer<CommandSourceStack>> var2) {
      ResultConsumer var3 = (ResultConsumer)var2.apply(this.consumer, var1);
      return this.withCallback(var3);
   }

   public CommandSourceStack withSuppressedOutput() {
      return !this.silent && !this.source.alwaysAccepts() ? new CommandSourceStack(this.source, this.worldPosition, this.rotation, this.level, this.permissionLevel, this.textName, this.displayName, this.server, this.entity, true, this.consumer, this.anchor, this.signingContext, this.chatMessageChainer, this.returnValueConsumer) : this;
   }

   public CommandSourceStack withPermission(int var1) {
      return var1 == this.permissionLevel ? this : new CommandSourceStack(this.source, this.worldPosition, this.rotation, this.level, var1, this.textName, this.displayName, this.server, this.entity, this.silent, this.consumer, this.anchor, this.signingContext, this.chatMessageChainer, this.returnValueConsumer);
   }

   public CommandSourceStack withMaximumPermission(int var1) {
      return var1 <= this.permissionLevel ? this : new CommandSourceStack(this.source, this.worldPosition, this.rotation, this.level, var1, this.textName, this.displayName, this.server, this.entity, this.silent, this.consumer, this.anchor, this.signingContext, this.chatMessageChainer, this.returnValueConsumer);
   }

   public CommandSourceStack withAnchor(EntityAnchorArgument.Anchor var1) {
      return var1 == this.anchor ? this : new CommandSourceStack(this.source, this.worldPosition, this.rotation, this.level, this.permissionLevel, this.textName, this.displayName, this.server, this.entity, this.silent, this.consumer, var1, this.signingContext, this.chatMessageChainer, this.returnValueConsumer);
   }

   public CommandSourceStack withLevel(ServerLevel var1) {
      if (var1 == this.level) {
         return this;
      } else {
         double var2 = DimensionType.getTeleportationScale(this.level.dimensionType(), var1.dimensionType());
         Vec3 var4 = new Vec3(this.worldPosition.x * var2, this.worldPosition.y, this.worldPosition.z * var2);
         return new CommandSourceStack(this.source, var4, this.rotation, var1, this.permissionLevel, this.textName, this.displayName, this.server, this.entity, this.silent, this.consumer, this.anchor, this.signingContext, this.chatMessageChainer, this.returnValueConsumer);
      }
   }

   public CommandSourceStack facing(Entity var1, EntityAnchorArgument.Anchor var2) {
      return this.facing(var2.apply(var1));
   }

   public CommandSourceStack facing(Vec3 var1) {
      Vec3 var2 = this.anchor.apply(this);
      double var3 = var1.x - var2.x;
      double var5 = var1.y - var2.y;
      double var7 = var1.z - var2.z;
      double var9 = Math.sqrt(var3 * var3 + var7 * var7);
      float var11 = Mth.wrapDegrees((float)(-(Mth.atan2(var5, var9) * 57.2957763671875D)));
      float var12 = Mth.wrapDegrees((float)(Mth.atan2(var7, var3) * 57.2957763671875D) - 90.0F);
      return this.withRotation(new Vec2(var11, var12));
   }

   public CommandSourceStack withSigningContext(CommandSigningContext var1) {
      return var1 == this.signingContext ? this : new CommandSourceStack(this.source, this.worldPosition, this.rotation, this.level, this.permissionLevel, this.textName, this.displayName, this.server, this.entity, this.silent, this.consumer, this.anchor, var1, this.chatMessageChainer, this.returnValueConsumer);
   }

   public CommandSourceStack withChatMessageChainer(TaskChainer var1) {
      return var1 == this.chatMessageChainer ? this : new CommandSourceStack(this.source, this.worldPosition, this.rotation, this.level, this.permissionLevel, this.textName, this.displayName, this.server, this.entity, this.silent, this.consumer, this.anchor, this.signingContext, var1, this.returnValueConsumer);
   }

   public CommandSourceStack withReturnValueConsumer(IntConsumer var1) {
      return var1 == this.returnValueConsumer ? this : new CommandSourceStack(this.source, this.worldPosition, this.rotation, this.level, this.permissionLevel, this.textName, this.displayName, this.server, this.entity, this.silent, this.consumer, this.anchor, this.signingContext, this.chatMessageChainer, var1);
   }

   public Component getDisplayName() {
      return this.displayName;
   }

   public String getTextName() {
      return this.textName;
   }

   public boolean hasPermission(int var1) {
      return this.permissionLevel >= var1;
   }

   public Vec3 getPosition() {
      return this.worldPosition;
   }

   public ServerLevel getLevel() {
      return this.level;
   }

   @Nullable
   public Entity getEntity() {
      return this.entity;
   }

   public Entity getEntityOrException() throws CommandSyntaxException {
      if (this.entity == null) {
         throw ERROR_NOT_ENTITY.create();
      } else {
         return this.entity;
      }
   }

   public ServerPlayer getPlayerOrException() throws CommandSyntaxException {
      Entity var2 = this.entity;
      if (var2 instanceof ServerPlayer) {
         ServerPlayer var1 = (ServerPlayer)var2;
         return var1;
      } else {
         throw ERROR_NOT_PLAYER.create();
      }
   }

   @Nullable
   public ServerPlayer getPlayer() {
      Entity var2 = this.entity;
      ServerPlayer var10000;
      if (var2 instanceof ServerPlayer) {
         ServerPlayer var1 = (ServerPlayer)var2;
         var10000 = var1;
      } else {
         var10000 = null;
      }

      return var10000;
   }

   public boolean isPlayer() {
      return this.entity instanceof ServerPlayer;
   }

   public Vec2 getRotation() {
      return this.rotation;
   }

   public MinecraftServer getServer() {
      return this.server;
   }

   public EntityAnchorArgument.Anchor getAnchor() {
      return this.anchor;
   }

   public CommandSigningContext getSigningContext() {
      return this.signingContext;
   }

   public TaskChainer getChatMessageChainer() {
      return this.chatMessageChainer;
   }

   public IntConsumer getReturnValueConsumer() {
      return this.returnValueConsumer;
   }

   public boolean shouldFilterMessageTo(ServerPlayer var1) {
      ServerPlayer var2 = this.getPlayer();
      if (var1 == var2) {
         return false;
      } else {
         return var2 != null && var2.isTextFilteringEnabled() || var1.isTextFilteringEnabled();
      }
   }

   public void sendChatMessage(OutgoingChatMessage var1, boolean var2, ChatType.Bound var3) {
      if (!this.silent) {
         ServerPlayer var4 = this.getPlayer();
         if (var4 != null) {
            var4.sendChatMessage(var1, var2, var3);
         } else {
            this.source.sendSystemMessage(var3.decorate(var1.content()));
         }

      }
   }

   public void sendSystemMessage(Component var1) {
      if (!this.silent) {
         ServerPlayer var2 = this.getPlayer();
         if (var2 != null) {
            var2.sendSystemMessage(var1);
         } else {
            this.source.sendSystemMessage(var1);
         }

      }
   }

   public void sendSuccess(Supplier<Component> var1, boolean var2) {
      boolean var3 = this.source.acceptsSuccess() && !this.silent;
      boolean var4 = var2 && this.source.shouldInformAdmins() && !this.silent;
      if (var3 || var4) {
         Component var5 = (Component)var1.get();
         if (var3) {
            this.source.sendSystemMessage(var5);
         }

         if (var4) {
            this.broadcastToAdmins(var5);
         }

      }
   }

   private void broadcastToAdmins(Component var1) {
      MutableComponent var2 = Component.translatable("chat.type.admin", this.getDisplayName(), var1).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
      if (this.server.getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK)) {
         Iterator var3 = this.server.getPlayerList().getPlayers().iterator();

         while(var3.hasNext()) {
            ServerPlayer var4 = (ServerPlayer)var3.next();
            if (var4 != this.source && this.server.getPlayerList().isOp(var4.getGameProfile())) {
               var4.sendSystemMessage(var2);
            }
         }
      }

      if (this.source != this.server && this.server.getGameRules().getBoolean(GameRules.RULE_LOGADMINCOMMANDS)) {
         this.server.sendSystemMessage(var2);
      }

   }

   public void sendFailure(Component var1) {
      if (this.source.acceptsFailure() && !this.silent) {
         this.source.sendSystemMessage(Component.empty().append(var1).withStyle(ChatFormatting.RED));
      }

   }

   public void onCommandComplete(CommandContext<CommandSourceStack> var1, boolean var2, int var3) {
      if (this.consumer != null) {
         this.consumer.onCommandComplete(var1, var2, var3);
      }

   }

   public Collection<String> getOnlinePlayerNames() {
      return Lists.newArrayList(this.server.getPlayerNames());
   }

   public Collection<String> getAllTeams() {
      return this.server.getScoreboard().getTeamNames();
   }

   public Stream<ResourceLocation> getAvailableSounds() {
      return BuiltInRegistries.SOUND_EVENT.stream().map(SoundEvent::getLocation);
   }

   public Stream<ResourceLocation> getRecipeNames() {
      return this.server.getRecipeManager().getRecipeIds();
   }

   public CompletableFuture<Suggestions> customSuggestion(CommandContext<?> var1) {
      return Suggestions.empty();
   }

   public CompletableFuture<Suggestions> suggestRegistryElements(ResourceKey<? extends Registry<?>> var1, SharedSuggestionProvider.ElementSuggestionType var2, SuggestionsBuilder var3, CommandContext<?> var4) {
      return (CompletableFuture)this.registryAccess().registry(var1).map((var3x) -> {
         this.suggestRegistryElements(var3x, var2, var3);
         return var3.buildFuture();
      }).orElseGet(Suggestions::empty);
   }

   public Set<ResourceKey<Level>> levels() {
      return this.server.levelKeys();
   }

   public RegistryAccess registryAccess() {
      return this.server.registryAccess();
   }

   public FeatureFlagSet enabledFeatures() {
      return this.level.enabledFeatures();
   }
}
