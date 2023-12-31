package com.mojang.realmsclient;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.RateLimiter;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import com.mojang.realmsclient.client.Ping;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.PingResult;
import com.mojang.realmsclient.dto.RealmsNotification;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsServerPlayerList;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.RealmsDataFetcher;
import com.mojang.realmsclient.gui.RealmsNewsManager;
import com.mojang.realmsclient.gui.RealmsServerList;
import com.mojang.realmsclient.gui.screens.RealmsClientOutdatedScreen;
import com.mojang.realmsclient.gui.screens.RealmsConfigureWorldScreen;
import com.mojang.realmsclient.gui.screens.RealmsCreateRealmScreen;
import com.mojang.realmsclient.gui.screens.RealmsGenericErrorScreen;
import com.mojang.realmsclient.gui.screens.RealmsLongConfirmationScreen;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTaskScreen;
import com.mojang.realmsclient.gui.screens.RealmsParentalConsentScreen;
import com.mojang.realmsclient.gui.screens.RealmsPendingInvitesScreen;
import com.mojang.realmsclient.gui.task.DataFetcher;
import com.mojang.realmsclient.util.RealmsPersistence;
import com.mojang.realmsclient.util.RealmsUtil;
import com.mojang.realmsclient.util.task.GetServerDetailsTask;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.ImageWidget;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.realms.RealmsObjectSelectionList;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.CommonLinks;
import net.minecraft.util.Mth;
import org.slf4j.Logger;

public class RealmsMainScreen extends RealmsScreen {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final ResourceLocation ON_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/on_icon.png");
   private static final ResourceLocation OFF_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/off_icon.png");
   private static final ResourceLocation EXPIRED_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/expired_icon.png");
   private static final ResourceLocation EXPIRES_SOON_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/expires_soon_icon.png");
   static final ResourceLocation INVITATION_ICONS_LOCATION = new ResourceLocation("realms", "textures/gui/realms/invitation_icons.png");
   static final ResourceLocation INVITE_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/invite_icon.png");
   static final ResourceLocation WORLDICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/world_icon.png");
   private static final ResourceLocation LOGO_LOCATION = new ResourceLocation("realms", "textures/gui/title/realms.png");
   private static final ResourceLocation NEWS_LOCATION = new ResourceLocation("realms", "textures/gui/realms/news_icon.png");
   private static final ResourceLocation POPUP_LOCATION = new ResourceLocation("realms", "textures/gui/realms/popup.png");
   private static final ResourceLocation DARKEN_LOCATION = new ResourceLocation("realms", "textures/gui/realms/darken.png");
   static final ResourceLocation CROSS_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/cross_icon.png");
   private static final ResourceLocation TRIAL_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/trial_icon.png");
   static final ResourceLocation INFO_ICON_LOCATION = new ResourceLocation("minecraft", "textures/gui/info_icon.png");
   static final List<Component> TRIAL_MESSAGE_LINES = ImmutableList.of(Component.translatable("mco.trial.message.line1"), Component.translatable("mco.trial.message.line2"));
   static final Component SERVER_UNITIALIZED_TEXT = Component.translatable("mco.selectServer.uninitialized");
   static final Component SUBSCRIPTION_EXPIRED_TEXT = Component.translatable("mco.selectServer.expiredList");
   private static final Component SUBSCRIPTION_RENEW_TEXT = Component.translatable("mco.selectServer.expiredRenew");
   static final Component TRIAL_EXPIRED_TEXT = Component.translatable("mco.selectServer.expiredTrial");
   static final Component SELECT_MINIGAME_PREFIX;
   private static final Component POPUP_TEXT;
   private static final Component PLAY_TEXT;
   private static final Component LEAVE_SERVER_TEXT;
   private static final Component CONFIGURE_SERVER_TEXT;
   private static final Component SERVER_EXPIRED_TOOLTIP;
   private static final Component SERVER_EXPIRES_SOON_TOOLTIP;
   private static final Component SERVER_EXPIRES_IN_DAY_TOOLTIP;
   private static final Component SERVER_OPEN_TOOLTIP;
   private static final Component SERVER_CLOSED_TOOLTIP;
   private static final Component NEWS_TOOLTIP;
   static final Component UNITIALIZED_WORLD_NARRATION;
   static final Component TRIAL_TEXT;
   private static final int BUTTON_WIDTH = 100;
   private static final int BUTTON_TOP_ROW_WIDTH = 308;
   private static final int BUTTON_BOTTOM_ROW_WIDTH = 204;
   private static final int FOOTER_HEIGHT = 64;
   private static final int LOGO_WIDTH = 128;
   private static final int LOGO_HEIGHT = 34;
   private static final int LOGO_TEXTURE_WIDTH = 128;
   private static final int LOGO_TEXTURE_HEIGHT = 64;
   private static final int LOGO_PADDING = 5;
   private static final int HEADER_HEIGHT = 44;
   private static List<ResourceLocation> teaserImages;
   @Nullable
   private DataFetcher.Subscription dataSubscription;
   private RealmsServerList serverList;
   private final Set<UUID> handledSeenNotifications = new HashSet();
   private static boolean overrideConfigure;
   private static int lastScrollYPosition;
   static volatile boolean hasParentalConsent;
   static volatile boolean checkedParentalConsent;
   static volatile boolean checkedClientCompatability;
   @Nullable
   static Screen realmsGenericErrorScreen;
   private static boolean regionsPinged;
   private final RateLimiter inviteNarrationLimiter;
   private boolean dontSetConnectedToRealms;
   final Screen lastScreen;
   RealmsMainScreen.RealmSelectionList realmSelectionList;
   private boolean realmsSelectionListAdded;
   private Button playButton;
   private Button backButton;
   private Button renewButton;
   private Button configureButton;
   private Button leaveButton;
   private List<RealmsServer> realmsServers = ImmutableList.of();
   volatile int numberOfPendingInvites;
   int animTick;
   private boolean hasFetchedServers;
   boolean popupOpenedByUser;
   private boolean justClosedPopup;
   private volatile boolean trialsAvailable;
   private volatile boolean createdTrial;
   private volatile boolean showingPopup;
   volatile boolean hasUnreadNews;
   @Nullable
   volatile String newsLink;
   private int carouselIndex;
   private int carouselTick;
   private boolean hasSwitchedCarouselImage;
   private List<KeyCombo> keyCombos;
   long lastClickTime;
   private ReentrantLock connectLock = new ReentrantLock();
   private MultiLineLabel formattedPopup;
   private final List<RealmsNotification> notifications;
   private Button showPopupButton;
   private RealmsMainScreen.PendingInvitesButton pendingInvitesButton;
   private Button newsButton;
   private Button createTrialButton;
   private Button buyARealmButton;
   private Button closeButton;

   public RealmsMainScreen(Screen var1) {
      super(GameNarrator.NO_TITLE);
      this.formattedPopup = MultiLineLabel.EMPTY;
      this.notifications = new ArrayList();
      this.lastScreen = var1;
      this.inviteNarrationLimiter = RateLimiter.create(0.01666666753590107D);
   }

   private boolean shouldShowMessageInList() {
      if (hasParentalConsent() && this.hasFetchedServers) {
         if (this.trialsAvailable && !this.createdTrial) {
            return true;
         } else {
            Iterator var1 = this.realmsServers.iterator();

            RealmsServer var2;
            do {
               if (!var1.hasNext()) {
                  return true;
               }

               var2 = (RealmsServer)var1.next();
            } while(!var2.ownerUUID.equals(this.minecraft.getUser().getUuid()));

            return false;
         }
      } else {
         return false;
      }
   }

   public boolean shouldShowPopup() {
      if (hasParentalConsent() && this.hasFetchedServers) {
         return this.popupOpenedByUser ? true : this.realmsServers.isEmpty();
      } else {
         return false;
      }
   }

