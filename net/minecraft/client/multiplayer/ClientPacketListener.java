package net.minecraft.client.multiplayer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.logging.LogUtils;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.advancements.Advancement;
import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.DebugQueryHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.MapRenderer;
import net.minecraft.client.gui.components.toasts.RecipeToast;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.DemoIntroScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.client.gui.screens.achievement.StatsUpdateListener;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.CommandBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.HorseInventoryScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.client.particle.ItemPickupParticle;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.debug.BeeDebugRenderer;
import net.minecraft.client.renderer.debug.BrainDebugRenderer;
import net.minecraft.client.renderer.debug.GoalSelectorDebugRenderer;
import net.minecraft.client.renderer.debug.NeighborsUpdateRenderer;
import net.minecraft.client.renderer.debug.WorldGenAttemptRenderer;
import net.minecraft.client.resources.sounds.BeeAggressiveSoundInstance;
import net.minecraft.client.resources.sounds.BeeFlyingSoundInstance;
import net.minecraft.client.resources.sounds.GuardianAttackSoundInstance;
import net.minecraft.client.resources.sounds.MinecartSoundInstance;
import net.minecraft.client.resources.sounds.SnifferSoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.searchtree.SearchRegistry;
import net.minecraft.client.telemetry.WorldSessionTelemetryManager;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ArgumentSignatures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.PositionImpl;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.TickablePacketListener;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.LastSeenMessagesTracker;
import net.minecraft.network.chat.LocalChatSession;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.MessageSignatureCache;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.RemoteChatSession;
import net.minecraft.network.chat.SignableCommand;
import net.minecraft.network.chat.SignedMessageBody;
import net.minecraft.network.chat.SignedMessageChain;
import net.minecraft.network.chat.SignedMessageLink;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundAddExperienceOrbPacket;
import net.minecraft.network.protocol.game.ClientboundAddPlayerPacket;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundAwardStatsPacket;
import net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundCooldownPacket;
import net.minecraft.network.protocol.game.ClientboundCustomChatCompletionsPacket;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundDeleteChatPacket;
import net.minecraft.network.protocol.game.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.game.ClientboundDisguisedChatPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundHorseScreenOpenPacket;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.network.protocol.game.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacketData;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ClientboundOpenBookPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket;
import net.minecraft.network.protocol.game.ClientboundPingPacket;
import net.minecraft.network.protocol.game.ClientboundPlaceGhostRecipePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatEndPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatEnterPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerLookAtPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundRecipePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundResourcePackPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSelectAdvancementsTabPacket;
import net.minecraft.network.protocol.game.ClientboundServerDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderLerpSizePacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderSizePacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDelayPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDistancePacket;
import net.minecraft.network.protocol.game.ClientboundSetCameraPacket;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.network.protocol.game.ClientboundSetSimulationDistancePacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.network.protocol.game.ClientboundTagQueryPacket;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateEnabledFeaturesPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateTagsPacket;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundChatAckPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundChatSessionUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPongPacket;
import net.minecraft.network.protocol.game.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.game.VecDeltaCodec;
import net.minecraft.realms.DisconnectedRealmsScreen;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatsCounter;
import net.minecraft.tags.TagNetworkSerialization;
import net.minecraft.util.Crypt;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.SignatureValidator;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.ProfileKeyPair;
import net.minecraft.world.entity.player.ProfilePublicKey;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.PositionSourceType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.slf4j.Logger;

