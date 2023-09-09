package net.minecraft.client.gui.components;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Optionull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

public class PlayerTabOverlay {
   private static final Comparator<PlayerInfo> PLAYER_COMPARATOR = Comparator.comparingInt((var0) -> {
      return var0.getGameMode() == GameType.SPECTATOR ? 1 : 0;
   }).thenComparing((var0) -> {
      return (String)Optionull.mapOrDefault(var0.getTeam(), PlayerTeam::getName, "");
   }).thenComparing((var0) -> {
      return var0.getProfile().getName();
   }, String::compareToIgnoreCase);
   private static final ResourceLocation GUI_ICONS_LOCATION = new ResourceLocation("textures/gui/icons.png");
   public static final int MAX_ROWS_PER_COL = 20;
   public static final int HEART_EMPTY_CONTAINER = 16;
   public static final int HEART_EMPTY_CONTAINER_BLINKING = 25;
   public static final int HEART_FULL = 52;
   public static final int HEART_HALF_FULL = 61;
   public static final int HEART_GOLDEN_FULL = 160;
   public static final int HEART_GOLDEN_HALF_FULL = 169;
   public static final int HEART_GHOST_FULL = 70;
   public static final int HEART_GHOST_HALF_FULL = 79;
   private final Minecraft minecraft;
   private final Gui gui;
   @Nullable
   private Component footer;
   @Nullable
   private Component header;
   private boolean visible;
   private final Map<UUID, PlayerTabOverlay.HealthState> healthStates = new Object2ObjectOpenHashMap();

   public PlayerTabOverlay(Minecraft var1, Gui var2) {
      this.minecraft = var1;
      this.gui = var2;
   }

   public Component getNameForDisplay(PlayerInfo var1) {
      return var1.getTabListDisplayName() != null ? this.decorateName(var1, var1.getTabListDisplayName().copy()) : this.decorateName(var1, PlayerTeam.formatNameForTeam(var1.getTeam(), Component.literal(var1.getProfile().getName())));
   }

   private Component decorateName(PlayerInfo var1, MutableComponent var2) {
      return var1.getGameMode() == GameType.SPECTATOR ? var2.withStyle(ChatFormatting.ITALIC) : var2;
   }

   public void setVisible(boolean var1) {
      if (this.visible != var1) {
         this.healthStates.clear();
         this.visible = var1;
         if (var1) {
            MutableComponent var2 = ComponentUtils.formatList(this.getPlayerInfos(), (Component)Component.literal(", "), this::getNameForDisplay);
            this.minecraft.getNarrator().sayNow((Component)Component.translatable("multiplayer.player.list.narration", var2));
         }
      }

   }

   private List<PlayerInfo> getPlayerInfos() {
      return this.minecraft.player.connection.getListedOnlinePlayers().stream().sorted(PLAYER_COMPARATOR).limit(80L).toList();
   }

