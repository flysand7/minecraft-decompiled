package net.minecraft.client.main;

import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.authlib.properties.PropertyMap.Serializer;
import com.mojang.blaze3d.platform.DisplayData;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import javax.annotation.Nullable;
import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.NonOptionArgumentSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.OptionSpecBuilder;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.User;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.client.telemetry.TelemetryProperty;
import net.minecraft.client.telemetry.events.GameLoadTimesEvent;
import net.minecraft.core.UUIDUtil;
import net.minecraft.obfuscate.DontObfuscate;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.NativeModuleLister;
import net.minecraft.util.profiling.jfr.Environment;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import org.slf4j.Logger;

public class Main {
   static final Logger LOGGER = LogUtils.getLogger();

   public Main() {
   }

   @DontObfuscate
   public static void main(String[] var0) {
      Stopwatch var1 = Stopwatch.createStarted(Ticker.systemTicker());
      Stopwatch var2 = Stopwatch.createStarted(Ticker.systemTicker());
      GameLoadTimesEvent.INSTANCE.beginStep(TelemetryProperty.LOAD_TIME_TOTAL_TIME_MS, var1);
      GameLoadTimesEvent.INSTANCE.beginStep(TelemetryProperty.LOAD_TIME_PRE_WINDOW_MS, var2);
      SharedConstants.tryDetectVersion();
      SharedConstants.enableDataFixerOptimizations();
      OptionParser var3 = new OptionParser();
      var3.allowsUnrecognizedOptions();
      var3.accepts("demo");
      var3.accepts("disableMultiplayer");
      var3.accepts("disableChat");
      var3.accepts("fullscreen");
      var3.accepts("checkGlErrors");
      OptionSpecBuilder var4 = var3.accepts("jfrProfile");
      ArgumentAcceptingOptionSpec var5 = var3.accepts("quickPlayPath").withRequiredArg();
      ArgumentAcceptingOptionSpec var6 = var3.accepts("quickPlaySingleplayer").withRequiredArg();
      ArgumentAcceptingOptionSpec var7 = var3.accepts("quickPlayMultiplayer").withRequiredArg();
      ArgumentAcceptingOptionSpec var8 = var3.accepts("quickPlayRealms").withRequiredArg();
      ArgumentAcceptingOptionSpec var9 = var3.accepts("gameDir").withRequiredArg().ofType(File.class).defaultsTo(new File("."), new File[0]);
      ArgumentAcceptingOptionSpec var10 = var3.accepts("assetsDir").withRequiredArg().ofType(File.class);
      ArgumentAcceptingOptionSpec var11 = var3.accepts("resourcePackDir").withRequiredArg().ofType(File.class);
      ArgumentAcceptingOptionSpec var12 = var3.accepts("proxyHost").withRequiredArg();
      ArgumentAcceptingOptionSpec var13 = var3.accepts("proxyPort").withRequiredArg().defaultsTo("8080", new String[0]).ofType(Integer.class);
      ArgumentAcceptingOptionSpec var14 = var3.accepts("proxyUser").withRequiredArg();
      ArgumentAcceptingOptionSpec var15 = var3.accepts("proxyPass").withRequiredArg();
      ArgumentAcceptingOptionSpec var16 = var3.accepts("username").withRequiredArg().defaultsTo("Player" + Util.getMillis() % 1000L, new String[0]);
      ArgumentAcceptingOptionSpec var17 = var3.accepts("uuid").withRequiredArg();
      ArgumentAcceptingOptionSpec var18 = var3.accepts("xuid").withOptionalArg().defaultsTo("", new String[0]);
      ArgumentAcceptingOptionSpec var19 = var3.accepts("clientId").withOptionalArg().defaultsTo("", new String[0]);
      ArgumentAcceptingOptionSpec var20 = var3.accepts("accessToken").withRequiredArg().required();
      ArgumentAcceptingOptionSpec var21 = var3.accepts("version").withRequiredArg().required();
      ArgumentAcceptingOptionSpec var22 = var3.accepts("width").withRequiredArg().ofType(Integer.class).defaultsTo(854, new Integer[0]);
      ArgumentAcceptingOptionSpec var23 = var3.accepts("height").withRequiredArg().ofType(Integer.class).defaultsTo(480, new Integer[0]);
      ArgumentAcceptingOptionSpec var24 = var3.accepts("fullscreenWidth").withRequiredArg().ofType(Integer.class);
      ArgumentAcceptingOptionSpec var25 = var3.accepts("fullscreenHeight").withRequiredArg().ofType(Integer.class);
      ArgumentAcceptingOptionSpec var26 = var3.accepts("userProperties").withRequiredArg().defaultsTo("{}", new String[0]);
      ArgumentAcceptingOptionSpec var27 = var3.accepts("profileProperties").withRequiredArg().defaultsTo("{}", new String[0]);
      ArgumentAcceptingOptionSpec var28 = var3.accepts("assetIndex").withRequiredArg();
      ArgumentAcceptingOptionSpec var29 = var3.accepts("userType").withRequiredArg().defaultsTo(User.Type.LEGACY.getName(), new String[0]);
      ArgumentAcceptingOptionSpec var30 = var3.accepts("versionType").withRequiredArg().defaultsTo("release", new String[0]);
      NonOptionArgumentSpec var31 = var3.nonOptions();
      OptionSet var32 = var3.parse(var0);
      List var33 = var32.valuesOf(var31);
      if (!var33.isEmpty()) {
         System.out.println("Completely ignored arguments: " + var33);
      }

      String var34 = (String)parseArgument(var32, var12);
      Proxy var35 = Proxy.NO_PROXY;
      if (var34 != null) {
         try {
            var35 = new Proxy(Type.SOCKS, new InetSocketAddress(var34, (Integer)parseArgument(var32, var13)));
         } catch (Exception var83) {
         }
      }

      final String var36 = (String)parseArgument(var32, var14);
      final String var37 = (String)parseArgument(var32, var15);
      if (!var35.equals(Proxy.NO_PROXY) && stringHasValue(var36) && stringHasValue(var37)) {
         Authenticator.setDefault(new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
               return new PasswordAuthentication(var36, var37.toCharArray());
            }
         });
      }

      int var38 = (Integer)parseArgument(var32, var22);
      int var39 = (Integer)parseArgument(var32, var23);
      OptionalInt var40 = ofNullable((Integer)parseArgument(var32, var24));
      OptionalInt var41 = ofNullable((Integer)parseArgument(var32, var25));
      boolean var42 = var32.has("fullscreen");
      boolean var43 = var32.has("demo");
      boolean var44 = var32.has("disableMultiplayer");
      boolean var45 = var32.has("disableChat");
      String var46 = (String)parseArgument(var32, var21);
      Gson var47 = (new GsonBuilder()).registerTypeAdapter(PropertyMap.class, new Serializer()).create();
      PropertyMap var48 = (PropertyMap)GsonHelper.fromJson(var47, (String)parseArgument(var32, var26), PropertyMap.class);
      PropertyMap var49 = (PropertyMap)GsonHelper.fromJson(var47, (String)parseArgument(var32, var27), PropertyMap.class);
      String var50 = (String)parseArgument(var32, var30);
      File var51 = (File)parseArgument(var32, var9);
      File var52 = var32.has(var10) ? (File)parseArgument(var32, var10) : new File(var51, "assets/");
      File var53 = var32.has(var11) ? (File)parseArgument(var32, var11) : new File(var51, "resourcepacks/");
      String var54 = var32.has(var17) ? (String)var17.value(var32) : UUIDUtil.createOfflinePlayerUUID((String)var16.value(var32)).toString();
      String var55 = var32.has(var28) ? (String)var28.value(var32) : null;
      String var56 = (String)var32.valueOf(var18);
      String var57 = (String)var32.valueOf(var19);
      String var58 = (String)parseArgument(var32, var5);
      String var59 = (String)parseArgument(var32, var6);
      String var60 = (String)parseArgument(var32, var7);
      String var61 = (String)parseArgument(var32, var8);
      if (var32.has(var4)) {
         JvmProfiler.INSTANCE.start(Environment.CLIENT);
      }

      CrashReport.preload();
      Bootstrap.bootStrap();
      GameLoadTimesEvent.INSTANCE.setBootstrapTime(Bootstrap.bootstrapDuration.get());
      Bootstrap.validate();
      Util.startTimerHackThread();
      String var62 = (String)var29.value(var32);
      User.Type var63 = User.Type.byName(var62);
      if (var63 == null) {
         LOGGER.warn("Unrecognized user type: {}", var62);
      }

      User var64 = new User((String)var16.value(var32), var54, (String)var20.value(var32), emptyStringToEmptyOptional(var56), emptyStringToEmptyOptional(var57), var63);
      GameConfig var65 = new GameConfig(new GameConfig.UserData(var64, var48, var49, var35), new DisplayData(var38, var39, var40, var41, var42), new GameConfig.FolderData(var51, var53, var52, var55), new GameConfig.GameData(var43, var46, var50, var44, var45), new GameConfig.QuickPlayData(var58, var59, var60, var61));
      Thread var66 = new Thread("Client Shutdown Thread") {
         public void run() {
            Minecraft var1 = Minecraft.getInstance();
            if (var1 != null) {
               IntegratedServer var2 = var1.getSingleplayerServer();
               if (var2 != null) {
                  var2.halt(true);
               }

            }
         }
      };
      var66.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
      Runtime.getRuntime().addShutdownHook(var66);

      final Minecraft var67;
      try {
         Thread.currentThread().setName("Render thread");
         RenderSystem.initRenderThread();
         RenderSystem.beginInitialization();
         var67 = new Minecraft(var65);
         RenderSystem.finishInitialization();
      } catch (SilentInitException var81) {
         LOGGER.warn("Failed to create window: ", var81);
         return;
      } catch (Throwable var82) {
         CrashReport var69 = CrashReport.forThrowable(var82, "Initializing game");
         CrashReportCategory var70 = var69.addCategory("Initialization");
         NativeModuleLister.addCrashSection(var70);
         Minecraft.fillReport((Minecraft)null, (LanguageManager)null, var65.game.launchVersion, (Options)null, var69);
         Minecraft.crash(var69);
         return;
      }

      Thread var68;
      if (var67.renderOnThread()) {
         var68 = new Thread("Game thread") {
            public void run() {
               try {
                  RenderSystem.initGameThread(true);
                  var67.run();
               } catch (Throwable var2) {
                  Main.LOGGER.error("Exception in client thread", var2);
               }

            }
         };
         var68.start();

         while(true) {
            if (var67.isRunning()) {
               continue;
            }
         }
      } else {
         var68 = null;

         try {
            RenderSystem.initGameThread(false);
            var67.run();
         } catch (Throwable var80) {
            LOGGER.error("Unhandled game exception", var80);
         }
      }

      BufferUploader.reset();

      try {
         var67.stop();
         if (var68 != null) {
            var68.join();
         }
      } catch (InterruptedException var78) {
         LOGGER.error("Exception during client thread shutdown", var78);
      } finally {
         var67.destroy();
      }

   }

   private static Optional<String> emptyStringToEmptyOptional(String var0) {
      return var0.isEmpty() ? Optional.empty() : Optional.of(var0);
   }

   private static OptionalInt ofNullable(@Nullable Integer var0) {
      return var0 != null ? OptionalInt.of(var0) : OptionalInt.empty();
   }

   @Nullable
   private static <T> T parseArgument(OptionSet var0, OptionSpec<T> var1) {
      try {
         return var0.valueOf(var1);
      } catch (Throwable var5) {
         if (var1 instanceof ArgumentAcceptingOptionSpec) {
            ArgumentAcceptingOptionSpec var3 = (ArgumentAcceptingOptionSpec)var1;
            List var4 = var3.defaultValues();
            if (!var4.isEmpty()) {
               return var4.get(0);
            }
         }

         throw var5;
      }
   }

   private static boolean stringHasValue(@Nullable String var0) {
      return var0 != null && !var0.isEmpty();
   }

   static {
      System.setProperty("java.awt.headless", "true");
   }
}
