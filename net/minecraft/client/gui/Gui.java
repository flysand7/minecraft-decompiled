package net.minecraft.client.gui;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.gui.components.SubtitleOverlay;
import net.minecraft.client.gui.components.spectator.SpectatorGui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.MobEffectTextureManager;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringUtil;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;

public class Gui {
   private static final ResourceLocation VIGNETTE_LOCATION = new ResourceLocation("textures/misc/vignette.png");
   private static final ResourceLocation WIDGETS_LOCATION = new ResourceLocation("textures/gui/widgets.png");
   private static final ResourceLocation PUMPKIN_BLUR_LOCATION = new ResourceLocation("textures/misc/pumpkinblur.png");
   private static final ResourceLocation SPYGLASS_SCOPE_LOCATION = new ResourceLocation("textures/misc/spyglass_scope.png");
   private static final ResourceLocation POWDER_SNOW_OUTLINE_LOCATION = new ResourceLocation("textures/misc/powder_snow_outline.png");
   private static final ResourceLocation GUI_ICONS_LOCATION = new ResourceLocation("textures/gui/icons.png");
   private static final Component DEMO_EXPIRED_TEXT = Component.translatable("demo.demoExpired");
   private static final Component SAVING_TEXT = Component.translatable("menu.savingLevel");
   private static final int COLOR_WHITE = 16777215;
   private static final float MIN_CROSSHAIR_ATTACK_SPEED = 5.0F;
   private static final int NUM_HEARTS_PER_ROW = 10;
   private static final int LINE_HEIGHT = 10;
   private static final String SPACER = ": ";
   private static final float PORTAL_OVERLAY_ALPHA_MIN = 0.2F;
   private static final int HEART_SIZE = 9;
   private static final int HEART_SEPARATION = 8;
   private static final float AUTOSAVE_FADE_SPEED_FACTOR = 0.2F;
   private final RandomSource random = RandomSource.create();
   private final Minecraft minecraft;
   private final ItemRenderer itemRenderer;
   private final ChatComponent chat;
   private int tickCount;
   @Nullable
   private Component overlayMessageString;
   private int overlayMessageTime;
   private boolean animateOverlayMessageColor;
   private boolean chatDisabledByPlayerShown;
   public float vignetteBrightness = 1.0F;
   private int toolHighlightTimer;
   private ItemStack lastToolHighlight;
   private final DebugScreenOverlay debugScreen;
   private final SubtitleOverlay subtitleOverlay;
   private final SpectatorGui spectatorGui;
   private final PlayerTabOverlay tabList;
   private final BossHealthOverlay bossOverlay;
   private int titleTime;
   @Nullable
   private Component title;
   @Nullable
   private Component subtitle;
   private int titleFadeInTime;
   private int titleStayTime;
   private int titleFadeOutTime;
   private int lastHealth;
   private int displayHealth;
   private long lastHealthTime;
   private long healthBlinkTime;
   private int screenWidth;
   private int screenHeight;
   private float autosaveIndicatorValue;
   private float lastAutosaveIndicatorValue;
   private float scopeScale;

   public Gui(Minecraft var1, ItemRenderer var2) {
      this.lastToolHighlight = ItemStack.EMPTY;
      this.minecraft = var1;
      this.itemRenderer = var2;
      this.debugScreen = new DebugScreenOverlay(var1);
      this.spectatorGui = new SpectatorGui(var1);
      this.chat = new ChatComponent(var1);
      this.tabList = new PlayerTabOverlay(var1, this);
      this.bossOverlay = new BossHealthOverlay(var1);
      this.subtitleOverlay = new SubtitleOverlay(var1);
      this.resetTitleTimes();
   }

   public void resetTitleTimes() {
      this.titleFadeInTime = 10;
      this.titleStayTime = 70;
      this.titleFadeOutTime = 20;
   }