   public void render(GuiGraphics var1, int var2, Scoreboard var3, @Nullable Objective var4) {
      List var5 = this.getPlayerInfos();
      int var6 = 0;
      int var7 = 0;
      Iterator var8 = var5.iterator();

      int var10;
      while(var8.hasNext()) {
         PlayerInfo var9 = (PlayerInfo)var8.next();
         var10 = this.minecraft.font.width((FormattedText)this.getNameForDisplay(var9));
         var6 = Math.max(var6, var10);
         if (var4 != null && var4.getRenderType() != ObjectiveCriteria.RenderType.HEARTS) {
            Font var10000 = this.minecraft.font;
            Score var10001 = var3.getOrCreatePlayerScore(var9.getProfile().getName(), var4);
            var10 = var10000.width(" " + var10001.getScore());
            var7 = Math.max(var7, var10);
         }
      }

      if (!this.healthStates.isEmpty()) {
         Set var30 = (Set)var5.stream().map((var0) -> {
            return var0.getProfile().getId();
         }).collect(Collectors.toSet());
         this.healthStates.keySet().removeIf((var1x) -> {
            return !var30.contains(var1x);
         });
      }

      int var31 = var5.size();
      int var32 = var31;

      for(var10 = 1; var32 > 20; var32 = (var31 + var10 - 1) / var10) {
         ++var10;
      }

      boolean var11 = this.minecraft.isLocalServer() || this.minecraft.getConnection().getConnection().isEncrypted();
      int var12;
      if (var4 != null) {
         if (var4.getRenderType() == ObjectiveCriteria.RenderType.HEARTS) {
            var12 = 90;
         } else {
            var12 = var7;
         }
      } else {
         var12 = 0;
      }

      int var13 = Math.min(var10 * ((var11 ? 9 : 0) + var6 + var12 + 13), var2 - 50) / var10;
      int var14 = var2 / 2 - (var13 * var10 + (var10 - 1) * 5) / 2;
      int var15 = 10;
      int var16 = var13 * var10 + (var10 - 1) * 5;
      List var17 = null;
      if (this.header != null) {
         var17 = this.minecraft.font.split(this.header, var2 - 50);

         FormattedCharSequence var19;
         for(Iterator var18 = var17.iterator(); var18.hasNext(); var16 = Math.max(var16, this.minecraft.font.width(var19))) {
            var19 = (FormattedCharSequence)var18.next();
         }
      }

      List var34 = null;
      FormattedCharSequence var20;
      Iterator var35;
      if (this.footer != null) {
         var34 = this.minecraft.font.split(this.footer, var2 - 50);

         for(var35 = var34.iterator(); var35.hasNext(); var16 = Math.max(var16, this.minecraft.font.width(var20))) {
            var20 = (FormattedCharSequence)var35.next();
         }
      }

      int var10002;
      int var10003;
      int var10005;
      int var21;
      int var33;
      if (var17 != null) {
         var33 = var2 / 2 - var16 / 2 - 1;
         var10002 = var15 - 1;
         var10003 = var2 / 2 + var16 / 2 + 1;
         var10005 = var17.size();
         Objects.requireNonNull(this.minecraft.font);
         var1.fill(var33, var10002, var10003, var15 + var10005 * 9, Integer.MIN_VALUE);

         for(var35 = var17.iterator(); var35.hasNext(); var15 += 9) {
            var20 = (FormattedCharSequence)var35.next();
            var21 = this.minecraft.font.width(var20);
            var1.drawString(this.minecraft.font, (FormattedCharSequence)var20, var2 / 2 - var21 / 2, var15, -1);
            Objects.requireNonNull(this.minecraft.font);
         }

         ++var15;
      }

      var1.fill(var2 / 2 - var16 / 2 - 1, var15 - 1, var2 / 2 + var16 / 2 + 1, var15 + var32 * 9, Integer.MIN_VALUE);
      int var36 = this.minecraft.options.getBackgroundColor(553648127);

      int var22;
      for(int var37 = 0; var37 < var31; ++var37) {
         var21 = var37 / var32;
         var22 = var37 % var32;
         int var23 = var14 + var21 * var13 + var21 * 5;
         int var24 = var15 + var22 * 9;
         var1.fill(var23, var24, var23 + var13, var24 + 8, var36);
         RenderSystem.enableBlend();
         if (var37 < var5.size()) {
            PlayerInfo var25 = (PlayerInfo)var5.get(var37);
            GameProfile var26 = var25.getProfile();
            if (var11) {
               Player var27 = this.minecraft.level.getPlayerByUUID(var26.getId());
               boolean var28 = var27 != null && LivingEntityRenderer.isEntityUpsideDown(var27);
               boolean var29 = var27 != null && var27.isModelPartShown(PlayerModelPart.HAT);
               PlayerFaceRenderer.draw(var1, var25.getSkinLocation(), var23, var24, 8, var29, var28);
               var23 += 9;
            }

            var1.drawString(this.minecraft.font, this.getNameForDisplay(var25), var23, var24, var25.getGameMode() == GameType.SPECTATOR ? -1862270977 : -1);
            if (var4 != null && var25.getGameMode() != GameType.SPECTATOR) {
               int var40 = var23 + var6 + 1;
               int var41 = var40 + var12;
               if (var41 - var40 > 5) {
                  this.renderTablistScore(var4, var24, var26.getName(), var40, var41, var26.getId(), var1);
               }
            }

            this.renderPingIcon(var1, var13, var23 - (var11 ? 9 : 0), var24, var25);
         }
      }

      if (var34 != null) {
         var15 += var32 * 9 + 1;
         var33 = var2 / 2 - var16 / 2 - 1;
         var10002 = var15 - 1;
         var10003 = var2 / 2 + var16 / 2 + 1;
         var10005 = var34.size();
         Objects.requireNonNull(this.minecraft.font);
         var1.fill(var33, var10002, var10003, var15 + var10005 * 9, Integer.MIN_VALUE);

         for(Iterator var38 = var34.iterator(); var38.hasNext(); var15 += 9) {
            FormattedCharSequence var39 = (FormattedCharSequence)var38.next();
            var22 = this.minecraft.font.width(var39);
            var1.drawString(this.minecraft.font, (FormattedCharSequence)var39, var2 / 2 - var22 / 2, var15, -1);
            Objects.requireNonNull(this.minecraft.font);
         }
      }

   }

   protected void renderPingIcon(GuiGraphics var1, int var2, int var3, int var4, PlayerInfo var5) {
      boolean var6 = false;
      byte var7;
      if (var5.getLatency() < 0) {
         var7 = 5;
      } else if (var5.getLatency() < 150) {
         var7 = 0;
      } else if (var5.getLatency() < 300) {
         var7 = 1;
      } else if (var5.getLatency() < 600) {
         var7 = 2;
      } else if (var5.getLatency() < 1000) {
         var7 = 3;
      } else {
         var7 = 4;
      }

      var1.pose().pushPose();
      var1.pose().translate(0.0F, 0.0F, 100.0F);
      var1.blit(GUI_ICONS_LOCATION, var3 + var2 - 11, var4, 0, 176 + var7 * 8, 10, 8);
      var1.pose().popPose();
   }

