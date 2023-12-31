package net.minecraft.client.gui;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Divisor;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector2ic;

public class GuiGraphics {
   public static final float MAX_GUI_Z = 10000.0F;
   public static final float MIN_GUI_Z = -10000.0F;
   private static final int EXTRA_SPACE_AFTER_FIRST_TOOLTIP_LINE = 2;
   private final Minecraft minecraft;
   private final PoseStack pose;
   private final MultiBufferSource.BufferSource bufferSource;
   private final GuiGraphics.ScissorStack scissorStack;
   private boolean managed;

   private GuiGraphics(Minecraft var1, PoseStack var2, MultiBufferSource.BufferSource var3) {
      this.scissorStack = new GuiGraphics.ScissorStack();
      this.minecraft = var1;
      this.pose = var2;
      this.bufferSource = var3;
   }

   public GuiGraphics(Minecraft var1, MultiBufferSource.BufferSource var2) {
      this(var1, new PoseStack(), var2);
   }

   /** @deprecated */
   @Deprecated
   public void drawManaged(Runnable var1) {
      this.flush();
      this.managed = true;
      var1.run();
      this.managed = false;
      this.flush();
   }

   /** @deprecated */
   @Deprecated
   private void flushIfUnmanaged() {
      if (!this.managed) {
         this.flush();
      }

   }

   /** @deprecated */
   @Deprecated
   private void flushIfManaged() {
      if (this.managed) {
         this.flush();
      }

   }

   public int guiWidth() {
      return this.minecraft.getWindow().getGuiScaledWidth();
   }

   public int guiHeight() {
      return this.minecraft.getWindow().getGuiScaledHeight();
   }

   public PoseStack pose() {
      return this.pose;
   }

   public MultiBufferSource.BufferSource bufferSource() {
      return this.bufferSource;
   }

   public void flush() {
      RenderSystem.disableDepthTest();
      this.bufferSource.endBatch();
      RenderSystem.enableDepthTest();
   }

   public void hLine(int var1, int var2, int var3, int var4) {
      this.hLine(RenderType.gui(), var1, var2, var3, var4);
   }

   public void hLine(RenderType var1, int var2, int var3, int var4, int var5) {
      if (var3 < var2) {
         int var6 = var2;
         var2 = var3;
         var3 = var6;
      }

      this.fill(var1, var2, var4, var3 + 1, var4 + 1, var5);
   }

   public void vLine(int var1, int var2, int var3, int var4) {
      this.vLine(RenderType.gui(), var1, var2, var3, var4);
   }

   public void vLine(RenderType var1, int var2, int var3, int var4, int var5) {
      if (var4 < var3) {
         int var6 = var3;
         var3 = var4;
         var4 = var6;
      }

      this.fill(var1, var2, var3 + 1, var2 + 1, var4, var5);
   }

   public void enableScissor(int var1, int var2, int var3, int var4) {
      this.applyScissor(this.scissorStack.push(new ScreenRectangle(var1, var2, var3 - var1, var4 - var2)));
   }

   public void disableScissor() {
      this.applyScissor(this.scissorStack.pop());
   }

   private void applyScissor(@Nullable ScreenRectangle var1) {
      this.flushIfManaged();
      if (var1 != null) {
         Window var2 = Minecraft.getInstance().getWindow();
         int var3 = var2.getHeight();
         double var4 = var2.getGuiScale();
         double var6 = (double)var1.left() * var4;
         double var8 = (double)var3 - (double)var1.bottom() * var4;
         double var10 = (double)var1.width() * var4;
         double var12 = (double)var1.height() * var4;
         RenderSystem.enableScissor((int)var6, (int)var8, Math.max(0, (int)var10), Math.max(0, (int)var12));
      } else {
         RenderSystem.disableScissor();
      }

   }

   public void setColor(float var1, float var2, float var3, float var4) {
      this.flushIfManaged();
      RenderSystem.setShaderColor(var1, var2, var3, var4);
   }

   public void fill(int var1, int var2, int var3, int var4, int var5) {
      this.fill(var1, var2, var3, var4, 0, var5);
   }