   public void render(GuiGraphics var1, float var2) {
      Window var3 = this.minecraft.getWindow();
      this.screenWidth = var1.guiWidth();
      this.screenHeight = var1.guiHeight();
      Font var4 = this.getFont();
      RenderSystem.enableBlend();
      if (Minecraft.useFancyGraphics()) {
         this.renderVignette(var1, this.minecraft.getCameraEntity());
      } else {
         RenderSystem.enableDepthTest();
      }

      float var5 = this.minecraft.getDeltaFrameTime();
      this.scopeScale = Mth.lerp(0.5F * var5, this.scopeScale, 1.125F);
      if (this.minecraft.options.getCameraType().isFirstPerson()) {
         if (this.minecraft.player.isScoping()) {
            this.renderSpyglassOverlay(var1, this.scopeScale);
         } else {
            this.scopeScale = 0.5F;
            ItemStack var6 = this.minecraft.player.getInventory().getArmor(3);
            if (var6.is(Blocks.CARVED_PUMPKIN.asItem())) {
               this.renderTextureOverlay(var1, PUMPKIN_BLUR_LOCATION, 1.0F);
            }
         }
      }

      if (this.minecraft.player.getTicksFrozen() > 0) {
         this.renderTextureOverlay(var1, POWDER_SNOW_OUTLINE_LOCATION, this.minecraft.player.getPercentFrozen());
      }

      float var13 = Mth.lerp(var2, this.minecraft.player.oSpinningEffectIntensity, this.minecraft.player.spinningEffectIntensity);
      if (var13 > 0.0F && !this.minecraft.player.hasEffect(MobEffects.CONFUSION)) {
         this.renderPortalOverlay(var1, var13);
      }

      if (this.minecraft.gameMode.getPlayerMode() == GameType.SPECTATOR) {
         this.spectatorGui.renderHotbar(var1);
      } else if (!this.minecraft.options.hideGui) {
         this.renderHotbar(var2, var1);
      }

      if (!this.minecraft.options.hideGui) {
         RenderSystem.enableBlend();
         this.renderCrosshair(var1);
         this.minecraft.getProfiler().push("bossHealth");
         this.bossOverlay.render(var1);
         this.minecraft.getProfiler().pop();
         if (this.minecraft.gameMode.canHurtPlayer()) {
            this.renderPlayerHealth(var1);
         }

         this.renderVehicleHealth(var1);
         RenderSystem.disableBlend();
         int var7 = this.screenWidth / 2 - 91;
         PlayerRideableJumping var8 = this.minecraft.player.jumpableVehicle();
         if (var8 != null) {
            this.renderJumpMeter(var8, var1, var7);
         } else if (this.minecraft.gameMode.hasExperience()) {
            this.renderExperienceBar(var1, var7);
         }

         if (this.minecraft.gameMode.getPlayerMode() != GameType.SPECTATOR) {
            this.renderSelectedItemName(var1);
         } else if (this.minecraft.player.isSpectator()) {
            this.spectatorGui.renderTooltip(var1);
         }
      }

      int var9;
      float var14;
      if (this.minecraft.player.getSleepTimer() > 0) {
         this.minecraft.getProfiler().push("sleep");
         var14 = (float)this.minecraft.player.getSleepTimer();
         float var15 = var14 / 100.0F;
         if (var15 > 1.0F) {
            var15 = 1.0F - (var14 - 100.0F) / 10.0F;
         }

         var9 = (int)(220.0F * var15) << 24 | 1052704;
         var1.fill(RenderType.guiOverlay(), 0, 0, this.screenWidth, this.screenHeight, var9);
         this.minecraft.getProfiler().pop();
      }

      if (this.minecraft.isDemo()) {
         this.renderDemoOverlay(var1);
      }

      this.renderEffects(var1);
      if (this.minecraft.options.renderDebug) {
         this.debugScreen.render(var1);
      }

      if (!this.minecraft.options.hideGui) {
         int var10;
         int var11;
         int var17;
         if (this.overlayMessageString != null && this.overlayMessageTime > 0) {
            this.minecraft.getProfiler().push("overlayMessage");
            var14 = (float)this.overlayMessageTime - var2;
            var17 = (int)(var14 * 255.0F / 20.0F);
            if (var17 > 255) {
               var17 = 255;
            }

            if (var17 > 8) {
               var1.pose().pushPose();
               var1.pose().translate((float)(this.screenWidth / 2), (float)(this.screenHeight - 68), 0.0F);
               var9 = 16777215;
               if (this.animateOverlayMessageColor) {
                  var9 = Mth.hsvToRgb(var14 / 50.0F, 0.7F, 0.6F) & 16777215;
               }

               var10 = var17 << 24 & -16777216;
               var11 = var4.width((FormattedText)this.overlayMessageString);
               this.drawBackdrop(var1, var4, -4, var11, 16777215 | var10);
               var1.drawString(var4, (Component)this.overlayMessageString, -var11 / 2, -4, var9 | var10);
               var1.pose().popPose();
            }

            this.minecraft.getProfiler().pop();
         }

         if (this.title != null && this.titleTime > 0) {
            this.minecraft.getProfiler().push("titleAndSubtitle");
            var14 = (float)this.titleTime - var2;
            var17 = 255;
            if (this.titleTime > this.titleFadeOutTime + this.titleStayTime) {
               float var18 = (float)(this.titleFadeInTime + this.titleStayTime + this.titleFadeOutTime) - var14;
               var17 = (int)(var18 * 255.0F / (float)this.titleFadeInTime);
            }

            if (this.titleTime <= this.titleFadeOutTime) {
               var17 = (int)(var14 * 255.0F / (float)this.titleFadeOutTime);
            }

            var17 = Mth.clamp(var17, 0, 255);
            if (var17 > 8) {
               var1.pose().pushPose();
               var1.pose().translate((float)(this.screenWidth / 2), (float)(this.screenHeight / 2), 0.0F);
               RenderSystem.enableBlend();
               var1.pose().pushPose();
               var1.pose().scale(4.0F, 4.0F, 4.0F);
               var9 = var17 << 24 & -16777216;
               var10 = var4.width((FormattedText)this.title);
               this.drawBackdrop(var1, var4, -10, var10, 16777215 | var9);
               var1.drawString(var4, (Component)this.title, -var10 / 2, -10, 16777215 | var9);
               var1.pose().popPose();
               if (this.subtitle != null) {
                  var1.pose().pushPose();
                  var1.pose().scale(2.0F, 2.0F, 2.0F);
                  var11 = var4.width((FormattedText)this.subtitle);
                  this.drawBackdrop(var1, var4, 5, var11, 16777215 | var9);
                  var1.drawString(var4, (Component)this.subtitle, -var11 / 2, 5, 16777215 | var9);
                  var1.pose().popPose();
               }

               RenderSystem.disableBlend();
               var1.pose().popPose();
            }

            this.minecraft.getProfiler().pop();
         }

         this.subtitleOverlay.render(var1);
         Scoreboard var16 = this.minecraft.level.getScoreboard();
         Objective var21 = null;
         PlayerTeam var19 = var16.getPlayersTeam(this.minecraft.player.getScoreboardName());
         if (var19 != null) {
            var10 = var19.getColor().getId();
            if (var10 >= 0) {
               var21 = var16.getDisplayObjective(3 + var10);
            }
         }

         Objective var20 = var21 != null ? var21 : var16.getDisplayObjective(1);
         if (var20 != null) {
            this.displayScoreboardSidebar(var1, var20);
         }

         RenderSystem.enableBlend();
         var11 = Mth.floor(this.minecraft.mouseHandler.xpos() * (double)var3.getGuiScaledWidth() / (double)var3.getScreenWidth());
         int var12 = Mth.floor(this.minecraft.mouseHandler.ypos() * (double)var3.getGuiScaledHeight() / (double)var3.getScreenHeight());
         this.minecraft.getProfiler().push("chat");
         this.chat.render(var1, this.tickCount, var11, var12);
         this.minecraft.getProfiler().pop();
         var20 = var16.getDisplayObjective(0);
         if (!this.minecraft.options.keyPlayerList.isDown() || this.minecraft.isLocalServer() && this.minecraft.player.connection.getListedOnlinePlayers().size() <= 1 && var20 == null) {
            this.tabList.setVisible(false);
         } else {
            this.tabList.setVisible(true);
            this.tabList.render(var1, this.screenWidth, var16, var20);
         }

         this.renderSavingIndicator(var1);
      }

   }

