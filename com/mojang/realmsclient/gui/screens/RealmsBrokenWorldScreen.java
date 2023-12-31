package com.mojang.realmsclient.gui.screens;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsWorldOptions;
import com.mojang.realmsclient.dto.WorldDownload;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.RealmsWorldSlotButton;
import com.mojang.realmsclient.util.RealmsTextureManager;
import com.mojang.realmsclient.util.task.OpenServerTask;
import com.mojang.realmsclient.util.task.SwitchSlotTask;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.slf4j.Logger;

public class RealmsBrokenWorldScreen extends RealmsScreen {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int DEFAULT_BUTTON_WIDTH = 80;
   private final Screen lastScreen;
   private final RealmsMainScreen mainScreen;
   @Nullable
   private RealmsServer serverData;
   private final long serverId;
   private final Component[] message = new Component[]{Component.translatable("mco.brokenworld.message.line1"), Component.translatable("mco.brokenworld.message.line2")};
   private int leftX;
   private int rightX;
   private final List<Integer> slotsThatHasBeenDownloaded = Lists.newArrayList();
   private int animTick;

   public RealmsBrokenWorldScreen(Screen var1, RealmsMainScreen var2, long var3, boolean var5) {
      super(var5 ? Component.translatable("mco.brokenworld.minigame.title") : Component.translatable("mco.brokenworld.title"));
      this.lastScreen = var1;
      this.mainScreen = var2;
      this.serverId = var3;
   }

