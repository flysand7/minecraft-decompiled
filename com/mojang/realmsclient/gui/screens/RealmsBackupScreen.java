package com.mojang.realmsclient.gui.screens;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.Backup;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsWorldOptions;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.util.RealmsUtil;
import com.mojang.realmsclient.util.task.DownloadTask;
import com.mojang.realmsclient.util.task.RestoreTask;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.realms.RealmsObjectSelectionList;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

public class RealmsBackupScreen extends RealmsScreen {
   static final Logger LOGGER = LogUtils.getLogger();
   static final ResourceLocation PLUS_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/plus_icon.png");
   static final ResourceLocation RESTORE_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/restore_icon.png");
   static final Component RESTORE_TOOLTIP = Component.translatable("mco.backup.button.restore");
   static final Component HAS_CHANGES_TOOLTIP = Component.translatable("mco.backup.changes.tooltip");
   private static final Component TITLE = Component.translatable("mco.configure.world.backup");
   private static final Component NO_BACKUPS_LABEL = Component.translatable("mco.backup.nobackups");
   private final RealmsConfigureWorldScreen lastScreen;
   List<Backup> backups = Collections.emptyList();
   RealmsBackupScreen.BackupObjectSelectionList backupObjectSelectionList;
   int selectedBackup = -1;
   private final int slotId;
   private Button downloadButton;
   private Button restoreButton;
   private Button changesButton;
   Boolean noBackups = false;
   final RealmsServer serverData;
   private static final String UPLOADED_KEY = "uploaded";

   public RealmsBackupScreen(RealmsConfigureWorldScreen var1, RealmsServer var2, int var3) {
      super(Component.translatable("mco.configure.world.backup"));
      this.lastScreen = var1;
      this.serverData = var2;
      this.slotId = var3;
   }