   public void init() {
      this.keyCombos = Lists.newArrayList(new KeyCombo[]{new KeyCombo(new char[]{'3', '2', '1', '4', '5', '6'}, () -> {
         overrideConfigure = !overrideConfigure;
      }), new KeyCombo(new char[]{'9', '8', '7', '1', '2', '3'}, () -> {
         if (RealmsClient.currentEnvironment == RealmsClient.Environment.STAGE) {
            this.switchToProd();
         } else {
            this.switchToStage();
         }

      }), new KeyCombo(new char[]{'9', '8', '7', '4', '5', '6'}, () -> {
         if (RealmsClient.currentEnvironment == RealmsClient.Environment.LOCAL) {
            this.switchToProd();
         } else {
            this.switchToLocal();
         }

      })});
      if (realmsGenericErrorScreen != null) {
         this.minecraft.setScreen(realmsGenericErrorScreen);
      } else {
         this.connectLock = new ReentrantLock();
         if (checkedClientCompatability && !hasParentalConsent()) {
            this.checkParentalConsent();
         }

         this.checkClientCompatability();
         if (!this.dontSetConnectedToRealms) {
            this.minecraft.setConnectedToRealms(false);
         }

         this.showingPopup = false;
         this.realmSelectionList = new RealmsMainScreen.RealmSelectionList();
         if (lastScrollYPosition != -1) {
            this.realmSelectionList.setScrollAmount((double)lastScrollYPosition);
         }

         this.addWidget(this.realmSelectionList);
         this.realmsSelectionListAdded = true;
         this.setInitialFocus(this.realmSelectionList);
         this.addMiddleButtons();
         this.addFooterButtons();
         this.addTopButtons();
         this.updateButtonStates((RealmsServer)null);
         this.formattedPopup = MultiLineLabel.create(this.font, POPUP_TEXT, 100);
         RealmsNewsManager var1 = this.minecraft.realmsDataFetcher().newsManager;
         this.hasUnreadNews = var1.hasUnreadNews();
         this.newsLink = var1.newsLink();
         if (this.serverList == null) {
            this.serverList = new RealmsServerList(this.minecraft);
         }

         if (this.dataSubscription != null) {
            this.dataSubscription.forceUpdate();
         }

      }
   }

   private static boolean hasParentalConsent() {
      return checkedParentalConsent && hasParentalConsent;
   }

   public void addTopButtons() {
      this.pendingInvitesButton = (RealmsMainScreen.PendingInvitesButton)this.addRenderableWidget(new RealmsMainScreen.PendingInvitesButton());
      this.newsButton = (Button)this.addRenderableWidget(new RealmsMainScreen.NewsButton());
      this.showPopupButton = (Button)this.addRenderableWidget(Button.builder(Component.translatable("mco.selectServer.purchase"), (var1) -> {
         this.popupOpenedByUser = !this.popupOpenedByUser;
      }).bounds(this.width - 90, 12, 80, 20).build());
   }

   public void addMiddleButtons() {
      this.createTrialButton = (Button)this.addWidget(Button.builder(Component.translatable("mco.selectServer.trial"), (var1) -> {
         if (this.trialsAvailable && !this.createdTrial) {
            Util.getPlatform().openUri("https://aka.ms/startjavarealmstrial");
            this.minecraft.setScreen(this.lastScreen);
         }
      }).bounds(this.width / 2 + 52, this.popupY0() + 137 - 20, 98, 20).build());
      this.buyARealmButton = (Button)this.addWidget(Button.builder(Component.translatable("mco.selectServer.buy"), (var0) -> {
         Util.getPlatform().openUri("https://aka.ms/BuyJavaRealms");
      }).bounds(this.width / 2 + 52, this.popupY0() + 160 - 20, 98, 20).build());
      this.closeButton = (Button)this.addWidget(new RealmsMainScreen.CloseButton());
   }

   public void addFooterButtons() {
      this.playButton = Button.builder(PLAY_TEXT, (var1x) -> {
         this.play(this.getSelectedServer(), this);
      }).width(100).build();
      this.configureButton = Button.builder(CONFIGURE_SERVER_TEXT, (var1x) -> {
         this.configureClicked(this.getSelectedServer());
      }).width(100).build();
      this.renewButton = Button.builder(SUBSCRIPTION_RENEW_TEXT, (var1x) -> {
         this.onRenew(this.getSelectedServer());
      }).width(100).build();
      this.leaveButton = Button.builder(LEAVE_SERVER_TEXT, (var1x) -> {
         this.leaveClicked(this.getSelectedServer());
      }).width(100).build();
      this.backButton = Button.builder(CommonComponents.GUI_BACK, (var1x) -> {
         if (!this.justClosedPopup) {
            this.minecraft.setScreen(this.lastScreen);
         }

      }).width(100).build();
      GridLayout var1 = new GridLayout();
      GridLayout.RowHelper var2 = var1.createRowHelper(1);
      LinearLayout var3 = (LinearLayout)var2.addChild(new LinearLayout(308, 20, LinearLayout.Orientation.HORIZONTAL), var2.newCellSettings().paddingBottom(4));
      var3.addChild(this.playButton);
      var3.addChild(this.configureButton);
      var3.addChild(this.renewButton);
      LinearLayout var4 = (LinearLayout)var2.addChild(new LinearLayout(204, 20, LinearLayout.Orientation.HORIZONTAL), var2.newCellSettings().alignHorizontallyCenter());
      var4.addChild(this.leaveButton);
      var4.addChild(this.backButton);
      var1.visitWidgets((var1x) -> {
         AbstractWidget var10000 = (AbstractWidget)this.addRenderableWidget(var1x);
      });
      var1.arrangeElements();
      FrameLayout.centerInRectangle(var1, 0, this.height - 64, this.width, 64);
   }

   void updateButtonStates(@Nullable RealmsServer var1) {
      this.backButton.active = true;
      if (hasParentalConsent() && this.hasFetchedServers) {
         boolean var2 = this.shouldShowPopup() && this.trialsAvailable && !this.createdTrial;
         this.createTrialButton.visible = var2;
         this.createTrialButton.active = var2;
         this.buyARealmButton.visible = this.shouldShowPopup();
         this.closeButton.visible = this.shouldShowPopup();
         this.newsButton.active = true;
         this.newsButton.visible = this.newsLink != null;
         this.pendingInvitesButton.active = true;
         this.pendingInvitesButton.visible = true;
         this.showPopupButton.active = !this.shouldShowPopup();
         this.playButton.visible = !this.shouldShowPopup();
         this.renewButton.visible = !this.shouldShowPopup();
         this.leaveButton.visible = !this.shouldShowPopup();
         this.configureButton.visible = !this.shouldShowPopup();
         this.backButton.visible = !this.shouldShowPopup();
         this.playButton.active = this.shouldPlayButtonBeActive(var1);
         this.renewButton.active = this.shouldRenewButtonBeActive(var1);
         this.leaveButton.active = this.shouldLeaveButtonBeActive(var1);
         this.configureButton.active = this.shouldConfigureButtonBeActive(var1);
      } else {
         hideWidgets(new AbstractWidget[]{this.playButton, this.renewButton, this.configureButton, this.createTrialButton, this.buyARealmButton, this.closeButton, this.newsButton, this.pendingInvitesButton, this.showPopupButton, this.leaveButton});
      }
   }

   private boolean shouldShowPopupButton() {
      return (!this.shouldShowPopup() || this.popupOpenedByUser) && hasParentalConsent() && this.hasFetchedServers;
   }

   boolean shouldPlayButtonBeActive(@Nullable RealmsServer var1) {
      return var1 != null && !var1.expired && var1.state == RealmsServer.State.OPEN;
   }

   private boolean shouldRenewButtonBeActive(@Nullable RealmsServer var1) {
      return var1 != null && var1.expired && this.isSelfOwnedServer(var1);
   }