   public void fill(int var1, int var2, int var3, int var4, int var5, int var6) {
      this.fill(RenderType.gui(), var1, var2, var3, var4, var5, var6);
   }

   public void fill(RenderType var1, int var2, int var3, int var4, int var5, int var6) {
      this.fill(var1, var2, var3, var4, var5, 0, var6);
   }

   public void fill(RenderType var1, int var2, int var3, int var4, int var5, int var6, int var7) {
      Matrix4f var8 = this.pose.last().pose();
      int var9;
      if (var2 < var4) {
         var9 = var2;
         var2 = var4;
         var4 = var9;
      }

      if (var3 < var5) {
         var9 = var3;
         var3 = var5;
         var5 = var9;
      }

      float var14 = (float)FastColor.ARGB32.alpha(var7) / 255.0F;
      float var10 = (float)FastColor.ARGB32.red(var7) / 255.0F;
      float var11 = (float)FastColor.ARGB32.green(var7) / 255.0F;
      float var12 = (float)FastColor.ARGB32.blue(var7) / 255.0F;
      VertexConsumer var13 = this.bufferSource.getBuffer(var1);
      var13.vertex(var8, (float)var2, (float)var3, (float)var6).color(var10, var11, var12, var14).endVertex();
      var13.vertex(var8, (float)var2, (float)var5, (float)var6).color(var10, var11, var12, var14).endVertex();
      var13.vertex(var8, (float)var4, (float)var5, (float)var6).color(var10, var11, var12, var14).endVertex();
      var13.vertex(var8, (float)var4, (float)var3, (float)var6).color(var10, var11, var12, var14).endVertex();
      this.flushIfUnmanaged();
   }

   public void fillGradient(int var1, int var2, int var3, int var4, int var5, int var6) {
      this.fillGradient(var1, var2, var3, var4, 0, var5, var6);
   }

   public void fillGradient(int var1, int var2, int var3, int var4, int var5, int var6, int var7) {
      this.fillGradient(RenderType.gui(), var1, var2, var3, var4, var6, var7, var5);
   }

   public void fillGradient(RenderType var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8) {
      VertexConsumer var9 = this.bufferSource.getBuffer(var1);
      this.fillGradient(var9, var2, var3, var4, var5, var8, var6, var7);
      this.flushIfUnmanaged();
   }

   private void fillGradient(VertexConsumer var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8) {
      float var9 = (float)FastColor.ARGB32.alpha(var7) / 255.0F;
      float var10 = (float)FastColor.ARGB32.red(var7) / 255.0F;
      float var11 = (float)FastColor.ARGB32.green(var7) / 255.0F;
      float var12 = (float)FastColor.ARGB32.blue(var7) / 255.0F;
      float var13 = (float)FastColor.ARGB32.alpha(var8) / 255.0F;
      float var14 = (float)FastColor.ARGB32.red(var8) / 255.0F;
      float var15 = (float)FastColor.ARGB32.green(var8) / 255.0F;
      float var16 = (float)FastColor.ARGB32.blue(var8) / 255.0F;
      Matrix4f var17 = this.pose.last().pose();
      var1.vertex(var17, (float)var2, (float)var3, (float)var6).color(var10, var11, var12, var9).endVertex();
      var1.vertex(var17, (float)var2, (float)var5, (float)var6).color(var14, var15, var16, var13).endVertex();
      var1.vertex(var17, (float)var4, (float)var5, (float)var6).color(var14, var15, var16, var13).endVertex();
      var1.vertex(var17, (float)var4, (float)var3, (float)var6).color(var10, var11, var12, var9).endVertex();
   }

   public void drawCenteredString(Font var1, String var2, int var3, int var4, int var5) {
      this.drawString(var1, var2, var3 - var1.width(var2) / 2, var4, var5);
   }

   public void drawCenteredString(Font var1, Component var2, int var3, int var4, int var5) {
      FormattedCharSequence var6 = var2.getVisualOrderText();
      this.drawString(var1, var6, var3 - var1.width(var6) / 2, var4, var5);
   }

