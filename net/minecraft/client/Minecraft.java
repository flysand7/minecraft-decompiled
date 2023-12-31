package net.minecraft.client;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Queues;
import com.google.common.collect.UnmodifiableIterator;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.BanDetails;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.minecraft.UserApiService.UserFlag;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.authlib.yggdrasil.ServicesKeyType;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.DisplayData;
import com.mojang.blaze3d.platform.GlDebug;
import com.mojang.blaze3d.platform.GlUtil;
import com.mojang.blaze3d.platform.IconSet;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.WindowEventHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.TimerQuery;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.gui.RealmsDataFetcher;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.Proxy;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.FileUtil;
import net.minecraft.Optionull;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.SystemReport;
import net.minecraft.Util;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.gui.components.toasts.TutorialToast;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.BanNoticeScreen;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.InBedChatScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.OutOfMemoryScreen;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.client.gui.screens.social.SocialInteractionsScreen;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.client.multiplayer.chat.report.ReportEnvironment;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.profiling.ClientMetricsSamplersProvider;
import net.minecraft.client.quickplay.QuickPlay;
import net.minecraft.client.quickplay.QuickPlayLog;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.GpuWarnlistManager;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.VirtualScreen;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.ClientPackSource;
import net.minecraft.client.resources.DownloadedPackSource;
import net.minecraft.client.resources.FoliageColorReloadListener;
import net.minecraft.client.resources.GrassColorReloadListener;
import net.minecraft.client.resources.MobEffectTextureManager;
import net.minecraft.client.resources.PaintingTextureManager;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.client.resources.SplashManager;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.searchtree.FullTextSearchTree;
import net.minecraft.client.searchtree.IdSearchTree;
import net.minecraft.client.searchtree.SearchRegistry;
import net.minecraft.client.searchtree.SearchTree;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.telemetry.ClientTelemetryManager;
import net.minecraft.client.telemetry.TelemetryProperty;
import net.minecraft.client.telemetry.events.GameLoadTimesEvent;
import net.minecraft.client.tutorial.Tutorial;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.KeybindResolver;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.server.WorldStem;
import net.minecraft.server.level.progress.ProcessorChunkProgressListener;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.Musics;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.FileZipper;
import net.minecraft.util.FrameTimer;
import net.minecraft.util.MemoryReserve;
import net.minecraft.util.ModCheck;
import net.minecraft.util.Mth;
import net.minecraft.util.SignatureValidator;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.Unit;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.profiling.ContinuousProfiler;
import net.minecraft.util.profiling.EmptyProfileResults;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.util.profiling.ProfileResults;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.ResultField;
import net.minecraft.util.profiling.SingleTickProfiler;
import net.minecraft.util.profiling.metrics.profiling.ActiveMetricsRecorder;
import net.minecraft.util.profiling.metrics.profiling.InactiveMetricsRecorder;
import net.minecraft.util.profiling.metrics.profiling.MetricsRecorder;
import net.minecraft.util.profiling.metrics.storage.MetricsPersister;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PlayerHeadItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.validation.DirectoryValidator;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.apache.commons.io.FileUtils;
import org.joml.Matrix4f;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.slf4j.Logger;

public class Minecraft extends ReentrantBlockableEventLoop<Runnable> implements WindowEventHandler {
   static Minecraft instance;
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final boolean ON_OSX;
   private static final int MAX_TICKS_PER_UPDATE = 10;
   public static final ResourceLocation DEFAULT_FONT;
   public static final ResourceLocation UNIFORM_FONT;
   public static final ResourceLocation ALT_FONT;
   private static final ResourceLocation REGIONAL_COMPLIANCIES;
   private static final CompletableFuture<Unit> RESOURCE_RELOAD_INITIAL_TASK;
   private static final Component SOCIAL_INTERACTIONS_NOT_AVAILABLE;
   public static final String UPDATE_DRIVERS_ADVICE = "Please make sure you have up-to-date drivers (see aka.ms/mcdriver for instructions).";
   private final Path resourcePackDirectory;
   private final PropertyMap profileProperties;
   private final TextureManager textureManager;
   private final DataFixer fixerUpper;
   private final VirtualScreen virtualScreen;
   private final Window window;
   private final Timer timer = new Timer(20.0F, 0L);
   private final RenderBuffers renderBuffers;
   public final LevelRenderer levelRenderer;
   private final EntityRenderDispatcher entityRenderDispatcher;
   private final ItemRenderer itemRenderer;
   public final ParticleEngine particleEngine;
   private final SearchRegistry searchRegistry = new SearchRegistry();
   private final User user;
   public final Font font;
   public final Font fontFilterFishy;
   public final GameRenderer gameRenderer;
   public final DebugRenderer debugRenderer;
   private final AtomicReference<StoringChunkProgressListener> progressListener = new AtomicReference();
   public final Gui gui;
   public final Options options;
   private final HotbarManager hotbarManager;
   public final MouseHandler mouseHandler;
   public final KeyboardHandler keyboardHandler;
   private InputType lastInputType;
   public final File gameDirectory;
   private final String launchedVersion;
   private final String versionType;
   private final Proxy proxy;
   private final LevelStorageSource levelSource;
   public final FrameTimer frameTimer;
   private final boolean is64bit;
   private final boolean demo;
   private final boolean allowsMultiplayer;
   private final boolean allowsChat;
   private final ReloadableResourceManager resourceManager;
   private final VanillaPackResources vanillaPackResources;
   private final DownloadedPackSource downloadedPackSource;
   private final PackRepository resourcePackRepository;
   private final LanguageManager languageManager;
   private final BlockColors blockColors;
   private final ItemColors itemColors;
   private final RenderTarget mainRenderTarget;
   private final SoundManager soundManager;
   private final MusicManager musicManager;
   private final FontManager fontManager;
   private final SplashManager splashManager;
   private final GpuWarnlistManager gpuWarnlistManager;
   private final PeriodicNotificationManager regionalCompliancies;
   private final YggdrasilAuthenticationService authenticationService;
   private final MinecraftSessionService minecraftSessionService;
   private final UserApiService userApiService;
   private final SkinManager skinManager;
   private final ModelManager modelManager;
   private final BlockRenderDispatcher blockRenderer;
   private final PaintingTextureManager paintingTextures;
   private final MobEffectTextureManager mobEffectTextures;
   private final ToastComponent toast;
   private final Tutorial tutorial;
   private final PlayerSocialManager playerSocialManager;
   private final EntityModelSet entityModels;
   private final BlockEntityRenderDispatcher blockEntityRenderDispatcher;
   private final ClientTelemetryManager telemetryManager;
   private final ProfileKeyPairManager profileKeyPairManager;
   private final RealmsDataFetcher realmsDataFetcher;
   private final QuickPlayLog quickPlayLog;
   @Nullable
   public MultiPlayerGameMode gameMode;
   @Nullable
   public ClientLevel level;
   @Nullable
   public LocalPlayer player;
   @Nullable
   private IntegratedServer singleplayerServer;
   @Nullable
   private Connection pendingConnection;
   private boolean isLocalServer;
   @Nullable
   public Entity cameraEntity;
   @Nullable
   public Entity crosshairPickEntity;
   @Nullable
   public HitResult hitResult;
   private int rightClickDelay;
   protected int missTime;
   private volatile boolean pause;
   private float pausePartialTick;
   private long lastNanoTime;
   private long lastTime;
   private int frames;
   public boolean noRender;
   @Nullable
   public Screen screen;
   @Nullable
   private Overlay overlay;
   private boolean connectedToRealms;
   private Thread gameThread;
   private volatile boolean running;
   @Nullable
   private Supplier<CrashReport> delayedCrash;
   private static int fps;
   public String fpsString;
   private long frameTimeNs;
   public boolean wireframe;
   public boolean chunkPath;
   public boolean chunkVisibility;
   public boolean smartCull;
   private boolean windowActive;
   private final Queue<Runnable> progressTasks;
   @Nullable
   private CompletableFuture<Void> pendingReload;
   @Nullable
   private TutorialToast socialInteractionsToast;
   private ProfilerFiller profiler;
   private int fpsPieRenderTicks;
   private final ContinuousProfiler fpsPieProfiler;
   @Nullable
   private ProfileResults fpsPieResults;
   private MetricsRecorder metricsRecorder;
   private final ResourceLoadStateTracker reloadStateTracker;
   private long savedCpuDuration;
   private double gpuUtilization;
   @Nullable
   private TimerQuery.FrameProfile currentFrameProfile;
   private final Realms32BitWarningStatus realms32BitWarningStatus;
   private final GameNarrator narrator;
   private final ChatListener chatListener;
   private ReportingContext reportingContext;
   private String debugPath;

