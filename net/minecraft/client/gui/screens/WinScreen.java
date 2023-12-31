package net.minecraft.client.gui.screens;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.Musics;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

public class WinScreen extends Screen {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final ResourceLocation VIGNETTE_LOCATION = new ResourceLocation("textures/misc/vignette.png");
   private static final Component SECTION_HEADING;
   private static final String NAME_PREFIX = "           ";
   private static final String OBFUSCATE_TOKEN;
   private static final float SPEEDUP_FACTOR = 5.0F;
   private static final float SPEEDUP_FACTOR_FAST = 15.0F;
   private final boolean poem;
   private final Runnable onFinished;
   private float scroll;
   private List<FormattedCharSequence> lines;
   private IntSet centeredLines;
   private int totalScrollLength;
   private boolean speedupActive;
   private final IntSet speedupModifiers = new IntOpenHashSet();
   private float scrollSpeed;
   private final float unmodifiedScrollSpeed;
   private int direction;
   private final LogoRenderer logoRenderer = new LogoRenderer(false);

   public WinScreen(boolean var1, Runnable var2) {
      super(GameNarrator.NO_TITLE);
      this.poem = var1;
      this.onFinished = var2;
      if (!var1) {
         this.unmodifiedScrollSpeed = 0.75F;
      } else {
         this.unmodifiedScrollSpeed = 0.5F;
      }

      this.direction = 1;
      this.scrollSpeed = this.unmodifiedScrollSpeed;
   }

   private float calculateScrollSpeed() {
      return this.speedupActive ? this.unmodifiedScrollSpeed * (5.0F + (float)this.speedupModifiers.size() * 15.0F) * (float)this.direction : this.unmodifiedScrollSpeed * (float)this.direction;
   }

   public void tick() {
      this.minecraft.getMusicManager().tick();
      this.minecraft.getSoundManager().tick(false);
      float var1 = (float)(this.totalScrollLength + this.height + this.height + 24);
      if (this.scroll > var1) {
         this.respawn();
      }

   }

   public boolean keyPressed(int var1, int var2, int var3) {
      if (var1 == 265) {
         this.direction = -1;
      } else if (var1 != 341 && var1 != 345) {
         if (var1 == 32) {
            this.speedupActive = true;
         }
      } else {
         this.speedupModifiers.add(var1);
      }

      this.scrollSpeed = this.calculateScrollSpeed();
      return super.keyPressed(var1, var2, var3);
   }

   public boolean keyReleased(int var1, int var2, int var3) {
      if (var1 == 265) {
         this.direction = 1;
      }

      if (var1 == 32) {
         this.speedupActive = false;
      } else if (var1 == 341 || var1 == 345) {
         this.speedupModifiers.remove(var1);
      }

      this.scrollSpeed = this.calculateScrollSpeed();
      return super.keyReleased(var1, var2, var3);
   }

   public void onClose() {
      this.respawn();
   }

   private void respawn() {
      this.onFinished.run();
   }

   protected void init() {
      if (this.lines == null) {
         this.lines = Lists.newArrayList();
         this.centeredLines = new IntOpenHashSet();
         if (this.poem) {
            this.wrapCreditsIO("texts/end.txt", this::addPoemFile);
         }

         this.wrapCreditsIO("texts/credits.json", this::addCreditsFile);
         if (this.poem) {
            this.wrapCreditsIO("texts/postcredits.txt", this::addPoemFile);
         }

         this.totalScrollLength = this.lines.size() * 12;
      }
   }

   private void wrapCreditsIO(String var1, WinScreen.CreditsReader var2) {
      try {
         BufferedReader var3 = this.minecraft.getResourceManager().openAsReader(new ResourceLocation(var1));

         try {
            var2.read(var3);
         } catch (Throwable var7) {
            if (var3 != null) {
               try {
                  var3.close();
               } catch (Throwable var6) {
                  var7.addSuppressed(var6);
               }
            }

            throw var7;
         }

         if (var3 != null) {
            var3.close();
         }
      } catch (Exception var8) {
         LOGGER.error("Couldn't load credits", var8);
      }

   }

   private void addPoemFile(Reader var1) throws IOException {
      BufferedReader var2 = new BufferedReader(var1);
      RandomSource var3 = RandomSource.create(8124371L);

      String var4;
      int var5;
      while((var4 = var2.readLine()) != null) {
         String var6;
         String var7;
         for(var4 = var4.replaceAll("PLAYERNAME", this.minecraft.getUser().getName()); (var5 = var4.indexOf(OBFUSCATE_TOKEN)) != -1; var4 = var6 + ChatFormatting.WHITE + ChatFormatting.OBFUSCATED + "XXXXXXXX".substring(0, var3.nextInt(4) + 3) + var7) {
            var6 = var4.substring(0, var5);
            var7 = var4.substring(var5 + OBFUSCATE_TOKEN.length());
         }

         this.addPoemLines(var4);
         this.addEmptyLine();
      }

      for(var5 = 0; var5 < 8; ++var5) {
         this.addEmptyLine();
      }

   }