   private void drawBackdrop(GuiGraphics var1, Font var2, int var3, int var4, int var5) {
      int var6 = this.minecraft.options.getBackgroundColor(0.0F);
      if (var6 != 0) {
         int var7 = -var4 / 2;
         int var10001 = var7 - 2;
         int var10002 = var3 - 2;
         int var10003 = var7 + var4 + 2;
         Objects.requireNonNull(var2);
         var1.fill(var10001, var10002, var10003, var3 + 9 + 2, FastColor.ARGB32.multiply(var6, var5));
      }

   }

   private void renderCrosshair(GuiGraphics var1) {
      Options var2 = this.minecraft.options;
      if (var2.getCameraType().isFirstPerson()) {
         if (this.minecraft.gameMode.getPlayerMode() != GameType.SPECTATOR || this.canRenderCrosshairForSpectator(this.minecraft.hitResult)) {
            if (var2.renderDebug && !var2.hideGui && !this.minecraft.player.isReducedDebugInfo() && !(Boolean)var2.reducedDebugInfo().get()) {
               Camera var9 = this.minecraft.gameRenderer.getMainCamera();
               PoseStack var10 = RenderSystem.getModelViewStack();
               var10.pushPose();
               var10.mulPoseMatrix(var1.pose().last().pose());
               var10.translate((float)(this.screenWidth / 2), (float)(this.screenHeight / 2), 0.0F);
               var10.mulPose(Axis.XN.rotationDegrees(var9.getXRot()));
               var10.mulPose(Axis.YP.rotationDegrees(var9.getYRot()));
               var10.scale(-1.0F, -1.0F, -1.0F);
               RenderSystem.applyModelViewMatrix();
               RenderSystem.renderCrosshair(10);
               var10.popPose();
               RenderSystem.applyModelViewMatrix();
            } else {
               RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
               boolean var3 = true;
               var1.blit(GUI_ICONS_LOCATION, (this.screenWidth - 15) / 2, (this.screenHeight - 15) / 2, 0, 0, 15, 15);
               if (this.minecraft.options.attackIndicator().get() == AttackIndicatorStatus.CROSSHAIR) {
                  float var4 = this.minecraft.player.getAttackStrengthScale(0.0F);
                  boolean var5 = false;
                  if (this.minecraft.crosshairPickEntity != null && this.minecraft.crosshairPickEntity instanceof LivingEntity && var4 >= 1.0F) {
                     var5 = this.minecraft.player.getCurrentItemAttackStrengthDelay() > 5.0F;
                     var5 &= this.minecraft.crosshairPickEntity.isAlive();
                  }

                  int var6 = this.screenHeight / 2 - 7 + 16;
                  int var7 = this.screenWidth / 2 - 8;
                  if (var5) {
                     var1.blit(GUI_ICONS_LOCATION, var7, var6, 68, 94, 16, 16);
                  } else if (var4 < 1.0F) {
                     int var8 = (int)(var4 * 17.0F);
                     var1.blit(GUI_ICONS_LOCATION, var7, var6, 36, 94, 16, 4);
                     var1.blit(GUI_ICONS_LOCATION, var7, var6, 52, 94, var8, 4);
                  }
               }

               RenderSystem.defaultBlendFunc();
            }

         }
      }
   }

   private boolean canRenderCrosshairForSpectator(HitResult var1) {
      if (var1 == null) {
         return false;
      } else if (var1.getType() == HitResult.Type.ENTITY) {
         return ((EntityHitResult)var1).getEntity() instanceof MenuProvider;
      } else if (var1.getType() == HitResult.Type.BLOCK) {
         BlockPos var2 = ((BlockHitResult)var1).getBlockPos();
         ClientLevel var3 = this.minecraft.level;
         return var3.getBlockState(var2).getMenuProvider(var3, var2) != null;
      } else {
         return false;
      }
   }

   protected void renderEffects(GuiGraphics var1) {
      Collection var2;
      label40: {
         var2 = this.minecraft.player.getActiveEffects();
         if (!var2.isEmpty()) {
            Screen var4 = this.minecraft.screen;
            if (!(var4 instanceof EffectRenderingInventoryScreen)) {
               break label40;
            }

            EffectRenderingInventoryScreen var3 = (EffectRenderingInventoryScreen)var4;
            if (!var3.canSeeEffects()) {
               break label40;
            }
         }

         return;
      }

      RenderSystem.enableBlend();
      int var17 = 0;
      int var18 = 0;
      MobEffectTextureManager var5 = this.minecraft.getMobEffectTextures();
      ArrayList var6 = Lists.newArrayListWithExpectedSize(var2.size());
      Iterator var7 = Ordering.natural().reverse().sortedCopy(var2).iterator();

      while(var7.hasNext()) {
         MobEffectInstance var8 = (MobEffectInstance)var7.next();
         MobEffect var9 = var8.getEffect();
         if (var8.showIcon()) {
            int var10 = this.screenWidth;
            int var11 = 1;
            if (this.minecraft.isDemo()) {
               var11 += 15;
            }

            if (var9.isBeneficial()) {
               ++var17;
               var10 -= 25 * var17;
            } else {
               ++var18;
               var10 -= 25 * var18;
               var11 += 26;
            }

            float var12 = 1.0F;
            if (var8.isAmbient()) {
               var1.blit(AbstractContainerScreen.INVENTORY_LOCATION, var10, var11, 165, 166, 24, 24);
            } else {
               var1.blit(AbstractContainerScreen.INVENTORY_LOCATION, var10, var11, 141, 166, 24, 24);
               if (var8.endsWithin(200)) {
                  int var13 = var8.getDuration();
                  int var14 = 10 - var13 / 20;
                  var12 = Mth.clamp((float)var13 / 10.0F / 5.0F * 0.5F, 0.0F, 0.5F) + Mth.cos((float)var13 * 3.1415927F / 5.0F) * Mth.clamp((float)var14 / 10.0F * 0.25F, 0.0F, 0.25F);
               }
            }

            TextureAtlasSprite var19 = var5.get(var9);
            var6.add(() -> {
               var1.setColor(1.0F, 1.0F, 1.0F, var12);
               var1.blit(var10 + 3, var11 + 3, 0, 18, 18, var19);
               var1.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            });
         }
      }

      var6.forEach(Runnable::run);
   }