   public void init() {
      this.leftX = this.width / 2 - 150;
      this.rightX = this.width / 2 + 190;
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, (var1) -> {
         this.backButtonClicked();
      }).bounds(this.rightX - 80 + 8, row(13) - 5, 70, 20).build());
      if (this.serverData == null) {
         this.fetchServerData(this.serverId);
      } else {
         this.addButtons();
      }

   }

   public Component getNarrationMessage() {
      return ComponentUtils.formatList((Collection)Stream.concat(Stream.of(this.title), Stream.of(this.message)).collect(Collectors.toList()), CommonComponents.SPACE);
   }

   private void addButtons() {
      Iterator var1 = this.serverData.slots.entrySet().iterator();

      while(var1.hasNext()) {
         Entry var2 = (Entry)var1.next();
         int var3 = (Integer)var2.getKey();
         boolean var4 = var3 != this.serverData.activeSlot || this.serverData.worldType == RealmsServer.WorldType.MINIGAME;
         Button var5;
         if (var4) {
            var5 = Button.builder(Component.translatable("mco.brokenworld.play"), (var2x) -> {
               if (((RealmsWorldOptions)this.serverData.slots.get(var3)).empty) {
                  RealmsResetWorldScreen var3x = new RealmsResetWorldScreen(this, this.serverData, Component.translatable("mco.configure.world.switch.slot"), Component.translatable("mco.configure.world.switch.slot.subtitle"), 10526880, CommonComponents.GUI_CANCEL, this::doSwitchOrReset, () -> {
                     this.minecraft.setScreen(this);
                     this.doSwitchOrReset();
                  });
                  var3x.setSlot(var3);
                  var3x.setResetTitle(Component.translatable("mco.create.world.reset.title"));
                  this.minecraft.setScreen(var3x);
               } else {
                  this.minecraft.setScreen(new RealmsLongRunningMcoTaskScreen(this.lastScreen, new SwitchSlotTask(this.serverData.id, var3, this::doSwitchOrReset)));
               }

            }).bounds(this.getFramePositionX(var3), row(8), 80, 20).build();
         } else {
            var5 = Button.builder(Component.translatable("mco.brokenworld.download"), (var2x) -> {
               MutableComponent var3x = Component.translatable("mco.configure.world.restore.download.question.line1");
               MutableComponent var4 = Component.translatable("mco.configure.world.restore.download.question.line2");
               this.minecraft.setScreen(new RealmsLongConfirmationScreen((var2) -> {
                  if (var2) {
                     this.downloadWorld(var3);
                  } else {
                     this.minecraft.setScreen(this);
                  }

               }, RealmsLongConfirmationScreen.Type.INFO, var3x, var4, true));
            }).bounds(this.getFramePositionX(var3), row(8), 80, 20).build();
         }

         if (this.slotsThatHasBeenDownloaded.contains(var3)) {
            var5.active = false;
            var5.setMessage(Component.translatable("mco.brokenworld.downloaded"));
         }

         this.addRenderableWidget(var5);
         this.addRenderableWidget(Button.builder(Component.translatable("mco.brokenworld.reset"), (var2x) -> {
            RealmsResetWorldScreen var3x = new RealmsResetWorldScreen(this, this.serverData, this::doSwitchOrReset, () -> {
               this.minecraft.setScreen(this);
               this.doSwitchOrReset();
            });
            if (var3 != this.serverData.activeSlot || this.serverData.worldType == RealmsServer.WorldType.MINIGAME) {
               var3x.setSlot(var3);
            }

            this.minecraft.setScreen(var3x);
         }).bounds(this.getFramePositionX(var3), row(10), 80, 20).build());
      }

   }

   public void tick() {
      ++this.animTick;
   }

   public void render(GuiGraphics var1, int var2, int var3, float var4) {
      this.renderBackground(var1);
      super.render(var1, var2, var3, var4);
      var1.drawCenteredString(this.font, (Component)this.title, this.width / 2, 17, 16777215);

      for(int var5 = 0; var5 < this.message.length; ++var5) {
         var1.drawCenteredString(this.font, this.message[var5], this.width / 2, row(-1) + 3 + var5 * 12, 10526880);
      }

      if (this.serverData != null) {
         Iterator var7 = this.serverData.slots.entrySet().iterator();

         while(true) {
            while(var7.hasNext()) {
               Entry var6 = (Entry)var7.next();
               if (((RealmsWorldOptions)var6.getValue()).templateImage != null && ((RealmsWorldOptions)var6.getValue()).templateId != -1L) {
                  this.drawSlotFrame(var1, this.getFramePositionX((Integer)var6.getKey()), row(1) + 5, var2, var3, this.serverData.activeSlot == (Integer)var6.getKey() && !this.isMinigame(), ((RealmsWorldOptions)var6.getValue()).getSlotName((Integer)var6.getKey()), (Integer)var6.getKey(), ((RealmsWorldOptions)var6.getValue()).templateId, ((RealmsWorldOptions)var6.getValue()).templateImage, ((RealmsWorldOptions)var6.getValue()).empty);
               } else {
                  this.drawSlotFrame(var1, this.getFramePositionX((Integer)var6.getKey()), row(1) + 5, var2, var3, this.serverData.activeSlot == (Integer)var6.getKey() && !this.isMinigame(), ((RealmsWorldOptions)var6.getValue()).getSlotName((Integer)var6.getKey()), (Integer)var6.getKey(), -1L, (String)null, ((RealmsWorldOptions)var6.getValue()).empty);
               }
            }

            return;
         }
      }
   }

   private int getFramePositionX(int var1) {
      return this.leftX + (var1 - 1) * 110;
   }

   public boolean keyPressed(int var1, int var2, int var3) {
      if (var1 == 256) {
         this.backButtonClicked();
         return true;
      } else {
         return super.keyPressed(var1, var2, var3);
      }
   }

   private void backButtonClicked() {
      this.minecraft.setScreen(this.lastScreen);
   }

   private void fetchServerData(long var1) {
      (new Thread(() -> {
         RealmsClient var3 = RealmsClient.create();

         try {
            this.serverData = var3.getOwnWorld(var1);
            this.addButtons();
         } catch (RealmsServiceException var5) {
            LOGGER.error("Couldn't get own world");
            this.minecraft.setScreen(new RealmsGenericErrorScreen(Component.nullToEmpty(var5.getMessage()), this.lastScreen));
         }

      })).start();
   }

   public void doSwitchOrReset() {
      (new Thread(() -> {
         RealmsClient var1 = RealmsClient.create();
         if (this.serverData.state == RealmsServer.State.CLOSED) {
            this.minecraft.execute(() -> {
               this.minecraft.setScreen(new RealmsLongRunningMcoTaskScreen(this, new OpenServerTask(this.serverData, this, this.mainScreen, true, this.minecraft)));
            });
         } else {
            try {
               RealmsServer var2 = var1.getOwnWorld(this.serverId);
               this.minecraft.execute(() -> {
                  this.mainScreen.newScreen().play(var2, this);
               });
            } catch (RealmsServiceException var3) {
               LOGGER.error("Couldn't get own world");
               this.minecraft.execute(() -> {
                  this.minecraft.setScreen(this.lastScreen);
               });
            }
         }

      })).start();
   }

   private void downloadWorld(int var1) {
      RealmsClient var2 = RealmsClient.create();

      try {
         WorldDownload var3 = var2.requestDownloadInfo(this.serverData.id, var1);
         RealmsDownloadLatestWorldScreen var4 = new RealmsDownloadLatestWorldScreen(this, var3, this.serverData.getWorldName(var1), (var2x) -> {
            if (var2x) {
               this.slotsThatHasBeenDownloaded.add(var1);
               this.clearWidgets();
               this.addButtons();
            } else {
               this.minecraft.setScreen(this);
            }

         });
         this.minecraft.setScreen(var4);
      } catch (RealmsServiceException var5) {
         LOGGER.error("Couldn't download world data");
         this.minecraft.setScreen(new RealmsGenericErrorScreen(var5, this));
      }

   }

   private boolean isMinigame() {
      return this.serverData != null && this.serverData.worldType == RealmsServer.WorldType.MINIGAME;
   }

   private void drawSlotFrame(GuiGraphics var1, int var2, int var3, int var4, int var5, boolean var6, String var7, int var8, long var9, @Nullable String var11, boolean var12) {
      ResourceLocation var13;
      if (var12) {
         var13 = RealmsWorldSlotButton.EMPTY_SLOT_LOCATION;
      } else if (var11 != null && var9 != -1L) {
         var13 = RealmsTextureManager.worldTemplate(String.valueOf(var9), var11);
      } else if (var8 == 1) {
         var13 = RealmsWorldSlotButton.DEFAULT_WORLD_SLOT_1;
      } else if (var8 == 2) {
         var13 = RealmsWorldSlotButton.DEFAULT_WORLD_SLOT_2;
      } else if (var8 == 3) {
         var13 = RealmsWorldSlotButton.DEFAULT_WORLD_SLOT_3;
      } else {
         var13 = RealmsTextureManager.worldTemplate(String.valueOf(this.serverData.minigameId), this.serverData.minigameImage);
      }

      if (!var6) {
         var1.setColor(0.56F, 0.56F, 0.56F, 1.0F);
      } else if (var6) {
         float var14 = 0.9F + 0.1F * Mth.cos((float)this.animTick * 0.2F);
         var1.setColor(var14, var14, var14, 1.0F);
      }

      var1.blit(var13, var2 + 3, var3 + 3, 0.0F, 0.0F, 74, 74, 74, 74);
      if (var6) {
         var1.setColor(1.0F, 1.0F, 1.0F, 1.0F);
      } else {
         var1.setColor(0.56F, 0.56F, 0.56F, 1.0F);
      }

      var1.blit(RealmsWorldSlotButton.SLOT_FRAME_LOCATION, var2, var3, 0.0F, 0.0F, 80, 80, 80, 80);
      var1.drawCenteredString(this.font, var7, var2 + 40, var3 + 66, 16777215);
      var1.setColor(1.0F, 1.0F, 1.0F, 1.0F);
   }
}