   public Minecraft(GameConfig var1) {
      super("Client");
      this.lastInputType = InputType.NONE;
      this.frameTimer = new FrameTimer();
      this.regionalCompliancies = new PeriodicNotificationManager(REGIONAL_COMPLIANCIES, Minecraft::countryEqualsISO3);
      this.lastNanoTime = Util.getNanos();
      this.fpsString = "";
      this.smartCull = true;
      this.progressTasks = Queues.newConcurrentLinkedQueue();
      this.profiler = InactiveProfiler.INSTANCE;
      this.fpsPieProfiler = new ContinuousProfiler(Util.timeSource, () -> {
         return this.fpsPieRenderTicks;
      });
      this.metricsRecorder = InactiveMetricsRecorder.INSTANCE;
      this.reloadStateTracker = new ResourceLoadStateTracker();
      this.debugPath = "root";
      instance = this;
      this.gameDirectory = var1.location.gameDirectory;
      File var2 = var1.location.assetDirectory;
      this.resourcePackDirectory = var1.location.resourcePackDirectory.toPath();
      this.launchedVersion = var1.game.launchVersion;
      this.versionType = var1.game.versionType;
      this.profileProperties = var1.user.profileProperties;
      ClientPackSource var3 = new ClientPackSource(var1.location.getExternalAssetSource());
      this.downloadedPackSource = new DownloadedPackSource(new File(this.gameDirectory, "server-resource-packs"));
      FolderRepositorySource var4 = new FolderRepositorySource(this.resourcePackDirectory, PackType.CLIENT_RESOURCES, PackSource.DEFAULT);
      this.resourcePackRepository = new PackRepository(new RepositorySource[]{var3, this.downloadedPackSource, var4});
      this.vanillaPackResources = var3.getVanillaPack();
      this.proxy = var1.user.proxy;
      this.authenticationService = new YggdrasilAuthenticationService(this.proxy);
      this.minecraftSessionService = this.authenticationService.createMinecraftSessionService();
      this.userApiService = this.createUserApiService(this.authenticationService, var1);
      this.user = var1.user.user;
      LOGGER.info("Setting user: {}", this.user.getName());
      LOGGER.debug("(Session ID is {})", this.user.getSessionId());
      this.demo = var1.game.demo;
      this.allowsMultiplayer = !var1.game.disableMultiplayer;
      this.allowsChat = !var1.game.disableChat;
      this.is64bit = checkIs64Bit();
      this.singleplayerServer = null;
      KeybindResolver.setKeyResolver(KeyMapping::createNameSupplier);
      this.fixerUpper = DataFixers.getDataFixer();
      this.toast = new ToastComponent(this);
      this.gameThread = Thread.currentThread();
      this.options = new Options(this, this.gameDirectory);
      RenderSystem.setShaderGlintAlpha((Double)this.options.glintStrength().get());
      this.running = true;
      this.tutorial = new Tutorial(this, this.options);
      this.hotbarManager = new HotbarManager(this.gameDirectory, this.fixerUpper);
      LOGGER.info("Backend library: {}", RenderSystem.getBackendDescription());
      DisplayData var5;
      if (this.options.overrideHeight > 0 && this.options.overrideWidth > 0) {
         var5 = new DisplayData(this.options.overrideWidth, this.options.overrideHeight, var1.display.fullscreenWidth, var1.display.fullscreenHeight, var1.display.isFullscreen);
      } else {
         var5 = var1.display;
      }

      Util.timeSource = RenderSystem.initBackendSystem();
      this.virtualScreen = new VirtualScreen(this);
      this.window = this.virtualScreen.newWindow(var5, this.options.fullscreenVideoModeString, this.createTitle());
      this.setWindowActive(true);
      GameLoadTimesEvent.INSTANCE.endStep(TelemetryProperty.LOAD_TIME_PRE_WINDOW_MS);

      try {
         this.window.setIcon(this.vanillaPackResources, SharedConstants.getCurrentVersion().isStable() ? IconSet.RELEASE : IconSet.SNAPSHOT);
      } catch (IOException var12) {
         LOGGER.error("Couldn't set icon", var12);
      }

      this.window.setFramerateLimit((Integer)this.options.framerateLimit().get());
      this.mouseHandler = new MouseHandler(this);
      this.mouseHandler.setup(this.window.getWindow());
      this.keyboardHandler = new KeyboardHandler(this);
      this.keyboardHandler.setup(this.window.getWindow());
      RenderSystem.initRenderer(this.options.glDebugVerbosity, false);
      this.mainRenderTarget = new MainTarget(this.window.getWidth(), this.window.getHeight());
      this.mainRenderTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
      this.mainRenderTarget.clear(ON_OSX);
      this.resourceManager = new ReloadableResourceManager(PackType.CLIENT_RESOURCES);
      this.resourcePackRepository.reload();
      this.options.loadSelectedResourcePacks(this.resourcePackRepository);
      this.languageManager = new LanguageManager(this.options.languageCode);
      this.resourceManager.registerReloadListener(this.languageManager);
      this.textureManager = new TextureManager(this.resourceManager);
      this.resourceManager.registerReloadListener(this.textureManager);
      this.skinManager = new SkinManager(this.textureManager, new File(var2, "skins"), this.minecraftSessionService);
      Path var6 = this.gameDirectory.toPath();
      DirectoryValidator var7 = LevelStorageSource.parseValidator(var6.resolve("allowed_symlinks.txt"));
      this.levelSource = new LevelStorageSource(var6.resolve("saves"), var6.resolve("backups"), var7, this.fixerUpper);
      this.soundManager = new SoundManager(this.options);
      this.resourceManager.registerReloadListener(this.soundManager);
      this.splashManager = new SplashManager(this.user);
      this.resourceManager.registerReloadListener(this.splashManager);
      this.musicManager = new MusicManager(this);
      this.fontManager = new FontManager(this.textureManager);
      this.font = this.fontManager.createFont();
      this.fontFilterFishy = this.fontManager.createFontFilterFishy();
      this.resourceManager.registerReloadListener(this.fontManager);
      this.selectMainFont(this.isEnforceUnicode());
      this.resourceManager.registerReloadListener(new GrassColorReloadListener());
      this.resourceManager.registerReloadListener(new FoliageColorReloadListener());
      this.window.setErrorSection("Startup");
      RenderSystem.setupDefaultState(0, 0, this.window.getWidth(), this.window.getHeight());
      this.window.setErrorSection("Post startup");
      this.blockColors = BlockColors.createDefault();
      this.itemColors = ItemColors.createDefault(this.blockColors);
      this.modelManager = new ModelManager(this.textureManager, this.blockColors, (Integer)this.options.mipmapLevels().get());
      this.resourceManager.registerReloadListener(this.modelManager);
      this.entityModels = new EntityModelSet();
      this.resourceManager.registerReloadListener(this.entityModels);
      this.blockEntityRenderDispatcher = new BlockEntityRenderDispatcher(this.font, this.entityModels, this::getBlockRenderer, this::getItemRenderer, this::getEntityRenderDispatcher);
      this.resourceManager.registerReloadListener(this.blockEntityRenderDispatcher);
      BlockEntityWithoutLevelRenderer var8 = new BlockEntityWithoutLevelRenderer(this.blockEntityRenderDispatcher, this.entityModels);
      this.resourceManager.registerReloadListener(var8);
      this.itemRenderer = new ItemRenderer(this, this.textureManager, this.modelManager, this.itemColors, var8);
      this.resourceManager.registerReloadListener(this.itemRenderer);
      this.renderBuffers = new RenderBuffers();
      this.playerSocialManager = new PlayerSocialManager(this, this.userApiService);
      this.blockRenderer = new BlockRenderDispatcher(this.modelManager.getBlockModelShaper(), var8, this.blockColors);
      this.resourceManager.registerReloadListener(this.blockRenderer);
      this.entityRenderDispatcher = new EntityRenderDispatcher(this, this.textureManager, this.itemRenderer, this.blockRenderer, this.font, this.options, this.entityModels);
      this.resourceManager.registerReloadListener(this.entityRenderDispatcher);
      this.gameRenderer = new GameRenderer(this, this.entityRenderDispatcher.getItemInHandRenderer(), this.resourceManager, this.renderBuffers);
      this.resourceManager.registerReloadListener(this.gameRenderer.createReloadListener());
      this.levelRenderer = new LevelRenderer(this, this.entityRenderDispatcher, this.blockEntityRenderDispatcher, this.renderBuffers);
      this.resourceManager.registerReloadListener(this.levelRenderer);
      this.createSearchTrees();
      this.resourceManager.registerReloadListener(this.searchRegistry);
      this.particleEngine = new ParticleEngine(this.level, this.textureManager);
      this.resourceManager.registerReloadListener(this.particleEngine);
      this.paintingTextures = new PaintingTextureManager(this.textureManager);
      this.resourceManager.registerReloadListener(this.paintingTextures);
      this.mobEffectTextures = new MobEffectTextureManager(this.textureManager);
      this.resourceManager.registerReloadListener(this.mobEffectTextures);
      this.gpuWarnlistManager = new GpuWarnlistManager();
      this.resourceManager.registerReloadListener(this.gpuWarnlistManager);
      this.resourceManager.registerReloadListener(this.regionalCompliancies);
      this.gui = new Gui(this, this.itemRenderer);
      this.debugRenderer = new DebugRenderer(this);
      RealmsClient var9 = RealmsClient.create(this);
      this.realmsDataFetcher = new RealmsDataFetcher(var9);
      RenderSystem.setErrorCallback(this::onFullscreenError);
      if (this.mainRenderTarget.width == this.window.getWidth() && this.mainRenderTarget.height == this.window.getHeight()) {
         if ((Boolean)this.options.fullscreen().get() && !this.window.isFullscreen()) {
            this.window.toggleFullScreen();
            this.options.fullscreen().set(this.window.isFullscreen());
         }
      } else {
         int var10002 = this.window.getWidth();
         StringBuilder var10 = new StringBuilder("Recovering from unsupported resolution (" + var10002 + "x" + this.window.getHeight() + ").\nPlease make sure you have up-to-date drivers (see aka.ms/mcdriver for instructions).");
         if (GlDebug.isDebugEnabled()) {
            var10.append("\n\nReported GL debug messages:\n").append(String.join("\n", GlDebug.getLastOpenGlDebugMessages()));
         }

         this.window.setWindowed(this.mainRenderTarget.width, this.mainRenderTarget.height);
         TinyFileDialogs.tinyfd_messageBox("Minecraft", var10.toString(), "ok", "error", false);
      }

      this.window.updateVsync((Boolean)this.options.enableVsync().get());
      this.window.updateRawMouseInput((Boolean)this.options.rawMouseInput().get());
      this.window.setDefaultErrorCallback();
      this.resizeDisplay();
      this.gameRenderer.preloadUiShader(this.vanillaPackResources.asProvider());
      this.telemetryManager = new ClientTelemetryManager(this, this.userApiService, this.user);
      this.profileKeyPairManager = ProfileKeyPairManager.create(this.userApiService, this.user, var6);
      this.realms32BitWarningStatus = new Realms32BitWarningStatus(this);
      this.narrator = new GameNarrator(this);
      this.narrator.checkStatus(this.options.narrator().get() != NarratorStatus.OFF);
      this.chatListener = new ChatListener(this);
      this.chatListener.setMessageDelay((Double)this.options.chatDelay().get());
      this.reportingContext = ReportingContext.create(ReportEnvironment.local(), this.userApiService);
      LoadingOverlay.registerTextures(this);
      List var13 = this.resourcePackRepository.openAllSelected();
      this.reloadStateTracker.startReload(ResourceLoadStateTracker.ReloadReason.INITIAL, var13);
      ReloadInstance var11 = this.resourceManager.createReload(Util.backgroundExecutor(), this, RESOURCE_RELOAD_INITIAL_TASK, var13);
      GameLoadTimesEvent.INSTANCE.beginStep(TelemetryProperty.LOAD_TIME_LOADING_OVERLAY_MS);
      this.setOverlay(new LoadingOverlay(this, var11, (var1x) -> {
         Util.ifElse(var1x, this::rollbackResourcePacks, () -> {
            if (SharedConstants.IS_RUNNING_IN_IDE) {
               this.selfTest();
            }

            this.reloadStateTracker.finishReload();
            this.onGameLoadFinished();
         });
      }, false));
      this.quickPlayLog = QuickPlayLog.of(var1.quickPlay.path());
      if (this.shouldShowBanNotice()) {
         this.setScreen(BanNoticeScreen.create((var4x) -> {
            if (var4x) {
               Util.getPlatform().openUri("https://aka.ms/mcjavamoderation");
            }

            this.setInitialScreen(var9, var11, var1.quickPlay);
         }, this.multiplayerBan()));
      } else {
         this.setInitialScreen(var9, var11, var1.quickPlay);
      }

   }

   private void onGameLoadFinished() {
      GameLoadTimesEvent.INSTANCE.endStep(TelemetryProperty.LOAD_TIME_LOADING_OVERLAY_MS);
      GameLoadTimesEvent.INSTANCE.endStep(TelemetryProperty.LOAD_TIME_TOTAL_TIME_MS);
      GameLoadTimesEvent.INSTANCE.send(this.telemetryManager.getOutsideSessionSender());
   }

   private void setInitialScreen(RealmsClient var1, ReloadInstance var2, GameConfig.QuickPlayData var3) {
      if (var3.isEnabled()) {
         QuickPlay.connect(this, var3, var2, var1);
      } else if (this.options.onboardAccessibility) {
         this.setScreen(new AccessibilityOnboardingScreen(this.options));
      } else {
         this.setScreen(new TitleScreen(true));
      }

   }

   private static boolean countryEqualsISO3(Object var0) {
      try {
         return Locale.getDefault().getISO3Country().equals(var0);
      } catch (MissingResourceException var2) {
         return false;
      }
   }

   public void updateTitle() {
      this.window.setTitle(this.createTitle());
   }

   private String createTitle() {
      StringBuilder var1 = new StringBuilder("Minecraft");
      if (checkModStatus().shouldReportAsModified()) {
         var1.append("*");
      }

      var1.append(" ");
      var1.append(SharedConstants.getCurrentVersion().getName());
      ClientPacketListener var2 = this.getConnection();
      if (var2 != null && var2.getConnection().isConnected()) {
         var1.append(" - ");
         if (this.singleplayerServer != null && !this.singleplayerServer.isPublished()) {
            var1.append(I18n.get("title.singleplayer"));
         } else if (this.isConnectedToRealms()) {
            var1.append(I18n.get("title.multiplayer.realms"));
         } else if (this.singleplayerServer == null && (this.getCurrentServer() == null || !this.getCurrentServer().isLan())) {
            var1.append(I18n.get("title.multiplayer.other"));
         } else {
            var1.append(I18n.get("title.multiplayer.lan"));
         }
      }

      return var1.toString();
   }

   private UserApiService createUserApiService(YggdrasilAuthenticationService var1, GameConfig var2) {
      try {
         return var1.createUserApiService(var2.user.user.getAccessToken());
      } catch (AuthenticationException var4) {
         LOGGER.error("Failed to verify authentication", var4);
         return UserApiService.OFFLINE;
      }
   }

   public static ModCheck checkModStatus() {
      return ModCheck.identify("vanilla", ClientBrandRetriever::getClientModName, "Client", Minecraft.class);
   }

   private void rollbackResourcePacks(Throwable var1) {
      if (this.resourcePackRepository.getSelectedIds().size() > 1) {
         this.clearResourcePacksOnError(var1, (Component)null);
      } else {
         Util.throwAsRuntime(var1);
      }

   }

   public void clearResourcePacksOnError(Throwable var1, @Nullable Component var2) {
      LOGGER.info("Caught error loading resourcepacks, removing all selected resourcepacks", var1);
      this.reloadStateTracker.startRecovery(var1);
      this.resourcePackRepository.setSelected(Collections.emptyList());
      this.options.resourcePacks.clear();
      this.options.incompatibleResourcePacks.clear();
      this.options.save();
      this.reloadResourcePacks(true).thenRun(() -> {
         this.addResourcePackLoadFailToast(var2);
      });
   }

   private void abortResourcePackRecovery() {
      this.setOverlay((Overlay)null);
      if (this.level != null) {
         this.level.disconnect();
         this.clearLevel();
      }

      this.setScreen(new TitleScreen());
      this.addResourcePackLoadFailToast((Component)null);
   }