   private void renderHotbar(float var1, GuiGraphics var2) {
      Player var3 = this.getCameraPlayer();
      if (var3 != null) {
         ItemStack var4 = var3.getOffhandItem();
         HumanoidArm var5 = var3.getMainArm().getOpposite();
         int var6 = this.screenWidth / 2;
         boolean var7 = true;
         boolean var8 = true;
         var2.pose().pushPose();
         var2.pose().translate(0.0F, 0.0F, -90.0F);
         var2.blit(WIDGETS_LOCATION, var6 - 91, this.screenHeight - 22, 0, 0, 182, 22);
         var2.blit(WIDGETS_LOCATION, var6 - 91 - 1 + var3.getInventory().selected * 20, this.screenHeight - 22 - 1, 0, 22, 24, 22);
         if (!var4.isEmpty()) {
            if (var5 == HumanoidArm.LEFT) {
               var2.blit(WIDGETS_LOCATION, var6 - 91 - 29, this.screenHeight - 23, 24, 22, 29, 24);
            } else {
               var2.blit(WIDGETS_LOCATION, var6 + 91, this.screenHeight - 23, 53, 22, 29, 24);
            }
         }

         var2.pose().popPose();
         int var9 = 1;

         int var10;
         int var11;
         int var12;
         for(var10 = 0; var10 < 9; ++var10) {
            var11 = var6 - 90 + var10 * 20 + 2;
            var12 = this.screenHeight - 16 - 3;
            this.renderSlot(var2, var11, var12, var1, var3, (ItemStack)var3.getInventory().items.get(var10), var9++);
         }

         if (!var4.isEmpty()) {
            var10 = this.screenHeight - 16 - 3;
            if (var5 == HumanoidArm.LEFT) {
               this.renderSlot(var2, var6 - 91 - 26, var10, var1, var3, var4, var9++);
            } else {
               this.renderSlot(var2, var6 + 91 + 10, var10, var1, var3, var4, var9++);
            }
         }

         RenderSystem.enableBlend();
         if (this.minecraft.options.attackIndicator().get() == AttackIndicatorStatus.HOTBAR) {
            float var14 = this.minecraft.player.getAttackStrengthScale(0.0F);
            if (var14 < 1.0F) {
               var11 = this.screenHeight - 20;
               var12 = var6 + 91 + 6;
               if (var5 == HumanoidArm.RIGHT) {
                  var12 = var6 - 91 - 22;
               }

               int var13 = (int)(var14 * 19.0F);
               var2.blit(GUI_ICONS_LOCATION, var12, var11, 0, 94, 18, 18);
               var2.blit(GUI_ICONS_LOCATION, var12, var11 + 18 - var13, 18, 112 - var13, 18, var13);
            }
         }

         RenderSystem.disableBlend();
      }
   }

   public void renderJumpMeter(PlayerRideableJumping var1, GuiGraphics var2, int var3) {
      this.minecraft.getProfiler().push("jumpBar");
      float var4 = this.minecraft.player.getJumpRidingScale();
      boolean var5 = true;
      int var6 = (int)(var4 * 183.0F);
      int var7 = this.screenHeight - 32 + 3;
      var2.blit(GUI_ICONS_LOCATION, var3, var7, 0, 84, 182, 5);
      if (var1.getJumpCooldown() > 0) {
         var2.blit(GUI_ICONS_LOCATION, var3, var7, 0, 74, 182, 5);
      } else if (var6 > 0) {
         var2.blit(GUI_ICONS_LOCATION, var3, var7, 0, 89, var6, 5);
      }

      this.minecraft.getProfiler().pop();
   }

   public void renderExperienceBar(GuiGraphics var1, int var2) {
      this.minecraft.getProfiler().push("expBar");
      int var3 = this.minecraft.player.getXpNeededForNextLevel();
      int var5;
      int var6;
      if (var3 > 0) {
         boolean var4 = true;
         var5 = (int)(this.minecraft.player.experienceProgress * 183.0F);
         var6 = this.screenHeight - 32 + 3;
         var1.blit(GUI_ICONS_LOCATION, var2, var6, 0, 64, 182, 5);
         if (var5 > 0) {
            var1.blit(GUI_ICONS_LOCATION, var2, var6, 0, 69, var5, 5);
         }
      }

      this.minecraft.getProfiler().pop();
      if (this.minecraft.player.experienceLevel > 0) {
         this.minecraft.getProfiler().push("expLevel");
         String var7 = this.minecraft.player.experienceLevel.makeConcatWithConstants<invokedynamic>(this.minecraft.player.experienceLevel);
         var5 = (this.screenWidth - this.getFont().width(var7)) / 2;
         var6 = this.screenHeight - 31 - 4;
         var1.drawString(this.getFont(), (String)var7, var5 + 1, var6, 0, false);
         var1.drawString(this.getFont(), (String)var7, var5 - 1, var6, 0, false);
         var1.drawString(this.getFont(), (String)var7, var5, var6 + 1, 0, false);
         var1.drawString(this.getFont(), (String)var7, var5, var6 - 1, 0, false);
         var1.drawString(this.getFont(), var7, var5, var6, 8453920, false);
         this.minecraft.getProfiler().pop();
      }

   }

   public void renderSelectedItemName(GuiGraphics var1) {
      this.minecraft.getProfiler().push("selectedItemName");
      if (this.toolHighlightTimer > 0 && !this.lastToolHighlight.isEmpty()) {
         MutableComponent var2 = Component.empty().append(this.lastToolHighlight.getHoverName()).withStyle(this.lastToolHighlight.getRarity().color);
         if (this.lastToolHighlight.hasCustomHoverName()) {
            var2.withStyle(ChatFormatting.ITALIC);
         }

         int var3 = this.getFont().width((FormattedText)var2);
         int var4 = (this.screenWidth - var3) / 2;
         int var5 = this.screenHeight - 59;
         if (!this.minecraft.gameMode.canHurtPlayer()) {
            var5 += 14;
         }

         int var6 = (int)((float)this.toolHighlightTimer * 256.0F / 10.0F);
         if (var6 > 255) {
            var6 = 255;
         }

         if (var6 > 0) {
            int var10001 = var4 - 2;
            int var10002 = var5 - 2;
            int var10003 = var4 + var3 + 2;
            Objects.requireNonNull(this.getFont());
            var1.fill(var10001, var10002, var10003, var5 + 9 + 2, this.minecraft.options.getBackgroundColor(0));
            var1.drawString(this.getFont(), (Component)var2, var4, var5, 16777215 + (var6 << 24));
         }
      }

      this.minecraft.getProfiler().pop();
   }