   public void drawCenteredString(Font var1, FormattedCharSequence var2, int var3, int var4, int var5) {
      this.drawString(var1, var2, var3 - var1.width(var2) / 2, var4, var5);
   }

   public int drawString(Font var1, @Nullable String var2, int var3, int var4, int var5) {
      return this.drawString(var1, var2, var3, var4, var5, true);
   }

   public int drawString(Font var1, @Nullable String var2, int var3, int var4, int var5, boolean var6) {
      if (var2 == null) {
         return 0;
      } else {
         int var7 = var1.drawInBatch(var2, (float)var3, (float)var4, var5, var6, this.pose.last().pose(), this.bufferSource, Font.DisplayMode.NORMAL, 0, 15728880, var1.isBidirectional());
         this.flushIfUnmanaged();
         return var7;
      }
   }

   public int drawString(Font var1, FormattedCharSequence var2, int var3, int var4, int var5) {
      return this.drawString(var1, var2, var3, var4, var5, true);
   }

   public int drawString(Font var1, FormattedCharSequence var2, int var3, int var4, int var5, boolean var6) {
      int var7 = var1.drawInBatch((FormattedCharSequence)var2, (float)var3, (float)var4, var5, var6, this.pose.last().pose(), this.bufferSource, Font.DisplayMode.NORMAL, 0, 15728880);
      this.flushIfUnmanaged();
      return var7;
   }

   public int drawString(Font var1, Component var2, int var3, int var4, int var5) {
      return this.drawString(var1, var2, var3, var4, var5, true);
   }

   public int drawString(Font var1, Component var2, int var3, int var4, int var5, boolean var6) {
      return this.drawString(var1, var2.getVisualOrderText(), var3, var4, var5, var6);
   }

   public void drawWordWrap(Font var1, FormattedText var2, int var3, int var4, int var5, int var6) {
      for(Iterator var7 = var1.split(var2, var5).iterator(); var7.hasNext(); var4 += 9) {
         FormattedCharSequence var8 = (FormattedCharSequence)var7.next();
         this.drawString(var1, var8, var3, var4, var6, false);
         Objects.requireNonNull(var1);
      }

   }

   public void blit(int var1, int var2, int var3, int var4, int var5, TextureAtlasSprite var6) {
      this.innerBlit(var6.atlasLocation(), var1, var1 + var4, var2, var2 + var5, var3, var6.getU0(), var6.getU1(), var6.getV0(), var6.getV1());
   }

   public void blit(int var1, int var2, int var3, int var4, int var5, TextureAtlasSprite var6, float var7, float var8, float var9, float var10) {
      this.innerBlit(var6.atlasLocation(), var1, var1 + var4, var2, var2 + var5, var3, var6.getU0(), var6.getU1(), var6.getV0(), var6.getV1(), var7, var8, var9, var10);
   }

   public void renderOutline(int var1, int var2, int var3, int var4, int var5) {
      this.fill(var1, var2, var1 + var3, var2 + 1, var5);
      this.fill(var1, var2 + var4 - 1, var1 + var3, var2 + var4, var5);
      this.fill(var1, var2 + 1, var1 + 1, var2 + var4 - 1, var5);
      this.fill(var1 + var3 - 1, var2 + 1, var1 + var3, var2 + var4 - 1, var5);
   }

   public void blit(ResourceLocation var1, int var2, int var3, int var4, int var5, int var6, int var7) {
      this.blit(var1, var2, var3, 0, (float)var4, (float)var5, var6, var7, 256, 256);
   }

   public void blit(ResourceLocation var1, int var2, int var3, int var4, float var5, float var6, int var7, int var8, int var9, int var10) {
      this.blit(var1, var2, var2 + var7, var3, var3 + var8, var4, var7, var8, var5, var6, var9, var10);
   }

   public void blit(ResourceLocation var1, int var2, int var3, int var4, int var5, float var6, float var7, int var8, int var9, int var10, int var11) {
      this.blit(var1, var2, var2 + var4, var3, var3 + var5, 0, var8, var9, var6, var7, var10, var11);
   }

   public void blit(ResourceLocation var1, int var2, int var3, float var4, float var5, int var6, int var7, int var8, int var9) {
      this.blit(var1, var2, var3, var6, var7, var4, var5, var6, var7, var8, var9);
   }