   public void init() {
      this.backupObjectSelectionList = new RealmsBackupScreen.BackupObjectSelectionList();
      (new Thread("Realms-fetch-backups") {
         public void run() {
            RealmsClient var1 = RealmsClient.create();

            try {
               List var2 = var1.backupsFor(RealmsBackupScreen.this.serverData.id).backups;
               RealmsBackupScreen.this.minecraft.execute(() -> {
                  RealmsBackupScreen.this.backups = var2;
                  RealmsBackupScreen.this.noBackups = RealmsBackupScreen.this.backups.isEmpty();
                  RealmsBackupScreen.this.backupObjectSelectionList.clear();
                  Iterator var2x = RealmsBackupScreen.this.backups.iterator();

                  while(var2x.hasNext()) {
                     Backup var3 = (Backup)var2x.next();
                     RealmsBackupScreen.this.backupObjectSelectionList.addEntry(var3);
                  }

               });
            } catch (RealmsServiceException var3) {
               RealmsBackupScreen.LOGGER.error("Couldn't request backups", var3);
            }

         }
      }).start();
      this.downloadButton = (Button)this.addRenderableWidget(Button.builder(Component.translatable("mco.backup.button.download"), (var1) -> {
         this.downloadClicked();
      }).bounds(this.width - 135, row(1), 120, 20).build());
      this.restoreButton = (Button)this.addRenderableWidget(Button.builder(Component.translatable("mco.backup.button.restore"), (var1) -> {
         this.restoreClicked(this.selectedBackup);
      }).bounds(this.width - 135, row(3), 120, 20).build());
      this.changesButton = (Button)this.addRenderableWidget(Button.builder(Component.translatable("mco.backup.changes.tooltip"), (var1) -> {
         this.minecraft.setScreen(new RealmsBackupInfoScreen(this, (Backup)this.backups.get(this.selectedBackup)));
         this.selectedBackup = -1;
      }).bounds(this.width - 135, row(5), 120, 20).build());
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, (var1) -> {
         this.minecraft.setScreen(this.lastScreen);
      }).bounds(this.width - 100, this.height - 35, 85, 20).build());
      this.addWidget(this.backupObjectSelectionList);
      this.magicalSpecialHackyFocus(this.backupObjectSelectionList);
      this.updateButtonStates();
   }

   void updateButtonStates() {
      this.restoreButton.visible = this.shouldRestoreButtonBeVisible();
      this.changesButton.visible = this.shouldChangesButtonBeVisible();
   }

   private boolean shouldChangesButtonBeVisible() {
      if (this.selectedBackup == -1) {
         return false;
      } else {
         return !((Backup)this.backups.get(this.selectedBackup)).changeList.isEmpty();
      }
   }

   private boolean shouldRestoreButtonBeVisible() {
      if (this.selectedBackup == -1) {
         return false;
      } else {
         return !this.serverData.expired;
      }
   }

   public boolean keyPressed(int var1, int var2, int var3) {
      if (var1 == 256) {
         this.minecraft.setScreen(this.lastScreen);
         return true;
      } else {
         return super.keyPressed(var1, var2, var3);
      }
   }

   void restoreClicked(int var1) {
      if (var1 >= 0 && var1 < this.backups.size() && !this.serverData.expired) {
         this.selectedBackup = var1;
         Date var2 = ((Backup)this.backups.get(var1)).lastModifiedDate;
         String var3 = DateFormat.getDateTimeInstance(3, 3).format(var2);
         Component var4 = RealmsUtil.convertToAgePresentationFromInstant(var2);
         MutableComponent var5 = Component.translatable("mco.configure.world.restore.question.line1", var3, var4);
         MutableComponent var6 = Component.translatable("mco.configure.world.restore.question.line2");
         this.minecraft.setScreen(new RealmsLongConfirmationScreen((var1x) -> {
            if (var1x) {
               this.restore();
            } else {
               this.selectedBackup = -1;
               this.minecraft.setScreen(this);
            }

         }, RealmsLongConfirmationScreen.Type.WARNING, var5, var6, true));
      }

   }

   private void downloadClicked() {
      MutableComponent var1 = Component.translatable("mco.configure.world.restore.download.question.line1");
      MutableComponent var2 = Component.translatable("mco.configure.world.restore.download.question.line2");
      this.minecraft.setScreen(new RealmsLongConfirmationScreen((var1x) -> {
         if (var1x) {
            this.downloadWorldData();
         } else {
            this.minecraft.setScreen(this);
         }

      }, RealmsLongConfirmationScreen.Type.INFO, var1, var2, true));
   }

   private void downloadWorldData() {
      Minecraft var10000 = this.minecraft;
      RealmsConfigureWorldScreen var10003 = this.lastScreen.getNewScreen();
      String var10008 = this.serverData.name;
      var10000.setScreen(new RealmsLongRunningMcoTaskScreen(var10003, new DownloadTask(this.serverData.id, this.slotId, var10008 + " (" + ((RealmsWorldOptions)this.serverData.slots.get(this.serverData.activeSlot)).getSlotName(this.serverData.activeSlot) + ")", this)));
   }

   private void restore() {
      Backup var1 = (Backup)this.backups.get(this.selectedBackup);
      this.selectedBackup = -1;
      this.minecraft.setScreen(new RealmsLongRunningMcoTaskScreen(this.lastScreen.getNewScreen(), new RestoreTask(var1, this.serverData.id, this.lastScreen)));
   }

   public void render(GuiGraphics var1, int var2, int var3, float var4) {
      this.renderBackground(var1);
      this.backupObjectSelectionList.render(var1, var2, var3, var4);
      var1.drawCenteredString(this.font, (Component)this.title, this.width / 2, 12, 16777215);
      var1.drawString(this.font, (Component)TITLE, (this.width - 150) / 2 - 90, 20, 10526880, false);
      if (this.noBackups) {
         var1.drawString(this.font, (Component)NO_BACKUPS_LABEL, 20, this.height / 2 - 10, 16777215, false);
      }

      this.downloadButton.active = !this.noBackups;
      super.render(var1, var2, var3, var4);
   }

   private class BackupObjectSelectionList extends RealmsObjectSelectionList<RealmsBackupScreen.Entry> {
      public BackupObjectSelectionList() {
         super(RealmsBackupScreen.this.width - 150, RealmsBackupScreen.this.height, 32, RealmsBackupScreen.this.height - 15, 36);
      }

      public void addEntry(Backup var1) {
         this.addEntry(RealmsBackupScreen.this.new Entry(var1));
      }

      public int getRowWidth() {
         return (int)((double)this.width * 0.93D);
      }

      public int getMaxPosition() {
         return this.getItemCount() * 36;
      }

      public void renderBackground(GuiGraphics var1) {
         RealmsBackupScreen.this.renderBackground(var1);
      }

      public int getScrollbarPosition() {
         return this.width - 5;
      }

      public void selectItem(int var1) {
         super.selectItem(var1);
         this.selectInviteListItem(var1);
      }

      public void selectInviteListItem(int var1) {
         RealmsBackupScreen.this.selectedBackup = var1;
         RealmsBackupScreen.this.updateButtonStates();
      }

      public void setSelected(@Nullable RealmsBackupScreen.Entry var1) {
         super.setSelected(var1);
         RealmsBackupScreen.this.selectedBackup = this.children().indexOf(var1);
         RealmsBackupScreen.this.updateButtonStates();
      }

      // $FF: synthetic method
      // $FF: bridge method
      public void setSelected(@Nullable AbstractSelectionList.Entry var1) {
         this.setSelected((RealmsBackupScreen.Entry)var1);
      }
   }

   private class Entry extends ObjectSelectionList.Entry<RealmsBackupScreen.Entry> {
      private static final int Y_PADDING = 2;
      private static final int X_PADDING = 7;
      private final Backup backup;
      private final List<AbstractWidget> children = new ArrayList();
      @Nullable
      private ImageButton restoreButton;
      @Nullable
      private ImageButton changesButton;

      public Entry(Backup var2) {
         this.backup = var2;
         this.populateChangeList(var2);
         if (!var2.changeList.isEmpty()) {
            this.addChangesButton();
         }

         if (!RealmsBackupScreen.this.serverData.expired) {
            this.addRestoreButton();
         }

      }

      private void populateChangeList(Backup var1) {
         int var2 = RealmsBackupScreen.this.backups.indexOf(var1);
         if (var2 != RealmsBackupScreen.this.backups.size() - 1) {
            Backup var3 = (Backup)RealmsBackupScreen.this.backups.get(var2 + 1);
            Iterator var4 = var1.metadata.keySet().iterator();

            while(true) {
               while(var4.hasNext()) {
                  String var5 = (String)var4.next();
                  if (!var5.contains("uploaded") && var3.metadata.containsKey(var5)) {
                     if (!((String)var1.metadata.get(var5)).equals(var3.metadata.get(var5))) {
                        this.addToChangeList(var5);
                     }
                  } else {
                     this.addToChangeList(var5);
                  }
               }

               return;
            }
         }
      }

      private void addToChangeList(String var1) {
         if (var1.contains("uploaded")) {
            String var2 = DateFormat.getDateTimeInstance(3, 3).format(this.backup.lastModifiedDate);
            this.backup.changeList.put(var1, var2);
            this.backup.setUploadedVersion(true);
         } else {
            this.backup.changeList.put(var1, (String)this.backup.metadata.get(var1));
         }

      }

      private void addChangesButton() {
         boolean var1 = true;
         boolean var2 = true;
         int var3 = RealmsBackupScreen.this.backupObjectSelectionList.getRowRight() - 9 - 28;
         int var4 = RealmsBackupScreen.this.backupObjectSelectionList.getRowTop(RealmsBackupScreen.this.backups.indexOf(this.backup)) + 2;
         this.changesButton = new ImageButton(var3, var4, 9, 9, 0, 0, 9, RealmsBackupScreen.PLUS_ICON_LOCATION, 9, 18, (var1x) -> {
            RealmsBackupScreen.this.minecraft.setScreen(new RealmsBackupInfoScreen(RealmsBackupScreen.this, this.backup));
         });
         this.changesButton.setTooltip(Tooltip.create(RealmsBackupScreen.HAS_CHANGES_TOOLTIP));
         this.children.add(this.changesButton);
      }

      private void addRestoreButton() {
         boolean var1 = true;
         boolean var2 = true;
         int var3 = RealmsBackupScreen.this.backupObjectSelectionList.getRowRight() - 17 - 7;
         int var4 = RealmsBackupScreen.this.backupObjectSelectionList.getRowTop(RealmsBackupScreen.this.backups.indexOf(this.backup)) + 2;
         this.restoreButton = new ImageButton(var3, var4, 17, 10, 0, 0, 10, RealmsBackupScreen.RESTORE_ICON_LOCATION, 17, 20, (var1x) -> {
            RealmsBackupScreen.this.restoreClicked(RealmsBackupScreen.this.backups.indexOf(this.backup));
         });
         this.restoreButton.setTooltip(Tooltip.create(RealmsBackupScreen.RESTORE_TOOLTIP));
         this.children.add(this.restoreButton);
      }

      public boolean mouseClicked(double var1, double var3, int var5) {
         if (this.restoreButton != null) {
            this.restoreButton.mouseClicked(var1, var3, var5);
         }

         if (this.changesButton != null) {
            this.changesButton.mouseClicked(var1, var3, var5);
         }

         return true;
      }

      public void render(GuiGraphics var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, boolean var9, float var10) {
         int var11 = this.backup.isUploadedVersion() ? -8388737 : 16777215;
         var1.drawString(RealmsBackupScreen.this.font, (Component)Component.translatable("mco.backup.entry", RealmsUtil.convertToAgePresentationFromInstant(this.backup.lastModifiedDate)), var4, var3 + 1, var11, false);
         var1.drawString(RealmsBackupScreen.this.font, this.getMediumDatePresentation(this.backup.lastModifiedDate), var4, var3 + 12, 5000268, false);
         this.children.forEach((var5x) -> {
            var5x.setY(var3 + 2);
            var5x.render(var1, var7, var8, var10);
         });
      }

      private String getMediumDatePresentation(Date var1) {
         return DateFormat.getDateTimeInstance(3, 3).format(var1);
      }

      public Component getNarration() {
         return Component.translatable("narrator.select", this.backup.lastModifiedDate.toString());
      }
   }
}
