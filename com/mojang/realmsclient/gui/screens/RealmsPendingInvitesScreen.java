package com.mojang.realmsclient.gui.screens;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.PendingInvite;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.RowButton;
import com.mojang.realmsclient.util.RealmsUtil;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.realms.RealmsObjectSelectionList;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

public class RealmsPendingInvitesScreen extends RealmsScreen {
   static final Logger LOGGER = LogUtils.getLogger();
   static final ResourceLocation ACCEPT_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/accept_icon.png");
   static final ResourceLocation REJECT_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/reject_icon.png");
   private static final Component NO_PENDING_INVITES_TEXT = Component.translatable("mco.invites.nopending");
   static final Component ACCEPT_INVITE_TOOLTIP = Component.translatable("mco.invites.button.accept");
   static final Component REJECT_INVITE_TOOLTIP = Component.translatable("mco.invites.button.reject");
   private final Screen lastScreen;
   @Nullable
   Component toolTip;
   boolean loaded;
   RealmsPendingInvitesScreen.PendingInvitationSelectionList pendingInvitationSelectionList;
   int selectedInvite = -1;
   private Button acceptButton;
   private Button rejectButton;

   public RealmsPendingInvitesScreen(Screen var1, Component var2) {
      super(var2);
      this.lastScreen = var1;
   }