   void blit(ResourceLocation var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, float var9, float var10, int var11, int var12) {
      this.innerBlit(var1, var2, var3, var4, var5, var6, (var9 + 0.0F) / (float)var11, (var9 + (float)var7) / (float)var11, (var10 + 0.0F) / (float)var12, (var10 + (float)var8) / (float)var12);
   }

   void innerBlit(ResourceLocation var1, int var2, int var3, int var4, int var5, int var6, float var7, float var8, float var9, float var10) {
      RenderSystem.setShaderTexture(0, var1);
      RenderSystem.setShader(GameRenderer::getPositionTexShader);
      Matrix4f var11 = this.pose.last().pose();
      BufferBuilder var12 = Tesselator.getInstance().getBuilder();
      var12.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
      var12.vertex(var11, (float)var2, (float)var4, (float)var6).uv(var7, var9).endVertex();
      var12.vertex(var11, (float)var2, (float)var5, (float)var6).uv(var7, var10).endVertex();
      var12.vertex(var11, (float)var3, (float)var5, (float)var6).uv(var8, var10).endVertex();
      var12.vertex(var11, (float)var3, (float)var4, (float)var6).uv(var8, var9).endVertex();
      BufferUploader.drawWithShader(var12.end());
   }

   void innerBlit(ResourceLocation var1, int var2, int var3, int var4, int var5, int var6, float var7, float var8, float var9, float var10, float var11, float var12, float var13, float var14) {
      RenderSystem.setShaderTexture(0, var1);
      RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
      RenderSystem.enableBlend();
      Matrix4f var15 = this.pose.last().pose();
      BufferBuilder var16 = Tesselator.getInstance().getBuilder();
      var16.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
      var16.vertex(var15, (float)var2, (float)var4, (float)var6).color(var11, var12, var13, var14).uv(var7, var9).endVertex();
      var16.vertex(var15, (float)var2, (float)var5, (float)var6).color(var11, var12, var13, var14).uv(var7, var10).endVertex();
      var16.vertex(var15, (float)var3, (float)var5, (float)var6).color(var11, var12, var13, var14).uv(var8, var10).endVertex();
      var16.vertex(var15, (float)var3, (float)var4, (float)var6).color(var11, var12, var13, var14).uv(var8, var9).endVertex();
      BufferUploader.drawWithShader(var16.end());
      RenderSystem.disableBlend();
   }

   public void blitNineSliced(ResourceLocation var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, int var9, int var10) {
      this.blitNineSliced(var1, var2, var3, var4, var5, var6, var6, var6, var6, var7, var8, var9, var10);
   }

   public void blitNineSliced(ResourceLocation var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, int var9, int var10, int var11) {
      this.blitNineSliced(var1, var2, var3, var4, var5, var6, var7, var6, var7, var8, var9, var10, var11);
   }

