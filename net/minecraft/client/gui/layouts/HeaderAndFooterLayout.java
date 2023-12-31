package net.minecraft.client.gui.layouts;

import java.util.function.Consumer;
import net.minecraft.client.gui.screens.Screen;

public class HeaderAndFooterLayout implements Layout {
   private static final int DEFAULT_HEADER_AND_FOOTER_HEIGHT = 36;
   private static final int DEFAULT_CONTENT_MARGIN_TOP = 30;
   private final FrameLayout headerFrame;
   private final FrameLayout footerFrame;
   private final FrameLayout contentsFrame;
   private final Screen screen;
   private int headerHeight;
   private int footerHeight;

   public HeaderAndFooterLayout(Screen var1) {
      this(var1, 36);
   }

   public HeaderAndFooterLayout(Screen var1, int var2) {
      this(var1, var2, var2);
   }

   public HeaderAndFooterLayout(Screen var1, int var2, int var3) {
      this.headerFrame = new FrameLayout();
      this.footerFrame = new FrameLayout();
      this.contentsFrame = new FrameLayout();
      this.screen = var1;
      this.headerHeight = var2;
      this.footerHeight = var3;
      this.headerFrame.defaultChildLayoutSetting().align(0.5F, 0.5F);
      this.footerFrame.defaultChildLayoutSetting().align(0.5F, 0.5F);
      this.contentsFrame.defaultChildLayoutSetting().align(0.5F, 0.0F).paddingTop(30);
   }

   public void setX(int var1) {
   }

   public void setY(int var1) {
   }

   public int getX() {
      return 0;
   }

   public int getY() {
      return 0;
   }

   public int getWidth() {
      return this.screen.width;
   }

   public int getHeight() {
      return this.screen.height;
   }

   public int getFooterHeight() {
      return this.footerHeight;
   }

   public void setFooterHeight(int var1) {
      this.footerHeight = var1;
   }

   public void setHeaderHeight(int var1) {
      this.headerHeight = var1;
   }

   public int getHeaderHeight() {
      return this.headerHeight;
   }

   public void visitChildren(Consumer<LayoutElement> var1) {
      this.headerFrame.visitChildren(var1);
      this.contentsFrame.visitChildren(var1);
      this.footerFrame.visitChildren(var1);
   }

   public void arrangeElements() {
      int var1 = this.getHeaderHeight();
      int var2 = this.getFooterHeight();
      this.headerFrame.setMinWidth(this.screen.width);
      this.headerFrame.setMinHeight(var1);
      this.headerFrame.setPosition(0, 0);
      this.headerFrame.arrangeElements();
      this.footerFrame.setMinWidth(this.screen.width);
      this.footerFrame.setMinHeight(var2);
      this.footerFrame.arrangeElements();
      this.footerFrame.setY(this.screen.height - var2);
      this.contentsFrame.setMinWidth(this.screen.width);
      this.contentsFrame.setMinHeight(this.screen.height - var1 - var2);
      this.contentsFrame.setPosition(0, var1);
      this.contentsFrame.arrangeElements();
   }

   public <T extends LayoutElement> T addToHeader(T var1) {
      return this.headerFrame.addChild(var1);
   }

   public <T extends LayoutElement> T addToHeader(T var1, LayoutSettings var2) {
      return this.headerFrame.addChild(var1, var2);
   }

   public <T extends LayoutElement> T addToFooter(T var1) {
      return this.footerFrame.addChild(var1);
   }

   public <T extends LayoutElement> T addToFooter(T var1, LayoutSettings var2) {
      return this.footerFrame.addChild(var1, var2);
   }

   public <T extends LayoutElement> T addToContents(T var1) {
      return this.contentsFrame.addChild(var1);
   }

   public <T extends LayoutElement> T addToContents(T var1, LayoutSettings var2) {
      return this.contentsFrame.addChild(var1, var2);
   }

   public LayoutSettings newHeaderLayoutSettings() {
      return this.headerFrame.newChildLayoutSettings();
   }

   public LayoutSettings newContentLayoutSettings() {
      return this.contentsFrame.newChildLayoutSettings();
   }

   public LayoutSettings newFooterLayoutSettings() {
      return this.footerFrame.newChildLayoutSettings();
   }
}