   public void renderDemoOverlay(GuiGraphics var1) {
      this.minecraft.getProfiler().push("demo");
      Object var2;
      if (this.minecraft.level.getGameTime() >= 120500L) {
         var2 = DEMO_EXPIRED_TEXT;
      } else {
         var2 = Component.translatable("demo.remainingTime", StringUtil.formatTickDuration((int)(120500L - this.minecraft.level.getGameTime())));
      }

      int var3 = this.getFont().width((FormattedText)var2);
      var1.drawString(this.getFont(), (Component)var2, this.screenWidth - var3 - 10, 5, 16777215);
      this.minecraft.getProfiler().pop();
   }

   private void displayScoreboardSidebar(GuiGraphics var1, Objective var2) {
      Scoreboard var3 = var2.getScoreboard();
      Collection var4 = var3.getPlayerScores(var2);
      List var5 = (List)var4.stream().filter((var0) -> {
         return var0.getOwner() != null && !var0.getOwner().startsWith("#");
      }).collect(Collectors.toList());
      Object var26;
      if (var5.size() > 15) {
         var26 = Lists.newArrayList(Iterables.skip(var5, var4.size() - 15));
      } else {
         var26 = var5;
      }

      ArrayList var6 = Lists.newArrayListWithCapacity(((Collection)var26).size());
      Component var7 = var2.getDisplayName();
      int var8 = this.getFont().width((FormattedText)var7);
      int var9 = var8;
      int var10 = this.getFont().width(": ");

      Score var12;
      MutableComponent var14;
      for(Iterator var11 = ((Collection)var26).iterator(); var11.hasNext(); var9 = Math.max(var9, this.getFont().width((FormattedText)var14) + var10 + this.getFont().width(Integer.toString(var12.getScore())))) {
         var12 = (Score)var11.next();
         PlayerTeam var13 = var3.getPlayersTeam(var12.getOwner());
         var14 = PlayerTeam.formatNameForTeam(var13, Component.literal(var12.getOwner()));
         var6.add(Pair.of(var12, var14));
      }

      int var10000 = ((Collection)var26).size();
      Objects.requireNonNull(this.getFont());
      int var27 = var10000 * 9;
      int var28 = this.screenHeight / 2 + var27 / 3;
      boolean var29 = true;
      int var30 = this.screenWidth - var9 - 3;
      int var15 = 0;
      int var16 = this.minecraft.options.getBackgroundColor(0.3F);
      int var17 = this.minecraft.options.getBackgroundColor(0.4F);
      Iterator var18 = var6.iterator();

      while(var18.hasNext()) {
         Pair var19 = (Pair)var18.next();
         ++var15;
         Score var20 = (Score)var19.getFirst();
         Component var21 = (Component)var19.getSecond();
         ChatFormatting var31 = ChatFormatting.RED;
         String var22 = var31 + var20.getScore();
         Objects.requireNonNull(this.getFont());
         int var24 = var28 - var15 * 9;
         int var25 = this.screenWidth - 3 + 2;
         int var10001 = var30 - 2;
         Objects.requireNonNull(this.getFont());
         var1.fill(var10001, var24, var25, var24 + 9, var16);
         var1.drawString(this.getFont(), (Component)var21, var30, var24, -1, false);
         var1.drawString(this.getFont(), (String)var22, var25 - this.getFont().width(var22), var24, -1, false);
         if (var15 == ((Collection)var26).size()) {
            var10001 = var30 - 2;
            Objects.requireNonNull(this.getFont());
            var1.fill(var10001, var24 - 9 - 1, var25, var24 - 1, var17);
            var1.fill(var30 - 2, var24 - 1, var25, var24, var16);
            Font var32 = this.getFont();
            int var10003 = var30 + var9 / 2 - var8 / 2;
            Objects.requireNonNull(this.getFont());
            var1.drawString(var32, (Component)var7, var10003, var24 - 9, -1, false);
         }
      }

   }

   private Player getCameraPlayer() {
      return !(this.minecraft.getCameraEntity() instanceof Player) ? null : (Player)this.minecraft.getCameraEntity();
   }

   private LivingEntity getPlayerVehicleWithHealth() {
      Player var1 = this.getCameraPlayer();
      if (var1 != null) {
         Entity var2 = var1.getVehicle();
         if (var2 == null) {
            return null;
         }

         if (var2 instanceof LivingEntity) {
            return (LivingEntity)var2;
         }
      }

      return null;
   }

   private int getVehicleMaxHearts(LivingEntity var1) {
      if (var1 != null && var1.showVehicleHealth()) {
         float var2 = var1.getMaxHealth();
         int var3 = (int)(var2 + 0.5F) / 2;
         if (var3 > 30) {
            var3 = 30;
         }

         return var3;
      } else {
         return 0;
      }
   }

   private int getVisibleVehicleHeartRows(int var1) {
      return (int)Math.ceil((double)var1 / 10.0D);
   }