   public void blitNineSliced(ResourceLocation var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, int var9, int var10, int var11, int var12, int var13) {
      var6 = Math.min(var6, var4 / 2);
      var8 = Math.min(var8, var4 / 2);
      var7 = Math.min(var7, var5 / 2);
      var9 = Math.min(var9, var5 / 2);
      if (var4 == var10 && var5 == var11) {
         this.blit(var1, var2, var3, var12, var13, var4, var5);
      } else if (var5 == var11) {
         this.blit(var1, var2, var3, var12, var13, var6, var5);
         this.blitRepeating(var1, var2 + var6, var3, var4 - var8 - var6, var5, var12 + var6, var13, var10 - var8 - var6, var11);
         this.blit(var1, var2 + var4 - var8, var3, var12 + var10 - var8, var13, var8, var5);
      } else if (var4 == var10) {
         this.blit(var1, var2, var3, var12, var13, var4, var7);
         this.blitRepeating(var1, var2, var3 + var7, var4, var5 - var9 - var7, var12, var13 + var7, var10, var11 - var9 - var7);
         this.blit(var1, var2, var3 + var5 - var9, var12, var13 + var11 - var9, var4, var9);
      } else {
         this.blit(var1, var2, var3, var12, var13, var6, var7);
         this.blitRepeating(var1, var2 + var6, var3, var4 - var8 - var6, var7, var12 + var6, var13, var10 - var8 - var6, var7);
         this.blit(var1, var2 + var4 - var8, var3, var12 + var10 - var8, var13, var8, var7);
         this.blit(var1, var2, var3 + var5 - var9, var12, var13 + var11 - var9, var6, var9);
         this.blitRepeating(var1, var2 + var6, var3 + var5 - var9, var4 - var8 - var6, var9, var12 + var6, var13 + var11 - var9, var10 - var8 - var6, var9);
         this.blit(var1, var2 + var4 - var8, var3 + var5 - var9, var12 + var10 - var8, var13 + var11 - var9, var8, var9);
         this.blitRepeating(var1, var2, var3 + var7, var6, var5 - var9 - var7, var12, var13 + var7, var6, var11 - var9 - var7);
         this.blitRepeating(var1, var2 + var6, var3 + var7, var4 - var8 - var6, var5 - var9 - var7, var12 + var6, var13 + var7, var10 - var8 - var6, var11 - var9 - var7);
         this.blitRepeating(var1, var2 + var4 - var8, var3 + var7, var6, var5 - var9 - var7, var12 + var10 - var8, var13 + var7, var8, var11 - var9 - var7);
      }
   }

   public void blitRepeating(ResourceLocation var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, int var9) {
      int var10 = var2;

      int var12;
      for(IntIterator var11 = slices(var4, var8); var11.hasNext(); var10 += var12) {
         var12 = var11.nextInt();
         int var13 = (var8 - var12) / 2;
         int var14 = var3;

         int var16;
         for(IntIterator var15 = slices(var5, var9); var15.hasNext(); var14 += var16) {
            var16 = var15.nextInt();
            int var17 = (var9 - var16) / 2;
            this.blit(var1, var10, var14, var6 + var13, var7 + var17, var12, var16);
         }
      }

   }

   private static IntIterator slices(int var0, int var1) {
      int var2 = Mth.positiveCeilDiv(var0, var1);
      return new Divisor(var0, var2);
   }

   public void renderItem(ItemStack var1, int var2, int var3) {
      this.renderItem(this.minecraft.player, this.minecraft.level, var1, var2, var3, 0);
   }

   public void renderItem(ItemStack var1, int var2, int var3, int var4) {
      this.renderItem(this.minecraft.player, this.minecraft.level, var1, var2, var3, var4);
   }

   public void renderItem(ItemStack var1, int var2, int var3, int var4, int var5) {
      this.renderItem(this.minecraft.player, this.minecraft.level, var1, var2, var3, var4, var5);
   }

   public void renderFakeItem(ItemStack var1, int var2, int var3) {
      this.renderItem((LivingEntity)null, this.minecraft.level, var1, var2, var3, 0);
   }

   public void renderItem(LivingEntity var1, ItemStack var2, int var3, int var4, int var5) {
      this.renderItem(var1, var1.level(), var2, var3, var4, var5);
   }

   private void renderItem(@Nullable LivingEntity var1, @Nullable Level var2, ItemStack var3, int var4, int var5, int var6) {
      this.renderItem(var1, var2, var3, var4, var5, var6, 0);
   }