   private void renderTablistScore(Objective var1, int var2, String var3, int var4, int var5, UUID var6, GuiGraphics var7) {
      int var8 = var1.getScoreboard().getOrCreatePlayerScore(var3, var1).getScore();
      if (var1.getRenderType() == ObjectiveCriteria.RenderType.HEARTS) {
         this.renderTablistHearts(var2, var4, var5, var6, var7, var8);
      } else {
         String var9 = ChatFormatting.YELLOW + var8;
         var7.drawString(this.minecraft.font, var9, var5 - this.minecraft.font.width(var9), var2, 16777215);
      }
   }

   private void renderTablistHearts(int var1, int var2, int var3, UUID var4, GuiGraphics var5, int var6) {
      PlayerTabOverlay.HealthState var7 = (PlayerTabOverlay.HealthState)this.healthStates.computeIfAbsent(var4, (var1x) -> {
         return new PlayerTabOverlay.HealthState(var6);
      });
      var7.update(var6, (long)this.gui.getGuiTicks());
      int var8 = Mth.positiveCeilDiv(Math.max(var6, var7.displayedValue()), 2);
      int var9 = Math.max(var6, Math.max(var7.displayedValue(), 20)) / 2;
      boolean var10 = var7.isBlinking((long)this.gui.getGuiTicks());
      if (var8 > 0) {
         int var11 = Mth.floor(Math.min((float)(var3 - var2 - 4) / (float)var9, 9.0F));
         if (var11 <= 3) {
            float var15 = Mth.clamp((float)var6 / 20.0F, 0.0F, 1.0F);
            int var13 = (int)((1.0F - var15) * 255.0F) << 16 | (int)(var15 * 255.0F) << 8;
            String var14 = ((float)var6 / 2.0F).makeConcatWithConstants<invokedynamic>((float)var6 / 2.0F);
            if (var3 - this.minecraft.font.width(var14 + "hp") >= var2) {
               var14 = var14 + "hp";
            }

            var5.drawString(this.minecraft.font, var14, (var3 + var2 - this.minecraft.font.width(var14)) / 2, var1, var13);
         } else {
            int var12;
            for(var12 = var8; var12 < var9; ++var12) {
               var5.blit(GUI_ICONS_LOCATION, var2 + var12 * var11, var1, var10 ? 25 : 16, 0, 9, 9);
            }

            for(var12 = 0; var12 < var8; ++var12) {
               var5.blit(GUI_ICONS_LOCATION, var2 + var12 * var11, var1, var10 ? 25 : 16, 0, 9, 9);
               if (var10) {
                  if (var12 * 2 + 1 < var7.displayedValue()) {
                     var5.blit(GUI_ICONS_LOCATION, var2 + var12 * var11, var1, 70, 0, 9, 9);
                  }

                  if (var12 * 2 + 1 == var7.displayedValue()) {
                     var5.blit(GUI_ICONS_LOCATION, var2 + var12 * var11, var1, 79, 0, 9, 9);
                  }
               }

               if (var12 * 2 + 1 < var6) {
                  var5.blit(GUI_ICONS_LOCATION, var2 + var12 * var11, var1, var12 >= 10 ? 160 : 52, 0, 9, 9);
               }

               if (var12 * 2 + 1 == var6) {
                  var5.blit(GUI_ICONS_LOCATION, var2 + var12 * var11, var1, var12 >= 10 ? 169 : 61, 0, 9, 9);
               }
            }

         }
      }
   }

   public void setFooter(@Nullable Component var1) {
      this.footer = var1;
   }

   public void setHeader(@Nullable Component var1) {
      this.header = var1;
   }

   public void reset() {
      this.header = null;
      this.footer = null;
   }

   private static class HealthState {
      private static final long DISPLAY_UPDATE_DELAY = 20L;
      private static final long DECREASE_BLINK_DURATION = 20L;
      private static final long INCREASE_BLINK_DURATION = 10L;
      private int lastValue;
      private int displayedValue;
      private long lastUpdateTick;
      private long blinkUntilTick;

      public HealthState(int var1) {
         this.displayedValue = var1;
         this.lastValue = var1;
      }

      public void update(int var1, long var2) {
         if (var1 != this.lastValue) {
            long var4 = var1 < this.lastValue ? 20L : 10L;
            this.blinkUntilTick = var2 + var4;
            this.lastValue = var1;
            this.lastUpdateTick = var2;
         }

         if (var2 - this.lastUpdateTick > 20L) {
            this.displayedValue = var1;
         }

      }

      public int displayedValue() {
         return this.displayedValue;
      }

      public boolean isBlinking(long var1) {
         return this.blinkUntilTick > var1 && (this.blinkUntilTick - var1) % 6L >= 3L;
      }
   }
}