   private boolean shouldConfigureButtonBeActive(@Nullable RealmsServer var1) {
      return var1 != null && this.isSelfOwnedServer(var1);
   }

   private boolean shouldLeaveButtonBeActive(@Nullable RealmsServer var1) {
      return var1 != null && !this.isSelfOwnedServer(var1);
   }

   public void tick() {
      super.tick();
      if (this.pendingInvitesButton != null) {
         this.pendingInvitesButton.tick();
      }

      this.justClosedPopup = false;
      ++this.animTick;
      boolean var1 = hasParentalConsent();
      if (this.dataSubscription == null && var1) {
         this.dataSubscription = this.initDataFetcher(this.minecraft.realmsDataFetcher());
      } else if (this.dataSubscription != null && !var1) {
         this.dataSubscription = null;
      }

      if (this.dataSubscription != null) {
         this.dataSubscription.tick();
      }

      if (this.shouldShowPopup()) {
         ++this.carouselTick;
      }

      if (this.showPopupButton != null) {
         this.showPopupButton.visible = this.shouldShowPopupButton();
         this.showPopupButton.active = this.showPopupButton.visible;
      }

   }

   private DataFetcher.Subscription initDataFetcher(RealmsDataFetcher var1) {
      DataFetcher.Subscription var2 = var1.dataFetcher.createSubscription();
      var2.subscribe(var1.serverListUpdateTask, (var1x) -> {
         List var2 = this.serverList.updateServersList(var1x);
         boolean var3 = false;
         Iterator var4 = var2.iterator();

         while(var4.hasNext()) {
            RealmsServer var5 = (RealmsServer)var4.next();
            if (this.isSelfOwnedNonExpiredServer(var5)) {
               var3 = true;
            }
         }

         this.realmsServers = var2;
         this.hasFetchedServers = true;
         this.refreshRealmsSelectionList();
         if (!regionsPinged && var3) {
            regionsPinged = true;
            this.pingRegions();
         }

      });
      callRealmsClient(RealmsClient::getNotifications, (var1x) -> {
         this.notifications.clear();
         this.notifications.addAll(var1x);
         this.refreshRealmsSelectionList();
      });
      var2.subscribe(var1.pendingInvitesTask, (var1x) -> {
         this.numberOfPendingInvites = var1x;
         if (this.numberOfPendingInvites > 0 && this.inviteNarrationLimiter.tryAcquire(1)) {
            this.minecraft.getNarrator().sayNow((Component)Component.translatable("mco.configure.world.invite.narration", this.numberOfPendingInvites));
         }

      });
      var2.subscribe(var1.trialAvailabilityTask, (var1x) -> {
         if (!this.createdTrial) {
            if (var1x != this.trialsAvailable && this.shouldShowPopup()) {
               this.trialsAvailable = var1x;
               this.showingPopup = false;
            } else {
               this.trialsAvailable = var1x;
            }

         }
      });
      var2.subscribe(var1.liveStatsTask, (var1x) -> {
         Iterator var2 = var1x.servers.iterator();

         while(true) {
            while(var2.hasNext()) {
               RealmsServerPlayerList var3 = (RealmsServerPlayerList)var2.next();
               Iterator var4 = this.realmsServers.iterator();

               while(var4.hasNext()) {
                  RealmsServer var5 = (RealmsServer)var4.next();
                  if (var5.id == var3.serverId) {
                     var5.updateServerPing(var3);
                     break;
                  }
               }
            }

            return;
         }
      });
      var2.subscribe(var1.newsTask, (var2x) -> {
         var1.newsManager.updateUnreadNews(var2x);
         this.hasUnreadNews = var1.newsManager.hasUnreadNews();
         this.newsLink = var1.newsManager.newsLink();
         this.updateButtonStates((RealmsServer)null);
      });
      return var2;
   }

   private static <T> void callRealmsClient(RealmsMainScreen.RealmsCall<T> var0, Consumer<T> var1) {
      Minecraft var2 = Minecraft.getInstance();
      CompletableFuture.supplyAsync(() -> {
         try {
            return var0.request(RealmsClient.create(var2));
         } catch (RealmsServiceException var3) {
            throw new RuntimeException(var3);
         }
      }).thenAcceptAsync(var1, var2).exceptionally((var0x) -> {
         LOGGER.error("Failed to execute call to Realms Service", var0x);
         return null;
      });
   }

   private void refreshRealmsSelectionList() {
      boolean var1 = !this.hasFetchedServers;
      this.realmSelectionList.clear();
      ArrayList var2 = new ArrayList();
      Iterator var3 = this.notifications.iterator();

      while(var3.hasNext()) {
         RealmsNotification var4 = (RealmsNotification)var3.next();
         this.addEntriesForNotification(this.realmSelectionList, var4);
         if (!var4.seen() && !this.handledSeenNotifications.contains(var4.uuid())) {
            var2.add(var4.uuid());
         }
      }

      if (!var2.isEmpty()) {
         callRealmsClient((var1x) -> {
            var1x.notificationsSeen(var2);
            return null;
         }, (var2x) -> {
            this.handledSeenNotifications.addAll(var2);
         });
      }

      if (this.shouldShowMessageInList()) {
         this.realmSelectionList.addEntry(new RealmsMainScreen.TrialEntry());
      }

      RealmsMainScreen.ServerEntry var8 = null;
      RealmsServer var9 = this.getSelectedServer();
      Iterator var5 = this.realmsServers.iterator();

      while(var5.hasNext()) {
         RealmsServer var6 = (RealmsServer)var5.next();
         RealmsMainScreen.ServerEntry var7 = new RealmsMainScreen.ServerEntry(var6);
         this.realmSelectionList.addEntry(var7);
         if (var9 != null && var9.id == var6.id) {
            var8 = var7;
         }
      }

      if (var1) {
         this.updateButtonStates((RealmsServer)null);
      } else {
         this.realmSelectionList.setSelected((RealmsMainScreen.Entry)var8);
      }

   }

   private void addEntriesForNotification(RealmsMainScreen.RealmSelectionList var1, RealmsNotification var2) {
      if (var2 instanceof RealmsNotification.VisitUrl) {
         RealmsNotification.VisitUrl var3 = (RealmsNotification.VisitUrl)var2;
         var1.addEntry(new RealmsMainScreen.NotificationMessageEntry(var3.getMessage(), var3));
         var1.addEntry(new RealmsMainScreen.ButtonEntry(var3.buildOpenLinkButton(this)));
      }

   }

   void refreshFetcher() {
      if (this.dataSubscription != null) {
         this.dataSubscription.reset();
      }

   }

   private void pingRegions() {
      (new Thread(() -> {
         List var1 = Ping.pingAllRegions();
         RealmsClient var2 = RealmsClient.create();
         PingResult var3 = new PingResult();
         var3.pingResults = var1;
         var3.worldIds = this.getOwnedNonExpiredWorldIds();

         try {
            var2.sendPingResults(var3);
         } catch (Throwable var5) {
            LOGGER.warn("Could not send ping result to Realms: ", var5);
         }

      })).start();
   }

   private List<Long> getOwnedNonExpiredWorldIds() {
      ArrayList var1 = Lists.newArrayList();
      Iterator var2 = this.realmsServers.iterator();

      while(var2.hasNext()) {
         RealmsServer var3 = (RealmsServer)var2.next();
         if (this.isSelfOwnedNonExpiredServer(var3)) {
            var1.add(var3.id);
         }
      }

      return var1;
   }

   public void setCreatedTrial(boolean var1) {
      this.createdTrial = var1;
   }

   private void onRenew(@Nullable RealmsServer var1) {
      if (var1 != null) {
         String var2 = CommonLinks.extendRealms(var1.remoteSubscriptionId, this.minecraft.getUser().getUuid(), var1.expiredTrial);
         this.minecraft.keyboardHandler.setClipboard(var2);
         Util.getPlatform().openUri(var2);
      }

   }