   private void renderItem(@Nullable LivingEntity var1, @Nullable Level var2, ItemStack var3, int var4, int var5, int var6, int var7) {
      if (!var3.isEmpty()) {
         BakedModel var8 = this.minecraft.getItemRenderer().getModel(var3, var2, var1, var6);
         this.pose.pushPose();
         this.pose.translate((float)(var4 + 8), (float)(var5 + 8), (float)(150 + (var8.isGui3d() ? var7 : 0)));

         try {
            this.pose.mulPoseMatrix((new Matrix4f()).scaling(1.0F, -1.0F, 1.0F));
            this.pose.scale(16.0F, 16.0F, 16.0F);
            boolean var9 = !var8.usesBlockLight();
            if (var9) {
               Lighting.setupForFlatItems();
            }

            this.minecraft.getItemRenderer().render(var3, ItemDisplayContext.GUI, false, this.pose, this.bufferSource(), 15728880, OverlayTexture.NO_OVERLAY, var8);
            this.flush();
            if (var9) {
               Lighting.setupFor3DItems();
            }
         } catch (Throwable var12) {
            CrashReport var10 = CrashReport.forThrowable(var12, "Rendering item");
            CrashReportCategory var11 = var10.addCategory("Item being rendered");
            var11.setDetail("Item Type", () -> {
               return String.valueOf(var3.getItem());
            });
            var11.setDetail("Item Damage", () -> {
               return String.valueOf(var3.getDamageValue());
            });
            var11.setDetail("Item NBT", () -> {
               return String.valueOf(var3.getTag());
            });
            var11.setDetail("Item Foil", () -> {
               return String.valueOf(var3.hasFoil());
            });
            throw new ReportedException(var10);
         }

         this.pose.popPose();
      }
   }

   public void renderItemDecorations(Font var1, ItemStack var2, int var3, int var4) {
      this.renderItemDecorations(var1, var2, var3, var4, (String)null);
   }

   public void renderItemDecorations(Font var1, ItemStack var2, int var3, int var4, @Nullable String var5) {
      if (!var2.isEmpty()) {
         this.pose.pushPose();
         if (var2.getCount() != 1 || var5 != null) {
            String var6 = var5 == null ? String.valueOf(var2.getCount()) : var5;
            this.pose.translate(0.0F, 0.0F, 200.0F);
            this.drawString(var1, var6, var3 + 19 - 2 - var1.width(var6), var4 + 6 + 3, 16777215, true);
         }

         int var8;
         int var9;
         if (var2.isBarVisible()) {
            int var10 = var2.getBarWidth();
            int var7 = var2.getBarColor();
            var8 = var3 + 2;
            var9 = var4 + 13;
            this.fill(RenderType.guiOverlay(), var8, var9, var8 + 13, var9 + 2, -16777216);
            this.fill(RenderType.guiOverlay(), var8, var9, var8 + var10, var9 + 1, var7 | -16777216);
         }

         LocalPlayer var11 = this.minecraft.player;
         float var12 = var11 == null ? 0.0F : var11.getCooldowns().getCooldownPercent(var2.getItem(), this.minecraft.getFrameTime());
         if (var12 > 0.0F) {
            var8 = var4 + Mth.floor(16.0F * (1.0F - var12));
            var9 = var8 + Mth.ceil(16.0F * var12);
            this.fill(RenderType.guiOverlay(), var3, var8, var3 + 16, var9, Integer.MAX_VALUE);
         }

         this.pose.popPose();
      }
   }

   public void renderTooltip(Font var1, ItemStack var2, int var3, int var4) {
      this.renderTooltip(var1, Screen.getTooltipFromItem(this.minecraft, var2), var2.getTooltipImage(), var3, var4);
   }

   public void renderTooltip(Font var1, List<Component> var2, Optional<TooltipComponent> var3, int var4, int var5) {
      List var6 = (List)var2.stream().map(Component::getVisualOrderText).map(ClientTooltipComponent::create).collect(Collectors.toList());
      var3.ifPresent((var1x) -> {
         var6.add(1, ClientTooltipComponent.create(var1x));
      });
      this.renderTooltipInternal(var1, var6, var4, var5, DefaultTooltipPositioner.INSTANCE);
   }

   public void renderTooltip(Font var1, Component var2, int var3, int var4) {
      this.renderTooltip(var1, List.of(var2.getVisualOrderText()), var3, var4);
   }

   public void renderComponentTooltip(Font var1, List<Component> var2, int var3, int var4) {
      this.renderTooltip(var1, Lists.transform(var2, Component::getVisualOrderText), var3, var4);
   }

   public void renderTooltip(Font var1, List<? extends FormattedCharSequence> var2, int var3, int var4) {
      this.renderTooltipInternal(var1, (List)var2.stream().map(ClientTooltipComponent::create).collect(Collectors.toList()), var3, var4, DefaultTooltipPositioner.INSTANCE);
   }