   public void init() {
      this.pendingInvitationSelectionList = new RealmsPendingInvitesScreen.PendingInvitationSelectionList();
      (new Thread("Realms-pending-invitations-fetcher") {
         public void run() {
            RealmsClient var1 = RealmsClient.create();

            try {
               List var2 = var1.pendingInvites().pendingInvites;
               List var3 = (List)var2.stream().map((var1x) -> {
                  return RealmsPendingInvitesScreen.this.new Entry(var1x);
               }).collect(Collectors.toList());
               RealmsPendingInvitesScreen.this.minecraft.execute(() -> {
                  RealmsPendingInvitesScreen.this.pendingInvitationSelectionList.replaceEntries(var3);
               });
            } catch (RealmsServiceException var7) {
               RealmsPendingInvitesScreen.LOGGER.error("Couldn't list invites");
            } finally {
               RealmsPendingInvitesScreen.this.loaded = true;
            }

         }
      }).start();
      this.addWidget(this.pendingInvitationSelectionList);
      this.acceptButton = (Button)this.addRenderableWidget(Button.builder(Component.translatable("mco.invites.button.accept"), (var1) -> {
         this.accept(this.selectedInvite);
         this.selectedInvite = -1;
         this.updateButtonStates();
      }).bounds(this.width / 2 - 174, this.height - 32, 100, 20).build());
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, (var1) -> {
         this.minecraft.setScreen(new RealmsMainScreen(this.lastScreen));
      }).bounds(this.width / 2 - 50, this.height - 32, 100, 20).build());
      this.rejectButton = (Button)this.addRenderableWidget(Button.builder(Component.translatable("mco.invites.button.reject"), (var1) -> {
         this.reject(this.selectedInvite);
         this.selectedInvite = -1;
         this.updateButtonStates();
      }).bounds(this.width / 2 + 74, this.height - 32, 100, 20).build());
      this.updateButtonStates();
   }

   public boolean keyPressed(int var1, int var2, int var3) {
      if (var1 == 256) {
         this.minecraft.setScreen(new RealmsMainScreen(this.lastScreen));
         return true;
      } else {
         return super.keyPressed(var1, var2, var3);
      }
   }

   void updateList(int var1) {
      this.pendingInvitationSelectionList.removeAtIndex(var1);
   }

   void reject(final int var1) {
      if (var1 < this.pendingInvitationSelectionList.getItemCount()) {
         (new Thread("Realms-reject-invitation") {
            public void run() {
               try {
                  RealmsClient var1x = RealmsClient.create();
                  var1x.rejectInvitation(((RealmsPendingInvitesScreen.Entry)RealmsPendingInvitesScreen.this.pendingInvitationSelectionList.children().get(var1)).pendingInvite.invitationId);
                  RealmsPendingInvitesScreen.this.minecraft.execute(() -> {
                     RealmsPendingInvitesScreen.this.updateList(var1);
                  });
               } catch (RealmsServiceException var2) {
                  RealmsPendingInvitesScreen.LOGGER.error("Couldn't reject invite");
               }

            }
         }).start();
      }

   }

   void accept(final int var1) {
      if (var1 < this.pendingInvitationSelectionList.getItemCount()) {
         (new Thread("Realms-accept-invitation") {
            public void run() {
               try {
                  RealmsClient var1x = RealmsClient.create();
                  var1x.acceptInvitation(((RealmsPendingInvitesScreen.Entry)RealmsPendingInvitesScreen.this.pendingInvitationSelectionList.children().get(var1)).pendingInvite.invitationId);
                  RealmsPendingInvitesScreen.this.minecraft.execute(() -> {
                     RealmsPendingInvitesScreen.this.updateList(var1);
                  });
               } catch (RealmsServiceException var2) {
                  RealmsPendingInvitesScreen.LOGGER.error("Couldn't accept invite");
               }

            }
         }).start();
      }

   }

   public void render(GuiGraphics var1, int var2, int var3, float var4) {
      this.toolTip = null;
      this.renderBackground(var1);
      this.pendingInvitationSelectionList.render(var1, var2, var3, var4);
      var1.drawCenteredString(this.font, (Component)this.title, this.width / 2, 12, 16777215);
      if (this.toolTip != null) {
         this.renderMousehoverTooltip(var1, this.toolTip, var2, var3);
      }

      if (this.pendingInvitationSelectionList.getItemCount() == 0 && this.loaded) {
         var1.drawCenteredString(this.font, NO_PENDING_INVITES_TEXT, this.width / 2, this.height / 2 - 20, 16777215);
      }

      super.render(var1, var2, var3, var4);
   }

   protected void renderMousehoverTooltip(GuiGraphics var1, @Nullable Component var2, int var3, int var4) {
      if (var2 != null) {
         int var5 = var3 + 12;
         int var6 = var4 - 12;
         int var7 = this.font.width((FormattedText)var2);
         var1.fillGradient(var5 - 3, var6 - 3, var5 + var7 + 3, var6 + 8 + 3, -1073741824, -1073741824);
         var1.drawString(this.font, var2, var5, var6, 16777215);
      }
   }

   void updateButtonStates() {
      this.acceptButton.visible = this.shouldAcceptAndRejectButtonBeVisible(this.selectedInvite);
      this.rejectButton.visible = this.shouldAcceptAndRejectButtonBeVisible(this.selectedInvite);
   }

   private boolean shouldAcceptAndRejectButtonBeVisible(int var1) {
      return var1 != -1;
   }

   class PendingInvitationSelectionList extends RealmsObjectSelectionList<RealmsPendingInvitesScreen.Entry> {
      public PendingInvitationSelectionList() {
         super(RealmsPendingInvitesScreen.this.width, RealmsPendingInvitesScreen.this.height, 32, RealmsPendingInvitesScreen.this.height - 40, 36);
      }

      public void removeAtIndex(int var1) {
         this.remove(var1);
      }

      public int getMaxPosition() {
         return this.getItemCount() * 36;
      }

      public int getRowWidth() {
         return 260;
      }

      public void renderBackground(GuiGraphics var1) {
         RealmsPendingInvitesScreen.this.renderBackground(var1);
      }

      public void selectItem(int var1) {
         super.selectItem(var1);
         this.selectInviteListItem(var1);
      }

      public void selectInviteListItem(int var1) {
         RealmsPendingInvitesScreen.this.selectedInvite = var1;
         RealmsPendingInvitesScreen.this.updateButtonStates();
      }

      public void setSelected(@Nullable RealmsPendingInvitesScreen.Entry var1) {
         super.setSelected(var1);
         RealmsPendingInvitesScreen.this.selectedInvite = this.children().indexOf(var1);
         RealmsPendingInvitesScreen.this.updateButtonStates();
      }

      // $FF: synthetic method
      // $FF: bridge method
      public void setSelected(@Nullable AbstractSelectionList.Entry var1) {
         this.setSelected((RealmsPendingInvitesScreen.Entry)var1);
      }
   }

   private class Entry extends ObjectSelectionList.Entry<RealmsPendingInvitesScreen.Entry> {
      private static final int TEXT_LEFT = 38;
      final PendingInvite pendingInvite;
      private final List<RowButton> rowButtons;

      Entry(PendingInvite var2) {
         this.pendingInvite = var2;
         this.rowButtons = Arrays.asList(new RealmsPendingInvitesScreen.Entry.AcceptRowButton(), new RealmsPendingInvitesScreen.Entry.RejectRowButton());
      }

      public void render(GuiGraphics var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, boolean var9, float var10) {
         this.renderPendingInvitationItem(var1, this.pendingInvite, var4, var3, var7, var8);
      }

      public boolean mouseClicked(double var1, double var3, int var5) {
         RowButton.rowButtonMouseClicked(RealmsPendingInvitesScreen.this.pendingInvitationSelectionList, this, this.rowButtons, var5, var1, var3);
         return true;
      }

      private void renderPendingInvitationItem(GuiGraphics var1, PendingInvite var2, int var3, int var4, int var5, int var6) {
         var1.drawString(RealmsPendingInvitesScreen.this.font, var2.worldName, var3 + 38, var4 + 1, 16777215, false);
         var1.drawString(RealmsPendingInvitesScreen.this.font, var2.worldOwnerName, var3 + 38, var4 + 12, 7105644, false);
         var1.drawString(RealmsPendingInvitesScreen.this.font, RealmsUtil.convertToAgePresentationFromInstant(var2.date), var3 + 38, var4 + 24, 7105644, false);
         RowButton.drawButtonsInRow(var1, this.rowButtons, RealmsPendingInvitesScreen.this.pendingInvitationSelectionList, var3, var4, var5, var6);
         RealmsUtil.renderPlayerFace(var1, var3, var4, 32, var2.worldOwnerUuid);
      }

      public Component getNarration() {
         Component var1 = CommonComponents.joinLines(Component.literal(this.pendingInvite.worldName), Component.literal(this.pendingInvite.worldOwnerName), RealmsUtil.convertToAgePresentationFromInstant(this.pendingInvite.date));
         return Component.translatable("narrator.select", var1);
      }

      class AcceptRowButton extends RowButton {
         AcceptRowButton() {
            super(15, 15, 215, 5);
         }

         protected void draw(GuiGraphics var1, int var2, int var3, boolean var4) {
            float var5 = var4 ? 19.0F : 0.0F;
            var1.blit(RealmsPendingInvitesScreen.ACCEPT_ICON_LOCATION, var2, var3, var5, 0.0F, 18, 18, 37, 18);
            if (var4) {
               RealmsPendingInvitesScreen.this.toolTip = RealmsPendingInvitesScreen.ACCEPT_INVITE_TOOLTIP;
            }

         }

         public void onClick(int var1) {
            RealmsPendingInvitesScreen.this.accept(var1);
         }
      }

      class RejectRowButton extends RowButton {
         RejectRowButton() {
            super(15, 15, 235, 5);
         }

         protected void draw(GuiGraphics var1, int var2, int var3, boolean var4) {
            float var5 = var4 ? 19.0F : 0.0F;
            var1.blit(RealmsPendingInvitesScreen.REJECT_ICON_LOCATION, var2, var3, var5, 0.0F, 18, 18, 37, 18);
            if (var4) {
               RealmsPendingInvitesScreen.this.toolTip = RealmsPendingInvitesScreen.REJECT_INVITE_TOOLTIP;
            }

         }

         public void onClick(int var1) {
            RealmsPendingInvitesScreen.this.reject(var1);
         }
      }
   }
}