   private void checkClientCompatability() {
      if (!checkedClientCompatability) {
         checkedClientCompatability = true;
         (new Thread("MCO Compatability Checker #1") {
            public void run() {
               RealmsClient var1 = RealmsClient.create();

               try {
                  RealmsClient.CompatibleVersionResponse var2 = var1.clientCompatible();
                  if (var2 != RealmsClient.CompatibleVersionResponse.COMPATIBLE) {
                     RealmsMainScreen.realmsGenericErrorScreen = new RealmsClientOutdatedScreen(RealmsMainScreen.this.lastScreen);
                     RealmsMainScreen.this.minecraft.execute(() -> {
                        RealmsMainScreen.this.minecraft.setScreen(RealmsMainScreen.realmsGenericErrorScreen);
                     });
                     return;
                  }

                  RealmsMainScreen.this.checkParentalConsent();
               } catch (RealmsServiceException var3) {
                  RealmsMainScreen.checkedClientCompatability = false;
                  RealmsMainScreen.LOGGER.error("Couldn't connect to realms", var3);
                  if (var3.httpResultCode == 401) {
                     RealmsMainScreen.realmsGenericErrorScreen = new RealmsGenericErrorScreen(Component.translatable("mco.error.invalid.session.title"), Component.translatable("mco.error.invalid.session.message"), RealmsMainScreen.this.lastScreen);
                     RealmsMainScreen.this.minecraft.execute(() -> {
                        RealmsMainScreen.this.minecraft.setScreen(RealmsMainScreen.realmsGenericErrorScreen);
                     });
                  } else {
                     RealmsMainScreen.this.minecraft.execute(() -> {
                        RealmsMainScreen.this.minecraft.setScreen(new RealmsGenericErrorScreen(var3, RealmsMainScreen.this.lastScreen));
                     });
                  }
               }

            }
         }).start();
      }

   }

   void checkParentalConsent() {
      (new Thread("MCO Compatability Checker #1") {
         public void run() {
            RealmsClient var1 = RealmsClient.create();

            try {
               Boolean var2 = var1.mcoEnabled();
               if (var2) {
                  RealmsMainScreen.LOGGER.info("Realms is available for this user");
                  RealmsMainScreen.hasParentalConsent = true;
               } else {
                  RealmsMainScreen.LOGGER.info("Realms is not available for this user");
                  RealmsMainScreen.hasParentalConsent = false;
                  RealmsMainScreen.this.minecraft.execute(() -> {
                     RealmsMainScreen.this.minecraft.setScreen(new RealmsParentalConsentScreen(RealmsMainScreen.this.lastScreen));
                  });
               }

               RealmsMainScreen.checkedParentalConsent = true;
            } catch (RealmsServiceException var3) {
               RealmsMainScreen.LOGGER.error("Couldn't connect to realms", var3);
               RealmsMainScreen.this.minecraft.execute(() -> {
                  RealmsMainScreen.this.minecraft.setScreen(new RealmsGenericErrorScreen(var3, RealmsMainScreen.this.lastScreen));
               });
            }

         }
      }).start();
   }

   private void switchToStage() {
      if (RealmsClient.currentEnvironment != RealmsClient.Environment.STAGE) {
         (new Thread("MCO Stage Availability Checker #1") {
            public void run() {
               RealmsClient var1 = RealmsClient.create();

               try {
                  Boolean var2 = var1.stageAvailable();
                  if (var2) {
                     RealmsClient.switchToStage();
                     RealmsMainScreen.LOGGER.info("Switched to stage");
                     RealmsMainScreen.this.refreshFetcher();
                  }
               } catch (RealmsServiceException var3) {
                  RealmsMainScreen.LOGGER.error("Couldn't connect to Realms: {}", var3.toString());
               }

            }
         }).start();
      }

   }

   private void switchToLocal() {
      if (RealmsClient.currentEnvironment != RealmsClient.Environment.LOCAL) {
         (new Thread("MCO Local Availability Checker #1") {
            public void run() {
               RealmsClient var1 = RealmsClient.create();

               try {
                  Boolean var2 = var1.stageAvailable();
                  if (var2) {
                     RealmsClient.switchToLocal();
                     RealmsMainScreen.LOGGER.info("Switched to local");
                     RealmsMainScreen.this.refreshFetcher();
                  }
               } catch (RealmsServiceException var3) {
                  RealmsMainScreen.LOGGER.error("Couldn't connect to Realms: {}", var3.toString());
               }

            }
         }).start();
      }

   }

   private void switchToProd() {
      RealmsClient.switchToProd();
      this.refreshFetcher();
   }

   private void configureClicked(@Nullable RealmsServer var1) {
      if (var1 != null && (this.minecraft.getUser().getUuid().equals(var1.ownerUUID) || overrideConfigure)) {
         this.saveListScrollPosition();
         this.minecraft.setScreen(new RealmsConfigureWorldScreen(this, var1.id));
      }

   }

   private void leaveClicked(@Nullable RealmsServer var1) {
      if (var1 != null && !this.minecraft.getUser().getUuid().equals(var1.ownerUUID)) {
         this.saveListScrollPosition();
         MutableComponent var2 = Component.translatable("mco.configure.world.leave.question.line1");
         MutableComponent var3 = Component.translatable("mco.configure.world.leave.question.line2");
         this.minecraft.setScreen(new RealmsLongConfirmationScreen((var2x) -> {
            this.leaveServer(var2x, var1);
         }, RealmsLongConfirmationScreen.Type.INFO, var2, var3, true));
      }

   }

   private void saveListScrollPosition() {
      lastScrollYPosition = (int)this.realmSelectionList.getScrollAmount();
   }

   @Nullable
   private RealmsServer getSelectedServer() {
      if (this.realmSelectionList == null) {
         return null;
      } else {
         RealmsMainScreen.Entry var1 = (RealmsMainScreen.Entry)this.realmSelectionList.getSelected();
         return var1 != null ? var1.getServer() : null;
      }
   }

   private void leaveServer(boolean var1, final RealmsServer var2) {
      if (var1) {
         (new Thread("Realms-leave-server") {
            public void run() {
               try {
                  RealmsClient var1 = RealmsClient.create();
                  var1.uninviteMyselfFrom(var2.id);
                  RealmsMainScreen.this.minecraft.execute(() -> {
                     RealmsMainScreen.this.removeServer(var2);
                  });
               } catch (RealmsServiceException var2x) {
                  RealmsMainScreen.LOGGER.error("Couldn't configure world");
                  RealmsMainScreen.this.minecraft.execute(() -> {
                     RealmsMainScreen.this.minecraft.setScreen(new RealmsGenericErrorScreen(var2x, RealmsMainScreen.this));
                  });
               }

            }
         }).start();
      }

      this.minecraft.setScreen(this);
   }

   void removeServer(RealmsServer var1) {
      this.realmsServers = this.serverList.removeItem(var1);
      this.realmSelectionList.children().removeIf((var1x) -> {
         RealmsServer var2 = var1x.getServer();
         return var2 != null && var2.id == var1.id;
      });
      this.realmSelectionList.setSelected((RealmsMainScreen.Entry)null);
      this.updateButtonStates((RealmsServer)null);
      this.playButton.active = false;
   }

   void dismissNotification(UUID var1) {
      callRealmsClient((var1x) -> {
         var1x.notificationsDismiss(List.of(var1));
         return null;
      }, (var2) -> {
         this.notifications.removeIf((var1x) -> {
            return var1x.dismissable() && var1.equals(var1x.uuid());
         });
         this.refreshRealmsSelectionList();
      });
   }

   public void resetScreen() {
      if (this.realmSelectionList != null) {
         this.realmSelectionList.setSelected((RealmsMainScreen.Entry)null);
      }

   }