   public void renderTooltip(Font var1, List<FormattedCharSequence> var2, ClientTooltipPositioner var3, int var4, int var5) {
      this.renderTooltipInternal(var1, (List)var2.stream().map(ClientTooltipComponent::create).collect(Collectors.toList()), var4, var5, var3);
   }

   private void renderTooltipInternal(Font var1, List<ClientTooltipComponent> var2, int var3, int var4, ClientTooltipPositioner var5) {
      if (!var2.isEmpty()) {
         int var6 = 0;
         int var7 = var2.size() == 1 ? -2 : 0;

         ClientTooltipComponent var9;
         for(Iterator var8 = var2.iterator(); var8.hasNext(); var7 += var9.getHeight()) {
            var9 = (ClientTooltipComponent)var8.next();
            int var10 = var9.getWidth(var1);
            if (var10 > var6) {
               var6 = var10;
            }
         }

         Vector2ic var17 = var5.positionTooltip(this.guiWidth(), this.guiHeight(), var3, var4, var6, var7);
         int var11 = var17.x();
         int var12 = var17.y();
         this.pose.pushPose();
         boolean var13 = true;
         this.drawManaged(() -> {
            TooltipRenderUtil.renderTooltipBackground(this, var11, var12, var6, var7, 400);
         });
         this.pose.translate(0.0F, 0.0F, 400.0F);
         int var14 = var12;

         int var15;
         ClientTooltipComponent var16;
         for(var15 = 0; var15 < var2.size(); ++var15) {
            var16 = (ClientTooltipComponent)var2.get(var15);
            var16.renderText(var1, var11, var14, this.pose.last().pose(), this.bufferSource);
            var14 += var16.getHeight() + (var15 == 0 ? 2 : 0);
         }

         var14 = var12;

         for(var15 = 0; var15 < var2.size(); ++var15) {
            var16 = (ClientTooltipComponent)var2.get(var15);
            var16.renderImage(var1, var11, var14, this);
            var14 += var16.getHeight() + (var15 == 0 ? 2 : 0);
         }

         this.pose.popPose();
      }
   }

   public void renderComponentHoverEffect(Font var1, @Nullable Style var2, int var3, int var4) {
      if (var2 != null && var2.getHoverEvent() != null) {
         HoverEvent var5 = var2.getHoverEvent();
         HoverEvent.ItemStackInfo var6 = (HoverEvent.ItemStackInfo)var5.getValue(HoverEvent.Action.SHOW_ITEM);
         if (var6 != null) {
            this.renderTooltip(var1, var6.getItemStack(), var3, var4);
         } else {
            HoverEvent.EntityTooltipInfo var7 = (HoverEvent.EntityTooltipInfo)var5.getValue(HoverEvent.Action.SHOW_ENTITY);
            if (var7 != null) {
               if (this.minecraft.options.advancedItemTooltips) {
                  this.renderComponentTooltip(var1, var7.getTooltipLines(), var3, var4);
               }
            } else {
               Component var8 = (Component)var5.getValue(HoverEvent.Action.SHOW_TEXT);
               if (var8 != null) {
                  this.renderTooltip(var1, var1.split(var8, Math.max(this.guiWidth() / 2, 200)), var3, var4);
               }
            }
         }

      }
   }

   static class ScissorStack {
      private final Deque<ScreenRectangle> stack = new ArrayDeque();

      ScissorStack() {
      }

      public ScreenRectangle push(ScreenRectangle var1) {
         ScreenRectangle var2 = (ScreenRectangle)this.stack.peekLast();
         if (var2 != null) {
            ScreenRectangle var3 = (ScreenRectangle)Objects.requireNonNullElse(var1.intersection(var2), ScreenRectangle.empty());
            this.stack.addLast(var3);
            return var3;
         } else {
            this.stack.addLast(var1);
            return var1;
         }
      }

      @Nullable
      public ScreenRectangle pop() {
         if (this.stack.isEmpty()) {
            throw new IllegalStateException("Scissor stack underflow");
         } else {
            this.stack.removeLast();
            return (ScreenRectangle)this.stack.peekLast();
         }
      }
   }
}