public class ClientPacketListener implements TickablePacketListener, ClientGamePacketListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component GENERIC_DISCONNECT_MESSAGE = Component.translatable("disconnect.lost");
   private static final Component UNSECURE_SERVER_TOAST_TITLE = Component.translatable("multiplayer.unsecureserver.toast.title");
   private static final Component UNSERURE_SERVER_TOAST = Component.translatable("multiplayer.unsecureserver.toast");
   private static final Component INVALID_PACKET = Component.translatable("multiplayer.disconnect.invalid_packet");
   private static final Component CHAT_VALIDATION_FAILED_ERROR = Component.translatable("multiplayer.disconnect.chat_validation_failed");
   private static final int PENDING_OFFSET_THRESHOLD = 64;
   private final Connection connection;
   private final List<ClientPacketListener.DeferredPacket> deferredPackets = new ArrayList();
   @Nullable
   private final ServerData serverData;
   private final GameProfile localGameProfile;
   private final Screen callbackScreen;
   private final Minecraft minecraft;
   private ClientLevel level;
   private ClientLevel.ClientLevelData levelData;
   private final Map<UUID, PlayerInfo> playerInfoMap = Maps.newHashMap();
   private final Set<PlayerInfo> listedPlayers = new ReferenceOpenHashSet();
   private final ClientAdvancements advancements;
   private final ClientSuggestionProvider suggestionsProvider;
   private final DebugQueryHandler debugQueryHandler = new DebugQueryHandler(this);
   private int serverChunkRadius = 3;
   private int serverSimulationDistance = 3;
   private final RandomSource random = RandomSource.createThreadSafe();
   private CommandDispatcher<SharedSuggestionProvider> commands = new CommandDispatcher();
   private final RecipeManager recipeManager = new RecipeManager();
   private final UUID id = UUID.randomUUID();
   private Set<ResourceKey<Level>> levels;
   private LayeredRegistryAccess<ClientRegistryLayer> registryAccess = ClientRegistryLayer.createRegistryAccess();
   private FeatureFlagSet enabledFeatures;
   private final WorldSessionTelemetryManager telemetryManager;
   @Nullable
   private LocalChatSession chatSession;
   private SignedMessageChain.Encoder signedMessageEncoder;
   private LastSeenMessagesTracker lastSeenMessages;
   private MessageSignatureCache messageSignatureCache;

   public ClientPacketListener(Minecraft var1, Screen var2, Connection var3, @Nullable ServerData var4, GameProfile var5, WorldSessionTelemetryManager var6) {
      this.enabledFeatures = FeatureFlags.DEFAULT_FLAGS;
      this.signedMessageEncoder = SignedMessageChain.Encoder.UNSIGNED;
      this.lastSeenMessages = new LastSeenMessagesTracker(20);
      this.messageSignatureCache = MessageSignatureCache.createDefault();
      this.minecraft = var1;
      this.callbackScreen = var2;
      this.connection = var3;
      this.serverData = var4;
      this.localGameProfile = var5;
      this.advancements = new ClientAdvancements(var1, var6);
      this.suggestionsProvider = new ClientSuggestionProvider(this, var1);
      this.telemetryManager = var6;
   }

   public ClientSuggestionProvider getSuggestionsProvider() {
      return this.suggestionsProvider;
   }

   public void close() {
      this.level = null;
      this.telemetryManager.onDisconnect();
   }

   public RecipeManager getRecipeManager() {
      return this.recipeManager;
   }

   public void handleLogin(ClientboundLoginPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      this.minecraft.gameMode = new MultiPlayerGameMode(this.minecraft, this);
      this.registryAccess = this.registryAccess.replaceFrom(ClientRegistryLayer.REMOTE, (RegistryAccess.Frozen[])(var1.registryHolder()));
      if (!this.connection.isMemoryConnection()) {
         this.registryAccess.compositeAccess().registries().forEach((var0) -> {
            var0.value().resetTags();
         });
      }

      ArrayList var2 = Lists.newArrayList(var1.levels());
      Collections.shuffle(var2);
      this.levels = Sets.newLinkedHashSet(var2);
      ResourceKey var3 = var1.dimension();
      Holder.Reference var4 = this.registryAccess.compositeAccess().registryOrThrow(Registries.DIMENSION_TYPE).getHolderOrThrow(var1.dimensionType());
      this.serverChunkRadius = var1.chunkRadius();
      this.serverSimulationDistance = var1.simulationDistance();
      boolean var5 = var1.isDebug();
      boolean var6 = var1.isFlat();
      ClientLevel.ClientLevelData var7 = new ClientLevel.ClientLevelData(Difficulty.NORMAL, var1.hardcore(), var6);
      this.levelData = var7;
      int var10007 = this.serverChunkRadius;
      int var10008 = this.serverSimulationDistance;
      Minecraft var10009 = this.minecraft;
      Objects.requireNonNull(var10009);
      this.level = new ClientLevel(this, var7, var3, var4, var10007, var10008, var10009::getProfiler, this.minecraft.levelRenderer, var5, var1.seed());
      this.minecraft.setLevel(this.level);
      if (this.minecraft.player == null) {
         this.minecraft.player = this.minecraft.gameMode.createPlayer(this.level, new StatsCounter(), new ClientRecipeBook());
         this.minecraft.player.setYRot(-180.0F);
         if (this.minecraft.getSingleplayerServer() != null) {
            this.minecraft.getSingleplayerServer().setUUID(this.minecraft.player.getUUID());
         }
      }

      this.minecraft.debugRenderer.clear();
      this.minecraft.player.resetPos();
      int var8 = var1.playerId();
      this.minecraft.player.setId(var8);
      this.level.addPlayer(var8, this.minecraft.player);
      this.minecraft.player.input = new KeyboardInput(this.minecraft.options);
      this.minecraft.gameMode.adjustPlayer(this.minecraft.player);
      this.minecraft.cameraEntity = this.minecraft.player;
      this.minecraft.setScreen(new ReceivingLevelScreen());
      this.minecraft.player.setReducedDebugInfo(var1.reducedDebugInfo());
      this.minecraft.player.setShowDeathScreen(var1.showDeathScreen());
      this.minecraft.player.setLastDeathLocation(var1.lastDeathLocation());
      this.minecraft.player.setPortalCooldown(var1.portalCooldown());
      this.minecraft.gameMode.setLocalMode(var1.gameType(), var1.previousGameType());
      this.minecraft.options.setServerRenderDistance(var1.chunkRadius());
      this.minecraft.options.broadcastOptions();
      this.connection.send(new ServerboundCustomPayloadPacket(ServerboundCustomPayloadPacket.BRAND, (new FriendlyByteBuf(Unpooled.buffer())).writeUtf(ClientBrandRetriever.getClientModName())));
      this.chatSession = null;
      this.lastSeenMessages = new LastSeenMessagesTracker(20);
      this.messageSignatureCache = MessageSignatureCache.createDefault();
      if (this.connection.isEncrypted()) {
         this.minecraft.getProfileKeyPairManager().prepareKeyPair().thenAcceptAsync((var1x) -> {
            var1x.ifPresent(this::setKeyPair);
         }, this.minecraft);
      }

      this.telemetryManager.onPlayerInfoReceived(var1.gameType(), var1.hardcore());
      this.minecraft.quickPlayLog().log(this.minecraft);
   }

   public void handleAddEntity(ClientboundAddEntityPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      EntityType var2 = var1.getType();
      Entity var3 = var2.create(this.level);
      if (var3 != null) {
         var3.recreateFromPacket(var1);
         int var4 = var1.getId();
         this.level.putNonPlayerEntity(var4, var3);
         this.postAddEntitySoundInstance(var3);
      } else {
         LOGGER.warn("Skipping Entity with id {}", var2);
      }

   }

   private void postAddEntitySoundInstance(Entity var1) {
      if (var1 instanceof AbstractMinecart) {
         this.minecraft.getSoundManager().play(new MinecartSoundInstance((AbstractMinecart)var1));
      } else if (var1 instanceof Bee) {
         boolean var2 = ((Bee)var1).isAngry();
         Object var3;
         if (var2) {
            var3 = new BeeAggressiveSoundInstance((Bee)var1);
         } else {
            var3 = new BeeFlyingSoundInstance((Bee)var1);
         }

         this.minecraft.getSoundManager().queueTickingSound((TickableSoundInstance)var3);
      }

   }

   public void handleAddExperienceOrb(ClientboundAddExperienceOrbPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      double var2 = var1.getX();
      double var4 = var1.getY();
      double var6 = var1.getZ();
      ExperienceOrb var8 = new ExperienceOrb(this.level, var2, var4, var6, var1.getValue());
      var8.syncPacketPositionCodec(var2, var4, var6);
      var8.setYRot(0.0F);
      var8.setXRot(0.0F);
      var8.setId(var1.getId());
      this.level.putNonPlayerEntity(var1.getId(), var8);
   }

   public void handleSetEntityMotion(ClientboundSetEntityMotionPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      Entity var2 = this.level.getEntity(var1.getId());
      if (var2 != null) {
         var2.lerpMotion((double)var1.getXa() / 8000.0D, (double)var1.getYa() / 8000.0D, (double)var1.getZa() / 8000.0D);
      }
   }

   public void handleSetEntityData(ClientboundSetEntityDataPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      Entity var2 = this.level.getEntity(var1.id());
      if (var2 != null) {
         var2.getEntityData().assignValues(var1.packedItems());
      }

   }

   public void handleAddPlayer(ClientboundAddPlayerPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      PlayerInfo var2 = this.getPlayerInfo(var1.getPlayerId());
      if (var2 == null) {
         LOGGER.warn("Server attempted to add player prior to sending player info (Player id {})", var1.getPlayerId());
      } else {
         double var3 = var1.getX();
         double var5 = var1.getY();
         double var7 = var1.getZ();
         float var9 = (float)(var1.getyRot() * 360) / 256.0F;
         float var10 = (float)(var1.getxRot() * 360) / 256.0F;
         int var11 = var1.getEntityId();
         RemotePlayer var12 = new RemotePlayer(this.minecraft.level, var2.getProfile());
         var12.setId(var11);
         var12.syncPacketPositionCodec(var3, var5, var7);
         var12.absMoveTo(var3, var5, var7, var9, var10);
         var12.setOldPosAndRot();
         this.level.addPlayer(var11, var12);
      }
   }

   public void handleTeleportEntity(ClientboundTeleportEntityPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      Entity var2 = this.level.getEntity(var1.getId());
      if (var2 != null) {
         double var3 = var1.getX();
         double var5 = var1.getY();
         double var7 = var1.getZ();
         var2.syncPacketPositionCodec(var3, var5, var7);
         if (!var2.isControlledByLocalInstance()) {
            float var9 = (float)(var1.getyRot() * 360) / 256.0F;
            float var10 = (float)(var1.getxRot() * 360) / 256.0F;
            var2.lerpTo(var3, var5, var7, var9, var10, 3, true);
            var2.setOnGround(var1.isOnGround());
         }

      }
   }

   public void handleSetCarriedItem(ClientboundSetCarriedItemPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      if (Inventory.isHotbarSlot(var1.getSlot())) {
         this.minecraft.player.getInventory().selected = var1.getSlot();
      }

   }

   public void handleMoveEntity(ClientboundMoveEntityPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      Entity var2 = var1.getEntity(this.level);
      if (var2 != null) {
         if (!var2.isControlledByLocalInstance()) {
            if (var1.hasPosition()) {
               VecDeltaCodec var3 = var2.getPositionCodec();
               Vec3 var4 = var3.decode((long)var1.getXa(), (long)var1.getYa(), (long)var1.getZa());
               var3.setBase(var4);
               float var5 = var1.hasRotation() ? (float)(var1.getyRot() * 360) / 256.0F : var2.getYRot();
               float var6 = var1.hasRotation() ? (float)(var1.getxRot() * 360) / 256.0F : var2.getXRot();
               var2.lerpTo(var4.x(), var4.y(), var4.z(), var5, var6, 3, false);
            } else if (var1.hasRotation()) {
               float var7 = (float)(var1.getyRot() * 360) / 256.0F;
               float var8 = (float)(var1.getxRot() * 360) / 256.0F;
               var2.lerpTo(var2.getX(), var2.getY(), var2.getZ(), var7, var8, 3, false);
            }

            var2.setOnGround(var1.isOnGround());
         }

      }
   }

   public void handleRotateMob(ClientboundRotateHeadPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      Entity var2 = var1.getEntity(this.level);
      if (var2 != null) {
         float var3 = (float)(var1.getYHeadRot() * 360) / 256.0F;
         var2.lerpHeadTo(var3, 3);
      }
   }

   public void handleRemoveEntities(ClientboundRemoveEntitiesPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      var1.getEntityIds().forEach((var1x) -> {
         this.level.removeEntity(var1x, Entity.RemovalReason.DISCARDED);
      });
   }

   public void handleMovePlayer(ClientboundPlayerPositionPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      LocalPlayer var2 = this.minecraft.player;
      Vec3 var3 = var2.getDeltaMovement();
      boolean var4 = var1.getRelativeArguments().contains(RelativeMovement.X);
      boolean var5 = var1.getRelativeArguments().contains(RelativeMovement.Y);
      boolean var6 = var1.getRelativeArguments().contains(RelativeMovement.Z);
      double var7;
      double var9;
      if (var4) {
         var7 = var3.x();
         var9 = var2.getX() + var1.getX();
         var2.xOld += var1.getX();
         var2.xo += var1.getX();
      } else {
         var7 = 0.0D;
         var9 = var1.getX();
         var2.xOld = var9;
         var2.xo = var9;
      }

      double var11;
      double var13;
      if (var5) {
         var11 = var3.y();
         var13 = var2.getY() + var1.getY();
         var2.yOld += var1.getY();
         var2.yo += var1.getY();
      } else {
         var11 = 0.0D;
         var13 = var1.getY();
         var2.yOld = var13;
         var2.yo = var13;
      }

      double var15;
      double var17;
      if (var6) {
         var15 = var3.z();
         var17 = var2.getZ() + var1.getZ();
         var2.zOld += var1.getZ();
         var2.zo += var1.getZ();
      } else {
         var15 = 0.0D;
         var17 = var1.getZ();
         var2.zOld = var17;
         var2.zo = var17;
      }

      var2.setPos(var9, var13, var17);
      var2.setDeltaMovement(var7, var11, var15);
      float var19 = var1.getYRot();
      float var20 = var1.getXRot();
      if (var1.getRelativeArguments().contains(RelativeMovement.X_ROT)) {
         var2.setXRot(var2.getXRot() + var20);
         var2.xRotO += var20;
      } else {
         var2.setXRot(var20);
         var2.xRotO = var20;
      }

      if (var1.getRelativeArguments().contains(RelativeMovement.Y_ROT)) {
         var2.setYRot(var2.getYRot() + var19);
         var2.yRotO += var19;
      } else {
         var2.setYRot(var19);
         var2.yRotO = var19;
      }

      this.connection.send(new ServerboundAcceptTeleportationPacket(var1.getId()));
      this.connection.send(new ServerboundMovePlayerPacket.PosRot(var2.getX(), var2.getY(), var2.getZ(), var2.getYRot(), var2.getXRot(), false));
   }

   public void handleChunkBlocksUpdate(ClientboundSectionBlocksUpdatePacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      var1.runUpdates((var1x, var2) -> {
         this.level.setServerVerifiedBlockState(var1x, var2, 19);
      });
   }

   public void handleLevelChunkWithLight(ClientboundLevelChunkWithLightPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      int var2 = var1.getX();
      int var3 = var1.getZ();
      this.updateLevelChunk(var2, var3, var1.getChunkData());
      ClientboundLightUpdatePacketData var4 = var1.getLightData();
      this.level.queueLightUpdate(() -> {
         this.applyLightData(var2, var3, var4);
         LevelChunk var4x = this.level.getChunkSource().getChunk(var2, var3, false);
         if (var4x != null) {
            this.enableChunkLight(var4x, var2, var3);
         }

      });
   }

   public void handleChunksBiomes(ClientboundChunksBiomesPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      Iterator var2 = var1.chunkBiomeData().iterator();

      ClientboundChunksBiomesPacket.ChunkBiomeData var3;
      while(var2.hasNext()) {
         var3 = (ClientboundChunksBiomesPacket.ChunkBiomeData)var2.next();
         this.level.getChunkSource().replaceBiomes(var3.pos().x, var3.pos().z, var3.getReadBuffer());
      }

      var2 = var1.chunkBiomeData().iterator();

      while(var2.hasNext()) {
         var3 = (ClientboundChunksBiomesPacket.ChunkBiomeData)var2.next();
         this.level.onChunkLoaded(new ChunkPos(var3.pos().x, var3.pos().z));
      }

      var2 = var1.chunkBiomeData().iterator();

      while(var2.hasNext()) {
         var3 = (ClientboundChunksBiomesPacket.ChunkBiomeData)var2.next();

         for(int var4 = -1; var4 <= 1; ++var4) {
            for(int var5 = -1; var5 <= 1; ++var5) {
               for(int var6 = this.level.getMinSection(); var6 < this.level.getMaxSection(); ++var6) {
                  this.minecraft.levelRenderer.setSectionDirty(var3.pos().x + var4, var6, var3.pos().z + var5);
               }
            }
         }
      }

   }

   private void updateLevelChunk(int var1, int var2, ClientboundLevelChunkPacketData var3) {
      this.level.getChunkSource().replaceWithPacketData(var1, var2, var3.getReadBuffer(), var3.getHeightmaps(), var3.getBlockEntitiesTagsConsumer(var1, var2));
   }

   private void enableChunkLight(LevelChunk var1, int var2, int var3) {
      LevelLightEngine var4 = this.level.getChunkSource().getLightEngine();
      LevelChunkSection[] var5 = var1.getSections();
      ChunkPos var6 = var1.getPos();

      for(int var7 = 0; var7 < var5.length; ++var7) {
         LevelChunkSection var8 = var5[var7];
         int var9 = this.level.getSectionYFromSectionIndex(var7);
         var4.updateSectionStatus(SectionPos.of(var6, var9), var8.hasOnlyAir());
         this.level.setSectionDirtyWithNeighbors(var2, var9, var3);
      }

   }

   public void handleForgetLevelChunk(ClientboundForgetLevelChunkPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      int var2 = var1.getX();
      int var3 = var1.getZ();
      ClientChunkCache var4 = this.level.getChunkSource();
      var4.drop(var2, var3);
      this.queueLightRemoval(var1);
   }

   private void queueLightRemoval(ClientboundForgetLevelChunkPacket var1) {
      ChunkPos var2 = new ChunkPos(var1.getX(), var1.getZ());
      this.level.queueLightUpdate(() -> {
         LevelLightEngine var2x = this.level.getLightEngine();
         var2x.setLightEnabled(var2, false);

         int var3;
         for(var3 = var2x.getMinLightSection(); var3 < var2x.getMaxLightSection(); ++var3) {
            SectionPos var4 = SectionPos.of(var2, var3);
            var2x.queueSectionData(LightLayer.BLOCK, var4, (DataLayer)null);
            var2x.queueSectionData(LightLayer.SKY, var4, (DataLayer)null);
         }

         for(var3 = this.level.getMinSection(); var3 < this.level.getMaxSection(); ++var3) {
            var2x.updateSectionStatus(SectionPos.of(var2, var3), true);
         }

      });
   }

   public void handleBlockUpdate(ClientboundBlockUpdatePacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      this.level.setServerVerifiedBlockState(var1.getPos(), var1.getBlockState(), 19);
   }

   public void handleDisconnect(ClientboundDisconnectPacket var1) {
      this.connection.disconnect(var1.getReason());
   }

   public void onDisconnect(Component var1) {
      this.minecraft.clearLevel();
      this.telemetryManager.onDisconnect();
      if (this.callbackScreen != null) {
         if (this.callbackScreen instanceof RealmsScreen) {
            this.minecraft.setScreen(new DisconnectedRealmsScreen(this.callbackScreen, GENERIC_DISCONNECT_MESSAGE, var1));
         } else {
            this.minecraft.setScreen(new DisconnectedScreen(this.callbackScreen, GENERIC_DISCONNECT_MESSAGE, var1));
         }
      } else {
         this.minecraft.setScreen(new DisconnectedScreen(new JoinMultiplayerScreen(new TitleScreen()), GENERIC_DISCONNECT_MESSAGE, var1));
      }

   }

   public void send(Packet<?> var1) {
      this.connection.send(var1);
   }

   public void handleTakeItemEntity(ClientboundTakeItemEntityPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      Entity var2 = this.level.getEntity(var1.getItemId());
      Object var3 = (LivingEntity)this.level.getEntity(var1.getPlayerId());
      if (var3 == null) {
         var3 = this.minecraft.player;
      }

      if (var2 != null) {
         if (var2 instanceof ExperienceOrb) {
            this.level.playLocalSound(var2.getX(), var2.getY(), var2.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.1F, (this.random.nextFloat() - this.random.nextFloat()) * 0.35F + 0.9F, false);
         } else {
            this.level.playLocalSound(var2.getX(), var2.getY(), var2.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, (this.random.nextFloat() - this.random.nextFloat()) * 1.4F + 2.0F, false);
         }

         this.minecraft.particleEngine.add(new ItemPickupParticle(this.minecraft.getEntityRenderDispatcher(), this.minecraft.renderBuffers(), this.level, var2, (Entity)var3));
         if (var2 instanceof ItemEntity) {
            ItemEntity var4 = (ItemEntity)var2;
            ItemStack var5 = var4.getItem();
            if (!var5.isEmpty()) {
               var5.shrink(var1.getAmount());
            }

            if (var5.isEmpty()) {
               this.level.removeEntity(var1.getItemId(), Entity.RemovalReason.DISCARDED);
            }
         } else if (!(var2 instanceof ExperienceOrb)) {
            this.level.removeEntity(var1.getItemId(), Entity.RemovalReason.DISCARDED);
         }
      }

   }

   public void handleSystemChat(ClientboundSystemChatPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      this.minecraft.getChatListener().handleSystemMessage(var1.content(), var1.overlay());
   }

   public void handlePlayerChat(ClientboundPlayerChatPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      Optional var2 = var1.body().unpack(this.messageSignatureCache);
      Optional var3 = var1.chatType().resolve(this.registryAccess.compositeAccess());
      if (!var2.isEmpty() && !var3.isEmpty()) {
         UUID var4 = var1.sender();
         PlayerInfo var5 = this.getPlayerInfo(var4);
         if (var5 == null) {
            this.connection.disconnect(CHAT_VALIDATION_FAILED_ERROR);
         } else {
            RemoteChatSession var6 = var5.getChatSession();
            SignedMessageLink var7;
            if (var6 != null) {
               var7 = new SignedMessageLink(var1.index(), var4, var6.sessionId());
            } else {
               var7 = SignedMessageLink.unsigned(var4);
            }

            PlayerChatMessage var8 = new PlayerChatMessage(var7, var1.signature(), (SignedMessageBody)var2.get(), var1.unsignedContent(), var1.filterMask());
            if (!var5.getMessageValidator().updateAndValidate(var8)) {
               this.connection.disconnect(CHAT_VALIDATION_FAILED_ERROR);
            } else {
               this.minecraft.getChatListener().handlePlayerChatMessage(var8, var5.getProfile(), (ChatType.Bound)var3.get());
               this.messageSignatureCache.push(var8);
            }
         }
      } else {
         this.connection.disconnect(INVALID_PACKET);
      }
   }

   public void handleDisguisedChat(ClientboundDisguisedChatPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      Optional var2 = var1.chatType().resolve(this.registryAccess.compositeAccess());
      if (var2.isEmpty()) {
         this.connection.disconnect(INVALID_PACKET);
      } else {
         this.minecraft.getChatListener().handleDisguisedChatMessage(var1.message(), (ChatType.Bound)var2.get());
      }
   }

   public void handleDeleteChat(ClientboundDeleteChatPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      Optional var2 = var1.messageSignature().unpack(this.messageSignatureCache);
      if (var2.isEmpty()) {
         this.connection.disconnect(INVALID_PACKET);
      } else {
         this.lastSeenMessages.ignorePending((MessageSignature)var2.get());
         if (!this.minecraft.getChatListener().removeFromDelayedMessageQueue((MessageSignature)var2.get())) {
            this.minecraft.gui.getChat().deleteMessage((MessageSignature)var2.get());
         }

      }
   }

   public void handleAnimate(ClientboundAnimatePacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      Entity var2 = this.level.getEntity(var1.getId());
      if (var2 != null) {
         LivingEntity var3;
         if (var1.getAction() == 0) {
            var3 = (LivingEntity)var2;
            var3.swing(InteractionHand.MAIN_HAND);
         } else if (var1.getAction() == 3) {
            var3 = (LivingEntity)var2;
            var3.swing(InteractionHand.OFF_HAND);
         } else if (var1.getAction() == 2) {
            Player var4 = (Player)var2;
            var4.stopSleepInBed(false, false);
         } else if (var1.getAction() == 4) {
            this.minecraft.particleEngine.createTrackingEmitter(var2, ParticleTypes.CRIT);
         } else if (var1.getAction() == 5) {
            this.minecraft.particleEngine.createTrackingEmitter(var2, ParticleTypes.ENCHANTED_HIT);
         }

      }
   }

   public void handleHurtAnimation(ClientboundHurtAnimationPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      Entity var2 = this.level.getEntity(var1.id());
      if (var2 != null) {
         var2.animateHurt(var1.yaw());
      }
   }

   public void handleSetTime(ClientboundSetTimePacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      this.minecraft.level.setGameTime(var1.getGameTime());
      this.minecraft.level.setDayTime(var1.getDayTime());
      this.telemetryManager.setTime(var1.getGameTime());
   }

   public void handleSetSpawn(ClientboundSetDefaultSpawnPositionPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      this.minecraft.level.setDefaultSpawnPos(var1.getPos(), var1.getAngle());
      Screen var3 = this.minecraft.screen;
      if (var3 instanceof ReceivingLevelScreen) {
         ReceivingLevelScreen var2 = (ReceivingLevelScreen)var3;
         var2.loadingPacketsReceived();
      }

   }

   public void handleSetEntityPassengersPacket(ClientboundSetPassengersPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      Entity var2 = this.level.getEntity(var1.getVehicle());
      if (var2 == null) {
         LOGGER.warn("Received passengers for unknown entity");
      } else {
         boolean var3 = var2.hasIndirectPassenger(this.minecraft.player);
         var2.ejectPassengers();
         int[] var4 = var1.getPassengers();
         int var5 = var4.length;

         for(int var6 = 0; var6 < var5; ++var6) {
            int var7 = var4[var6];
            Entity var8 = this.level.getEntity(var7);
            if (var8 != null) {
               var8.startRiding(var2, true);
               if (var8 == this.minecraft.player && !var3) {
                  if (var2 instanceof Boat) {
                     this.minecraft.player.yRotO = var2.getYRot();
                     this.minecraft.player.setYRot(var2.getYRot());
                     this.minecraft.player.setYHeadRot(var2.getYRot());
                  }

                  MutableComponent var9 = Component.translatable("mount.onboard", this.minecraft.options.keyShift.getTranslatedKeyMessage());
                  this.minecraft.gui.setOverlayMessage(var9, false);
                  this.minecraft.getNarrator().sayNow((Component)var9);
               }
            }
         }

      }
   }

   public void handleEntityLinkPacket(ClientboundSetEntityLinkPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      Entity var2 = this.level.getEntity(var1.getSourceId());
      if (var2 instanceof Mob) {
         ((Mob)var2).setDelayedLeashHolderId(var1.getDestId());
      }

   }

   private static ItemStack findTotem(Player var0) {
      InteractionHand[] var1 = InteractionHand.values();
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         InteractionHand var4 = var1[var3];
         ItemStack var5 = var0.getItemInHand(var4);
         if (var5.is(Items.TOTEM_OF_UNDYING)) {
            return var5;
         }
      }

      return new ItemStack(Items.TOTEM_OF_UNDYING);
   }

   public void handleEntityEvent(ClientboundEntityEventPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      Entity var2 = var1.getEntity(this.level);
      if (var2 != null) {
         switch(var1.getEventId()) {
         case 21:
            this.minecraft.getSoundManager().play(new GuardianAttackSoundInstance((Guardian)var2));
            break;
         case 35:
            boolean var3 = true;
            this.minecraft.particleEngine.createTrackingEmitter(var2, ParticleTypes.TOTEM_OF_UNDYING, 30);
            this.level.playLocalSound(var2.getX(), var2.getY(), var2.getZ(), SoundEvents.TOTEM_USE, var2.getSoundSource(), 1.0F, 1.0F, false);
            if (var2 == this.minecraft.player) {
               this.minecraft.gameRenderer.displayItemActivation(findTotem(this.minecraft.player));
            }
            break;
         case 63:
            this.minecraft.getSoundManager().play(new SnifferSoundInstance((Sniffer)var2));
            break;
         default:
            var2.handleEntityEvent(var1.getEventId());
         }
      }

   }

   public void handleDamageEvent(ClientboundDamageEventPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      Entity var2 = this.level.getEntity(var1.entityId());
      if (var2 != null) {
         var2.handleDamageEvent(var1.getSource(this.level));
      }
   }

   public void handleSetHealth(ClientboundSetHealthPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      this.minecraft.player.hurtTo(var1.getHealth());
      this.minecraft.player.getFoodData().setFoodLevel(var1.getFood());
      this.minecraft.player.getFoodData().setSaturation(var1.getSaturation());
   }

   public void handleSetExperience(ClientboundSetExperiencePacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      this.minecraft.player.setExperienceValues(var1.getExperienceProgress(), var1.getTotalExperience(), var1.getExperienceLevel());
   }

   public void handleRespawn(ClientboundRespawnPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      ResourceKey var2 = var1.getDimension();
      Holder.Reference var3 = this.registryAccess.compositeAccess().registryOrThrow(Registries.DIMENSION_TYPE).getHolderOrThrow(var1.getDimensionType());
      LocalPlayer var4 = this.minecraft.player;
      int var5 = var4.getId();
      if (var2 != var4.level().dimension()) {
         Scoreboard var6 = this.level.getScoreboard();
         Map var7 = this.level.getAllMapData();
         boolean var8 = var1.isDebug();
         boolean var9 = var1.isFlat();
         ClientLevel.ClientLevelData var10 = new ClientLevel.ClientLevelData(this.levelData.getDifficulty(), this.levelData.isHardcore(), var9);
         this.levelData = var10;
         int var10007 = this.serverChunkRadius;
         int var10008 = this.serverSimulationDistance;
         Minecraft var10009 = this.minecraft;
         Objects.requireNonNull(var10009);
         this.level = new ClientLevel(this, var10, var2, var3, var10007, var10008, var10009::getProfiler, this.minecraft.levelRenderer, var8, var1.getSeed());
         this.level.setScoreboard(var6);
         this.level.addMapData(var7);
         this.minecraft.setLevel(this.level);
         this.minecraft.setScreen(new ReceivingLevelScreen());
      }

      String var11 = var4.getServerBrand();
      this.minecraft.cameraEntity = null;
      if (var4.hasContainerOpen()) {
         var4.closeContainer();
      }

      LocalPlayer var12;
      if (var1.shouldKeep((byte)2)) {
         var12 = this.minecraft.gameMode.createPlayer(this.level, var4.getStats(), var4.getRecipeBook(), var4.isShiftKeyDown(), var4.isSprinting());
      } else {
         var12 = this.minecraft.gameMode.createPlayer(this.level, var4.getStats(), var4.getRecipeBook());
      }

      var12.setId(var5);
      this.minecraft.player = var12;
      if (var2 != var4.level().dimension()) {
         this.minecraft.getMusicManager().stopPlaying();
      }

      this.minecraft.cameraEntity = var12;
      if (var1.shouldKeep((byte)2)) {
         List var13 = var4.getEntityData().getNonDefaultValues();
         if (var13 != null) {
            var12.getEntityData().assignValues(var13);
         }
      }

      if (var1.shouldKeep((byte)1)) {
         var12.getAttributes().assignValues(var4.getAttributes());
      }

      var12.resetPos();
      var12.setServerBrand(var11);
      this.level.addPlayer(var5, var12);
      var12.setYRot(-180.0F);
      var12.input = new KeyboardInput(this.minecraft.options);
      this.minecraft.gameMode.adjustPlayer(var12);
      var12.setReducedDebugInfo(var4.isReducedDebugInfo());
      var12.setShowDeathScreen(var4.shouldShowDeathScreen());
      var12.setLastDeathLocation(var1.getLastDeathLocation());
      var12.setPortalCooldown(var1.getPortalCooldown());
      var12.spinningEffectIntensity = var4.spinningEffectIntensity;
      var12.oSpinningEffectIntensity = var4.oSpinningEffectIntensity;
      if (this.minecraft.screen instanceof DeathScreen || this.minecraft.screen instanceof DeathScreen.TitleConfirmScreen) {
         this.minecraft.setScreen((Screen)null);
      }

      this.minecraft.gameMode.setLocalMode(var1.getPlayerGameType(), var1.getPreviousPlayerGameType());
   }

   public void handleExplosion(ClientboundExplodePacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      Explosion var2 = new Explosion(this.minecraft.level, (Entity)null, var1.getX(), var1.getY(), var1.getZ(), var1.getPower(), var1.getToBlow());
      var2.finalizeExplosion(true);
      this.minecraft.player.setDeltaMovement(this.minecraft.player.getDeltaMovement().add((double)var1.getKnockbackX(), (double)var1.getKnockbackY(), (double)var1.getKnockbackZ()));
   }

   public void handleHorseScreenOpen(ClientboundHorseScreenOpenPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      Entity var2 = this.level.getEntity(var1.getEntityId());
      if (var2 instanceof AbstractHorse) {
         LocalPlayer var3 = this.minecraft.player;
         AbstractHorse var4 = (AbstractHorse)var2;
         SimpleContainer var5 = new SimpleContainer(var1.getSize());
         HorseInventoryMenu var6 = new HorseInventoryMenu(var1.getContainerId(), var3.getInventory(), var5, var4);
         var3.containerMenu = var6;
         this.minecraft.setScreen(new HorseInventoryScreen(var6, var3.getInventory(), var4));
      }

   }

   public void handleOpenScreen(ClientboundOpenScreenPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      MenuScreens.create(var1.getType(), this.minecraft, var1.getContainerId(), var1.getTitle());
   }

   public void handleContainerSetSlot(ClientboundContainerSetSlotPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      LocalPlayer var2 = this.minecraft.player;
      ItemStack var3 = var1.getItem();
      int var4 = var1.getSlot();
      this.minecraft.getTutorial().onGetItem(var3);
      if (var1.getContainerId() == -1) {
         if (!(this.minecraft.screen instanceof CreativeModeInventoryScreen)) {
            var2.containerMenu.setCarried(var3);
         }
      } else if (var1.getContainerId() == -2) {
         var2.getInventory().setItem(var4, var3);
      } else {
         boolean var5 = false;
         Screen var7 = this.minecraft.screen;
         if (var7 instanceof CreativeModeInventoryScreen) {
            CreativeModeInventoryScreen var6 = (CreativeModeInventoryScreen)var7;
            var5 = !var6.isInventoryOpen();
         }

         if (var1.getContainerId() == 0 && InventoryMenu.isHotbarSlot(var4)) {
            if (!var3.isEmpty()) {
               ItemStack var8 = var2.inventoryMenu.getSlot(var4).getItem();
               if (var8.isEmpty() || var8.getCount() < var3.getCount()) {
                  var3.setPopTime(5);
               }
            }

            var2.inventoryMenu.setItem(var4, var1.getStateId(), var3);
         } else if (var1.getContainerId() == var2.containerMenu.containerId && (var1.getContainerId() != 0 || !var5)) {
            var2.containerMenu.setItem(var4, var1.getStateId(), var3);
         }
      }

   }

   public void handleContainerContent(ClientboundContainerSetContentPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      LocalPlayer var2 = this.minecraft.player;
      if (var1.getContainerId() == 0) {
         var2.inventoryMenu.initializeContents(var1.getStateId(), var1.getItems(), var1.getCarriedItem());
      } else if (var1.getContainerId() == var2.containerMenu.containerId) {
         var2.containerMenu.initializeContents(var1.getStateId(), var1.getItems(), var1.getCarriedItem());
      }

   }

   public void handleOpenSignEditor(ClientboundOpenSignEditorPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      BlockPos var2 = var1.getPos();
      BlockEntity var4 = this.level.getBlockEntity(var2);
      if (var4 instanceof SignBlockEntity) {
         SignBlockEntity var3 = (SignBlockEntity)var4;
         this.minecraft.player.openTextEdit(var3, var1.isFrontText());
      } else {
         BlockState var6 = this.level.getBlockState(var2);
         SignBlockEntity var5 = new SignBlockEntity(var2, var6);
         var5.setLevel(this.level);
         this.minecraft.player.openTextEdit(var5, var1.isFrontText());
      }

   }

   public void handleBlockEntityData(ClientboundBlockEntityDataPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      BlockPos var2 = var1.getPos();
      this.minecraft.level.getBlockEntity(var2, var1.getType()).ifPresent((var2x) -> {
         CompoundTag var3 = var1.getTag();
         if (var3 != null) {
            var2x.load(var3);
         }

         if (var2x instanceof CommandBlockEntity && this.minecraft.screen instanceof CommandBlockEditScreen) {
            ((CommandBlockEditScreen)this.minecraft.screen).updateGui();
         }

      });
   }

   public void handleContainerSetData(ClientboundContainerSetDataPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      LocalPlayer var2 = this.minecraft.player;
      if (var2.containerMenu != null && var2.containerMenu.containerId == var1.getContainerId()) {
         var2.containerMenu.setData(var1.getId(), var1.getValue());
      }

   }

   public void handleSetEquipment(ClientboundSetEquipmentPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      Entity var2 = this.level.getEntity(var1.getEntity());
      if (var2 != null) {
         var1.getSlots().forEach((var1x) -> {
            var2.setItemSlot((EquipmentSlot)var1x.getFirst(), (ItemStack)var1x.getSecond());
         });
      }

   }

   public void handleContainerClose(ClientboundContainerClosePacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      this.minecraft.player.clientSideCloseContainer();
   }

   public void handleBlockEvent(ClientboundBlockEventPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      this.minecraft.level.blockEvent(var1.getPos(), var1.getBlock(), var1.getB0(), var1.getB1());
   }

   public void handleBlockDestruction(ClientboundBlockDestructionPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      this.minecraft.level.destroyBlockProgress(var1.getId(), var1.getPos(), var1.getProgress());
   }

   public void handleGameEvent(ClientboundGameEventPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      LocalPlayer var2 = this.minecraft.player;
      ClientboundGameEventPacket.Type var3 = var1.getEvent();
      float var4 = var1.getParam();
      int var5 = Mth.floor(var4 + 0.5F);
      if (var3 == ClientboundGameEventPacket.NO_RESPAWN_BLOCK_AVAILABLE) {
         var2.displayClientMessage(Component.translatable("block.minecraft.spawn.not_valid"), false);
      } else if (var3 == ClientboundGameEventPacket.START_RAINING) {
         this.level.getLevelData().setRaining(true);
         this.level.setRainLevel(0.0F);
      } else if (var3 == ClientboundGameEventPacket.STOP_RAINING) {
         this.level.getLevelData().setRaining(false);
         this.level.setRainLevel(1.0F);
      } else if (var3 == ClientboundGameEventPacket.CHANGE_GAME_MODE) {
         this.minecraft.gameMode.setLocalMode(GameType.byId(var5));
      } else if (var3 == ClientboundGameEventPacket.WIN_GAME) {
         if (var5 == 0) {
            this.minecraft.player.connection.send((Packet)(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN)));
            this.minecraft.setScreen(new ReceivingLevelScreen());
         } else if (var5 == 1) {
            this.minecraft.setScreen(new WinScreen(true, () -> {
               this.minecraft.player.connection.send((Packet)(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN)));
               this.minecraft.setScreen((Screen)null);
            }));
         }
      } else if (var3 == ClientboundGameEventPacket.DEMO_EVENT) {
         Options var6 = this.minecraft.options;
         if (var4 == 0.0F) {
            this.minecraft.setScreen(new DemoIntroScreen());
         } else if (var4 == 101.0F) {
            this.minecraft.gui.getChat().addMessage(Component.translatable("demo.help.movement", var6.keyUp.getTranslatedKeyMessage(), var6.keyLeft.getTranslatedKeyMessage(), var6.keyDown.getTranslatedKeyMessage(), var6.keyRight.getTranslatedKeyMessage()));
         } else if (var4 == 102.0F) {
            this.minecraft.gui.getChat().addMessage(Component.translatable("demo.help.jump", var6.keyJump.getTranslatedKeyMessage()));
         } else if (var4 == 103.0F) {
            this.minecraft.gui.getChat().addMessage(Component.translatable("demo.help.inventory", var6.keyInventory.getTranslatedKeyMessage()));
         } else if (var4 == 104.0F) {
            this.minecraft.gui.getChat().addMessage(Component.translatable("demo.day.6", var6.keyScreenshot.getTranslatedKeyMessage()));
         }
      } else if (var3 == ClientboundGameEventPacket.ARROW_HIT_PLAYER) {
         this.level.playSound(var2, var2.getX(), var2.getEyeY(), var2.getZ(), SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 0.18F, 0.45F);
      } else if (var3 == ClientboundGameEventPacket.RAIN_LEVEL_CHANGE) {
         this.level.setRainLevel(var4);
      } else if (var3 == ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE) {
         this.level.setThunderLevel(var4);
      } else if (var3 == ClientboundGameEventPacket.PUFFER_FISH_STING) {
         this.level.playSound(var2, var2.getX(), var2.getY(), var2.getZ(), SoundEvents.PUFFER_FISH_STING, SoundSource.NEUTRAL, 1.0F, 1.0F);
      } else if (var3 == ClientboundGameEventPacket.GUARDIAN_ELDER_EFFECT) {
         this.level.addParticle(ParticleTypes.ELDER_GUARDIAN, var2.getX(), var2.getY(), var2.getZ(), 0.0D, 0.0D, 0.0D);
         if (var5 == 1) {
            this.level.playSound(var2, var2.getX(), var2.getY(), var2.getZ(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 1.0F, 1.0F);
         }
      } else if (var3 == ClientboundGameEventPacket.IMMEDIATE_RESPAWN) {
         this.minecraft.player.setShowDeathScreen(var4 == 0.0F);
      }

   }

   public void handleMapItemData(ClientboundMapItemDataPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      MapRenderer var2 = this.minecraft.gameRenderer.getMapRenderer();
      int var3 = var1.getMapId();
      String var4 = MapItem.makeKey(var3);
      MapItemSavedData var5 = this.minecraft.level.getMapData(var4);
      if (var5 == null) {
         var5 = MapItemSavedData.createForClient(var1.getScale(), var1.isLocked(), this.minecraft.level.dimension());
         this.minecraft.level.overrideMapData(var4, var5);
      }

      var1.applyToMap(var5);
      var2.update(var3, var5);
   }

   public void handleLevelEvent(ClientboundLevelEventPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      if (var1.isGlobalEvent()) {
         this.minecraft.level.globalLevelEvent(var1.getType(), var1.getPos(), var1.getData());
      } else {
         this.minecraft.level.levelEvent(var1.getType(), var1.getPos(), var1.getData());
      }

   }

   public void handleUpdateAdvancementsPacket(ClientboundUpdateAdvancementsPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      this.advancements.update(var1);
   }

   public void handleSelectAdvancementsTab(ClientboundSelectAdvancementsTabPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      ResourceLocation var2 = var1.getTab();
      if (var2 == null) {
         this.advancements.setSelectedTab((Advancement)null, false);
      } else {
         Advancement var3 = this.advancements.getAdvancements().get(var2);
         this.advancements.setSelectedTab(var3, false);
      }

   }

   public void handleCommands(ClientboundCommandsPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      this.commands = new CommandDispatcher(var1.getRoot(CommandBuildContext.simple(this.registryAccess.compositeAccess(), this.enabledFeatures)));
   }

   public void handleStopSoundEvent(ClientboundStopSoundPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      this.minecraft.getSoundManager().stop(var1.getName(), var1.getSource());
   }

   public void handleCommandSuggestions(ClientboundCommandSuggestionsPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      this.suggestionsProvider.completeCustomSuggestions(var1.getId(), var1.getSuggestions());
   }

   public void handleUpdateRecipes(ClientboundUpdateRecipesPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      this.recipeManager.replaceRecipes(var1.getRecipes());
      ClientRecipeBook var2 = this.minecraft.player.getRecipeBook();
      var2.setupCollections(this.recipeManager.getRecipes(), this.minecraft.level.registryAccess());
      this.minecraft.populateSearchTree(SearchRegistry.RECIPE_COLLECTIONS, var2.getCollections());
   }

   public void handleLookAt(ClientboundPlayerLookAtPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      Vec3 var2 = var1.getPosition(this.level);
      if (var2 != null) {
         this.minecraft.player.lookAt(var1.getFromAnchor(), var2);
      }

   }

   public void handleTagQueryPacket(ClientboundTagQueryPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      if (!this.debugQueryHandler.handleResponse(var1.getTransactionId(), var1.getTag())) {
         LOGGER.debug("Got unhandled response to tag query {}", var1.getTransactionId());
      }

   }

   public void handleAwardStats(ClientboundAwardStatsPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      Iterator var2 = var1.getStats().entrySet().iterator();

      while(var2.hasNext()) {
         Entry var3 = (Entry)var2.next();
         Stat var4 = (Stat)var3.getKey();
         int var5 = (Integer)var3.getValue();
         this.minecraft.player.getStats().setValue(this.minecraft.player, var4, var5);
      }

      if (this.minecraft.screen instanceof StatsUpdateListener) {
         ((StatsUpdateListener)this.minecraft.screen).onStatsUpdated();
      }

   }

   public void handleAddOrRemoveRecipes(ClientboundRecipePacket var1) {
      ClientRecipeBook var2;
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      var2 = this.minecraft.player.getRecipeBook();
      var2.setBookSettings(var1.getBookSettings());
      ClientboundRecipePacket.State var3 = var1.getState();
      Optional var10000;
      Iterator var4;
      ResourceLocation var5;
      label45:
      switch(var3) {
      case REMOVE:
         var4 = var1.getRecipes().iterator();

         while(true) {
            if (!var4.hasNext()) {
               break label45;
            }

            var5 = (ResourceLocation)var4.next();
            var10000 = this.recipeManager.byKey(var5);
            Objects.requireNonNull(var2);
            var10000.ifPresent(var2::remove);
         }
      case INIT:
         var4 = var1.getRecipes().iterator();

         while(var4.hasNext()) {
            var5 = (ResourceLocation)var4.next();
            var10000 = this.recipeManager.byKey(var5);
            Objects.requireNonNull(var2);
            var10000.ifPresent(var2::add);
         }

         var4 = var1.getHighlights().iterator();

         while(true) {
            if (!var4.hasNext()) {
               break label45;
            }

            var5 = (ResourceLocation)var4.next();
            var10000 = this.recipeManager.byKey(var5);
            Objects.requireNonNull(var2);
            var10000.ifPresent(var2::addHighlight);
         }
      case ADD:
         var4 = var1.getRecipes().iterator();

         while(var4.hasNext()) {
            var5 = (ResourceLocation)var4.next();
            this.recipeManager.byKey(var5).ifPresent((var2x) -> {
               var2.add(var2x);
               var2.addHighlight(var2x);
               if (var2x.showNotification()) {
                  RecipeToast.addOrUpdate(this.minecraft.getToasts(), var2x);
               }

            });
         }
      }

      var2.getCollections().forEach((var1x) -> {
         var1x.updateKnownRecipes(var2);
      });
      if (this.minecraft.screen instanceof RecipeUpdateListener) {
         ((RecipeUpdateListener)this.minecraft.screen).recipesUpdated();
      }

   }

   public void handleUpdateMobEffect(ClientboundUpdateMobEffectPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      Entity var2 = this.level.getEntity(var1.getEntityId());
      if (var2 instanceof LivingEntity) {
         MobEffect var3 = var1.getEffect();
         if (var3 != null) {
            MobEffectInstance var4 = new MobEffectInstance(var3, var1.getEffectDurationTicks(), var1.getEffectAmplifier(), var1.isEffectAmbient(), var1.isEffectVisible(), var1.effectShowsIcon(), (MobEffectInstance)null, Optional.ofNullable(var1.getFactorData()));
            ((LivingEntity)var2).forceAddEffect(var4, (Entity)null);
         }
      }
   }

   public void handleUpdateTags(ClientboundUpdateTagsPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      var1.getTags().forEach(this::updateTagsForRegistry);
      if (!this.connection.isMemoryConnection()) {
         Blocks.rebuildCache();
      }

      CreativeModeTabs.searchTab().rebuildSearchTree();
   }

   public void handleEnabledFeatures(ClientboundUpdateEnabledFeaturesPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      this.enabledFeatures = FeatureFlags.REGISTRY.fromNames(var1.features());
   }

   private <T> void updateTagsForRegistry(ResourceKey<? extends Registry<? extends T>> var1, TagNetworkSerialization.NetworkPayload var2) {
      if (!var2.isEmpty()) {
         Registry var3 = (Registry)this.registryAccess.compositeAccess().registry(var1).orElseThrow(() -> {
            return new IllegalStateException("Unknown registry " + var1);
         });
         HashMap var5 = new HashMap();
         Objects.requireNonNull(var5);
         TagNetworkSerialization.deserializeTagsFromNetwork(var1, var3, var2, var5::put);
         var3.bindTags(var5);
      }
   }

   public void handlePlayerCombatEnd(ClientboundPlayerCombatEndPacket var1) {
   }

   public void handlePlayerCombatEnter(ClientboundPlayerCombatEnterPacket var1) {
   }

   public void handlePlayerCombatKill(ClientboundPlayerCombatKillPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      Entity var2 = this.level.getEntity(var1.getPlayerId());
      if (var2 == this.minecraft.player) {
         if (this.minecraft.player.shouldShowDeathScreen()) {
            this.minecraft.setScreen(new DeathScreen(var1.getMessage(), this.level.getLevelData().isHardcore()));
         } else {
            this.minecraft.player.respawn();
         }
      }

   }

   public void handleChangeDifficulty(ClientboundChangeDifficultyPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      this.levelData.setDifficulty(var1.getDifficulty());
      this.levelData.setDifficultyLocked(var1.isLocked());
   }

   public void handleSetCamera(ClientboundSetCameraPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      Entity var2 = var1.getEntity(this.level);
      if (var2 != null) {
         this.minecraft.setCameraEntity(var2);
      }

   }

   public void handleInitializeBorder(ClientboundInitializeBorderPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      WorldBorder var2 = this.level.getWorldBorder();
      var2.setCenter(var1.getNewCenterX(), var1.getNewCenterZ());
      long var3 = var1.getLerpTime();
      if (var3 > 0L) {
         var2.lerpSizeBetween(var1.getOldSize(), var1.getNewSize(), var3);
      } else {
         var2.setSize(var1.getNewSize());
      }

      var2.setAbsoluteMaxSize(var1.getNewAbsoluteMaxSize());
      var2.setWarningBlocks(var1.getWarningBlocks());
      var2.setWarningTime(var1.getWarningTime());
   }

   public void handleSetBorderCenter(ClientboundSetBorderCenterPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      this.level.getWorldBorder().setCenter(var1.getNewCenterX(), var1.getNewCenterZ());
   }

   public void handleSetBorderLerpSize(ClientboundSetBorderLerpSizePacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      this.level.getWorldBorder().lerpSizeBetween(var1.getOldSize(), var1.getNewSize(), var1.getLerpTime());
   }

   public void handleSetBorderSize(ClientboundSetBorderSizePacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      this.level.getWorldBorder().setSize(var1.getSize());
   }

   public void handleSetBorderWarningDistance(ClientboundSetBorderWarningDistancePacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      this.level.getWorldBorder().setWarningBlocks(var1.getWarningBlocks());
   }

   public void handleSetBorderWarningDelay(ClientboundSetBorderWarningDelayPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      this.level.getWorldBorder().setWarningTime(var1.getWarningDelay());
   }

   public void handleTitlesClear(ClientboundClearTitlesPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      this.minecraft.gui.clear();
      if (var1.shouldResetTimes()) {
         this.minecraft.gui.resetTitleTimes();
      }

   }

   public void handleServerData(ClientboundServerDataPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      if (this.serverData != null) {
         this.serverData.motd = var1.getMotd();
         Optional var10000 = var1.getIconBytes();
         ServerData var10001 = this.serverData;
         Objects.requireNonNull(var10001);
         var10000.ifPresent(var10001::setIconBytes);
         this.serverData.setEnforcesSecureChat(var1.enforcesSecureChat());
         ServerList.saveSingleServer(this.serverData);
         if (!var1.enforcesSecureChat()) {
            SystemToast var2 = SystemToast.multiline(this.minecraft, SystemToast.SystemToastIds.UNSECURE_SERVER_WARNING, UNSECURE_SERVER_TOAST_TITLE, UNSERURE_SERVER_TOAST);
            this.minecraft.getToasts().addToast(var2);
         }

      }
   }

   public void handleCustomChatCompletions(ClientboundCustomChatCompletionsPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      this.suggestionsProvider.modifyCustomCompletions(var1.action(), var1.entries());
   }

   public void setActionBarText(ClientboundSetActionBarTextPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      this.minecraft.gui.setOverlayMessage(var1.getText(), false);
   }

   public void setTitleText(ClientboundSetTitleTextPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      this.minecraft.gui.setTitle(var1.getText());
   }

   public void setSubtitleText(ClientboundSetSubtitleTextPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      this.minecraft.gui.setSubtitle(var1.getText());
   }

   public void setTitlesAnimation(ClientboundSetTitlesAnimationPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      this.minecraft.gui.setTimes(var1.getFadeIn(), var1.getStay(), var1.getFadeOut());
   }

   public void handleTabListCustomisation(ClientboundTabListPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      this.minecraft.gui.getTabList().setHeader(var1.getHeader().getString().isEmpty() ? null : var1.getHeader());
      this.minecraft.gui.getTabList().setFooter(var1.getFooter().getString().isEmpty() ? null : var1.getFooter());
   }

   public void handleRemoveMobEffect(ClientboundRemoveMobEffectPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      Entity var2 = var1.getEntity(this.level);
      if (var2 instanceof LivingEntity) {
         ((LivingEntity)var2).removeEffectNoUpdate(var1.getEffect());
      }

   }

   public void handlePlayerInfoRemove(ClientboundPlayerInfoRemovePacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      Iterator var2 = var1.profileIds().iterator();

      while(var2.hasNext()) {
         UUID var3 = (UUID)var2.next();
         this.minecraft.getPlayerSocialManager().removePlayer(var3);
         PlayerInfo var4 = (PlayerInfo)this.playerInfoMap.remove(var3);
         if (var4 != null) {
            this.listedPlayers.remove(var4);
         }
      }

   }

   public void handlePlayerInfoUpdate(ClientboundPlayerInfoUpdatePacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      Iterator var2 = var1.newEntries().iterator();

      ClientboundPlayerInfoUpdatePacket.Entry var3;
      PlayerInfo var4;
      while(var2.hasNext()) {
         var3 = (ClientboundPlayerInfoUpdatePacket.Entry)var2.next();
         var4 = new PlayerInfo(var3.profile(), this.enforcesSecureChat());
         if (this.playerInfoMap.putIfAbsent(var3.profileId(), var4) == null) {
            this.minecraft.getPlayerSocialManager().addPlayer(var4);
         }
      }

      var2 = var1.entries().iterator();

      while(true) {
         while(var2.hasNext()) {
            var3 = (ClientboundPlayerInfoUpdatePacket.Entry)var2.next();
            var4 = (PlayerInfo)this.playerInfoMap.get(var3.profileId());
            if (var4 == null) {
               LOGGER.warn("Ignoring player info update for unknown player {}", var3.profileId());
            } else {
               Iterator var5 = var1.actions().iterator();

               while(var5.hasNext()) {
                  ClientboundPlayerInfoUpdatePacket.Action var6 = (ClientboundPlayerInfoUpdatePacket.Action)var5.next();
                  this.applyPlayerInfoUpdate(var6, var3, var4);
               }
            }
         }

         return;
      }
   }

   private void applyPlayerInfoUpdate(ClientboundPlayerInfoUpdatePacket.Action var1, ClientboundPlayerInfoUpdatePacket.Entry var2, PlayerInfo var3) {
      switch(var1) {
      case INITIALIZE_CHAT:
         this.initializeChatSession(var2, var3);
         break;
      case UPDATE_GAME_MODE:
         if (var3.getGameMode() != var2.gameMode() && this.minecraft.player != null && this.minecraft.player.getUUID().equals(var2.profileId())) {
            this.minecraft.player.onGameModeChanged(var2.gameMode());
         }

         var3.setGameMode(var2.gameMode());
         break;
      case UPDATE_LISTED:
         if (var2.listed()) {
            this.listedPlayers.add(var3);
         } else {
            this.listedPlayers.remove(var3);
         }
         break;
      case UPDATE_LATENCY:
         var3.setLatency(var2.latency());
         break;
      case UPDATE_DISPLAY_NAME:
         var3.setTabListDisplayName(var2.displayName());
      }

   }

   private void initializeChatSession(ClientboundPlayerInfoUpdatePacket.Entry var1, PlayerInfo var2) {
      GameProfile var3 = var2.getProfile();
      SignatureValidator var4 = this.minecraft.getProfileKeySignatureValidator();
      if (var4 == null) {
         LOGGER.warn("Ignoring chat session from {} due to missing Services public key", var3.getName());
         var2.clearChatSession(this.enforcesSecureChat());
      } else {
         RemoteChatSession.Data var5 = var1.chatSession();
         if (var5 != null) {
            try {
               RemoteChatSession var6 = var5.validate(var3, var4, ProfilePublicKey.EXPIRY_GRACE_PERIOD);
               var2.setChatSession(var6);
            } catch (ProfilePublicKey.ValidationException var7) {
               LOGGER.error("Failed to validate profile key for player: '{}'", var3.getName(), var7);
               var2.clearChatSession(this.enforcesSecureChat());
            }
         } else {
            var2.clearChatSession(this.enforcesSecureChat());
         }

      }
   }

   private boolean enforcesSecureChat() {
      return this.serverData != null && this.serverData.enforcesSecureChat();
   }

   public void handleKeepAlive(ClientboundKeepAlivePacket var1) {
      this.sendWhen(new ServerboundKeepAlivePacket(var1.getId()), () -> {
         return !RenderSystem.isFrozenAtPollEvents();
      }, Duration.ofMinutes(1L));
   }

   private void sendWhen(Packet<ServerGamePacketListener> var1, BooleanSupplier var2, Duration var3) {
      if (var2.getAsBoolean()) {
         this.send(var1);
      } else {
         this.deferredPackets.add(new ClientPacketListener.DeferredPacket(var1, var2, Util.getMillis() + var3.toMillis()));
      }

   }

   private void sendDeferredPackets() {
      Iterator var1 = this.deferredPackets.iterator();

      while(var1.hasNext()) {
         ClientPacketListener.DeferredPacket var2 = (ClientPacketListener.DeferredPacket)var1.next();
         if (var2.sendCondition().getAsBoolean()) {
            this.send(var2.packet);
            var1.remove();
         } else if (var2.expirationTime() <= Util.getMillis()) {
            var1.remove();
         }
      }

   }

   public void handlePlayerAbilities(ClientboundPlayerAbilitiesPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      LocalPlayer var2 = this.minecraft.player;
      var2.getAbilities().flying = var1.isFlying();
      var2.getAbilities().instabuild = var1.canInstabuild();
      var2.getAbilities().invulnerable = var1.isInvulnerable();
      var2.getAbilities().mayfly = var1.canFly();
      var2.getAbilities().setFlyingSpeed(var1.getFlyingSpeed());
      var2.getAbilities().setWalkingSpeed(var1.getWalkingSpeed());
   }

   public void handleSoundEvent(ClientboundSoundPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      this.minecraft.level.playSeededSound(this.minecraft.player, var1.getX(), var1.getY(), var1.getZ(), var1.getSound(), var1.getSource(), var1.getVolume(), var1.getPitch(), var1.getSeed());
   }

   public void handleSoundEntityEvent(ClientboundSoundEntityPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      Entity var2 = this.level.getEntity(var1.getId());
      if (var2 != null) {
         this.minecraft.level.playSeededSound(this.minecraft.player, var2, var1.getSound(), var1.getSource(), var1.getVolume(), var1.getPitch(), var1.getSeed());
      }
   }

   public void handleResourcePack(ClientboundResourcePackPacket var1) {
      URL var2 = parseResourcePackUrl(var1.getUrl());
      if (var2 == null) {
         this.send(ServerboundResourcePackPacket.Action.FAILED_DOWNLOAD);
      } else {
         String var3 = var1.getHash();
         boolean var4 = var1.isRequired();
         if (this.serverData != null && this.serverData.getResourcePackStatus() == ServerData.ServerPackStatus.ENABLED) {
            this.send(ServerboundResourcePackPacket.Action.ACCEPTED);
            this.downloadCallback(this.minecraft.getDownloadedPackSource().downloadAndSelectResourcePack(var2, var3, true));
         } else if (this.serverData == null || this.serverData.getResourcePackStatus() == ServerData.ServerPackStatus.PROMPT || var4 && this.serverData.getResourcePackStatus() == ServerData.ServerPackStatus.DISABLED) {
            this.minecraft.execute(() -> {
               this.minecraft.setScreen(new ConfirmScreen((var4x) -> {
                  this.minecraft.setScreen((Screen)null);
                  if (var4x) {
                     if (this.serverData != null) {
                        this.serverData.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
                     }

                     this.send(ServerboundResourcePackPacket.Action.ACCEPTED);
                     this.downloadCallback(this.minecraft.getDownloadedPackSource().downloadAndSelectResourcePack(var2, var3, true));
                  } else {
                     this.send(ServerboundResourcePackPacket.Action.DECLINED);
                     if (var4) {
                        this.connection.disconnect(Component.translatable("multiplayer.requiredTexturePrompt.disconnect"));
                     } else if (this.serverData != null) {
                        this.serverData.setResourcePackStatus(ServerData.ServerPackStatus.DISABLED);
                     }
                  }

                  if (this.serverData != null) {
                     ServerList.saveSingleServer(this.serverData);
                  }

               }, var4 ? Component.translatable("multiplayer.requiredTexturePrompt.line1") : Component.translatable("multiplayer.texturePrompt.line1"), preparePackPrompt(var4 ? Component.translatable("multiplayer.requiredTexturePrompt.line2").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD) : Component.translatable("multiplayer.texturePrompt.line2"), var1.getPrompt()), var4 ? CommonComponents.GUI_PROCEED : CommonComponents.GUI_YES, (Component)(var4 ? Component.translatable("menu.disconnect") : CommonComponents.GUI_NO)));
            });
         } else {
            this.send(ServerboundResourcePackPacket.Action.DECLINED);
            if (var4) {
               this.connection.disconnect(Component.translatable("multiplayer.requiredTexturePrompt.disconnect"));
            }
         }

      }
   }

   private static Component preparePackPrompt(Component var0, @Nullable Component var1) {
      return (Component)(var1 == null ? var0 : Component.translatable("multiplayer.texturePrompt.serverPrompt", var0, var1));
   }

   @Nullable
   private static URL parseResourcePackUrl(String var0) {
      try {
         URL var1 = new URL(var0);
         String var2 = var1.getProtocol();
         return !"http".equals(var2) && !"https".equals(var2) ? null : var1;
      } catch (MalformedURLException var3) {
         return null;
      }
   }

   private void downloadCallback(CompletableFuture<?> var1) {
      var1.thenRun(() -> {
         this.send(ServerboundResourcePackPacket.Action.SUCCESSFULLY_LOADED);
      }).exceptionally((var1x) -> {
         this.send(ServerboundResourcePackPacket.Action.FAILED_DOWNLOAD);
         return null;
      });
   }

   private void send(ServerboundResourcePackPacket.Action var1) {
      this.connection.send(new ServerboundResourcePackPacket(var1));
   }

   public void handleBossUpdate(ClientboundBossEventPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      this.minecraft.gui.getBossOverlay().update(var1);
   }

   public void handleItemCooldown(ClientboundCooldownPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      if (var1.getDuration() == 0) {
         this.minecraft.player.getCooldowns().removeCooldown(var1.getItem());
      } else {
         this.minecraft.player.getCooldowns().addCooldown(var1.getItem(), var1.getDuration());
      }

   }

   public void handleMoveVehicle(ClientboundMoveVehiclePacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      Entity var2 = this.minecraft.player.getRootVehicle();
      if (var2 != this.minecraft.player && var2.isControlledByLocalInstance()) {
         var2.absMoveTo(var1.getX(), var1.getY(), var1.getZ(), var1.getYRot(), var1.getXRot());
         this.connection.send(new ServerboundMoveVehiclePacket(var2));
      }

   }

   public void handleOpenBook(ClientboundOpenBookPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      ItemStack var2 = this.minecraft.player.getItemInHand(var1.getHand());
      if (var2.is(Items.WRITTEN_BOOK)) {
         this.minecraft.setScreen(new BookViewScreen(new BookViewScreen.WrittenBookAccess(var2)));
      }

   }

   public void handleCustomPayload(ClientboundCustomPayloadPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      ResourceLocation var2 = var1.getIdentifier();
      FriendlyByteBuf var3 = null;

      try {
         var3 = var1.getData();
         if (ClientboundCustomPayloadPacket.BRAND.equals(var2)) {
            String var4 = var3.readUtf();
            this.minecraft.player.setServerBrand(var4);
            this.telemetryManager.onServerBrandReceived(var4);
         } else {
            int var34;
            if (ClientboundCustomPayloadPacket.DEBUG_PATHFINDING_PACKET.equals(var2)) {
               var34 = var3.readInt();
               float var5 = var3.readFloat();
               Path var6 = Path.createFromStream(var3);
               this.minecraft.debugRenderer.pathfindingRenderer.addPath(var34, var6, var5);
            } else if (ClientboundCustomPayloadPacket.DEBUG_NEIGHBORSUPDATE_PACKET.equals(var2)) {
               long var35 = var3.readVarLong();
               BlockPos var40 = var3.readBlockPos();
               ((NeighborsUpdateRenderer)this.minecraft.debugRenderer.neighborsUpdateRenderer).addUpdate(var35, var40);
            } else {
               ArrayList var7;
               int var9;
               int var41;
               if (ClientboundCustomPayloadPacket.DEBUG_STRUCTURES_PACKET.equals(var2)) {
                  DimensionType var36 = (DimensionType)this.registryAccess.compositeAccess().registryOrThrow(Registries.DIMENSION_TYPE).get(var3.readResourceLocation());
                  BoundingBox var37 = new BoundingBox(var3.readInt(), var3.readInt(), var3.readInt(), var3.readInt(), var3.readInt(), var3.readInt());
                  var41 = var3.readInt();
                  var7 = Lists.newArrayList();
                  ArrayList var8 = Lists.newArrayList();

                  for(var9 = 0; var9 < var41; ++var9) {
                     var7.add(new BoundingBox(var3.readInt(), var3.readInt(), var3.readInt(), var3.readInt(), var3.readInt(), var3.readInt()));
                     var8.add(var3.readBoolean());
                  }

                  this.minecraft.debugRenderer.structureRenderer.addBoundingBox(var37, var7, var8, var36);
               } else if (ClientboundCustomPayloadPacket.DEBUG_WORLDGENATTEMPT_PACKET.equals(var2)) {
                  ((WorldGenAttemptRenderer)this.minecraft.debugRenderer.worldGenAttemptRenderer).addPos(var3.readBlockPos(), var3.readFloat(), var3.readFloat(), var3.readFloat(), var3.readFloat(), var3.readFloat());
               } else {
                  int var38;
                  if (ClientboundCustomPayloadPacket.DEBUG_VILLAGE_SECTIONS.equals(var2)) {
                     var34 = var3.readInt();

                     for(var38 = 0; var38 < var34; ++var38) {
                        this.minecraft.debugRenderer.villageSectionsDebugRenderer.setVillageSection(var3.readSectionPos());
                     }

                     var38 = var3.readInt();

                     for(var41 = 0; var41 < var38; ++var41) {
                        this.minecraft.debugRenderer.villageSectionsDebugRenderer.setNotVillageSection(var3.readSectionPos());
                     }
                  } else {
                     BlockPos var39;
                     String var42;
                     if (ClientboundCustomPayloadPacket.DEBUG_POI_ADDED_PACKET.equals(var2)) {
                        var39 = var3.readBlockPos();
                        var42 = var3.readUtf();
                        var41 = var3.readInt();
                        BrainDebugRenderer.PoiInfo var43 = new BrainDebugRenderer.PoiInfo(var39, var42, var41);
                        this.minecraft.debugRenderer.brainDebugRenderer.addPoi(var43);
                     } else if (ClientboundCustomPayloadPacket.DEBUG_POI_REMOVED_PACKET.equals(var2)) {
                        var39 = var3.readBlockPos();
                        this.minecraft.debugRenderer.brainDebugRenderer.removePoi(var39);
                     } else if (ClientboundCustomPayloadPacket.DEBUG_POI_TICKET_COUNT_PACKET.equals(var2)) {
                        var39 = var3.readBlockPos();
                        var38 = var3.readInt();
                        this.minecraft.debugRenderer.brainDebugRenderer.setFreeTicketCount(var39, var38);
                     } else if (ClientboundCustomPayloadPacket.DEBUG_GOAL_SELECTOR.equals(var2)) {
                        var39 = var3.readBlockPos();
                        var38 = var3.readInt();
                        var41 = var3.readInt();
                        var7 = Lists.newArrayList();

                        for(int var47 = 0; var47 < var41; ++var47) {
                           var9 = var3.readInt();
                           boolean var10 = var3.readBoolean();
                           String var11 = var3.readUtf(255);
                           var7.add(new GoalSelectorDebugRenderer.DebugGoal(var39, var9, var11, var10));
                        }

                        this.minecraft.debugRenderer.goalSelectorRenderer.addGoalSelector(var38, var7);
                     } else if (ClientboundCustomPayloadPacket.DEBUG_RAIDS.equals(var2)) {
                        var34 = var3.readInt();
                        ArrayList var45 = Lists.newArrayList();

                        for(var41 = 0; var41 < var34; ++var41) {
                           var45.add(var3.readBlockPos());
                        }

                        this.minecraft.debugRenderer.raidDebugRenderer.setRaidCenters(var45);
                     } else {
                        int var12;
                        int var15;
                        double var44;
                        double var50;
                        double var51;
                        PositionImpl var57;
                        UUID var58;
                        if (ClientboundCustomPayloadPacket.DEBUG_BRAIN.equals(var2)) {
                           var44 = var3.readDouble();
                           var50 = var3.readDouble();
                           var51 = var3.readDouble();
                           var57 = new PositionImpl(var44, var50, var51);
                           var58 = var3.readUUID();
                           var12 = var3.readInt();
                           String var13 = var3.readUtf();
                           String var14 = var3.readUtf();
                           var15 = var3.readInt();
                           float var16 = var3.readFloat();
                           float var17 = var3.readFloat();
                           String var18 = var3.readUtf();
                           Path var19 = (Path)var3.readNullable(Path::createFromStream);
                           boolean var20 = var3.readBoolean();
                           int var21 = var3.readInt();
                           BrainDebugRenderer.BrainDump var22 = new BrainDebugRenderer.BrainDump(var58, var12, var13, var14, var15, var16, var17, var57, var18, var19, var20, var21);
                           int var23 = var3.readVarInt();

                           int var24;
                           for(var24 = 0; var24 < var23; ++var24) {
                              String var25 = var3.readUtf();
                              var22.activities.add(var25);
                           }

                           var24 = var3.readVarInt();

                           int var68;
                           for(var68 = 0; var68 < var24; ++var68) {
                              String var26 = var3.readUtf();
                              var22.behaviors.add(var26);
                           }

                           var68 = var3.readVarInt();

                           int var69;
                           for(var69 = 0; var69 < var68; ++var69) {
                              String var27 = var3.readUtf();
                              var22.memories.add(var27);
                           }

                           var69 = var3.readVarInt();

                           int var70;
                           for(var70 = 0; var70 < var69; ++var70) {
                              BlockPos var28 = var3.readBlockPos();
                              var22.pois.add(var28);
                           }

                           var70 = var3.readVarInt();

                           int var71;
                           for(var71 = 0; var71 < var70; ++var71) {
                              BlockPos var29 = var3.readBlockPos();
                              var22.potentialPois.add(var29);
                           }

                           var71 = var3.readVarInt();

                           for(int var72 = 0; var72 < var71; ++var72) {
                              String var30 = var3.readUtf();
                              var22.gossips.add(var30);
                           }

                           this.minecraft.debugRenderer.brainDebugRenderer.addOrUpdateBrainDump(var22);
                        } else if (ClientboundCustomPayloadPacket.DEBUG_BEE.equals(var2)) {
                           var44 = var3.readDouble();
                           var50 = var3.readDouble();
                           var51 = var3.readDouble();
                           var57 = new PositionImpl(var44, var50, var51);
                           var58 = var3.readUUID();
                           var12 = var3.readInt();
                           BlockPos var59 = (BlockPos)var3.readNullable(FriendlyByteBuf::readBlockPos);
                           BlockPos var60 = (BlockPos)var3.readNullable(FriendlyByteBuf::readBlockPos);
                           var15 = var3.readInt();
                           Path var61 = (Path)var3.readNullable(Path::createFromStream);
                           BeeDebugRenderer.BeeInfo var62 = new BeeDebugRenderer.BeeInfo(var58, var12, var57, var61, var59, var60, var15);
                           int var63 = var3.readVarInt();

                           int var64;
                           for(var64 = 0; var64 < var63; ++var64) {
                              String var65 = var3.readUtf();
                              var62.goals.add(var65);
                           }

                           var64 = var3.readVarInt();

                           for(int var66 = 0; var66 < var64; ++var66) {
                              BlockPos var67 = var3.readBlockPos();
                              var62.blacklistedHives.add(var67);
                           }

                           this.minecraft.debugRenderer.beeDebugRenderer.addOrUpdateBeeInfo(var62);
                        } else {
                           int var46;
                           if (ClientboundCustomPayloadPacket.DEBUG_HIVE.equals(var2)) {
                              var39 = var3.readBlockPos();
                              var42 = var3.readUtf();
                              var41 = var3.readInt();
                              var46 = var3.readInt();
                              boolean var54 = var3.readBoolean();
                              BeeDebugRenderer.HiveInfo var55 = new BeeDebugRenderer.HiveInfo(var39, var42, var41, var46, var54, this.level.getGameTime());
                              this.minecraft.debugRenderer.beeDebugRenderer.addOrUpdateHiveInfo(var55);
                           } else if (ClientboundCustomPayloadPacket.DEBUG_GAME_TEST_CLEAR.equals(var2)) {
                              this.minecraft.debugRenderer.gameTestDebugRenderer.clear();
                           } else if (ClientboundCustomPayloadPacket.DEBUG_GAME_TEST_ADD_MARKER.equals(var2)) {
                              var39 = var3.readBlockPos();
                              var38 = var3.readInt();
                              String var56 = var3.readUtf();
                              var46 = var3.readInt();
                              this.minecraft.debugRenderer.gameTestDebugRenderer.addMarker(var39, var38, var56, var46);
                           } else if (ClientboundCustomPayloadPacket.DEBUG_GAME_EVENT.equals(var2)) {
                              GameEvent var48 = (GameEvent)BuiltInRegistries.GAME_EVENT.get(new ResourceLocation(var3.readUtf()));
                              Vec3 var49 = new Vec3(var3.readDouble(), var3.readDouble(), var3.readDouble());
                              this.minecraft.debugRenderer.gameEventListenerRenderer.trackGameEvent(var48, var49);
                           } else if (ClientboundCustomPayloadPacket.DEBUG_GAME_EVENT_LISTENER.equals(var2)) {
                              ResourceLocation var52 = var3.readResourceLocation();
                              PositionSource var53 = ((PositionSourceType)BuiltInRegistries.POSITION_SOURCE_TYPE.getOptional(var52).orElseThrow(() -> {
                                 return new IllegalArgumentException("Unknown position source type " + var52);
                              })).read(var3);
                              var41 = var3.readVarInt();
                              this.minecraft.debugRenderer.gameEventListenerRenderer.trackListener(var53, var41);
                           } else {
                              LOGGER.warn("Unknown custom packed identifier: {}", var2);
                           }
                        }
                     }
                  }
               }
            }
         }
      } finally {
         if (var3 != null) {
            var3.release();
         }

      }

   }

   public void handleAddObjective(ClientboundSetObjectivePacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      Scoreboard var2 = this.level.getScoreboard();
      String var3 = var1.getObjectiveName();
      if (var1.getMethod() == 0) {
         var2.addObjective(var3, ObjectiveCriteria.DUMMY, var1.getDisplayName(), var1.getRenderType());
      } else if (var2.hasObjective(var3)) {
         Objective var4 = var2.getObjective(var3);
         if (var1.getMethod() == 1) {
            var2.removeObjective(var4);
         } else if (var1.getMethod() == 2) {
            var4.setRenderType(var1.getRenderType());
            var4.setDisplayName(var1.getDisplayName());
         }
      }

   }

   public void handleSetScore(ClientboundSetScorePacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      Scoreboard var2 = this.level.getScoreboard();
      String var3 = var1.getObjectiveName();
      switch(var1.getMethod()) {
      case CHANGE:
         Objective var4 = var2.getOrCreateObjective(var3);
         Score var5 = var2.getOrCreatePlayerScore(var1.getOwner(), var4);
         var5.setScore(var1.getScore());
         break;
      case REMOVE:
         var2.resetPlayerScore(var1.getOwner(), var2.getObjective(var3));
      }

   }

   public void handleSetDisplayObjective(ClientboundSetDisplayObjectivePacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      Scoreboard var2 = this.level.getScoreboard();
      String var3 = var1.getObjectiveName();
      Objective var4 = var3 == null ? null : var2.getOrCreateObjective(var3);
      var2.setDisplayObjective(var1.getSlot(), var4);
   }

   public void handleSetPlayerTeamPacket(ClientboundSetPlayerTeamPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      Scoreboard var2 = this.level.getScoreboard();
      ClientboundSetPlayerTeamPacket.Action var4 = var1.getTeamAction();
      PlayerTeam var3;
      if (var4 == ClientboundSetPlayerTeamPacket.Action.ADD) {
         var3 = var2.addPlayerTeam(var1.getName());
      } else {
         var3 = var2.getPlayerTeam(var1.getName());
         if (var3 == null) {
            LOGGER.warn("Received packet for unknown team {}: team action: {}, player action: {}", new Object[]{var1.getName(), var1.getTeamAction(), var1.getPlayerAction()});
            return;
         }
      }

      Optional var5 = var1.getParameters();
      var5.ifPresent((var1x) -> {
         var3.setDisplayName(var1x.getDisplayName());
         var3.setColor(var1x.getColor());
         var3.unpackOptions(var1x.getOptions());
         Team.Visibility var2 = Team.Visibility.byName(var1x.getNametagVisibility());
         if (var2 != null) {
            var3.setNameTagVisibility(var2);
         }

         Team.CollisionRule var3x = Team.CollisionRule.byName(var1x.getCollisionRule());
         if (var3x != null) {
            var3.setCollisionRule(var3x);
         }

         var3.setPlayerPrefix(var1x.getPlayerPrefix());
         var3.setPlayerSuffix(var1x.getPlayerSuffix());
      });
      ClientboundSetPlayerTeamPacket.Action var6 = var1.getPlayerAction();
      Iterator var7;
      String var8;
      if (var6 == ClientboundSetPlayerTeamPacket.Action.ADD) {
         var7 = var1.getPlayers().iterator();

         while(var7.hasNext()) {
            var8 = (String)var7.next();
            var2.addPlayerToTeam(var8, var3);
         }
      } else if (var6 == ClientboundSetPlayerTeamPacket.Action.REMOVE) {
         var7 = var1.getPlayers().iterator();

         while(var7.hasNext()) {
            var8 = (String)var7.next();
            var2.removePlayerFromTeam(var8, var3);
         }
      }

      if (var4 == ClientboundSetPlayerTeamPacket.Action.REMOVE) {
         var2.removePlayerTeam(var3);
      }

   }

   public void handleParticleEvent(ClientboundLevelParticlesPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      if (var1.getCount() == 0) {
         double var2 = (double)(var1.getMaxSpeed() * var1.getXDist());
         double var4 = (double)(var1.getMaxSpeed() * var1.getYDist());
         double var6 = (double)(var1.getMaxSpeed() * var1.getZDist());

         try {
            this.level.addParticle(var1.getParticle(), var1.isOverrideLimiter(), var1.getX(), var1.getY(), var1.getZ(), var2, var4, var6);
         } catch (Throwable var17) {
            LOGGER.warn("Could not spawn particle effect {}", var1.getParticle());
         }
      } else {
         for(int var18 = 0; var18 < var1.getCount(); ++var18) {
            double var3 = this.random.nextGaussian() * (double)var1.getXDist();
            double var5 = this.random.nextGaussian() * (double)var1.getYDist();
            double var7 = this.random.nextGaussian() * (double)var1.getZDist();
            double var9 = this.random.nextGaussian() * (double)var1.getMaxSpeed();
            double var11 = this.random.nextGaussian() * (double)var1.getMaxSpeed();
            double var13 = this.random.nextGaussian() * (double)var1.getMaxSpeed();

            try {
               this.level.addParticle(var1.getParticle(), var1.isOverrideLimiter(), var1.getX() + var3, var1.getY() + var5, var1.getZ() + var7, var9, var11, var13);
            } catch (Throwable var16) {
               LOGGER.warn("Could not spawn particle effect {}", var1.getParticle());
               return;
            }
         }
      }

   }

   public void handlePing(ClientboundPingPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      this.send((Packet)(new ServerboundPongPacket(var1.getId())));
   }

   public void handleUpdateAttributes(ClientboundUpdateAttributesPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      Entity var2 = this.level.getEntity(var1.getEntityId());
      if (var2 != null) {
         if (!(var2 instanceof LivingEntity)) {
            throw new IllegalStateException("Server tried to update attributes of a non-living entity (actually: " + var2 + ")");
         } else {
            AttributeMap var3 = ((LivingEntity)var2).getAttributes();
            Iterator var4 = var1.getValues().iterator();

            while(true) {
               while(var4.hasNext()) {
                  ClientboundUpdateAttributesPacket.AttributeSnapshot var5 = (ClientboundUpdateAttributesPacket.AttributeSnapshot)var4.next();
                  AttributeInstance var6 = var3.getInstance(var5.getAttribute());
                  if (var6 == null) {
                     LOGGER.warn("Entity {} does not have attribute {}", var2, BuiltInRegistries.ATTRIBUTE.getKey(var5.getAttribute()));
                  } else {
                     var6.setBaseValue(var5.getBase());
                     var6.removeModifiers();
                     Iterator var7 = var5.getModifiers().iterator();

                     while(var7.hasNext()) {
                        AttributeModifier var8 = (AttributeModifier)var7.next();
                        var6.addTransientModifier(var8);
                     }
                  }
               }

               return;
            }
         }
      }
   }

   public void handlePlaceRecipe(ClientboundPlaceGhostRecipePacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      AbstractContainerMenu var2 = this.minecraft.player.containerMenu;
      if (var2.containerId == var1.getContainerId()) {
         this.recipeManager.byKey(var1.getRecipe()).ifPresent((var2x) -> {
            if (this.minecraft.screen instanceof RecipeUpdateListener) {
               RecipeBookComponent var3 = ((RecipeUpdateListener)this.minecraft.screen).getRecipeBookComponent();
               var3.setupGhostRecipe(var2x, var2.slots);
            }

         });
      }
   }

   public void handleLightUpdatePacket(ClientboundLightUpdatePacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      int var2 = var1.getX();
      int var3 = var1.getZ();
      ClientboundLightUpdatePacketData var4 = var1.getLightData();
      this.level.queueLightUpdate(() -> {
         this.applyLightData(var2, var3, var4);
      });
   }

   private void applyLightData(int var1, int var2, ClientboundLightUpdatePacketData var3) {
      LevelLightEngine var4 = this.level.getChunkSource().getLightEngine();
      BitSet var5 = var3.getSkyYMask();
      BitSet var6 = var3.getEmptySkyYMask();
      Iterator var7 = var3.getSkyUpdates().iterator();
      this.readSectionList(var1, var2, var4, LightLayer.SKY, var5, var6, var7);
      BitSet var8 = var3.getBlockYMask();
      BitSet var9 = var3.getEmptyBlockYMask();
      Iterator var10 = var3.getBlockUpdates().iterator();
      this.readSectionList(var1, var2, var4, LightLayer.BLOCK, var8, var9, var10);
      var4.setLightEnabled(new ChunkPos(var1, var2), true);
   }

   public void handleMerchantOffers(ClientboundMerchantOffersPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      AbstractContainerMenu var2 = this.minecraft.player.containerMenu;
      if (var1.getContainerId() == var2.containerId && var2 instanceof MerchantMenu) {
         MerchantMenu var3 = (MerchantMenu)var2;
         var3.setOffers(new MerchantOffers(var1.getOffers().createTag()));
         var3.setXp(var1.getVillagerXp());
         var3.setMerchantLevel(var1.getVillagerLevel());
         var3.setShowProgressBar(var1.showProgress());
         var3.setCanRestock(var1.canRestock());
      }

   }

   public void handleSetChunkCacheRadius(ClientboundSetChunkCacheRadiusPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      this.serverChunkRadius = var1.getRadius();
      this.minecraft.options.setServerRenderDistance(this.serverChunkRadius);
      this.level.getChunkSource().updateViewRadius(var1.getRadius());
   }

   public void handleSetSimulationDistance(ClientboundSetSimulationDistancePacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      this.serverSimulationDistance = var1.simulationDistance();
      this.level.setServerSimulationDistance(this.serverSimulationDistance);
   }

   public void handleSetChunkCacheCenter(ClientboundSetChunkCacheCenterPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      this.level.getChunkSource().updateViewCenter(var1.getX(), var1.getZ());
   }

   public void handleBlockChangedAck(ClientboundBlockChangedAckPacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      this.level.handleBlockChangedAck(var1.sequence());
   }

   public void handleBundlePacket(ClientboundBundlePacket var1) {
      PacketUtils.ensureRunningOnSameThread(var1, this, (BlockableEventLoop)this.minecraft);
      Iterator var2 = var1.subPackets().iterator();

      while(var2.hasNext()) {
         Packet var3 = (Packet)var2.next();
         var3.handle(this);
      }

   }

   private void readSectionList(int var1, int var2, LevelLightEngine var3, LightLayer var4, BitSet var5, BitSet var6, Iterator<byte[]> var7) {
      for(int var8 = 0; var8 < var3.getLightSectionCount(); ++var8) {
         int var9 = var3.getMinLightSection() + var8;
         boolean var10 = var5.get(var8);
         boolean var11 = var6.get(var8);
         if (var10 || var11) {
            var3.queueSectionData(var4, SectionPos.of(var1, var9, var2), var10 ? new DataLayer((byte[])((byte[])var7.next()).clone()) : new DataLayer());
            this.level.setSectionDirtyWithNeighbors(var1, var9, var2);
         }
      }

   }

   public Connection getConnection() {
      return this.connection;
   }

   public boolean isAcceptingMessages() {
      return this.connection.isConnected();
   }

   public Collection<PlayerInfo> getListedOnlinePlayers() {
      return this.listedPlayers;
   }

   public Collection<PlayerInfo> getOnlinePlayers() {
      return this.playerInfoMap.values();
   }

   public Collection<UUID> getOnlinePlayerIds() {
      return this.playerInfoMap.keySet();
   }

   @Nullable
   public PlayerInfo getPlayerInfo(UUID var1) {
      return (PlayerInfo)this.playerInfoMap.get(var1);
   }

   @Nullable
   public PlayerInfo getPlayerInfo(String var1) {
      Iterator var2 = this.playerInfoMap.values().iterator();

      PlayerInfo var3;
      do {
         if (!var2.hasNext()) {
            return null;
         }

         var3 = (PlayerInfo)var2.next();
      } while(!var3.getProfile().getName().equals(var1));

      return var3;
   }

   public GameProfile getLocalGameProfile() {
      return this.localGameProfile;
   }

   public ClientAdvancements getAdvancements() {
      return this.advancements;
   }

   public CommandDispatcher<SharedSuggestionProvider> getCommands() {
      return this.commands;
   }

   public ClientLevel getLevel() {
      return this.level;
   }

   public DebugQueryHandler getDebugQueryHandler() {
      return this.debugQueryHandler;
   }

   public UUID getId() {
      return this.id;
   }

   public Set<ResourceKey<Level>> levels() {
      return this.levels;
   }

   public RegistryAccess registryAccess() {
      return this.registryAccess.compositeAccess();
   }

   public void markMessageAsProcessed(PlayerChatMessage var1, boolean var2) {
      MessageSignature var3 = var1.signature();
      if (var3 != null && this.lastSeenMessages.addPending(var3, var2) && this.lastSeenMessages.offset() > 64) {
         this.sendChatAcknowledgement();
      }

   }

   private void sendChatAcknowledgement() {
      int var1 = this.lastSeenMessages.getAndClearOffset();
      if (var1 > 0) {
         this.send((Packet)(new ServerboundChatAckPacket(var1)));
      }

   }

   public void sendChat(String var1) {
      Instant var2 = Instant.now();
      long var3 = Crypt.SaltSupplier.getLong();
      LastSeenMessagesTracker.Update var5 = this.lastSeenMessages.generateAndApplyUpdate();
      MessageSignature var6 = this.signedMessageEncoder.pack(new SignedMessageBody(var1, var2, var3, var5.lastSeen()));
      this.send((Packet)(new ServerboundChatPacket(var1, var2, var3, var6, var5.update())));
   }

   public void sendCommand(String var1) {
      Instant var2 = Instant.now();
      long var3 = Crypt.SaltSupplier.getLong();
      LastSeenMessagesTracker.Update var5 = this.lastSeenMessages.generateAndApplyUpdate();
      ArgumentSignatures var6 = ArgumentSignatures.signCommand(SignableCommand.of(this.parseCommand(var1)), (var5x) -> {
         SignedMessageBody var6 = new SignedMessageBody(var5x, var2, var3, var5.lastSeen());
         return this.signedMessageEncoder.pack(var6);
      });
      this.send((Packet)(new ServerboundChatCommandPacket(var1, var2, var3, var6, var5.update())));
   }

   public boolean sendUnsignedCommand(String var1) {
      if (SignableCommand.of(this.parseCommand(var1)).arguments().isEmpty()) {
         LastSeenMessagesTracker.Update var2 = this.lastSeenMessages.generateAndApplyUpdate();
         this.send((Packet)(new ServerboundChatCommandPacket(var1, Instant.now(), 0L, ArgumentSignatures.EMPTY, var2.update())));
         return true;
      } else {
         return false;
      }
   }

   private ParseResults<SharedSuggestionProvider> parseCommand(String var1) {
      return this.commands.parse(var1, this.suggestionsProvider);
   }

   public void tick() {
      if (this.connection.isEncrypted()) {
         ProfileKeyPairManager var1 = this.minecraft.getProfileKeyPairManager();
         if (var1.shouldRefreshKeyPair()) {
            var1.prepareKeyPair().thenAcceptAsync((var1x) -> {
               var1x.ifPresent(this::setKeyPair);
            }, this.minecraft);
         }
      }

      this.sendDeferredPackets();
      this.telemetryManager.tick();
   }

   public void setKeyPair(ProfileKeyPair var1) {
      if (this.localGameProfile.getId().equals(this.minecraft.getUser().getProfileId())) {
         if (this.chatSession == null || !this.chatSession.keyPair().equals(var1)) {
            this.chatSession = LocalChatSession.create(var1);
            this.signedMessageEncoder = this.chatSession.createMessageEncoder(this.localGameProfile.getId());
            this.send((Packet)(new ServerboundChatSessionUpdatePacket(this.chatSession.asRemote().asData())));
         }
      }
   }

   @Nullable
   public ServerData getServerData() {
      return this.serverData;
   }

   public FeatureFlagSet enabledFeatures() {
      return this.enabledFeatures;
   }

   public boolean isFeatureEnabled(FeatureFlagSet var1) {
      return var1.isSubsetOf(this.enabledFeatures());
   }

   private static record DeferredPacket(Packet<ServerGamePacketListener> a, BooleanSupplier b, long c) {
      final Packet<ServerGamePacketListener> packet;
      private final BooleanSupplier sendCondition;
      private final long expirationTime;

      DeferredPacket(Packet<ServerGamePacketListener> var1, BooleanSupplier var2, long var3) {
         this.packet = var1;
         this.sendCondition = var2;
         this.expirationTime = var3;
      }

      public Packet<ServerGamePacketListener> packet() {
         return this.packet;
      }

      public BooleanSupplier sendCondition() {
         return this.sendCondition;
      }

      public long expirationTime() {
         return this.expirationTime;
      }
   }
}