   public boolean keyPressed(int var1, int var2, int var3) {
      if (var1 == 256) {
         this.keyCombos.forEach(KeyCombo::reset);
         this.onClosePopup();
         return true;
      } else {
         return super.keyPressed(var1, var2, var3);
      }
   }

   void onClosePopup() {
      if (this.shouldShowPopup() && this.popupOpenedByUser) {
         this.popupOpenedByUser = false;
      } else {
         this.minecraft.setScreen(this.lastScreen);
      }

   }

   public boolean charTyped(char var1, int var2) {
      this.keyCombos.forEach((var1x) -> {
         var1x.keyPressed(var1);
      });
      return true;
   }

   public void render(GuiGraphics var1, int var2, int var3, float var4) {
      this.renderBackground(var1);
      this.realmSelectionList.render(var1, var2, var3, var4);
      var1.blit(LOGO_LOCATION, this.width / 2 - 64, 5, 0.0F, 0.0F, 128, 34, 128, 64);
      if (RealmsClient.currentEnvironment == RealmsClient.Environment.STAGE) {
         this.renderStage(var1);
      }

      if (RealmsClient.currentEnvironment == RealmsClient.Environment.LOCAL) {
         this.renderLocal(var1);
      }

      if (this.shouldShowPopup()) {
         var1.pose().pushPose();
         var1.pose().translate(0.0F, 0.0F, 100.0F);
         this.drawPopup(var1, var2, var3, var4);
         var1.pose().popPose();
      } else {
         if (this.showingPopup) {
            this.updateButtonStates((RealmsServer)null);
            if (!this.realmsSelectionListAdded) {
               this.addWidget(this.realmSelectionList);
               this.realmsSelectionListAdded = true;
            }

            this.playButton.active = this.shouldPlayButtonBeActive(this.getSelectedServer());
         }

         this.showingPopup = false;
      }

      super.render(var1, var2, var3, var4);
      if (this.trialsAvailable && !this.createdTrial && this.shouldShowPopup()) {
         boolean var5 = true;
         boolean var6 = true;
         byte var7 = 0;
         if ((Util.getMillis() / 800L & 1L) == 1L) {
            var7 = 8;
         }

         var1.pose().pushPose();
         var1.pose().translate(0.0F, 0.0F, 110.0F);
         var1.blit(TRIAL_ICON_LOCATION, this.createTrialButton.getX() + this.createTrialButton.getWidth() - 8 - 4, this.createTrialButton.getY() + this.createTrialButton.getHeight() / 2 - 4, 0.0F, (float)var7, 8, 8, 8, 16);
         var1.pose().popPose();
      }

   }

   public boolean mouseClicked(double var1, double var3, int var5) {
      if (this.isOutsidePopup(var1, var3) && this.popupOpenedByUser) {
         this.popupOpenedByUser = false;
         this.justClosedPopup = true;
         return true;
      } else {
         return super.mouseClicked(var1, var3, var5);
      }
   }

   private boolean isOutsidePopup(double var1, double var3) {
      int var5 = this.popupX0();
      int var6 = this.popupY0();
      return var1 < (double)(var5 - 5) || var1 > (double)(var5 + 315) || var3 < (double)(var6 - 5) || var3 > (double)(var6 + 171);
   }

   private void drawPopup(GuiGraphics var1, int var2, int var3, float var4) {
      int var5 = this.popupX0();
      int var6 = this.popupY0();
      if (!this.showingPopup) {
         this.carouselIndex = 0;
         this.carouselTick = 0;
         this.hasSwitchedCarouselImage = true;
         this.updateButtonStates((RealmsServer)null);
         if (this.realmsSelectionListAdded) {
            this.removeWidget(this.realmSelectionList);
            this.realmsSelectionListAdded = false;
         }

         this.minecraft.getNarrator().sayNow(POPUP_TEXT);
      }

      if (this.hasFetchedServers) {
         this.showingPopup = true;
      }

      var1.setColor(1.0F, 1.0F, 1.0F, 0.7F);
      RenderSystem.enableBlend();
      var1.blit(DARKEN_LOCATION, 0, 44, 0.0F, 0.0F, this.width, this.height - 44, 310, 166);
      RenderSystem.disableBlend();
      var1.setColor(1.0F, 1.0F, 1.0F, 1.0F);
      var1.blit(POPUP_LOCATION, var5, var6, 0.0F, 0.0F, 310, 166, 310, 166);
      if (!teaserImages.isEmpty()) {
         var1.blit((ResourceLocation)teaserImages.get(this.carouselIndex), var5 + 7, var6 + 7, 0.0F, 0.0F, 195, 152, 195, 152);
         if (this.carouselTick % 95 < 5) {
            if (!this.hasSwitchedCarouselImage) {
               this.carouselIndex = (this.carouselIndex + 1) % teaserImages.size();
               this.hasSwitchedCarouselImage = true;
            }
         } else {
            this.hasSwitchedCarouselImage = false;
         }
      }

      this.formattedPopup.renderLeftAlignedNoShadow(var1, this.width / 2 + 52, var6 + 7, 10, 16777215);
      this.createTrialButton.render(var1, var2, var3, var4);
      this.buyARealmButton.render(var1, var2, var3, var4);
      this.closeButton.render(var1, var2, var3, var4);
   }

   int popupX0() {
      return (this.width - 310) / 2;
   }

   int popupY0() {
      return this.height / 2 - 80;
   }

   public void play(@Nullable RealmsServer var1, Screen var2) {
      if (var1 != null) {
         try {
            if (!this.connectLock.tryLock(1L, TimeUnit.SECONDS)) {
               return;
            }

            if (this.connectLock.getHoldCount() > 1) {
               return;
            }
         } catch (InterruptedException var4) {
            return;
         }

         this.dontSetConnectedToRealms = true;
         this.minecraft.setScreen(new RealmsLongRunningMcoTaskScreen(var2, new GetServerDetailsTask(this, var2, var1, this.connectLock)));
      }

   }

   boolean isSelfOwnedServer(RealmsServer var1) {
      return var1.ownerUUID != null && var1.ownerUUID.equals(this.minecraft.getUser().getUuid());
   }

   private boolean isSelfOwnedNonExpiredServer(RealmsServer var1) {
      return this.isSelfOwnedServer(var1) && !var1.expired;
   }

   void drawExpired(GuiGraphics var1, int var2, int var3, int var4, int var5) {
      var1.blit(EXPIRED_ICON_LOCATION, var2, var3, 0.0F, 0.0F, 10, 28, 10, 28);
      if (var4 >= var2 && var4 <= var2 + 9 && var5 >= var3 && var5 <= var3 + 27 && var5 < this.height - 40 && var5 > 32 && !this.shouldShowPopup()) {
         this.setTooltipForNextRenderPass(SERVER_EXPIRED_TOOLTIP);
      }

   }

   void drawExpiring(GuiGraphics var1, int var2, int var3, int var4, int var5, int var6) {
      if (this.animTick % 20 < 10) {
         var1.blit(EXPIRES_SOON_ICON_LOCATION, var2, var3, 0.0F, 0.0F, 10, 28, 20, 28);
      } else {
         var1.blit(EXPIRES_SOON_ICON_LOCATION, var2, var3, 10.0F, 0.0F, 10, 28, 20, 28);
      }

      if (var4 >= var2 && var4 <= var2 + 9 && var5 >= var3 && var5 <= var3 + 27 && var5 < this.height - 40 && var5 > 32 && !this.shouldShowPopup()) {
         if (var6 <= 0) {
            this.setTooltipForNextRenderPass(SERVER_EXPIRES_SOON_TOOLTIP);
         } else if (var6 == 1) {
            this.setTooltipForNextRenderPass(SERVER_EXPIRES_IN_DAY_TOOLTIP);
         } else {
            this.setTooltipForNextRenderPass(Component.translatable("mco.selectServer.expires.days", var6));
         }
      }

   }