   private void addCreditsFile(Reader var1) {
      JsonArray var2 = GsonHelper.parseArray(var1);
      Iterator var3 = var2.iterator();

      while(var3.hasNext()) {
         JsonElement var4 = (JsonElement)var3.next();
         JsonObject var5 = var4.getAsJsonObject();
         String var6 = var5.get("section").getAsString();
         this.addCreditsLine(SECTION_HEADING, true);
         this.addCreditsLine(Component.literal(var6).withStyle(ChatFormatting.YELLOW), true);
         this.addCreditsLine(SECTION_HEADING, true);
         this.addEmptyLine();
         this.addEmptyLine();
         JsonArray var7 = var5.getAsJsonArray("disciplines");
         Iterator var8 = var7.iterator();

         while(var8.hasNext()) {
            JsonElement var9 = (JsonElement)var8.next();
            JsonObject var10 = var9.getAsJsonObject();
            String var11 = var10.get("discipline").getAsString();
            if (StringUtils.isNotEmpty(var11)) {
               this.addCreditsLine(Component.literal(var11).withStyle(ChatFormatting.YELLOW), true);
               this.addEmptyLine();
               this.addEmptyLine();
            }

            JsonArray var12 = var10.getAsJsonArray("titles");
            Iterator var13 = var12.iterator();

            while(var13.hasNext()) {
               JsonElement var14 = (JsonElement)var13.next();
               JsonObject var15 = var14.getAsJsonObject();
               String var16 = var15.get("title").getAsString();
               JsonArray var17 = var15.getAsJsonArray("names");
               this.addCreditsLine(Component.literal(var16).withStyle(ChatFormatting.GRAY), false);
               Iterator var18 = var17.iterator();

               while(var18.hasNext()) {
                  JsonElement var19 = (JsonElement)var18.next();
                  String var20 = var19.getAsString();
                  this.addCreditsLine(Component.literal("           ").append(var20).withStyle(ChatFormatting.WHITE), false);
               }

               this.addEmptyLine();
               this.addEmptyLine();
            }
         }
      }

   }

   private void addEmptyLine() {
      this.lines.add(FormattedCharSequence.EMPTY);
   }

   private void addPoemLines(String var1) {
      this.lines.addAll(this.minecraft.font.split(Component.literal(var1), 256));
   }

   private void addCreditsLine(Component var1, boolean var2) {
      if (var2) {
         this.centeredLines.add(this.lines.size());
      }

      this.lines.add(var1.getVisualOrderText());
   }

   private void renderBg(GuiGraphics var1) {
      int var2 = this.width;
      float var3 = this.scroll * 0.5F;
      boolean var4 = true;
      float var5 = this.scroll / this.unmodifiedScrollSpeed;
      float var6 = var5 * 0.02F;
      float var7 = (float)(this.totalScrollLength + this.height + this.height + 24) / this.unmodifiedScrollSpeed;
      float var8 = (var7 - 20.0F - var5) * 0.005F;
      if (var8 < var6) {
         var6 = var8;
      }

      if (var6 > 1.0F) {
         var6 = 1.0F;
      }

      var6 *= var6;
      var6 = var6 * 96.0F / 255.0F;
      var1.setColor(var6, var6, var6, 1.0F);
      var1.blit(BACKGROUND_LOCATION, 0, 0, 0, 0.0F, var3, var2, this.height, 64, 64);
      var1.setColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public void render(GuiGraphics var1, int var2, int var3, float var4) {
      this.scroll = Math.max(0.0F, this.scroll + var4 * this.scrollSpeed);
      this.renderBg(var1);
      int var5 = this.width / 2 - 128;
      int var6 = this.height + 50;
      float var7 = -this.scroll;
      var1.pose().pushPose();
      var1.pose().translate(0.0F, var7, 0.0F);
      this.logoRenderer.renderLogo(var1, this.width, 1.0F, var6);
      int var8 = var6 + 100;

      for(int var9 = 0; var9 < this.lines.size(); ++var9) {
         if (var9 == this.lines.size() - 1) {
            float var10 = (float)var8 + var7 - (float)(this.height / 2 - 6);
            if (var10 < 0.0F) {
               var1.pose().translate(0.0F, -var10, 0.0F);
            }
         }

         if ((float)var8 + var7 + 12.0F + 8.0F > 0.0F && (float)var8 + var7 < (float)this.height) {
            FormattedCharSequence var11 = (FormattedCharSequence)this.lines.get(var9);
            if (this.centeredLines.contains(var9)) {
               var1.drawCenteredString(this.font, var11, var5 + 128, var8, 16777215);
            } else {
               var1.drawString(this.font, var11, var5, var8, 16777215);
            }
         }

         var8 += 12;
      }

      var1.pose().popPose();
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR);
      var1.blit(VIGNETTE_LOCATION, 0, 0, 0, 0.0F, 0.0F, this.width, this.height, this.width, this.height);
      RenderSystem.disableBlend();
      RenderSystem.defaultBlendFunc();
      super.render(var1, var2, var3, var4);
   }

   public void removed() {
      this.minecraft.getMusicManager().stopPlaying(Musics.CREDITS);
   }

   public Music getBackgroundMusic() {
      return Musics.CREDITS;
   }

   static {
      SECTION_HEADING = Component.literal("============").withStyle(ChatFormatting.WHITE);
      OBFUSCATE_TOKEN = ChatFormatting.WHITE + ChatFormatting.OBFUSCATED + ChatFormatting.GREEN + ChatFormatting.AQUA;
   }

   @FunctionalInterface
   private interface CreditsReader {
      void read(Reader var1) throws IOException;
   }
}
