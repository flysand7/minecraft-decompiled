package net.minecraft.client.multiplayer.chat.report;

import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.exceptions.MinecraftClientHttpException;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.minecraft.report.AbuseReport;
import com.mojang.authlib.minecraft.report.AbuseReportLimits;
import com.mojang.authlib.yggdrasil.request.AbuseReportRequest;
import com.mojang.datafixers.util.Unit;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ThrowingComponent;

public interface AbuseReportSender {
   static AbuseReportSender create(ReportEnvironment var0, UserApiService var1) {
      return new AbuseReportSender.Services(var0, var1);
   }

   CompletableFuture<Unit> send(UUID var1, AbuseReport var2);

   boolean isEnabled();

   default AbuseReportLimits reportLimits() {
      return AbuseReportLimits.DEFAULTS;
   }

   public static record Services(ReportEnvironment a, UserApiService b) implements AbuseReportSender {
      private final ReportEnvironment environment;
      private final UserApiService userApiService;
      private static final Component SERVICE_UNAVAILABLE_TEXT = Component.translatable("gui.abuseReport.send.service_unavailable");
      private static final Component HTTP_ERROR_TEXT = Component.translatable("gui.abuseReport.send.http_error");
      private static final Component JSON_ERROR_TEXT = Component.translatable("gui.abuseReport.send.json_error");

      public Services(ReportEnvironment var1, UserApiService var2) {
         this.environment = var1;
         this.userApiService = var2;
      }

      public CompletableFuture<Unit> send(UUID var1, AbuseReport var2) {
         return CompletableFuture.supplyAsync(() -> {
            AbuseReportRequest var3 = new AbuseReportRequest(1, var1, var2, this.environment.clientInfo(), this.environment.thirdPartyServerInfo(), this.environment.realmInfo());

            Component var5;
            try {
               this.userApiService.reportAbuse(var3);
               return Unit.INSTANCE;
            } catch (MinecraftClientHttpException var6) {
               var5 = this.getHttpErrorDescription(var6);
               throw new CompletionException(new AbuseReportSender.SendException(var5, var6));
            } catch (MinecraftClientException var7) {
               var5 = this.getErrorDescription(var7);
               throw new CompletionException(new AbuseReportSender.SendException(var5, var7));
            }
         }, Util.ioPool());
      }

      public boolean isEnabled() {
         return this.userApiService.canSendReports();
      }

      private Component getHttpErrorDescription(MinecraftClientHttpException var1) {
         return Component.translatable("gui.abuseReport.send.error_message", var1.getMessage());
      }

      private Component getErrorDescription(MinecraftClientException var1) {
         Component var10000;
         switch(var1.getType()) {
         case SERVICE_UNAVAILABLE:
            var10000 = SERVICE_UNAVAILABLE_TEXT;
            break;
         case HTTP_ERROR:
            var10000 = HTTP_ERROR_TEXT;
            break;
         case JSON_ERROR:
            var10000 = JSON_ERROR_TEXT;
            break;
         default:
            throw new IncompatibleClassChangeError();
         }

         return var10000;
      }

      public AbuseReportLimits reportLimits() {
         return this.userApiService.getAbuseReportLimits();
      }

      public ReportEnvironment environment() {
         return this.environment;
      }

      public UserApiService userApiService() {
         return this.userApiService;
      }
   }

   public static class SendException extends ThrowingComponent {
      public SendException(Component var1, Throwable var2) {
         super(var1, var2);
      }
   }
}