   void drawOpen(GuiGraphics var1, int var2, int var3, int var4, int var5) {
      var1.blit(ON_ICON_LOCATION, var2, var3, 0.0F, 0.0F, 10, 28, 10, 28);
      if (var4 >= var2 && var4 <= var2 + 9 && var5 >= var3 && var5 <= var3 + 27 && var5 < this.height - 40 && var5 > 32 && !this.shouldShowPopup()) {
         this.setTooltipForNextRenderPass(SERVER_OPEN_TOOLTIP);
      }

   }

   void drawClose(GuiGraphics var1, int var2, int var3, int var4, int var5) {
      var1.blit(OFF_ICON_LOCATION, var2, var3, 0.0F, 0.0F, 10, 28, 10, 28);
      if (var4 >= var2 && var4 <= var2 + 9 && var5 >= var3 && var5 <= var3 + 27 && var5 < this.height - 40 && var5 > 32 && !this.shouldShowPopup()) {
         this.setTooltipForNextRenderPass(SERVER_CLOSED_TOOLTIP);
      }

   }

   void renderNews(GuiGraphics var1, int var2, int var3, boolean var4, int var5, int var6, boolean var7, boolean var8) {
      boolean var9 = false;
      if (var2 >= var5 && var2 <= var5 + 20 && var3 >= var6 && var3 <= var6 + 20) {
         var9 = true;
      }

      if (!var8) {
         var1.setColor(0.5F, 0.5F, 0.5F, 1.0F);
      }

      boolean var10 = var8 && var7;
      float var11 = var10 ? 20.0F : 0.0F;
      var1.blit(NEWS_LOCATION, var5, var6, var11, 0.0F, 20, 20, 40, 20);
      if (var9 && var8) {
         this.setTooltipForNextRenderPass(NEWS_TOOLTIP);
      }

      var1.setColor(1.0F, 1.0F, 1.0F, 1.0F);
      if (var4 && var8) {
         int var12 = var9 ? 0 : (int)(Math.max(0.0F, Math.max(Mth.sin((float)(10 + this.animTick) * 0.57F), Mth.cos((float)this.animTick * 0.35F))) * -6.0F);
         var1.blit(INVITATION_ICONS_LOCATION, var5 + 10, var6 + 2 + var12, 40.0F, 0.0F, 8, 8, 48, 16);
      }

   }

   private void renderLocal(GuiGraphics var1) {
      String var2 = "LOCAL!";
      var1.pose().pushPose();
      var1.pose().translate((float)(this.width / 2 - 25), 20.0F, 0.0F);
      var1.pose().mulPose(Axis.ZP.rotationDegrees(-20.0F));
      var1.pose().scale(1.5F, 1.5F, 1.5F);
      var1.drawString(this.font, (String)"LOCAL!", 0, 0, 8388479, false);
      var1.pose().popPose();
   }

   private void renderStage(GuiGraphics var1) {
      String var2 = "STAGE!";
      var1.pose().pushPose();
      var1.pose().translate((float)(this.width / 2 - 25), 20.0F, 0.0F);
      var1.pose().mulPose(Axis.ZP.rotationDegrees(-20.0F));
      var1.pose().scale(1.5F, 1.5F, 1.5F);
      var1.drawString(this.font, (String)"STAGE!", 0, 0, -256, false);
      var1.pose().popPose();
   }

   public RealmsMainScreen newScreen() {
      RealmsMainScreen var1 = new RealmsMainScreen(this.lastScreen);
      var1.init(this.minecraft, this.width, this.height);
      return var1;
   }

   public static void updateTeaserImages(ResourceManager var0) {
      Set var1 = var0.listResources("textures/gui/images", (var0x) -> {
         return var0x.getPath().endsWith(".png");
      }).keySet();
      teaserImages = var1.stream().filter((var0x) -> {
         return var0x.getNamespace().equals("realms");
      }).toList();
   }

   static {
      SELECT_MINIGAME_PREFIX = Component.translatable("mco.selectServer.minigame").append(CommonComponents.SPACE);
      POPUP_TEXT = Component.translatable("mco.selectServer.popup");
      PLAY_TEXT = Component.translatable("mco.selectServer.play");
      LEAVE_SERVER_TEXT = Component.translatable("mco.selectServer.leave");
      CONFIGURE_SERVER_TEXT = Component.translatable("mco.selectServer.configure");
      SERVER_EXPIRED_TOOLTIP = Component.translatable("mco.selectServer.expired");
      SERVER_EXPIRES_SOON_TOOLTIP = Component.translatable("mco.selectServer.expires.soon");
      SERVER_EXPIRES_IN_DAY_TOOLTIP = Component.translatable("mco.selectServer.expires.day");
      SERVER_OPEN_TOOLTIP = Component.translatable("mco.selectServer.open");
      SERVER_CLOSED_TOOLTIP = Component.translatable("mco.selectServer.closed");
      NEWS_TOOLTIP = Component.translatable("mco.news");
      UNITIALIZED_WORLD_NARRATION = Component.translatable("gui.narrate.button", SERVER_UNITIALIZED_TEXT);
      TRIAL_TEXT = CommonComponents.joinLines((Collection)TRIAL_MESSAGE_LINES);
      teaserImages = ImmutableList.of();
      lastScrollYPosition = -1;
   }

   class RealmSelectionList extends RealmsObjectSelectionList<RealmsMainScreen.Entry> {
      public RealmSelectionList() {
         super(RealmsMainScreen.this.width, RealmsMainScreen.this.height, 44, RealmsMainScreen.this.height - 64, 36);
      }

      public void setSelected(@Nullable RealmsMainScreen.Entry var1) {
         super.setSelected(var1);
         if (var1 != null) {
            RealmsMainScreen.this.updateButtonStates(var1.getServer());
         } else {
            RealmsMainScreen.this.updateButtonStates((RealmsServer)null);
         }

      }

      public int getMaxPosition() {
         return this.getItemCount() * 36;
      }

      public int getRowWidth() {
         return 300;
      }

      // $FF: synthetic method
      // $FF: bridge method
      public void setSelected(@Nullable AbstractSelectionList.Entry var1) {
         this.setSelected((RealmsMainScreen.Entry)var1);
      }
   }

   private class PendingInvitesButton extends ImageButton {
      private static final Component TITLE = Component.translatable("mco.invites.title");
      private static final Tooltip NO_PENDING_INVITES = Tooltip.create(Component.translatable("mco.invites.nopending"));
      private static final Tooltip PENDING_INVITES = Tooltip.create(Component.translatable("mco.invites.pending"));
      private static final int WIDTH = 18;
      private static final int HEIGHT = 15;
      private static final int X_OFFSET = 10;
      private static final int INVITES_WIDTH = 8;
      private static final int INVITES_HEIGHT = 8;
      private static final int INVITES_OFFSET = 11;

      public PendingInvitesButton() {
         super(RealmsMainScreen.this.width / 2 + 64 + 10, 15, 18, 15, 0, 0, 15, RealmsMainScreen.INVITE_ICON_LOCATION, 18, 30, (var1x) -> {
            RealmsMainScreen.this.minecraft.setScreen(new RealmsPendingInvitesScreen(RealmsMainScreen.this.lastScreen, TITLE));
         }, TITLE);
         this.setTooltip(NO_PENDING_INVITES);
      }

      public void tick() {
         this.setTooltip(RealmsMainScreen.this.numberOfPendingInvites == 0 ? NO_PENDING_INVITES : PENDING_INVITES);
      }

      public void renderWidget(GuiGraphics var1, int var2, int var3, float var4) {
         super.renderWidget(var1, var2, var3, var4);
         this.drawInvitations(var1);
      }