   private void renderPlayerHealth(GuiGraphics var1) {
      Player var2 = this.getCameraPlayer();
      if (var2 != null) {
         int var3 = Mth.ceil(var2.getHealth());
         boolean var4 = this.healthBlinkTime > (long)this.tickCount && (this.healthBlinkTime - (long)this.tickCount) / 3L % 2L == 1L;
         long var5 = Util.getMillis();
         if (var3 < this.lastHealth && var2.invulnerableTime > 0) {
            this.lastHealthTime = var5;
            this.healthBlinkTime = (long)(this.tickCount + 20);
         } else if (var3 > this.lastHealth && var2.invulnerableTime > 0) {
            this.lastHealthTime = var5;
            this.healthBlinkTime = (long)(this.tickCount + 10);
         }

         if (var5 - this.lastHealthTime > 1000L) {
            this.lastHealth = var3;
            this.displayHealth = var3;
            this.lastHealthTime = var5;
         }

         this.lastHealth = var3;
         int var7 = this.displayHealth;
         this.random.setSeed((long)(this.tickCount * 312871));
         FoodData var8 = var2.getFoodData();
         int var9 = var8.getFoodLevel();
         int var10 = this.screenWidth / 2 - 91;
         int var11 = this.screenWidth / 2 + 91;
         int var12 = this.screenHeight - 39;
         float var13 = Math.max((float)var2.getAttributeValue(Attributes.MAX_HEALTH), (float)Math.max(var7, var3));
         int var14 = Mth.ceil(var2.getAbsorptionAmount());
         int var15 = Mth.ceil((var13 + (float)var14) / 2.0F / 10.0F);
         int var16 = Math.max(10 - (var15 - 2), 3);
         int var17 = var12 - (var15 - 1) * var16 - 10;
         int var18 = var12 - 10;
         int var19 = var2.getArmorValue();
         int var20 = -1;
         if (var2.hasEffect(MobEffects.REGENERATION)) {
            var20 = this.tickCount % Mth.ceil(var13 + 5.0F);
         }

         this.minecraft.getProfiler().push("armor");

         int var22;
         for(int var21 = 0; var21 < 10; ++var21) {
            if (var19 > 0) {
               var22 = var10 + var21 * 8;
               if (var21 * 2 + 1 < var19) {
                  var1.blit(GUI_ICONS_LOCATION, var22, var17, 34, 9, 9, 9);
               }

               if (var21 * 2 + 1 == var19) {
                  var1.blit(GUI_ICONS_LOCATION, var22, var17, 25, 9, 9, 9);
               }

               if (var21 * 2 + 1 > var19) {
                  var1.blit(GUI_ICONS_LOCATION, var22, var17, 16, 9, 9, 9);
               }
            }
         }

         this.minecraft.getProfiler().popPush("health");
         this.renderHearts(var1, var2, var10, var12, var16, var20, var13, var3, var7, var14, var4);
         LivingEntity var29 = this.getPlayerVehicleWithHealth();
         var22 = this.getVehicleMaxHearts(var29);
         int var23;
         int var24;
         int var25;
         int var27;
         if (var22 == 0) {
            this.minecraft.getProfiler().popPush("food");

            for(var23 = 0; var23 < 10; ++var23) {
               var24 = var12;
               var25 = 16;
               byte var26 = 0;
               if (var2.hasEffect(MobEffects.HUNGER)) {
                  var25 += 36;
                  var26 = 13;
               }

               if (var2.getFoodData().getSaturationLevel() <= 0.0F && this.tickCount % (var9 * 3 + 1) == 0) {
                  var24 = var12 + (this.random.nextInt(3) - 1);
               }

               var27 = var11 - var23 * 8 - 9;
               var1.blit(GUI_ICONS_LOCATION, var27, var24, 16 + var26 * 9, 27, 9, 9);
               if (var23 * 2 + 1 < var9) {
                  var1.blit(GUI_ICONS_LOCATION, var27, var24, var25 + 36, 27, 9, 9);
               }

               if (var23 * 2 + 1 == var9) {
                  var1.blit(GUI_ICONS_LOCATION, var27, var24, var25 + 45, 27, 9, 9);
               }
            }

            var18 -= 10;
         }

         this.minecraft.getProfiler().popPush("air");
         var23 = var2.getMaxAirSupply();
         var24 = Math.min(var2.getAirSupply(), var23);
         if (var2.isEyeInFluid(FluidTags.WATER) || var24 < var23) {
            var25 = this.getVisibleVehicleHeartRows(var22) - 1;
            var18 -= var25 * 10;
            int var30 = Mth.ceil((double)(var24 - 2) * 10.0D / (double)var23);
            var27 = Mth.ceil((double)var24 * 10.0D / (double)var23) - var30;

            for(int var28 = 0; var28 < var30 + var27; ++var28) {
               if (var28 < var30) {
                  var1.blit(GUI_ICONS_LOCATION, var11 - var28 * 8 - 9, var18, 16, 18, 9, 9);
               } else {
                  var1.blit(GUI_ICONS_LOCATION, var11 - var28 * 8 - 9, var18, 25, 18, 9, 9);
               }
            }
         }

         this.minecraft.getProfiler().pop();
      }
   }

   private void renderHearts(GuiGraphics var1, Player var2, int var3, int var4, int var5, int var6, float var7, int var8, int var9, int var10, boolean var11) {
      Gui.HeartType var12 = Gui.HeartType.forPlayer(var2);
      int var13 = 9 * (var2.level().getLevelData().isHardcore() ? 5 : 0);
      int var14 = Mth.ceil((double)var7 / 2.0D);
      int var15 = Mth.ceil((double)var10 / 2.0D);
      int var16 = var14 * 2;

      for(int var17 = var14 + var15 - 1; var17 >= 0; --var17) {
         int var18 = var17 / 10;
         int var19 = var17 % 10;
         int var20 = var3 + var19 * 8;
         int var21 = var4 - var18 * var5;
         if (var8 + var10 <= 4) {
            var21 += this.random.nextInt(2);
         }

         if (var17 < var14 && var17 == var6) {
            var21 -= 2;
         }

         this.renderHeart(var1, Gui.HeartType.CONTAINER, var20, var21, var13, var11, false);
         int var22 = var17 * 2;
         boolean var23 = var17 >= var14;
         if (var23) {
            int var24 = var22 - var16;
            if (var24 < var10) {
               boolean var25 = var24 + 1 == var10;
               this.renderHeart(var1, var12 == Gui.HeartType.WITHERED ? var12 : Gui.HeartType.ABSORBING, var20, var21, var13, false, var25);
            }
         }

         boolean var26;
         if (var11 && var22 < var9) {
            var26 = var22 + 1 == var9;
            this.renderHeart(var1, var12, var20, var21, var13, true, var26);
         }

         if (var22 < var8) {
            var26 = var22 + 1 == var8;
            this.renderHeart(var1, var12, var20, var21, var13, false, var26);
         }
      }

   }

   private void renderHeart(GuiGraphics var1, Gui.HeartType var2, int var3, int var4, int var5, boolean var6, boolean var7) {
      var1.blit(GUI_ICONS_LOCATION, var3, var4, var2.getX(var7, var6), var5, 9, 9);
   }