   private void addResourcePackLoadFailToast(@Nullable Component var1) {
      ToastComponent var2 = this.getToasts();
      SystemToast.addOrUpdate(var2, SystemToast.SystemToastIds.PACK_LOAD_FAILURE, Component.translatable("resourcePack.load_fail"), var1);
   }

   public void run() {
      this.gameThread = Thread.currentThread();
      if (Runtime.getRuntime().availableProcessors() > 4) {
         this.gameThread.setPriority(10);
      }

      try {
         boolean var1 = false;

         while(this.running) {
            if (this.delayedCrash != null) {
               crash((CrashReport)this.delayedCrash.get());
               return;
            }

            try {
               SingleTickProfiler var7 = SingleTickProfiler.createTickProfiler("Renderer");
               boolean var3 = this.shouldRenderFpsPie();
               this.profiler = this.constructProfiler(var3, var7);
               this.profiler.startTick();
               this.metricsRecorder.startTick();
               this.runTick(!var1);
               this.metricsRecorder.endTick();
               this.profiler.endTick();
               this.finishProfilers(var3, var7);
            } catch (OutOfMemoryError var4) {
               if (var1) {
                  throw var4;
               }

               this.emergencySave();
               this.setScreen(new OutOfMemoryScreen());
               System.gc();
               LOGGER.error(LogUtils.FATAL_MARKER, "Out of memory", var4);
               var1 = true;
            }
         }
      } catch (ReportedException var5) {
         this.fillReport(var5.getReport());
         this.emergencySave();
         LOGGER.error(LogUtils.FATAL_MARKER, "Reported exception thrown!", var5);
         crash(var5.getReport());
      } catch (Throwable var6) {
         CrashReport var2 = this.fillReport(new CrashReport("Unexpected error", var6));
         LOGGER.error(LogUtils.FATAL_MARKER, "Unreported exception thrown!", var6);
         this.emergencySave();
         crash(var2);
      }

   }

   void selectMainFont(boolean var1) {
      this.fontManager.setRenames(var1 ? ImmutableMap.of(DEFAULT_FONT, UNIFORM_FONT) : ImmutableMap.of());
   }

   private void createSearchTrees() {
      this.searchRegistry.register(SearchRegistry.CREATIVE_NAMES, (var0) -> {
         return new FullTextSearchTree((var0x) -> {
            return var0x.getTooltipLines((Player)null, TooltipFlag.Default.NORMAL.asCreative()).stream().map((var0) -> {
               return ChatFormatting.stripFormatting(var0.getString()).trim();
            }).filter((var0) -> {
               return !var0.isEmpty();
            });
         }, (var0x) -> {
            return Stream.of(BuiltInRegistries.ITEM.getKey(var0x.getItem()));
         }, var0);
      });
      this.searchRegistry.register(SearchRegistry.CREATIVE_TAGS, (var0) -> {
         return new IdSearchTree((var0x) -> {
            return var0x.getTags().map(TagKey::location);
         }, var0);
      });
      this.searchRegistry.register(SearchRegistry.RECIPE_COLLECTIONS, (var0) -> {
         return new FullTextSearchTree((var0x) -> {
            return var0x.getRecipes().stream().flatMap((var1) -> {
               return var1.getResultItem(var0x.registryAccess()).getTooltipLines((Player)null, TooltipFlag.Default.NORMAL).stream();
            }).map((var0) -> {
               return ChatFormatting.stripFormatting(var0.getString()).trim();
            }).filter((var0) -> {
               return !var0.isEmpty();
            });
         }, (var0x) -> {
            return var0x.getRecipes().stream().map((var1) -> {
               return BuiltInRegistries.ITEM.getKey(var1.getResultItem(var0x.registryAccess()).getItem());
            });
         }, var0);
      });
      CreativeModeTabs.searchTab().setSearchTreeBuilder((var1) -> {
         this.populateSearchTree(SearchRegistry.CREATIVE_NAMES, var1);
         this.populateSearchTree(SearchRegistry.CREATIVE_TAGS, var1);
      });
   }

   private void onFullscreenError(int var1, long var2) {
      this.options.enableVsync().set(false);
      this.options.save();
   }

   private static boolean checkIs64Bit() {
      String[] var0 = new String[]{"sun.arch.data.model", "com.ibm.vm.bitmode", "os.arch"};
      String[] var1 = var0;
      int var2 = var0.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         String var4 = var1[var3];
         String var5 = System.getProperty(var4);
         if (var5 != null && var5.contains("64")) {
            return true;
         }
      }