      private void drawInvitations(GuiGraphics var1) {
         boolean var2 = this.active && RealmsMainScreen.this.numberOfPendingInvites != 0;
         if (var2) {
            int var3 = (Math.min(RealmsMainScreen.this.numberOfPendingInvites, 6) - 1) * 8;
            int var4 = (int)(Math.max(0.0F, Math.max(Mth.sin((float)(10 + RealmsMainScreen.this.animTick) * 0.57F), Mth.cos((float)RealmsMainScreen.this.animTick * 0.35F))) * -6.0F);
            float var5 = this.isHoveredOrFocused() ? 8.0F : 0.0F;
            var1.blit(RealmsMainScreen.INVITATION_ICONS_LOCATION, this.getX() + 11, this.getY() + var4, (float)var3, var5, 8, 8, 48, 16);
         }

      }
   }

   private class NewsButton extends Button {
      private static final int SIDE = 20;

      public NewsButton() {
         super(RealmsMainScreen.this.width - 115, 12, 20, 20, Component.translatable("mco.news"), (var1x) -> {
            if (RealmsMainScreen.this.newsLink != null) {
               ConfirmLinkScreen.confirmLinkNow(RealmsMainScreen.this.newsLink, RealmsMainScreen.this, true);
               if (RealmsMainScreen.this.hasUnreadNews) {
                  RealmsPersistence.RealmsPersistenceData var2 = RealmsPersistence.readFile();
                  var2.hasUnreadNews = false;
                  RealmsMainScreen.this.hasUnreadNews = false;
                  RealmsPersistence.writeFile(var2);
               }

            }
         }, DEFAULT_NARRATION);
      }

      public void renderWidget(GuiGraphics var1, int var2, int var3, float var4) {
         RealmsMainScreen.this.renderNews(var1, var2, var3, RealmsMainScreen.this.hasUnreadNews, this.getX(), this.getY(), this.isHoveredOrFocused(), this.active);
      }
   }

   private class CloseButton extends RealmsMainScreen.CrossButton {
      public CloseButton() {
         super(RealmsMainScreen.this.popupX0() + 4, RealmsMainScreen.this.popupY0() + 4, (var1x) -> {
            RealmsMainScreen.this.onClosePopup();
         }, Component.translatable("mco.selectServer.close"));
      }
   }

   interface RealmsCall<T> {
      T request(RealmsClient var1) throws RealmsServiceException;
   }

   class TrialEntry extends RealmsMainScreen.Entry {
      TrialEntry() {
         super();
      }

      public void render(GuiGraphics var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, boolean var9, float var10) {
         this.renderTrialItem(var1, var2, var4, var3, var7, var8);
      }

      public boolean mouseClicked(double var1, double var3, int var5) {
         RealmsMainScreen.this.popupOpenedByUser = true;
         return true;
      }

      private void renderTrialItem(GuiGraphics var1, int var2, int var3, int var4, int var5, int var6) {
         int var7 = var4 + 8;
         int var8 = 0;
         boolean var9 = false;
         if (var3 <= var5 && var5 <= (int)RealmsMainScreen.this.realmSelectionList.getScrollAmount() && var4 <= var6 && var6 <= var4 + 32) {
            var9 = true;
         }

         int var10 = 8388479;
         if (var9 && !RealmsMainScreen.this.shouldShowPopup()) {
            var10 = 6077788;
         }

         for(Iterator var11 = RealmsMainScreen.TRIAL_MESSAGE_LINES.iterator(); var11.hasNext(); var8 += 10) {
            Component var12 = (Component)var11.next();
            var1.drawCenteredString(RealmsMainScreen.this.font, var12, RealmsMainScreen.this.width / 2, var7 + var8, var10);
         }

      }

      public Component getNarration() {
         return RealmsMainScreen.TRIAL_TEXT;
      }
   }

   private class ServerEntry extends RealmsMainScreen.Entry {
      private static final int SKIN_HEAD_LARGE_WIDTH = 36;
      private final RealmsServer serverData;

      public ServerEntry(RealmsServer var2) {
         super();
         this.serverData = var2;
      }

      public void render(GuiGraphics var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, boolean var9, float var10) {
         this.renderMcoServerItem(this.serverData, var1, var4, var3, var7, var8);
      }

      public boolean mouseClicked(double var1, double var3, int var5) {
         if (this.serverData.state == RealmsServer.State.UNINITIALIZED) {
            RealmsMainScreen.this.minecraft.setScreen(new RealmsCreateRealmScreen(this.serverData, RealmsMainScreen.this));
         } else if (RealmsMainScreen.this.shouldPlayButtonBeActive(this.serverData)) {
            if (Util.getMillis() - RealmsMainScreen.this.lastClickTime < 250L && this.isFocused()) {
               RealmsMainScreen.this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI((Holder)SoundEvents.UI_BUTTON_CLICK, 1.0F));
               RealmsMainScreen.this.play(this.serverData, RealmsMainScreen.this);
            }

            RealmsMainScreen.this.lastClickTime = Util.getMillis();
         }

         return true;
      }