   private void renderVehicleHealth(GuiGraphics var1) {
      LivingEntity var2 = this.getPlayerVehicleWithHealth();
      if (var2 != null) {
         int var3 = this.getVehicleMaxHearts(var2);
         if (var3 != 0) {
            int var4 = (int)Math.ceil((double)var2.getHealth());
            this.minecraft.getProfiler().popPush("mountHealth");
            int var5 = this.screenHeight - 39;
            int var6 = this.screenWidth / 2 + 91;
            int var7 = var5;
            int var8 = 0;

            for(boolean var9 = false; var3 > 0; var8 += 20) {
               int var10 = Math.min(var3, 10);
               var3 -= var10;

               for(int var11 = 0; var11 < var10; ++var11) {
                  boolean var12 = true;
                  byte var13 = 0;
                  int var14 = var6 - var11 * 8 - 9;
                  var1.blit(GUI_ICONS_LOCATION, var14, var7, 52 + var13 * 9, 9, 9, 9);
                  if (var11 * 2 + 1 + var8 < var4) {
                     var1.blit(GUI_ICONS_LOCATION, var14, var7, 88, 9, 9, 9);
                  }

                  if (var11 * 2 + 1 + var8 == var4) {
                     var1.blit(GUI_ICONS_LOCATION, var14, var7, 97, 9, 9, 9);
                  }
               }

               var7 -= 10;
            }

         }
      }
   }