      return false;
   }

   public RenderTarget getMainRenderTarget() {
      return this.mainRenderTarget;
   }

   public String getLaunchedVersion() {
      return this.launchedVersion;
   }

   public String getVersionType() {
      return this.versionType;
   }

   public void delayCrash(CrashReport var1) {
      this.delayedCrash = () -> {
         return this.fillReport(var1);
      };
   }

   public void delayCrashRaw(CrashReport var1) {
      this.delayedCrash = () -> {
         return var1;
      };
   }

   public static void crash(CrashReport var0) {
      File var1 = new File(getInstance().gameDirectory, "crash-reports");
      File var2 = new File(var1, "crash-" + Util.getFilenameFormattedDateTime() + "-client.txt");
      Bootstrap.realStdoutPrintln(var0.getFriendlyReport());
      if (var0.getSaveFile() != null) {
         Bootstrap.realStdoutPrintln("#@!@# Game crashed! Crash report saved to: #@!@# " + var0.getSaveFile());
         System.exit(-1);
      } else if (var0.saveToFile(var2)) {
         Bootstrap.realStdoutPrintln("#@!@# Game crashed! Crash report saved to: #@!@# " + var2.getAbsolutePath());
         System.exit(-1);
      } else {
         Bootstrap.realStdoutPrintln("#@?@# Game crashed! Crash report could not be saved. #@?@#");
         System.exit(-2);
      }

   }

   public boolean isEnforceUnicode() {
      return (Boolean)this.options.forceUnicodeFont().get();
   }

   public CompletableFuture<Void> reloadResourcePacks() {
      return this.reloadResourcePacks(false);
   }

   private CompletableFuture<Void> reloadResourcePacks(boolean var1) {
      if (this.pendingReload != null) {
         return this.pendingReload;
      } else {
         CompletableFuture var2 = new CompletableFuture();
         if (!var1 && this.overlay instanceof LoadingOverlay) {
            this.pendingReload = var2;
            return var2;
         } else {
            this.resourcePackRepository.reload();
            List var3 = this.resourcePackRepository.openAllSelected();
            if (!var1) {
               this.reloadStateTracker.startReload(ResourceLoadStateTracker.ReloadReason.MANUAL, var3);
            }

            this.setOverlay(new LoadingOverlay(this, this.resourceManager.createReload(Util.backgroundExecutor(), this, RESOURCE_RELOAD_INITIAL_TASK, var3), (var3x) -> {
               Util.ifElse(var3x, (var2x) -> {
                  if (var1) {
                     this.abortResourcePackRecovery();
                  } else {
                     this.rollbackResourcePacks(var2x);
                  }

               }, () -> {
                  this.levelRenderer.allChanged();
                  this.reloadStateTracker.finishReload();
                  var2.complete((Object)null);
               });
            }, true));
            return var2;
         }
      }
   }

   private void selfTest() {
      boolean var1 = false;
      BlockModelShaper var2 = this.getBlockRenderer().getBlockModelShaper();
      BakedModel var3 = var2.getModelManager().getMissingModel();
      Iterator var4 = BuiltInRegistries.BLOCK.iterator();

      while(var4.hasNext()) {
         Block var5 = (Block)var4.next();
         UnmodifiableIterator var6 = var5.getStateDefinition().getPossibleStates().iterator();

         while(var6.hasNext()) {
            BlockState var7 = (BlockState)var6.next();
            if (var7.getRenderShape() == RenderShape.MODEL) {
               BakedModel var8 = var2.getBlockModel(var7);
               if (var8 == var3) {
                  LOGGER.debug("Missing model for: {}", var7);
                  var1 = true;
               }
            }
         }
      }

      TextureAtlasSprite var10 = var3.getParticleIcon();
      Iterator var11 = BuiltInRegistries.BLOCK.iterator();

      while(var11.hasNext()) {
         Block var12 = (Block)var11.next();
         UnmodifiableIterator var14 = var12.getStateDefinition().getPossibleStates().iterator();

         while(var14.hasNext()) {
            BlockState var16 = (BlockState)var14.next();
            TextureAtlasSprite var9 = var2.getParticleIcon(var16);
            if (!var16.isAir() && var9 == var10) {
               LOGGER.debug("Missing particle icon for: {}", var16);
            }
         }
      }

      var11 = BuiltInRegistries.ITEM.iterator();

      while(var11.hasNext()) {
         Item var13 = (Item)var11.next();
         ItemStack var15 = var13.getDefaultInstance();
         String var17 = var15.getDescriptionId();
         String var18 = Component.translatable(var17).getString();
         if (var18.toLowerCase(Locale.ROOT).equals(var13.getDescriptionId())) {
            LOGGER.debug("Missing translation for: {} {} {}", new Object[]{var15, var17, var13});
         }
      }

      var1 |= MenuScreens.selfTest();
      var1 |= EntityRenderers.validateRegistrations();
      if (var1) {
         throw new IllegalStateException("Your game data is foobar, fix the errors above!");
      }
   }

   public LevelStorageSource getLevelSource() {
      return this.levelSource;
   }

   private void openChatScreen(String var1) {
      Minecraft.ChatStatus var2 = this.getChatStatus();
      if (!var2.isChatAllowed(this.isLocalServer())) {
         if (this.gui.isShowingChatDisabledByPlayer()) {
            this.gui.setChatDisabledByPlayerShown(false);
            this.setScreen(new ConfirmLinkScreen((var1x) -> {
               if (var1x) {
                  Util.getPlatform().openUri("https://aka.ms/JavaAccountSettings");
               }

               this.setScreen((Screen)null);
            }, Minecraft.ChatStatus.INFO_DISABLED_BY_PROFILE, "https://aka.ms/JavaAccountSettings", true));
         } else {
            Component var3 = var2.getMessage();
            this.gui.setOverlayMessage(var3, false);
            this.narrator.sayNow(var3);
            this.gui.setChatDisabledByPlayerShown(var2 == Minecraft.ChatStatus.DISABLED_BY_PROFILE);
         }
      } else {
         this.setScreen(new ChatScreen(var1));
      }

   }

   public void setScreen(@Nullable Screen var1) {
      if (SharedConstants.IS_RUNNING_IN_IDE && Thread.currentThread() != this.gameThread) {
         LOGGER.error("setScreen called from non-game thread");
      }

      if (this.screen != null) {
         this.screen.removed();
      }

      if (var1 == null && this.level == null) {
         var1 = new TitleScreen();
      } else if (var1 == null && this.player.isDeadOrDying()) {
         if (this.player.shouldShowDeathScreen()) {
            var1 = new DeathScreen((Component)null, this.level.getLevelData().isHardcore());
         } else {
            this.player.respawn();
         }
      }

      this.screen = (Screen)var1;
      if (this.screen != null) {
         this.screen.added();
      }

      BufferUploader.reset();
      if (var1 != null) {
         this.mouseHandler.releaseMouse();
         KeyMapping.releaseAll();
         ((Screen)var1).init(this, this.window.getGuiScaledWidth(), this.window.getGuiScaledHeight());
         this.noRender = false;
      } else {
         this.soundManager.resume();
         this.mouseHandler.grabMouse();
      }

      this.updateTitle();
   }

   public void setOverlay(@Nullable Overlay var1) {
      this.overlay = var1;
   }

   public void destroy() {
      try {
         LOGGER.info("Stopping!");

         try {
            this.narrator.destroy();
         } catch (Throwable var7) {
         }

         try {
            if (this.level != null) {
               this.level.disconnect();
            }

            this.clearLevel();
         } catch (Throwable var6) {
         }

         if (this.screen != null) {
            this.screen.removed();
         }

         this.close();
      } finally {
         Util.timeSource = System::nanoTime;
         if (this.delayedCrash == null) {
            System.exit(0);
         }

      }

   }

   public void close() {
      if (this.currentFrameProfile != null) {
         this.currentFrameProfile.cancel();
      }

      try {
         this.telemetryManager.close();
         this.regionalCompliancies.close();
         this.modelManager.close();
         this.fontManager.close();
         this.gameRenderer.close();
         this.levelRenderer.close();
         this.soundManager.destroy();
         this.particleEngine.close();
         this.mobEffectTextures.close();
         this.paintingTextures.close();
         this.textureManager.close();
         this.resourceManager.close();
         Util.shutdownExecutors();
      } catch (Throwable var5) {
         LOGGER.error("Shutdown failure!", var5);
         throw var5;
      } finally {
         this.virtualScreen.close();
         this.window.close();
      }

   }

   private void runTick(boolean var1) {
      this.window.setErrorSection("Pre render");
      long var2 = Util.getNanos();
      if (this.window.shouldClose()) {
         this.stop();
      }

      if (this.pendingReload != null && !(this.overlay instanceof LoadingOverlay)) {
         CompletableFuture var4 = this.pendingReload;
         this.pendingReload = null;
         this.reloadResourcePacks().thenRun(() -> {
            var4.complete((Object)null);
         });
      }

      Runnable var15;
      while((var15 = (Runnable)this.progressTasks.poll()) != null) {
         var15.run();
      }

      if (var1) {
         int var5 = this.timer.advanceTime(Util.getMillis());
         this.profiler.push("scheduledExecutables");
         this.runAllTasks();
         this.profiler.pop();
         this.profiler.push("tick");

         for(int var6 = 0; var6 < Math.min(10, var5); ++var6) {
            this.profiler.incrementCounter("clientTick");
            this.tick();
         }

         this.profiler.pop();
      }

      this.mouseHandler.turnPlayer();
      this.window.setErrorSection("Render");
      this.profiler.push("sound");
      this.soundManager.updateSource(this.gameRenderer.getMainCamera());
      this.profiler.pop();
      this.profiler.push("render");
      long var16 = Util.getNanos();
      boolean var7;
      if (!this.options.renderDebug && !this.metricsRecorder.isRecording()) {
         var7 = false;
         this.gpuUtilization = 0.0D;
      } else {
         var7 = this.currentFrameProfile == null || this.currentFrameProfile.isDone();
         if (var7) {
            TimerQuery.getInstance().ifPresent(TimerQuery::beginProfile);
         }
      }

      RenderSystem.clear(16640, ON_OSX);
      this.mainRenderTarget.bindWrite(true);
      FogRenderer.setupNoFog();
      this.profiler.push("display");
      RenderSystem.enableCull();
      this.profiler.pop();
      if (!this.noRender) {
         this.profiler.popPush("gameRenderer");
         this.gameRenderer.render(this.pause ? this.pausePartialTick : this.timer.partialTick, var2, var1);
         this.profiler.pop();
      }

      if (this.fpsPieResults != null) {
         this.profiler.push("fpsPie");
         GuiGraphics var8 = new GuiGraphics(this, this.renderBuffers.bufferSource());
         this.renderFpsMeter(var8, this.fpsPieResults);
         var8.flush();
         this.profiler.pop();
      }

      this.profiler.push("blit");
      this.mainRenderTarget.unbindWrite();
      this.mainRenderTarget.blitToScreen(this.window.getWidth(), this.window.getHeight());
      this.frameTimeNs = Util.getNanos() - var16;
      if (var7) {
         TimerQuery.getInstance().ifPresent((var1x) -> {
            this.currentFrameProfile = var1x.endProfile();
         });
      }

      this.profiler.popPush("updateDisplay");
      this.window.updateDisplay();
      int var17 = this.getFramerateLimit();
      if (var17 < 260) {
         RenderSystem.limitDisplayFPS(var17);
      }

      this.profiler.popPush("yield");
      Thread.yield();
      this.profiler.pop();
      this.window.setErrorSection("Post render");
      ++this.frames;
      boolean var9 = this.hasSingleplayerServer() && (this.screen != null && this.screen.isPauseScreen() || this.overlay != null && this.overlay.isPauseScreen()) && !this.singleplayerServer.isPublished();
      if (this.pause != var9) {
         if (this.pause) {
            this.pausePartialTick = this.timer.partialTick;
         } else {
            this.timer.partialTick = this.pausePartialTick;
         }

         this.pause = var9;
      }

      long var10 = Util.getNanos();
      long var12 = var10 - this.lastNanoTime;
      if (var7) {
         this.savedCpuDuration = var12;
      }

      this.frameTimer.logFrameDuration(var12);
      this.lastNanoTime = var10;
      this.profiler.push("fpsUpdate");
      if (this.currentFrameProfile != null && this.currentFrameProfile.isDone()) {
         this.gpuUtilization = (double)this.currentFrameProfile.get() * 100.0D / (double)this.savedCpuDuration;
      }

      while(Util.getMillis() >= this.lastTime + 1000L) {
         String var14;
         if (this.gpuUtilization > 0.0D) {
            String var10000 = this.gpuUtilization > 100.0D ? ChatFormatting.RED + "100%" : Math.round(this.gpuUtilization) + "%";
            var14 = " GPU: " + var10000;
         } else {
            var14 = "";
         }

         fps = this.frames;
         this.fpsString = String.format(Locale.ROOT, "%d fps T: %s%s%s%s B: %d%s", fps, var17 == 260 ? "inf" : var17, (Boolean)this.options.enableVsync().get() ? " vsync" : "", this.options.graphicsMode().get(), this.options.cloudStatus().get() == CloudStatus.OFF ? "" : (this.options.cloudStatus().get() == CloudStatus.FAST ? " fast-clouds" : " fancy-clouds"), this.options.biomeBlendRadius().get(), var14);
         this.lastTime += 1000L;
         this.frames = 0;
      }

      this.profiler.pop();
   }

   private boolean shouldRenderFpsPie() {
      return this.options.renderDebug && this.options.renderDebugCharts && !this.options.hideGui;
   }

   private ProfilerFiller constructProfiler(boolean var1, @Nullable SingleTickProfiler var2) {
      if (!var1) {
         this.fpsPieProfiler.disable();
         if (!this.metricsRecorder.isRecording() && var2 == null) {
            return InactiveProfiler.INSTANCE;
         }
      }

      Object var3;
      if (var1) {
         if (!this.fpsPieProfiler.isEnabled()) {
            this.fpsPieRenderTicks = 0;
            this.fpsPieProfiler.enable();
         }

         ++this.fpsPieRenderTicks;
         var3 = this.fpsPieProfiler.getFiller();
      } else {
         var3 = InactiveProfiler.INSTANCE;
      }

      if (this.metricsRecorder.isRecording()) {
         var3 = ProfilerFiller.tee((ProfilerFiller)var3, this.metricsRecorder.getProfiler());
      }

      return SingleTickProfiler.decorateFiller((ProfilerFiller)var3, var2);
   }

   private void finishProfilers(boolean var1, @Nullable SingleTickProfiler var2) {
      if (var2 != null) {
         var2.endTick();
      }

      if (var1) {
         this.fpsPieResults = this.fpsPieProfiler.getResults();
      } else {
         this.fpsPieResults = null;
      }

      this.profiler = this.fpsPieProfiler.getFiller();
   }

   public void resizeDisplay() {
      int var1 = this.window.calculateScale((Integer)this.options.guiScale().get(), this.isEnforceUnicode());
      this.window.setGuiScale((double)var1);
      if (this.screen != null) {
         this.screen.resize(this, this.window.getGuiScaledWidth(), this.window.getGuiScaledHeight());
      }

      RenderTarget var2 = this.getMainRenderTarget();
      var2.resize(this.window.getWidth(), this.window.getHeight(), ON_OSX);
      this.gameRenderer.resize(this.window.getWidth(), this.window.getHeight());
      this.mouseHandler.setIgnoreFirstMove();
   }

   public void cursorEntered() {
      this.mouseHandler.cursorEntered();
   }

   public int getFps() {
      return fps;
   }

   public long getFrameTimeNs() {
      return this.frameTimeNs;
   }

   private int getFramerateLimit() {
      return this.level != null || this.screen == null && this.overlay == null ? this.window.getFramerateLimit() : 60;
   }

   public void emergencySave() {
      try {
         MemoryReserve.release();
         this.levelRenderer.clear();
      } catch (Throwable var3) {
      }

      try {
         System.gc();
         if (this.isLocalServer && this.singleplayerServer != null) {
            this.singleplayerServer.halt(true);
         }

         this.clearLevel(new GenericDirtMessageScreen(Component.translatable("menu.savingLevel")));
      } catch (Throwable var2) {
      }

      System.gc();
   }

   public boolean debugClientMetricsStart(Consumer<Component> var1) {
      if (this.metricsRecorder.isRecording()) {
         this.debugClientMetricsStop();
         return false;
      } else {
         Consumer var2 = (var2x) -> {
            if (var2x != EmptyProfileResults.EMPTY) {
               int var3 = var2x.getTickDuration();
               double var4 = (double)var2x.getNanoDuration() / (double)TimeUtil.NANOSECONDS_PER_SECOND;
               this.execute(() -> {
                  var1.accept(Component.translatable("commands.debug.stopped", String.format(Locale.ROOT, "%.2f", var4), var3, String.format(Locale.ROOT, "%.2f", (double)var3 / var4)));
               });
            }
         };
         Consumer var3 = (var2x) -> {
            MutableComponent var3 = Component.literal(var2x.toString()).withStyle(ChatFormatting.UNDERLINE).withStyle((var1x) -> {
               return var1x.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, var2x.toFile().getParent()));
            });
            this.execute(() -> {
               var1.accept(Component.translatable("debug.profiling.stop", var3));
            });
         };
         SystemReport var4 = fillSystemReport(new SystemReport(), this, this.languageManager, this.launchedVersion, this.options);
         Consumer var5 = (var3x) -> {
            Path var4x = this.archiveProfilingReport(var4, var3x);
            var3.accept(var4x);
         };
         Consumer var6;
         if (this.singleplayerServer == null) {
            var6 = (var1x) -> {
               var5.accept(ImmutableList.of(var1x));
            };
         } else {
            this.singleplayerServer.fillSystemReport(var4);
            CompletableFuture var7 = new CompletableFuture();
            CompletableFuture var8 = new CompletableFuture();
            CompletableFuture.allOf(var7, var8).thenRunAsync(() -> {
               var5.accept(ImmutableList.of((Path)var7.join(), (Path)var8.join()));
            }, Util.ioPool());
            IntegratedServer var10000 = this.singleplayerServer;
            Consumer var10001 = (var0) -> {
            };
            Objects.requireNonNull(var8);
            var10000.startRecordingMetrics(var10001, var8::complete);
            Objects.requireNonNull(var7);
            var6 = var7::complete;
         }

         this.metricsRecorder = ActiveMetricsRecorder.createStarted(new ClientMetricsSamplersProvider(Util.timeSource, this.levelRenderer), Util.timeSource, Util.ioPool(), new MetricsPersister("client"), (var2x) -> {
            this.metricsRecorder = InactiveMetricsRecorder.INSTANCE;
            var2.accept(var2x);
         }, var6);
         return true;
      }
   }

   private void debugClientMetricsStop() {
      this.metricsRecorder.end();
      if (this.singleplayerServer != null) {
         this.singleplayerServer.finishRecordingMetrics();
      }

   }

   private void debugClientMetricsCancel() {
      this.metricsRecorder.cancel();
      if (this.singleplayerServer != null) {
         this.singleplayerServer.cancelRecordingMetrics();
      }

   }

   private Path archiveProfilingReport(SystemReport var1, List<Path> var2) {
      String var4;
      if (this.isLocalServer()) {
         var4 = this.getSingleplayerServer().getWorldData().getLevelName();
      } else {
         ServerData var5 = this.getCurrentServer();
         var4 = var5 != null ? var5.name : "unknown";
      }

      Path var3;
      try {
         String var25 = String.format(Locale.ROOT, "%s-%s-%s", Util.getFilenameFormattedDateTime(), var4, SharedConstants.getCurrentVersion().getId());
         String var6 = FileUtil.findAvailableName(MetricsPersister.PROFILING_RESULTS_DIR, var25, ".zip");
         var3 = MetricsPersister.PROFILING_RESULTS_DIR.resolve(var6);
      } catch (IOException var23) {
         throw new UncheckedIOException(var23);
      }

      boolean var18 = false;

      try {
         var18 = true;
         FileZipper var26 = new FileZipper(var3);

         try {
            var26.add(Paths.get("system.txt"), var1.toLineSeparatedString());
            var26.add(Paths.get("client").resolve(this.options.getFile().getName()), this.options.dumpOptionsForReport());
            Objects.requireNonNull(var26);
            var2.forEach(var26::add);
         } catch (Throwable var22) {
            try {
               var26.close();
            } catch (Throwable var19) {
               var22.addSuppressed(var19);
            }

            throw var22;
         }

         var26.close();
         var18 = false;
      } finally {
         if (var18) {
            Iterator var9 = var2.iterator();

            while(var9.hasNext()) {
               Path var10 = (Path)var9.next();

               try {
                  FileUtils.forceDelete(var10.toFile());
               } catch (IOException var20) {
                  LOGGER.warn("Failed to delete temporary profiling result {}", var10, var20);
               }
            }

         }
      }

      Iterator var27 = var2.iterator();

      while(var27.hasNext()) {
         Path var28 = (Path)var27.next();

         try {
            FileUtils.forceDelete(var28.toFile());
         } catch (IOException var21) {
            LOGGER.warn("Failed to delete temporary profiling result {}", var28, var21);
         }
      }

      return var3;
   }

   public void debugFpsMeterKeyPress(int var1) {
      if (this.fpsPieResults != null) {
         List var2 = this.fpsPieResults.getTimes(this.debugPath);
         if (!var2.isEmpty()) {
            ResultField var3 = (ResultField)var2.remove(0);
            if (var1 == 0) {
               if (!var3.name.isEmpty()) {
                  int var4 = this.debugPath.lastIndexOf(30);
                  if (var4 >= 0) {
                     this.debugPath = this.debugPath.substring(0, var4);
                  }
               }
            } else {
               --var1;
               if (var1 < var2.size() && !"unspecified".equals(((ResultField)var2.get(var1)).name)) {
                  if (!this.debugPath.isEmpty()) {
                     this.debugPath = this.debugPath + "\u001e";
                  }

                  String var10001 = this.debugPath;
                  this.debugPath = var10001 + ((ResultField)var2.get(var1)).name;
               }
            }

         }
      }
   }

   private void renderFpsMeter(GuiGraphics var1, ProfileResults var2) {
      List var3 = var2.getTimes(this.debugPath);
      ResultField var4 = (ResultField)var3.remove(0);
      RenderSystem.clear(256, ON_OSX);
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      Matrix4f var5 = (new Matrix4f()).setOrtho(0.0F, (float)this.window.getWidth(), (float)this.window.getHeight(), 0.0F, 1000.0F, 3000.0F);
      RenderSystem.setProjectionMatrix(var5, VertexSorting.ORTHOGRAPHIC_Z);
      PoseStack var6 = RenderSystem.getModelViewStack();
      var6.pushPose();
      var6.setIdentity();
      var6.translate(0.0F, 0.0F, -2000.0F);
      RenderSystem.applyModelViewMatrix();
      RenderSystem.lineWidth(1.0F);
      Tesselator var7 = Tesselator.getInstance();
      BufferBuilder var8 = var7.getBuilder();
      boolean var9 = true;
      int var10 = this.window.getWidth() - 160 - 10;
      int var11 = this.window.getHeight() - 320;
      RenderSystem.enableBlend();
      var8.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      var8.vertex((double)((float)var10 - 176.0F), (double)((float)var11 - 96.0F - 16.0F), 0.0D).color(200, 0, 0, 0).endVertex();
      var8.vertex((double)((float)var10 - 176.0F), (double)(var11 + 320), 0.0D).color(200, 0, 0, 0).endVertex();
      var8.vertex((double)((float)var10 + 176.0F), (double)(var11 + 320), 0.0D).color(200, 0, 0, 0).endVertex();
      var8.vertex((double)((float)var10 + 176.0F), (double)((float)var11 - 96.0F - 16.0F), 0.0D).color(200, 0, 0, 0).endVertex();
      var7.end();
      RenderSystem.disableBlend();
      double var12 = 0.0D;

      ResultField var15;
      int var17;
      for(Iterator var14 = var3.iterator(); var14.hasNext(); var12 += var15.percentage) {
         var15 = (ResultField)var14.next();
         int var16 = Mth.floor(var15.percentage / 4.0D) + 1;
         var8.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
         var17 = var15.getColor();
         int var18 = var17 >> 16 & 255;
         int var19 = var17 >> 8 & 255;
         int var20 = var17 & 255;
         var8.vertex((double)var10, (double)var11, 0.0D).color(var18, var19, var20, 255).endVertex();

         int var21;
         float var22;
         float var23;
         float var24;
         for(var21 = var16; var21 >= 0; --var21) {
            var22 = (float)((var12 + var15.percentage * (double)var21 / (double)var16) * 6.2831854820251465D / 100.0D);
            var23 = Mth.sin(var22) * 160.0F;
            var24 = Mth.cos(var22) * 160.0F * 0.5F;
            var8.vertex((double)((float)var10 + var23), (double)((float)var11 - var24), 0.0D).color(var18, var19, var20, 255).endVertex();
         }

         var7.end();
         var8.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

         for(var21 = var16; var21 >= 0; --var21) {
            var22 = (float)((var12 + var15.percentage * (double)var21 / (double)var16) * 6.2831854820251465D / 100.0D);
            var23 = Mth.sin(var22) * 160.0F;
            var24 = Mth.cos(var22) * 160.0F * 0.5F;
            if (!(var24 > 0.0F)) {
               var8.vertex((double)((float)var10 + var23), (double)((float)var11 - var24), 0.0D).color(var18 >> 1, var19 >> 1, var20 >> 1, 255).endVertex();
               var8.vertex((double)((float)var10 + var23), (double)((float)var11 - var24 + 10.0F), 0.0D).color(var18 >> 1, var19 >> 1, var20 >> 1, 255).endVertex();
            }
         }

         var7.end();
      }

      DecimalFormat var25 = new DecimalFormat("##0.00");
      var25.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ROOT));
      String var26 = ProfileResults.demanglePath(var4.name);
      String var28 = "";
      if (!"unspecified".equals(var26)) {
         var28 = var28 + "[0] ";
      }

      if (var26.isEmpty()) {
         var28 = var28 + "ROOT ";
      } else {
         var28 = var28 + var26 + " ";
      }

      var17 = 16777215;
      var1.drawString(this.font, var28, var10 - 160, var11 - 80 - 16, 16777215);
      String var10000 = var25.format(var4.globalPercentage);
      var28 = var10000 + "%";
      var1.drawString(this.font, var28, var10 + 160 - this.font.width(var28), var11 - 80 - 16, 16777215);

      for(int var27 = 0; var27 < var3.size(); ++var27) {
         ResultField var31 = (ResultField)var3.get(var27);
         StringBuilder var29 = new StringBuilder();
         if ("unspecified".equals(var31.name)) {
            var29.append("[?] ");
         } else {
            var29.append("[").append(var27 + 1).append("] ");
         }

         String var30 = var29.append(var31.name).toString();
         var1.drawString(this.font, var30, var10 - 160, var11 + 80 + var27 * 8 + 20, var31.getColor());
         var10000 = var25.format(var31.percentage);
         var30 = var10000 + "%";
         var1.drawString(this.font, var30, var10 + 160 - 50 - this.font.width(var30), var11 + 80 + var27 * 8 + 20, var31.getColor());
         var10000 = var25.format(var31.globalPercentage);
         var30 = var10000 + "%";
         var1.drawString(this.font, var30, var10 + 160 - this.font.width(var30), var11 + 80 + var27 * 8 + 20, var31.getColor());
      }

      var6.popPose();
      RenderSystem.applyModelViewMatrix();
   }

   public void stop() {
      this.running = false;
   }

   public boolean isRunning() {
      return this.running;
   }

   public void pauseGame(boolean var1) {
      if (this.screen == null) {
         boolean var2 = this.hasSingleplayerServer() && !this.singleplayerServer.isPublished();
         if (var2) {
            this.setScreen(new PauseScreen(!var1));
            this.soundManager.pause();
         } else {
            this.setScreen(new PauseScreen(true));
         }

      }
   }

   private void continueAttack(boolean var1) {
      if (!var1) {
         this.missTime = 0;
      }

      if (this.missTime <= 0 && !this.player.isUsingItem()) {
         if (var1 && this.hitResult != null && this.hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult var2 = (BlockHitResult)this.hitResult;
            BlockPos var3 = var2.getBlockPos();
            if (!this.level.getBlockState(var3).isAir()) {
               Direction var4 = var2.getDirection();
               if (this.gameMode.continueDestroyBlock(var3, var4)) {
                  this.particleEngine.crack(var3, var4);
                  this.player.swing(InteractionHand.MAIN_HAND);
               }
            }

         } else {
            this.gameMode.stopDestroyBlock();
         }
      }
   }

   private boolean startAttack() {
      if (this.missTime > 0) {
         return false;
      } else if (this.hitResult == null) {
         LOGGER.error("Null returned as 'hitResult', this shouldn't happen!");
         if (this.gameMode.hasMissTime()) {
            this.missTime = 10;
         }

         return false;
      } else if (this.player.isHandsBusy()) {
         return false;
      } else {
         ItemStack var1 = this.player.getItemInHand(InteractionHand.MAIN_HAND);
         if (!var1.isItemEnabled(this.level.enabledFeatures())) {
            return false;
         } else {
            boolean var2 = false;
            switch(this.hitResult.getType()) {
            case ENTITY:
               this.gameMode.attack(this.player, ((EntityHitResult)this.hitResult).getEntity());
               break;
            case BLOCK:
               BlockHitResult var3 = (BlockHitResult)this.hitResult;
               BlockPos var4 = var3.getBlockPos();
               if (!this.level.getBlockState(var4).isAir()) {
                  this.gameMode.startDestroyBlock(var4, var3.getDirection());
                  if (this.level.getBlockState(var4).isAir()) {
                     var2 = true;
                  }
                  break;
               }
            case MISS:
               if (this.gameMode.hasMissTime()) {
                  this.missTime = 10;
               }

               this.player.resetAttackStrengthTicker();
            }

            this.player.swing(InteractionHand.MAIN_HAND);
            return var2;
         }
      }
   }

   private void startUseItem() {
      if (!this.gameMode.isDestroying()) {
         this.rightClickDelay = 4;
         if (!this.player.isHandsBusy()) {
            if (this.hitResult == null) {
               LOGGER.warn("Null returned as 'hitResult', this shouldn't happen!");
            }

            InteractionHand[] var1 = InteractionHand.values();
            int var2 = var1.length;

            for(int var3 = 0; var3 < var2; ++var3) {
               InteractionHand var4 = var1[var3];
               ItemStack var5 = this.player.getItemInHand(var4);
               if (!var5.isItemEnabled(this.level.enabledFeatures())) {
                  return;
               }

               if (this.hitResult != null) {
                  switch(this.hitResult.getType()) {
                  case ENTITY:
                     EntityHitResult var6 = (EntityHitResult)this.hitResult;
                     Entity var7 = var6.getEntity();
                     if (!this.level.getWorldBorder().isWithinBounds(var7.blockPosition())) {
                        return;
                     }

                     InteractionResult var8 = this.gameMode.interactAt(this.player, var7, var6, var4);
                     if (!var8.consumesAction()) {
                        var8 = this.gameMode.interact(this.player, var7, var4);
                     }

                     if (var8.consumesAction()) {
                        if (var8.shouldSwing()) {
                           this.player.swing(var4);
                        }

                        return;
                     }
                     break;
                  case BLOCK:
                     BlockHitResult var9 = (BlockHitResult)this.hitResult;
                     int var10 = var5.getCount();
                     InteractionResult var11 = this.gameMode.useItemOn(this.player, var4, var9);
                     if (var11.consumesAction()) {
                        if (var11.shouldSwing()) {
                           this.player.swing(var4);
                           if (!var5.isEmpty() && (var5.getCount() != var10 || this.gameMode.hasInfiniteItems())) {
                              this.gameRenderer.itemInHandRenderer.itemUsed(var4);
                           }
                        }

                        return;
                     }

                     if (var11 == InteractionResult.FAIL) {
                        return;
                     }
                  }
               }

               if (!var5.isEmpty()) {
                  InteractionResult var12 = this.gameMode.useItem(this.player, var4);
                  if (var12.consumesAction()) {
                     if (var12.shouldSwing()) {
                        this.player.swing(var4);
                     }

                     this.gameRenderer.itemInHandRenderer.itemUsed(var4);
                     return;
                  }
               }
            }

         }
      }
   }

   public MusicManager getMusicManager() {
      return this.musicManager;
   }

   public void tick() {
      if (this.rightClickDelay > 0) {
         --this.rightClickDelay;
      }

      this.profiler.push("gui");
      this.chatListener.tick();
      this.gui.tick(this.pause);
      this.profiler.pop();
      this.gameRenderer.pick(1.0F);
      this.tutorial.onLookAt(this.level, this.hitResult);
      this.profiler.push("gameMode");
      if (!this.pause && this.level != null) {
         this.gameMode.tick();
      }

      this.profiler.popPush("textures");
      if (this.level != null) {
         this.textureManager.tick();
      }

      if (this.screen == null && this.player != null) {
         if (this.player.isDeadOrDying() && !(this.screen instanceof DeathScreen)) {
            this.setScreen((Screen)null);
         } else if (this.player.isSleeping() && this.level != null) {
            this.setScreen(new InBedChatScreen());
         }
      } else {
         Screen var2 = this.screen;
         if (var2 instanceof InBedChatScreen) {
            InBedChatScreen var1 = (InBedChatScreen)var2;
            if (!this.player.isSleeping()) {
               var1.onPlayerWokeUp();
            }
         }
      }

      if (this.screen != null) {
         this.missTime = 10000;
      }

      if (this.screen != null) {
         Screen.wrapScreenError(() -> {
            this.screen.tick();
         }, "Ticking screen", this.screen.getClass().getCanonicalName());
      }

      if (!this.options.renderDebug) {
         this.gui.clearCache();
      }

      if (this.overlay == null && this.screen == null) {
         this.profiler.popPush("Keybindings");
         this.handleKeybinds();
         if (this.missTime > 0) {
            --this.missTime;
         }
      }

      if (this.level != null) {
         this.profiler.popPush("gameRenderer");
         if (!this.pause) {
            this.gameRenderer.tick();
         }

         this.profiler.popPush("levelRenderer");
         if (!this.pause) {
            this.levelRenderer.tick();
         }

         this.profiler.popPush("level");
         if (!this.pause) {
            this.level.tickEntities();
         }
      } else if (this.gameRenderer.currentEffect() != null) {
         this.gameRenderer.shutdownEffect();
      }

      if (!this.pause) {
         this.musicManager.tick();
      }

      this.soundManager.tick(this.pause);
      if (this.level != null) {
         if (!this.pause) {
            if (!this.options.joinedFirstServer && this.isMultiplayerServer()) {
               MutableComponent var5 = Component.translatable("tutorial.socialInteractions.title");
               MutableComponent var6 = Component.translatable("tutorial.socialInteractions.description", Tutorial.key("socialInteractions"));
               this.socialInteractionsToast = new TutorialToast(TutorialToast.Icons.SOCIAL_INTERACTIONS, var5, var6, true);
               this.tutorial.addTimedToast(this.socialInteractionsToast, 160);
               this.options.joinedFirstServer = true;
               this.options.save();
            }

            this.tutorial.tick();

            try {
               this.level.tick(() -> {
                  return true;
               });
            } catch (Throwable var4) {
               CrashReport var7 = CrashReport.forThrowable(var4, "Exception in world tick");
               if (this.level == null) {
                  CrashReportCategory var3 = var7.addCategory("Affected level");
                  var3.setDetail("Problem", (Object)"Level is null!");
               } else {
                  this.level.fillReportDetails(var7);
               }

               throw new ReportedException(var7);
            }
         }

         this.profiler.popPush("animateTick");
         if (!this.pause && this.level != null) {
            this.level.animateTick(this.player.getBlockX(), this.player.getBlockY(), this.player.getBlockZ());
         }

         this.profiler.popPush("particles");
         if (!this.pause) {
            this.particleEngine.tick();
         }
      } else if (this.pendingConnection != null) {
         this.profiler.popPush("pendingConnection");
         this.pendingConnection.tick();
      }

      this.profiler.popPush("keyboard");
      this.keyboardHandler.tick();
      this.profiler.pop();
   }

   private boolean isMultiplayerServer() {
      return !this.isLocalServer || this.singleplayerServer != null && this.singleplayerServer.isPublished();
   }

   private void handleKeybinds() {
      for(; this.options.keyTogglePerspective.consumeClick(); this.levelRenderer.needsUpdate()) {
         CameraType var1 = this.options.getCameraType();
         this.options.setCameraType(this.options.getCameraType().cycle());
         if (var1.isFirstPerson() != this.options.getCameraType().isFirstPerson()) {
            this.gameRenderer.checkEntityPostEffect(this.options.getCameraType().isFirstPerson() ? this.getCameraEntity() : null);
         }
      }

      while(this.options.keySmoothCamera.consumeClick()) {
         this.options.smoothCamera = !this.options.smoothCamera;
      }

      for(int var4 = 0; var4 < 9; ++var4) {
         boolean var2 = this.options.keySaveHotbarActivator.isDown();
         boolean var3 = this.options.keyLoadHotbarActivator.isDown();
         if (this.options.keyHotbarSlots[var4].consumeClick()) {
            if (this.player.isSpectator()) {
               this.gui.getSpectatorGui().onHotbarSelected(var4);
            } else if (!this.player.isCreative() || this.screen != null || !var3 && !var2) {
               this.player.getInventory().selected = var4;
            } else {
               CreativeModeInventoryScreen.handleHotbarLoadOrSave(this, var4, var3, var2);
            }
         }
      }

      while(this.options.keySocialInteractions.consumeClick()) {
         if (!this.isMultiplayerServer()) {
            this.player.displayClientMessage(SOCIAL_INTERACTIONS_NOT_AVAILABLE, true);
            this.narrator.sayNow(SOCIAL_INTERACTIONS_NOT_AVAILABLE);
         } else {
            if (this.socialInteractionsToast != null) {
               this.tutorial.removeTimedToast(this.socialInteractionsToast);
               this.socialInteractionsToast = null;
            }

            this.setScreen(new SocialInteractionsScreen());
         }
      }

      while(this.options.keyInventory.consumeClick()) {
         if (this.gameMode.isServerControlledInventory()) {
            this.player.sendOpenInventory();
         } else {
            this.tutorial.onOpenInventory();
            this.setScreen(new InventoryScreen(this.player));
         }
      }

      while(this.options.keyAdvancements.consumeClick()) {
         this.setScreen(new AdvancementsScreen(this.player.connection.getAdvancements()));
      }

      while(this.options.keySwapOffhand.consumeClick()) {
         if (!this.player.isSpectator()) {
            this.getConnection().send((Packet)(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN)));
         }
      }

      while(this.options.keyDrop.consumeClick()) {
         if (!this.player.isSpectator() && this.player.drop(Screen.hasControlDown())) {
            this.player.swing(InteractionHand.MAIN_HAND);
         }
      }

      while(this.options.keyChat.consumeClick()) {
         this.openChatScreen("");
      }

      if (this.screen == null && this.overlay == null && this.options.keyCommand.consumeClick()) {
         this.openChatScreen("/");
      }

      boolean var5 = false;
      if (this.player.isUsingItem()) {
         if (!this.options.keyUse.isDown()) {
            this.gameMode.releaseUsingItem(this.player);
         }

         label117:
         while(true) {
            if (!this.options.keyAttack.consumeClick()) {
               while(this.options.keyUse.consumeClick()) {
               }

               while(true) {
                  if (this.options.keyPickItem.consumeClick()) {
                     continue;
                  }
                  break label117;
               }
            }
         }
      } else {
         while(this.options.keyAttack.consumeClick()) {
            var5 |= this.startAttack();
         }

         while(this.options.keyUse.consumeClick()) {
            this.startUseItem();
         }

         while(this.options.keyPickItem.consumeClick()) {
            this.pickBlock();
         }
      }

      if (this.options.keyUse.isDown() && this.rightClickDelay == 0 && !this.player.isUsingItem()) {
         this.startUseItem();
      }

      this.continueAttack(this.screen == null && !var5 && this.options.keyAttack.isDown() && this.mouseHandler.isMouseGrabbed());
   }

   public ClientTelemetryManager getTelemetryManager() {
      return this.telemetryManager;
   }

   public double getGpuUtilization() {
      return this.gpuUtilization;
   }

   public ProfileKeyPairManager getProfileKeyPairManager() {
      return this.profileKeyPairManager;
   }

   public WorldOpenFlows createWorldOpenFlows() {
      return new WorldOpenFlows(this, this.levelSource);
   }

   public void doWorldLoad(String var1, LevelStorageSource.LevelStorageAccess var2, PackRepository var3, WorldStem var4, boolean var5) {
      this.clearLevel();
      this.progressListener.set((Object)null);
      Instant var6 = Instant.now();

      try {
         var2.saveDataTag(var4.registries().compositeAccess(), var4.worldData());
         Services var7 = Services.create(this.authenticationService, this.gameDirectory);
         var7.profileCache().setExecutor(this);
         SkullBlockEntity.setup(var7, this);
         GameProfileCache.setUsesAuthentication(false);
         this.singleplayerServer = (IntegratedServer)MinecraftServer.spin((var5x) -> {
            return new IntegratedServer(var5x, this, var2, var3, var4, var7, (var1) -> {
               StoringChunkProgressListener var2 = new StoringChunkProgressListener(var1 + 0);
               this.progressListener.set(var2);
               Queue var10001 = this.progressTasks;
               Objects.requireNonNull(var10001);
               return ProcessorChunkProgressListener.createStarted(var2, var10001::add);
            });
         });
         this.isLocalServer = true;
         this.updateReportEnvironment(ReportEnvironment.local());
         this.quickPlayLog.setWorldData(QuickPlayLog.Type.SINGLEPLAYER, var1, var4.worldData().getLevelName());
      } catch (Throwable var12) {
         CrashReport var8 = CrashReport.forThrowable(var12, "Starting integrated server");
         CrashReportCategory var9 = var8.addCategory("Starting integrated server");
         var9.setDetail("Level ID", (Object)var1);
         var9.setDetail("Level Name", () -> {
            return var4.worldData().getLevelName();
         });
         throw new ReportedException(var8);
      }

      while(this.progressListener.get() == null) {
         Thread.yield();
      }

      LevelLoadingScreen var13 = new LevelLoadingScreen((StoringChunkProgressListener)this.progressListener.get());
      this.setScreen(var13);
      this.profiler.push("waitForServer");

      while(!this.singleplayerServer.isReady()) {
         var13.tick();
         this.runTick(false);

         try {
            Thread.sleep(16L);
         } catch (InterruptedException var11) {
         }

         if (this.delayedCrash != null) {
            crash((CrashReport)this.delayedCrash.get());
            return;
         }
      }

      this.profiler.pop();
      Duration var14 = Duration.between(var6, Instant.now());
      SocketAddress var15 = this.singleplayerServer.getConnection().startMemoryChannel();
      Connection var10 = Connection.connectToLocalServer(var15);
      var10.setListener(new ClientHandshakePacketListenerImpl(var10, this, (ServerData)null, (Screen)null, var5, var14, (var0) -> {
      }));
      var10.send(new ClientIntentionPacket(var15.toString(), 0, ConnectionProtocol.LOGIN));
      var10.send(new ServerboundHelloPacket(this.getUser().getName(), Optional.ofNullable(this.getUser().getProfileId())));
      this.pendingConnection = var10;
   }

   public void setLevel(ClientLevel var1) {
      ProgressScreen var2 = new ProgressScreen(true);
      var2.progressStartNoAbort(Component.translatable("connect.joining"));
      this.updateScreenAndTick(var2);
      this.level = var1;
      this.updateLevelInEngines(var1);
      if (!this.isLocalServer) {
         Services var3 = Services.create(this.authenticationService, this.gameDirectory);
         var3.profileCache().setExecutor(this);
         SkullBlockEntity.setup(var3, this);
         GameProfileCache.setUsesAuthentication(false);
      }

   }

   public void clearLevel() {
      this.clearLevel(new ProgressScreen(true));
   }

   public void clearLevel(Screen var1) {
      ClientPacketListener var2 = this.getConnection();
      if (var2 != null) {
         this.dropAllTasks();
         var2.close();
      }

      this.playerSocialManager.stopOnlineMode();
      if (this.metricsRecorder.isRecording()) {
         this.debugClientMetricsCancel();
      }

      IntegratedServer var3 = this.singleplayerServer;
      this.singleplayerServer = null;
      this.gameRenderer.resetData();
      this.gameMode = null;
      this.narrator.clear();
      this.updateScreenAndTick(var1);
      if (this.level != null) {
         if (var3 != null) {
            this.profiler.push("waitForServer");

            while(!var3.isShutdown()) {
               this.runTick(false);
            }

            this.profiler.pop();
         }

         this.downloadedPackSource.clearServerPack();
         this.gui.onDisconnected();
         this.isLocalServer = false;
      }

      this.level = null;
      this.updateLevelInEngines((ClientLevel)null);
      this.player = null;
      SkullBlockEntity.clear();
   }

   private void updateScreenAndTick(Screen var1) {
      this.profiler.push("forcedTick");
      this.soundManager.stop();
      this.cameraEntity = null;
      this.pendingConnection = null;
      this.setScreen(var1);
      this.runTick(false);
      this.profiler.pop();
   }

   public void forceSetScreen(Screen var1) {
      this.profiler.push("forcedTick");
      this.setScreen(var1);
      this.runTick(false);
      this.profiler.pop();
   }

   private void updateLevelInEngines(@Nullable ClientLevel var1) {
      this.levelRenderer.setLevel(var1);
      this.particleEngine.setLevel(var1);
      this.blockEntityRenderDispatcher.setLevel(var1);
      this.updateTitle();
   }

   public boolean telemetryOptInExtra() {
      return this.extraTelemetryAvailable() && (Boolean)this.options.telemetryOptInExtra().get();
   }

   public boolean extraTelemetryAvailable() {
      return this.allowsTelemetry() && this.userApiService.properties().flag(UserFlag.OPTIONAL_TELEMETRY_AVAILABLE);
   }

   public boolean allowsTelemetry() {
      return this.userApiService.properties().flag(UserFlag.TELEMETRY_ENABLED);
   }

   public boolean allowsMultiplayer() {
      return this.allowsMultiplayer && this.userApiService.properties().flag(UserFlag.SERVERS_ALLOWED) && this.multiplayerBan() == null;
   }

   public boolean allowsRealms() {
      return this.userApiService.properties().flag(UserFlag.REALMS_ALLOWED) && this.multiplayerBan() == null;
   }

   public boolean shouldShowBanNotice() {
      return this.multiplayerBan() != null;
   }

   @Nullable
   public BanDetails multiplayerBan() {
      return (BanDetails)this.userApiService.properties().bannedScopes().get("MULTIPLAYER");
   }

   public boolean isBlocked(UUID var1) {
      if (this.getChatStatus().isChatAllowed(false)) {
         return this.playerSocialManager.shouldHideMessageFrom(var1);
      } else {
         return (this.player == null || !var1.equals(this.player.getUUID())) && !var1.equals(Util.NIL_UUID);
      }
   }

   public Minecraft.ChatStatus getChatStatus() {
      if (this.options.chatVisibility().get() == ChatVisiblity.HIDDEN) {
         return Minecraft.ChatStatus.DISABLED_BY_OPTIONS;
      } else if (!this.allowsChat) {
         return Minecraft.ChatStatus.DISABLED_BY_LAUNCHER;
      } else {
         return !this.userApiService.properties().flag(UserFlag.CHAT_ALLOWED) ? Minecraft.ChatStatus.DISABLED_BY_PROFILE : Minecraft.ChatStatus.ENABLED;
      }
   }

   public final boolean isDemo() {
      return this.demo;
   }

   @Nullable
   public ClientPacketListener getConnection() {
      return this.player == null ? null : this.player.connection;
   }

   public static boolean renderNames() {
      return !instance.options.hideGui;
   }

   public static boolean useFancyGraphics() {
      return ((GraphicsStatus)instance.options.graphicsMode().get()).getId() >= GraphicsStatus.FANCY.getId();
   }

   public static boolean useShaderTransparency() {
      return !instance.gameRenderer.isPanoramicMode() && ((GraphicsStatus)instance.options.graphicsMode().get()).getId() >= GraphicsStatus.FABULOUS.getId();
   }

   public static boolean useAmbientOcclusion() {
      return (Boolean)instance.options.ambientOcclusion().get();
   }

   private void pickBlock() {
      if (this.hitResult != null && this.hitResult.getType() != HitResult.Type.MISS) {
         boolean var1 = this.player.getAbilities().instabuild;
         BlockEntity var2 = null;
         HitResult.Type var4 = this.hitResult.getType();
         ItemStack var3;
         if (var4 == HitResult.Type.BLOCK) {
            BlockPos var8 = ((BlockHitResult)this.hitResult).getBlockPos();
            BlockState var6 = this.level.getBlockState(var8);
            if (var6.isAir()) {
               return;
            }

            Block var7 = var6.getBlock();
            var3 = var7.getCloneItemStack(this.level, var8, var6);
            if (var3.isEmpty()) {
               return;
            }

            if (var1 && Screen.hasControlDown() && var6.hasBlockEntity()) {
               var2 = this.level.getBlockEntity(var8);
            }
         } else {
            if (var4 != HitResult.Type.ENTITY || !var1) {
               return;
            }

            Entity var5 = ((EntityHitResult)this.hitResult).getEntity();
            var3 = var5.getPickResult();
            if (var3 == null) {
               return;
            }
         }

         if (var3.isEmpty()) {
            String var10 = "";
            if (var4 == HitResult.Type.BLOCK) {
               var10 = BuiltInRegistries.BLOCK.getKey(this.level.getBlockState(((BlockHitResult)this.hitResult).getBlockPos()).getBlock()).toString();
            } else if (var4 == HitResult.Type.ENTITY) {
               var10 = BuiltInRegistries.ENTITY_TYPE.getKey(((EntityHitResult)this.hitResult).getEntity().getType()).toString();
            }

            LOGGER.warn("Picking on: [{}] {} gave null item", var4, var10);
         } else {
            Inventory var9 = this.player.getInventory();
            if (var2 != null) {
               this.addCustomNbtData(var3, var2);
            }

            int var11 = var9.findSlotMatchingItem(var3);
            if (var1) {
               var9.setPickedItem(var3);
               this.gameMode.handleCreativeModeItemAdd(this.player.getItemInHand(InteractionHand.MAIN_HAND), 36 + var9.selected);
            } else if (var11 != -1) {
               if (Inventory.isHotbarSlot(var11)) {
                  var9.selected = var11;
               } else {
                  this.gameMode.handlePickItem(var11);
               }
            }

         }
      }
   }

   private void addCustomNbtData(ItemStack var1, BlockEntity var2) {
      CompoundTag var3 = var2.saveWithFullMetadata();
      BlockItem.setBlockEntityData(var1, var2.getType(), var3);
      CompoundTag var4;
      if (var1.getItem() instanceof PlayerHeadItem && var3.contains("SkullOwner")) {
         var4 = var3.getCompound("SkullOwner");
         CompoundTag var7 = var1.getOrCreateTag();
         var7.put("SkullOwner", var4);
         CompoundTag var6 = var7.getCompound("BlockEntityTag");
         var6.remove("SkullOwner");
         var6.remove("x");
         var6.remove("y");
         var6.remove("z");
      } else {
         var4 = new CompoundTag();
         ListTag var5 = new ListTag();
         var5.add(StringTag.valueOf("\"(+NBT)\""));
         var4.put("Lore", var5);
         var1.addTagElement("display", var4);
      }
   }

   public CrashReport fillReport(CrashReport var1) {
      SystemReport var2 = var1.getSystemReport();
      fillSystemReport(var2, this, this.languageManager, this.launchedVersion, this.options);
      if (this.level != null) {
         this.level.fillReportDetails(var1);
      }

      if (this.singleplayerServer != null) {
         this.singleplayerServer.fillSystemReport(var2);
      }

      this.reloadStateTracker.fillCrashReport(var1);
      return var1;
   }

   public static void fillReport(@Nullable Minecraft var0, @Nullable LanguageManager var1, String var2, @Nullable Options var3, CrashReport var4) {
      SystemReport var5 = var4.getSystemReport();
      fillSystemReport(var5, var0, var1, var2, var3);
   }

   private static SystemReport fillSystemReport(SystemReport var0, @Nullable Minecraft var1, @Nullable LanguageManager var2, String var3, Options var4) {
      var0.setDetail("Launched Version", () -> {
         return var3;
      });
      var0.setDetail("Backend library", RenderSystem::getBackendDescription);
      var0.setDetail("Backend API", RenderSystem::getApiDescription);
      var0.setDetail("Window size", () -> {
         return var1 != null ? var1.window.getWidth() + "x" + var1.window.getHeight() : "<not initialized>";
      });
      var0.setDetail("GL Caps", RenderSystem::getCapsString);
      var0.setDetail("GL debug messages", () -> {
         return GlDebug.isDebugEnabled() ? String.join("\n", GlDebug.getLastOpenGlDebugMessages()) : "<disabled>";
      });
      var0.setDetail("Using VBOs", () -> {
         return "Yes";
      });
      var0.setDetail("Is Modded", () -> {
         return checkModStatus().fullDescription();
      });
      var0.setDetail("Type", "Client (map_client.txt)");
      if (var4 != null) {
         if (instance != null) {
            String var5 = instance.getGpuWarnlistManager().getAllWarnings();
            if (var5 != null) {
               var0.setDetail("GPU Warnings", var5);
            }
         }

         var0.setDetail("Graphics mode", ((GraphicsStatus)var4.graphicsMode().get()).toString());
         var0.setDetail("Resource Packs", () -> {
            StringBuilder var1 = new StringBuilder();
            Iterator var2 = var4.resourcePacks.iterator();

            while(var2.hasNext()) {
               String var3 = (String)var2.next();
               if (var1.length() > 0) {
                  var1.append(", ");
               }

               var1.append(var3);
               if (var4.incompatibleResourcePacks.contains(var3)) {
                  var1.append(" (incompatible)");
               }
            }

            return var1.toString();
         });
      }

      if (var2 != null) {
         var0.setDetail("Current Language", () -> {
            return var2.getSelected();
         });
      }

      var0.setDetail("CPU", GlUtil::getCpuInfo);
      return var0;
   }

   public static Minecraft getInstance() {
      return instance;
   }

   public CompletableFuture<Void> delayTextureReload() {
      return this.submit(this::reloadResourcePacks).thenCompose((var0) -> {
         return var0;
      });
   }

   public void updateReportEnvironment(ReportEnvironment var1) {
      if (!this.reportingContext.matches(var1)) {
         this.reportingContext = ReportingContext.create(var1, this.userApiService);
      }

   }

   @Nullable
   public ServerData getCurrentServer() {
      return (ServerData)Optionull.map(this.getConnection(), ClientPacketListener::getServerData);
   }

   public boolean isLocalServer() {
      return this.isLocalServer;
   }

   public boolean hasSingleplayerServer() {
      return this.isLocalServer && this.singleplayerServer != null;
   }

   @Nullable
   public IntegratedServer getSingleplayerServer() {
      return this.singleplayerServer;
   }

   public boolean isSingleplayer() {
      IntegratedServer var1 = this.getSingleplayerServer();
      return var1 != null && !var1.isPublished();
   }

   public User getUser() {
      return this.user;
   }

   public PropertyMap getProfileProperties() {
      if (this.profileProperties.isEmpty()) {
         GameProfile var1 = this.getMinecraftSessionService().fillProfileProperties(this.user.getGameProfile(), false);
         this.profileProperties.putAll(var1.getProperties());
      }

      return this.profileProperties;
   }

   public Proxy getProxy() {
      return this.proxy;
   }

   public TextureManager getTextureManager() {
      return this.textureManager;
   }

   public ResourceManager getResourceManager() {
      return this.resourceManager;
   }

   public PackRepository getResourcePackRepository() {
      return this.resourcePackRepository;
   }

   public VanillaPackResources getVanillaPackResources() {
      return this.vanillaPackResources;
   }

   public DownloadedPackSource getDownloadedPackSource() {
      return this.downloadedPackSource;
   }

   public Path getResourcePackDirectory() {
      return this.resourcePackDirectory;
   }

   public LanguageManager getLanguageManager() {
      return this.languageManager;
   }

   public Function<ResourceLocation, TextureAtlasSprite> getTextureAtlas(ResourceLocation var1) {
      TextureAtlas var10000 = this.modelManager.getAtlas(var1);
      Objects.requireNonNull(var10000);
      return var10000::getSprite;
   }

   public boolean is64Bit() {
      return this.is64bit;
   }

   public boolean isPaused() {
      return this.pause;
   }

   public GpuWarnlistManager getGpuWarnlistManager() {
      return this.gpuWarnlistManager;
   }

   public SoundManager getSoundManager() {
      return this.soundManager;
   }

   public Music getSituationalMusic() {
      Music var1 = (Music)Optionull.map(this.screen, Screen::getBackgroundMusic);
      if (var1 != null) {
         return var1;
      } else if (this.player != null) {
         if (this.player.level().dimension() == Level.END) {
            return this.gui.getBossOverlay().shouldPlayMusic() ? Musics.END_BOSS : Musics.END;
         } else {
            Holder var2 = this.player.level().getBiome(this.player.blockPosition());
            if (this.musicManager.isPlayingMusic(Musics.UNDER_WATER) || this.player.isUnderWater() && var2.is(BiomeTags.PLAYS_UNDERWATER_MUSIC)) {
               return Musics.UNDER_WATER;
            } else {
               return this.player.level().dimension() != Level.NETHER && this.player.getAbilities().instabuild && this.player.getAbilities().mayfly ? Musics.CREATIVE : (Music)((Biome)var2.value()).getBackgroundMusic().orElse(Musics.GAME);
            }
         }
      } else {
         return Musics.MENU;
      }
   }

   public MinecraftSessionService getMinecraftSessionService() {
      return this.minecraftSessionService;
   }

   public SkinManager getSkinManager() {
      return this.skinManager;
   }

   @Nullable
   public Entity getCameraEntity() {
      return this.cameraEntity;
   }

   public void setCameraEntity(Entity var1) {
      this.cameraEntity = var1;
      this.gameRenderer.checkEntityPostEffect(var1);
   }

   public boolean shouldEntityAppearGlowing(Entity var1) {
      return var1.isCurrentlyGlowing() || this.player != null && this.player.isSpectator() && this.options.keySpectatorOutlines.isDown() && var1.getType() == EntityType.PLAYER;
   }

   protected Thread getRunningThread() {
      return this.gameThread;
   }

   protected Runnable wrapRunnable(Runnable var1) {
      return var1;
   }

   protected boolean shouldRun(Runnable var1) {
      return true;
   }

   public BlockRenderDispatcher getBlockRenderer() {
      return this.blockRenderer;
   }

   public EntityRenderDispatcher getEntityRenderDispatcher() {
      return this.entityRenderDispatcher;
   }

   public BlockEntityRenderDispatcher getBlockEntityRenderDispatcher() {
      return this.blockEntityRenderDispatcher;
   }

   public ItemRenderer getItemRenderer() {
      return this.itemRenderer;
   }

   public <T> SearchTree<T> getSearchTree(SearchRegistry.Key<T> var1) {
      return this.searchRegistry.getTree(var1);
   }

   public <T> void populateSearchTree(SearchRegistry.Key<T> var1, List<T> var2) {
      this.searchRegistry.populate(var1, var2);
   }

   public FrameTimer getFrameTimer() {
      return this.frameTimer;
   }

   public boolean isConnectedToRealms() {
      return this.connectedToRealms;
   }

   public void setConnectedToRealms(boolean var1) {
      this.connectedToRealms = var1;
   }

   public DataFixer getFixerUpper() {
      return this.fixerUpper;
   }

   public float getFrameTime() {
      return this.timer.partialTick;
   }

   public float getDeltaFrameTime() {
      return this.timer.tickDelta;
   }

   public BlockColors getBlockColors() {
      return this.blockColors;
   }

   public boolean showOnlyReducedInfo() {
      return this.player != null && this.player.isReducedDebugInfo() || (Boolean)this.options.reducedDebugInfo().get();
   }

   public ToastComponent getToasts() {
      return this.toast;
   }

   public Tutorial getTutorial() {
      return this.tutorial;
   }

   public boolean isWindowActive() {
      return this.windowActive;
   }

   public HotbarManager getHotbarManager() {
      return this.hotbarManager;
   }

   public ModelManager getModelManager() {
      return this.modelManager;
   }

   public PaintingTextureManager getPaintingTextures() {
      return this.paintingTextures;
   }

   public MobEffectTextureManager getMobEffectTextures() {
      return this.mobEffectTextures;
   }

   public void setWindowActive(boolean var1) {
      this.windowActive = var1;
   }

   public Component grabPanoramixScreenshot(File var1, int var2, int var3) {
      int var4 = this.window.getWidth();
      int var5 = this.window.getHeight();
      TextureTarget var6 = new TextureTarget(var2, var3, true, ON_OSX);
      float var7 = this.player.getXRot();
      float var8 = this.player.getYRot();
      float var9 = this.player.xRotO;
      float var10 = this.player.yRotO;
      this.gameRenderer.setRenderBlockOutline(false);

      MutableComponent var12;
      try {
         this.gameRenderer.setPanoramicMode(true);
         this.levelRenderer.graphicsChanged();
         this.window.setWidth(var2);
         this.window.setHeight(var3);

         for(int var11 = 0; var11 < 6; ++var11) {
            switch(var11) {
            case 0:
               this.player.setYRot(var8);
               this.player.setXRot(0.0F);
               break;
            case 1:
               this.player.setYRot((var8 + 90.0F) % 360.0F);
               this.player.setXRot(0.0F);
               break;
            case 2:
               this.player.setYRot((var8 + 180.0F) % 360.0F);
               this.player.setXRot(0.0F);
               break;
            case 3:
               this.player.setYRot((var8 - 90.0F) % 360.0F);
               this.player.setXRot(0.0F);
               break;
            case 4:
               this.player.setYRot(var8);
               this.player.setXRot(-90.0F);
               break;
            case 5:
            default:
               this.player.setYRot(var8);
               this.player.setXRot(90.0F);
            }

            this.player.yRotO = this.player.getYRot();
            this.player.xRotO = this.player.getXRot();
            var6.bindWrite(true);
            this.gameRenderer.renderLevel(1.0F, 0L, new PoseStack());

            try {
               Thread.sleep(10L);
            } catch (InterruptedException var17) {
            }

            Screenshot.grab(var1, "panorama_" + var11 + ".png", var6, (var0) -> {
            });
         }

         MutableComponent var20 = Component.literal(var1.getName()).withStyle(ChatFormatting.UNDERLINE).withStyle((var1x) -> {
            return var1x.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, var1.getAbsolutePath()));
         });
         var12 = Component.translatable("screenshot.success", var20);
         return var12;
      } catch (Exception var18) {
         LOGGER.error("Couldn't save image", var18);
         var12 = Component.translatable("screenshot.failure", var18.getMessage());
      } finally {
         this.player.setXRot(var7);
         this.player.setYRot(var8);
         this.player.xRotO = var9;
         this.player.yRotO = var10;
         this.gameRenderer.setRenderBlockOutline(true);
         this.window.setWidth(var4);
         this.window.setHeight(var5);
         var6.destroyBuffers();
         this.gameRenderer.setPanoramicMode(false);
         this.levelRenderer.graphicsChanged();
         this.getMainRenderTarget().bindWrite(true);
      }

      return var12;
   }

   private Component grabHugeScreenshot(File var1, int var2, int var3, int var4, int var5) {
      try {
         ByteBuffer var6 = GlUtil.allocateMemory(var2 * var3 * 3);
         Screenshot var7 = new Screenshot(var1, var4, var5, var3);
         float var8 = (float)var4 / (float)var2;
         float var9 = (float)var5 / (float)var3;
         float var10 = var8 > var9 ? var8 : var9;

         for(int var11 = (var5 - 1) / var3 * var3; var11 >= 0; var11 -= var3) {
            for(int var12 = 0; var12 < var4; var12 += var2) {
               RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
               float var13 = (float)(var4 - var2) / 2.0F * 2.0F - (float)(var12 * 2);
               float var14 = (float)(var5 - var3) / 2.0F * 2.0F - (float)(var11 * 2);
               var13 /= (float)var2;
               var14 /= (float)var3;
               this.gameRenderer.renderZoomed(var10, var13, var14);
               var6.clear();
               RenderSystem.pixelStore(3333, 1);
               RenderSystem.pixelStore(3317, 1);
               RenderSystem.readPixels(0, 0, var2, var3, 32992, 5121, var6);
               var7.addRegion(var6, var12, var11, var2, var3);
            }

            var7.saveRow();
         }

         File var16 = var7.close();
         GlUtil.freeMemory(var6);
         MutableComponent var17 = Component.literal(var16.getName()).withStyle(ChatFormatting.UNDERLINE).withStyle((var1x) -> {
            return var1x.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, var16.getAbsolutePath()));
         });
         return Component.translatable("screenshot.success", var17);
      } catch (Exception var15) {
         LOGGER.warn("Couldn't save screenshot", var15);
         return Component.translatable("screenshot.failure", var15.getMessage());
      }
   }

   public ProfilerFiller getProfiler() {
      return this.profiler;
   }

   @Nullable
   public StoringChunkProgressListener getProgressListener() {
      return (StoringChunkProgressListener)this.progressListener.get();
   }

   public SplashManager getSplashManager() {
      return this.splashManager;
   }

   @Nullable
   public Overlay getOverlay() {
      return this.overlay;
   }

   public PlayerSocialManager getPlayerSocialManager() {
      return this.playerSocialManager;
   }

   public boolean renderOnThread() {
      return false;
   }

   public Window getWindow() {
      return this.window;
   }

   public RenderBuffers renderBuffers() {
      return this.renderBuffers;
   }

   public void updateMaxMipLevel(int var1) {
      this.modelManager.updateMaxMipLevel(var1);
   }

   public EntityModelSet getEntityModels() {
      return this.entityModels;
   }

   public boolean isTextFilteringEnabled() {
      return this.userApiService.properties().flag(UserFlag.PROFANITY_FILTER_ENABLED);
   }

   public void prepareForMultiplayer() {
      this.playerSocialManager.startOnlineMode();
      this.getProfileKeyPairManager().prepareKeyPair();
   }

   public Realms32BitWarningStatus getRealms32BitWarningStatus() {
      return this.realms32BitWarningStatus;
   }

   @Nullable
   public SignatureValidator getProfileKeySignatureValidator() {
      return SignatureValidator.from(this.authenticationService.getServicesKeySet(), ServicesKeyType.PROFILE_KEY);
   }

   public InputType getLastInputType() {
      return this.lastInputType;
   }

   public void setLastInputType(InputType var1) {
      this.lastInputType = var1;
   }

   public GameNarrator getNarrator() {
      return this.narrator;
   }

   public ChatListener getChatListener() {
      return this.chatListener;
   }

   public ReportingContext getReportingContext() {
      return this.reportingContext;
   }

   public RealmsDataFetcher realmsDataFetcher() {
      return this.realmsDataFetcher;
   }

   public QuickPlayLog quickPlayLog() {
      return this.quickPlayLog;
   }

   static {
      ON_OSX = Util.getPlatform() == Util.OS.OSX;
      DEFAULT_FONT = new ResourceLocation("default");
      UNIFORM_FONT = new ResourceLocation("uniform");
      ALT_FONT = new ResourceLocation("alt");
      REGIONAL_COMPLIANCIES = new ResourceLocation("regional_compliancies.json");
      RESOURCE_RELOAD_INITIAL_TASK = CompletableFuture.completedFuture(Unit.INSTANCE);
      SOCIAL_INTERACTIONS_NOT_AVAILABLE = Component.translatable("multiplayer.socialInteractions.not_available");
   }

   public static enum ChatStatus {
      ENABLED(CommonComponents.EMPTY) {
         public boolean isChatAllowed(boolean var1) {
            return true;
         }
      },
      DISABLED_BY_OPTIONS(Component.translatable("chat.disabled.options").withStyle(ChatFormatting.RED)) {
         public boolean isChatAllowed(boolean var1) {
            return false;
         }
      },
      DISABLED_BY_LAUNCHER(Component.translatable("chat.disabled.launcher").withStyle(ChatFormatting.RED)) {
         public boolean isChatAllowed(boolean var1) {
            return var1;
         }
      },
      DISABLED_BY_PROFILE(Component.translatable("chat.disabled.profile", Component.keybind(Minecraft.instance.options.keyChat.getName())).withStyle(ChatFormatting.RED)) {
         public boolean isChatAllowed(boolean var1) {
            return var1;
         }
      };

      static final Component INFO_DISABLED_BY_PROFILE = Component.translatable("chat.disabled.profile.moreInfo");
      private final Component message;

      ChatStatus(Component var3) {
         this.message = var3;
      }

      public Component getMessage() {
         return this.message;
      }

      public abstract boolean isChatAllowed(boolean var1);

      // $FF: synthetic method
      private static Minecraft.ChatStatus[] $values() {
         return new Minecraft.ChatStatus[]{ENABLED, DISABLED_BY_OPTIONS, DISABLED_BY_LAUNCHER, DISABLED_BY_PROFILE};
      }
   }
}