      public boolean keyPressed(int var1, int var2, int var3) {
         if (CommonInputs.selected(var1) && RealmsMainScreen.this.shouldPlayButtonBeActive(this.serverData)) {
            RealmsMainScreen.this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI((Holder)SoundEvents.UI_BUTTON_CLICK, 1.0F));
            RealmsMainScreen.this.play(this.serverData, RealmsMainScreen.this);
            return true;
         } else {
            return super.keyPressed(var1, var2, var3);
         }
      }

      private void renderMcoServerItem(RealmsServer var1, GuiGraphics var2, int var3, int var4, int var5, int var6) {
         this.renderLegacy(var1, var2, var3 + 36, var4, var5, var6);
      }

      private void renderLegacy(RealmsServer var1, GuiGraphics var2, int var3, int var4, int var5, int var6) {
         if (var1.state == RealmsServer.State.UNINITIALIZED) {
            var2.blit(RealmsMainScreen.WORLDICON_LOCATION, var3 + 10, var4 + 6, 0.0F, 0.0F, 40, 20, 40, 20);
            float var11 = 0.5F + (1.0F + Mth.sin((float)RealmsMainScreen.this.animTick * 0.25F)) * 0.25F;
            int var12 = -16777216 | (int)(127.0F * var11) << 16 | (int)(255.0F * var11) << 8 | (int)(127.0F * var11);
            var2.drawCenteredString(RealmsMainScreen.this.font, RealmsMainScreen.SERVER_UNITIALIZED_TEXT, var3 + 10 + 40 + 75, var4 + 12, var12);
         } else {
            boolean var7 = true;
            boolean var8 = true;
            this.renderStatusLights(var1, var2, var3, var4, var5, var6, 225, 2);
            if (!"0".equals(var1.serverPing.nrOfPlayers)) {
               String var9 = ChatFormatting.GRAY + var1.serverPing.nrOfPlayers;
               var2.drawString(RealmsMainScreen.this.font, var9, var3 + 207 - RealmsMainScreen.this.font.width(var9), var4 + 3, 8421504, false);
               if (var5 >= var3 + 207 - RealmsMainScreen.this.font.width(var9) && var5 <= var3 + 207 && var6 >= var4 + 1 && var6 <= var4 + 10 && var6 < RealmsMainScreen.this.height - 40 && var6 > 32 && !RealmsMainScreen.this.shouldShowPopup()) {
                  RealmsMainScreen.this.setTooltipForNextRenderPass(Component.literal(var1.serverPing.playerList));
               }
            }

            int var10;
            if (RealmsMainScreen.this.isSelfOwnedServer(var1) && var1.expired) {
               Component var14 = var1.expiredTrial ? RealmsMainScreen.TRIAL_EXPIRED_TEXT : RealmsMainScreen.SUBSCRIPTION_EXPIRED_TEXT;
               var10 = var4 + 11 + 5;
               var2.drawString(RealmsMainScreen.this.font, var14, var3 + 2, var10 + 1, 15553363, false);
            } else {
               if (var1.worldType == RealmsServer.WorldType.MINIGAME) {
                  int var13 = 13413468;
                  var10 = RealmsMainScreen.this.font.width((FormattedText)RealmsMainScreen.SELECT_MINIGAME_PREFIX);
                  var2.drawString(RealmsMainScreen.this.font, RealmsMainScreen.SELECT_MINIGAME_PREFIX, var3 + 2, var4 + 12, 13413468, false);
                  var2.drawString(RealmsMainScreen.this.font, var1.getMinigameName(), var3 + 2 + var10, var4 + 12, 7105644, false);
               } else {
                  var2.drawString(RealmsMainScreen.this.font, var1.getDescription(), var3 + 2, var4 + 12, 7105644, false);
               }

               if (!RealmsMainScreen.this.isSelfOwnedServer(var1)) {
                  var2.drawString(RealmsMainScreen.this.font, var1.owner, var3 + 2, var4 + 12 + 11, 5000268, false);
               }
            }

            var2.drawString(RealmsMainScreen.this.font, var1.getName(), var3 + 2, var4 + 1, 16777215, false);
            RealmsUtil.renderPlayerFace(var2, var3 - 36, var4, 32, var1.ownerUUID);
         }
      }

      private void renderStatusLights(RealmsServer var1, GuiGraphics var2, int var3, int var4, int var5, int var6, int var7, int var8) {
         int var9 = var3 + var7 + 22;
         if (var1.expired) {
            RealmsMainScreen.this.drawExpired(var2, var9, var4 + var8, var5, var6);
         } else if (var1.state == RealmsServer.State.CLOSED) {
            RealmsMainScreen.this.drawClose(var2, var9, var4 + var8, var5, var6);
         } else if (RealmsMainScreen.this.isSelfOwnedServer(var1) && var1.daysLeft < 7) {
            RealmsMainScreen.this.drawExpiring(var2, var9, var4 + var8, var5, var6, var1.daysLeft);
         } else if (var1.state == RealmsServer.State.OPEN) {
            RealmsMainScreen.this.drawOpen(var2, var9, var4 + var8, var5, var6);
         }

      }

      public Component getNarration() {
         return (Component)(this.serverData.state == RealmsServer.State.UNINITIALIZED ? RealmsMainScreen.UNITIALIZED_WORLD_NARRATION : Component.translatable("narrator.select", this.serverData.name));
      }

      @Nullable
      public RealmsServer getServer() {
         return this.serverData;
      }
   }

   abstract class Entry extends ObjectSelectionList.Entry<RealmsMainScreen.Entry> {
      Entry() {
      }

      @Nullable
      public RealmsServer getServer() {
         return null;
      }
   }

   class NotificationMessageEntry extends RealmsMainScreen.Entry {
      private static final int SIDE_MARGINS = 40;
      private static final int ITEM_HEIGHT = 36;
      private static final int OUTLINE_COLOR = -12303292;
      private final Component text;
      private final List<AbstractWidget> children = new ArrayList();
      @Nullable
      private final RealmsMainScreen.CrossButton dismissButton;
      private final MultiLineTextWidget textWidget;
      private final GridLayout gridLayout;
      private final FrameLayout textFrame;
      private int lastEntryWidth = -1;

      public NotificationMessageEntry(Component var2, RealmsNotification var3) {
         super();
         this.text = var2;
         this.gridLayout = new GridLayout();
         boolean var4 = true;
         this.gridLayout.addChild(new ImageWidget(20, 20, RealmsMainScreen.INFO_ICON_LOCATION), 0, 0, this.gridLayout.newCellSettings().padding(7, 7, 0, 0));
         this.gridLayout.addChild(SpacerElement.width(40), 0, 0);
         GridLayout var10001 = this.gridLayout;
         Objects.requireNonNull(RealmsMainScreen.this.font);
         this.textFrame = (FrameLayout)var10001.addChild(new FrameLayout(0, 9 * 3), 0, 1, this.gridLayout.newCellSettings().paddingTop(7));
         this.textWidget = (MultiLineTextWidget)this.textFrame.addChild((new MultiLineTextWidget(var2, RealmsMainScreen.this.font)).setCentered(true).setMaxRows(3), this.textFrame.newChildLayoutSettings().alignHorizontallyCenter().alignVerticallyTop());
         this.gridLayout.addChild(SpacerElement.width(40), 0, 2);
         if (var3.dismissable()) {
            this.dismissButton = (RealmsMainScreen.CrossButton)this.gridLayout.addChild(new RealmsMainScreen.CrossButton((var2x) -> {
               RealmsMainScreen.this.dismissNotification(var3.uuid());
            }, Component.translatable("mco.notification.dismiss")), 0, 2, this.gridLayout.newCellSettings().alignHorizontallyRight().padding(0, 7, 7, 0));
         } else {
            this.dismissButton = null;
         }

         GridLayout var10000 = this.gridLayout;
         List var5 = this.children;
         Objects.requireNonNull(var5);
         var10000.visitWidgets(var5::add);
      }

      public boolean keyPressed(int var1, int var2, int var3) {
         return this.dismissButton != null && this.dismissButton.keyPressed(var1, var2, var3) ? true : super.keyPressed(var1, var2, var3);
      }

      private void updateEntryWidth(int var1) {
         if (this.lastEntryWidth != var1) {
            this.refreshLayout(var1);
            this.lastEntryWidth = var1;
         }

      }

      private void refreshLayout(int var1) {
         int var2 = var1 - 80;
         this.textFrame.setMinWidth(var2);
         this.textWidget.setMaxWidth(var2);
         this.gridLayout.arrangeElements();
      }

      public void renderBack(GuiGraphics var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, boolean var9, float var10) {
         super.renderBack(var1, var2, var3, var4, var5, var6, var7, var8, var9, var10);
         var1.renderOutline(var4 - 2, var3 - 2, var5, 70, -12303292);
      }

      public void render(GuiGraphics var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, boolean var9, float var10) {
         this.gridLayout.setPosition(var4, var3);
         this.updateEntryWidth(var5 - 4);
         this.children.forEach((var4x) -> {
            var4x.render(var1, var7, var8, var10);
         });
      }

      public boolean mouseClicked(double var1, double var3, int var5) {
         if (this.dismissButton != null) {
            this.dismissButton.mouseClicked(var1, var3, var5);
         }

         return true;
      }

      public Component getNarration() {
         return this.text;
      }
   }

   class ButtonEntry extends RealmsMainScreen.Entry {
      private final Button button;
      private final int xPos;

      public ButtonEntry(Button var2) {
         super();
         this.xPos = RealmsMainScreen.this.width / 2 - 75;
         this.button = var2;
      }

      public boolean mouseClicked(double var1, double var3, int var5) {
         this.button.mouseClicked(var1, var3, var5);
         return true;
      }

      public boolean keyPressed(int var1, int var2, int var3) {
         return this.button.keyPressed(var1, var2, var3) ? true : super.keyPressed(var1, var2, var3);
      }

      public void render(GuiGraphics var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, boolean var9, float var10) {
         this.button.setPosition(this.xPos, var3 + 4);
         this.button.render(var1, var7, var8, var10);
      }

      public Component getNarration() {
         return this.button.getMessage();
      }
   }

   private static class CrossButton extends Button {
      protected CrossButton(Button.OnPress var1, Component var2) {
         this(0, 0, var1, var2);
      }

      protected CrossButton(int var1, int var2, Button.OnPress var3, Component var4) {
         super(var1, var2, 14, 14, var4, var3, DEFAULT_NARRATION);
         this.setTooltip(Tooltip.create(var4));
      }

      public void renderWidget(GuiGraphics var1, int var2, int var3, float var4) {
         float var5 = this.isHoveredOrFocused() ? 14.0F : 0.0F;
         var1.blit(RealmsMainScreen.CROSS_ICON_LOCATION, this.getX(), this.getY(), 0.0F, var5, 14, 14, 14, 28);
      }
   }
}