   private void renderTextureOverlay(GuiGraphics var1, ResourceLocation var2, float var3) {
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      var1.setColor(1.0F, 1.0F, 1.0F, var3);
      var1.blit(var2, 0, 0, -90, 0.0F, 0.0F, this.screenWidth, this.screenHeight, this.screenWidth, this.screenHeight);
      RenderSystem.depthMask(true);
      RenderSystem.enableDepthTest();
      var1.setColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private void renderSpyglassOverlay(GuiGraphics var1, float var2) {
      float var3 = (float)Math.min(this.screenWidth, this.screenHeight);
      float var5 = Math.min((float)this.screenWidth / var3, (float)this.screenHeight / var3) * var2;
      int var6 = Mth.floor(var3 * var5);
      int var7 = Mth.floor(var3 * var5);
      int var8 = (this.screenWidth - var6) / 2;
      int var9 = (this.screenHeight - var7) / 2;
      int var10 = var8 + var6;
      int var11 = var9 + var7;
      var1.blit(SPYGLASS_SCOPE_LOCATION, var8, var9, -90, 0.0F, 0.0F, var6, var7, var6, var7);
      var1.fill(RenderType.guiOverlay(), 0, var11, this.screenWidth, this.screenHeight, -90, -16777216);
      var1.fill(RenderType.guiOverlay(), 0, 0, this.screenWidth, var9, -90, -16777216);
      var1.fill(RenderType.guiOverlay(), 0, var9, var8, var11, -90, -16777216);
      var1.fill(RenderType.guiOverlay(), var10, var9, this.screenWidth, var11, -90, -16777216);
   }

   private void updateVignetteBrightness(Entity var1) {
      if (var1 != null) {
         BlockPos var2 = BlockPos.containing(var1.getX(), var1.getEyeY(), var1.getZ());
         float var3 = LightTexture.getBrightness(var1.level().dimensionType(), var1.level().getMaxLocalRawBrightness(var2));
         float var4 = Mth.clamp(1.0F - var3, 0.0F, 1.0F);
         this.vignetteBrightness += (var4 - this.vignetteBrightness) * 0.01F;
      }
   }

   private void renderVignette(GuiGraphics var1, Entity var2) {
      WorldBorder var3 = this.minecraft.level.getWorldBorder();
      float var4 = (float)var3.getDistanceToBorder(var2);
      double var5 = Math.min(var3.getLerpSpeed() * (double)var3.getWarningTime() * 1000.0D, Math.abs(var3.getLerpTarget() - var3.getSize()));
      double var7 = Math.max((double)var3.getWarningBlocks(), var5);
      if ((double)var4 < var7) {
         var4 = 1.0F - (float)((double)var4 / var7);
      } else {
         var4 = 0.0F;
      }

      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
      if (var4 > 0.0F) {
         var4 = Mth.clamp(var4, 0.0F, 1.0F);
         var1.setColor(0.0F, var4, var4, 1.0F);
      } else {
         float var9 = this.vignetteBrightness;
         var9 = Mth.clamp(var9, 0.0F, 1.0F);
         var1.setColor(var9, var9, var9, 1.0F);
      }

      var1.blit(VIGNETTE_LOCATION, 0, 0, -90, 0.0F, 0.0F, this.screenWidth, this.screenHeight, this.screenWidth, this.screenHeight);
      RenderSystem.depthMask(true);
      RenderSystem.enableDepthTest();
      var1.setColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.defaultBlendFunc();
   }

   private void renderPortalOverlay(GuiGraphics var1, float var2) {
      if (var2 < 1.0F) {
         var2 *= var2;
         var2 *= var2;
         var2 = var2 * 0.8F + 0.2F;
      }

      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      var1.setColor(1.0F, 1.0F, 1.0F, var2);
      TextureAtlasSprite var3 = this.minecraft.getBlockRenderer().getBlockModelShaper().getParticleIcon(Blocks.NETHER_PORTAL.defaultBlockState());
      var1.blit(0, 0, -90, this.screenWidth, this.screenHeight, var3);
      RenderSystem.depthMask(true);
      RenderSystem.enableDepthTest();
      var1.setColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private void renderSlot(GuiGraphics var1, int var2, int var3, float var4, Player var5, ItemStack var6, int var7) {
      if (!var6.isEmpty()) {
         float var8 = (float)var6.getPopTime() - var4;
         if (var8 > 0.0F) {
            float var9 = 1.0F + var8 / 5.0F;
            var1.pose().pushPose();
            var1.pose().translate((float)(var2 + 8), (float)(var3 + 12), 0.0F);
            var1.pose().scale(1.0F / var9, (var9 + 1.0F) / 2.0F, 1.0F);
            var1.pose().translate((float)(-(var2 + 8)), (float)(-(var3 + 12)), 0.0F);
         }

         var1.renderItem(var5, var6, var2, var3, var7);
         if (var8 > 0.0F) {
            var1.pose().popPose();
         }

         var1.renderItemDecorations(this.minecraft.font, var6, var2, var3);
      }
   }

   public void tick(boolean var1) {
      this.tickAutosaveIndicator();
      if (!var1) {
         this.tick();
      }

   }

   private void tick() {
      if (this.overlayMessageTime > 0) {
         --this.overlayMessageTime;
      }

      if (this.titleTime > 0) {
         --this.titleTime;
         if (this.titleTime <= 0) {
            this.title = null;
            this.subtitle = null;
         }
      }

      ++this.tickCount;
      Entity var1 = this.minecraft.getCameraEntity();
      if (var1 != null) {
         this.updateVignetteBrightness(var1);
      }

      if (this.minecraft.player != null) {
         ItemStack var2 = this.minecraft.player.getInventory().getSelected();
         if (var2.isEmpty()) {
            this.toolHighlightTimer = 0;
         } else if (!this.lastToolHighlight.isEmpty() && var2.is(this.lastToolHighlight.getItem()) && var2.getHoverName().equals(this.lastToolHighlight.getHoverName())) {
            if (this.toolHighlightTimer > 0) {
               --this.toolHighlightTimer;
            }
         } else {
            this.toolHighlightTimer = (int)(40.0D * (Double)this.minecraft.options.notificationDisplayTime().get());
         }

         this.lastToolHighlight = var2;
      }

      this.chat.tick();
   }

   private void tickAutosaveIndicator() {
      IntegratedServer var1 = this.minecraft.getSingleplayerServer();
      boolean var2 = var1 != null && var1.isCurrentlySaving();
      this.lastAutosaveIndicatorValue = this.autosaveIndicatorValue;
      this.autosaveIndicatorValue = Mth.lerp(0.2F, this.autosaveIndicatorValue, var2 ? 1.0F : 0.0F);
   }

   public void setNowPlaying(Component var1) {
      MutableComponent var2 = Component.translatable("record.nowPlaying", var1);
      this.setOverlayMessage(var2, true);
      this.minecraft.getNarrator().sayNow((Component)var2);
   }

   public void setOverlayMessage(Component var1, boolean var2) {
      this.setChatDisabledByPlayerShown(false);
      this.overlayMessageString = var1;
      this.overlayMessageTime = 60;
      this.animateOverlayMessageColor = var2;
   }

   public void setChatDisabledByPlayerShown(boolean var1) {
      this.chatDisabledByPlayerShown = var1;
   }

   public boolean isShowingChatDisabledByPlayer() {
      return this.chatDisabledByPlayerShown && this.overlayMessageTime > 0;
   }

   public void setTimes(int var1, int var2, int var3) {
      if (var1 >= 0) {
         this.titleFadeInTime = var1;
      }

      if (var2 >= 0) {
         this.titleStayTime = var2;
      }

      if (var3 >= 0) {
         this.titleFadeOutTime = var3;
      }

      if (this.titleTime > 0) {
         this.titleTime = this.titleFadeInTime + this.titleStayTime + this.titleFadeOutTime;
      }

   }

   public void setSubtitle(Component var1) {
      this.subtitle = var1;
   }

   public void setTitle(Component var1) {
      this.title = var1;
      this.titleTime = this.titleFadeInTime + this.titleStayTime + this.titleFadeOutTime;
   }

   public void clear() {
      this.title = null;
      this.subtitle = null;
      this.titleTime = 0;
   }

   public ChatComponent getChat() {
      return this.chat;
   }

   public int getGuiTicks() {
      return this.tickCount;
   }

   public Font getFont() {
      return this.minecraft.font;
   }

   public SpectatorGui getSpectatorGui() {
      return this.spectatorGui;
   }

   public PlayerTabOverlay getTabList() {
      return this.tabList;
   }

   public void onDisconnected() {
      this.tabList.reset();
      this.bossOverlay.reset();
      this.minecraft.getToasts().clear();
      this.minecraft.options.renderDebug = false;
      this.chat.clearMessages(true);
   }

   public BossHealthOverlay getBossOverlay() {
      return this.bossOverlay;
   }

   public void clearCache() {
      this.debugScreen.clearChunkCache();
   }

   private void renderSavingIndicator(GuiGraphics var1) {
      if ((Boolean)this.minecraft.options.showAutosaveIndicator().get() && (this.autosaveIndicatorValue > 0.0F || this.lastAutosaveIndicatorValue > 0.0F)) {
         int var2 = Mth.floor(255.0F * Mth.clamp(Mth.lerp(this.minecraft.getFrameTime(), this.lastAutosaveIndicatorValue, this.autosaveIndicatorValue), 0.0F, 1.0F));
         if (var2 > 8) {
            Font var3 = this.getFont();
            int var4 = var3.width((FormattedText)SAVING_TEXT);
            int var5 = 16777215 | var2 << 24 & -16777216;
            var1.drawString(var3, SAVING_TEXT, this.screenWidth - var4 - 10, this.screenHeight - 15, var5);
         }
      }

   }

   private static enum HeartType {
      CONTAINER(0, false),
      NORMAL(2, true),
      POISIONED(4, true),
      WITHERED(6, true),
      ABSORBING(8, false),
      FROZEN(9, false);

      private final int index;
      private final boolean canBlink;

      private HeartType(int var3, boolean var4) {
         this.index = var3;
         this.canBlink = var4;
      }

      public int getX(boolean var1, boolean var2) {
         int var3;
         if (this == CONTAINER) {
            var3 = var2 ? 1 : 0;
         } else {
            int var4 = var1 ? 1 : 0;
            int var5 = this.canBlink && var2 ? 2 : 0;
            var3 = var4 + var5;
         }

         return 16 + (this.index * 2 + var3) * 9;
      }

      static Gui.HeartType forPlayer(Player var0) {
         Gui.HeartType var1;
         if (var0.hasEffect(MobEffects.POISON)) {
            var1 = POISIONED;
         } else if (var0.hasEffect(MobEffects.WITHER)) {
            var1 = WITHERED;
         } else if (var0.isFullyFrozen()) {
            var1 = FROZEN;
         } else {
            var1 = NORMAL;
         }

         return var1;
      }

      // $FF: synthetic method
      private static Gui.HeartType[] $values() {
         return new Gui.HeartType[]{CONTAINER, NORMAL, POISIONED, WITHERED, ABSORBING, FROZEN};
      }
   }
}
