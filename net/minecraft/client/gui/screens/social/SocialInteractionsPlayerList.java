package net.minecraft.client.gui.screens.social;

import com.google.common.base.Strings;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.chat.ChatLog;
import net.minecraft.client.multiplayer.chat.LoggedChatEvent;
import net.minecraft.client.multiplayer.chat.LoggedChatMessage;

public class SocialInteractionsPlayerList extends ContainerObjectSelectionList<PlayerEntry> {
   private final SocialInteractionsScreen socialInteractionsScreen;
   private final List<PlayerEntry> players = Lists.newArrayList();
   @Nullable
   private String filter;

   public SocialInteractionsPlayerList(SocialInteractionsScreen var1, Minecraft var2, int var3, int var4, int var5, int var6, int var7) {
      super(var2, var3, var4, var5, var6, var7);
      this.socialInteractionsScreen = var1;
      this.setRenderBackground(false);
      this.setRenderTopAndBottom(false);
   }

   protected void enableScissor(GuiGraphics var1) {
      var1.enableScissor(this.x0, this.y0 + 4, this.x1, this.y1);
   }

   public void updatePlayerList(Collection<UUID> var1, double var2, boolean var4) {
      HashMap var5 = new HashMap();
      this.addOnlinePlayers(var1, var5);
      this.updatePlayersFromChatLog(var5, var4);
      this.updateFiltersAndScroll(var5.values(), var2);
   }

   private void addOnlinePlayers(Collection<UUID> var1, Map<UUID, PlayerEntry> var2) {
      ClientPacketListener var3 = this.minecraft.player.connection;
      Iterator var4 = var1.iterator();

      while(var4.hasNext()) {
         UUID var5 = (UUID)var4.next();
         PlayerInfo var6 = var3.getPlayerInfo(var5);
         if (var6 != null) {
            boolean var7 = var6.hasVerifiableChat();
            Minecraft var10004 = this.minecraft;
            SocialInteractionsScreen var10005 = this.socialInteractionsScreen;
            String var10007 = var6.getProfile().getName();
            Objects.requireNonNull(var6);
            var2.put(var5, new PlayerEntry(var10004, var10005, var5, var10007, var6::getSkinLocation, var7));
         }
      }

   }

   private void updatePlayersFromChatLog(Map<UUID, PlayerEntry> var1, boolean var2) {
      Collection var3 = collectProfilesFromChatLog(this.minecraft.getReportingContext().chatLog());
      Iterator var4 = var3.iterator();

      while(true) {
         PlayerEntry var6;
         do {
            if (!var4.hasNext()) {
               return;
            }

            GameProfile var5 = (GameProfile)var4.next();
            if (var2) {
               var6 = (PlayerEntry)var1.computeIfAbsent(var5.getId(), (var2x) -> {
                  PlayerEntry var3 = new PlayerEntry(this.minecraft, this.socialInteractionsScreen, var5.getId(), var5.getName(), Suppliers.memoize(() -> {
                     return this.minecraft.getSkinManager().getInsecureSkinLocation(var5);
                  }), true);
                  var3.setRemoved(true);
                  return var3;
               });
               break;
            }

            var6 = (PlayerEntry)var1.get(var5.getId());
         } while(var6 == null);

         var6.setHasRecentMessages(true);
      }
   }

   private static Collection<GameProfile> collectProfilesFromChatLog(ChatLog var0) {
      ObjectLinkedOpenHashSet var1 = new ObjectLinkedOpenHashSet();

      for(int var2 = var0.end(); var2 >= var0.start(); --var2) {
         LoggedChatEvent var3 = var0.lookup(var2);
         if (var3 instanceof LoggedChatMessage.Player) {
            LoggedChatMessage.Player var4 = (LoggedChatMessage.Player)var3;
            if (var4.message().hasSignature()) {
               var1.add(var4.profile());
            }
         }
      }

      return var1;
   }

   private void sortPlayerEntries() {
      this.players.sort(Comparator.comparing((var1) -> {
         if (var1.getPlayerId().equals(this.minecraft.getUser().getProfileId())) {
            return 0;
         } else if (var1.getPlayerId().version() == 2) {
            return 4;
         } else if (this.minecraft.getReportingContext().hasDraftReportFor(var1.getPlayerId())) {
            return 1;
         } else {
            return var1.hasRecentMessages() ? 2 : 3;
         }
      }).thenComparing((var0) -> {
         if (!var0.getPlayerName().isBlank()) {
            int var1 = var0.getPlayerName().codePointAt(0);
            if (var1 == 95 || var1 >= 97 && var1 <= 122 || var1 >= 65 && var1 <= 90 || var1 >= 48 && var1 <= 57) {
               return 0;
            }
         }

         return 1;
      }).thenComparing(PlayerEntry::getPlayerName, String::compareToIgnoreCase));
   }

   private void updateFiltersAndScroll(Collection<PlayerEntry> var1, double var2) {
      this.players.clear();
      this.players.addAll(var1);
      this.sortPlayerEntries();
      this.updateFilteredPlayers();
      this.replaceEntries(this.players);
      this.setScrollAmount(var2);
   }

   private void updateFilteredPlayers() {
      if (this.filter != null) {
         this.players.removeIf((var1) -> {
            return !var1.getPlayerName().toLowerCase(Locale.ROOT).contains(this.filter);
         });
         this.replaceEntries(this.players);
      }

   }

   public void setFilter(String var1) {
      this.filter = var1;
   }

   public boolean isEmpty() {
      return this.players.isEmpty();
   }

   public void addPlayer(PlayerInfo var1, SocialInteractionsScreen.Page var2) {
      UUID var3 = var1.getProfile().getId();
      Iterator var4 = this.players.iterator();

      PlayerEntry var5;
      while(var4.hasNext()) {
         var5 = (PlayerEntry)var4.next();
         if (var5.getPlayerId().equals(var3)) {
            var5.setRemoved(false);
            return;
         }
      }

      if ((var2 == SocialInteractionsScreen.Page.ALL || this.minecraft.getPlayerSocialManager().shouldHideMessageFrom(var3)) && (Strings.isNullOrEmpty(this.filter) || var1.getProfile().getName().toLowerCase(Locale.ROOT).contains(this.filter))) {
         boolean var6 = var1.hasVerifiableChat();
         Minecraft var10002 = this.minecraft;
         SocialInteractionsScreen var10003 = this.socialInteractionsScreen;
         UUID var10004 = var1.getProfile().getId();
         String var10005 = var1.getProfile().getName();
         Objects.requireNonNull(var1);
         var5 = new PlayerEntry(var10002, var10003, var10004, var10005, var1::getSkinLocation, var6);
         this.addEntry(var5);
         this.players.add(var5);
      }

   }

   public void removePlayer(UUID var1) {
      Iterator var2 = this.players.iterator();

      PlayerEntry var3;
      do {
         if (!var2.hasNext()) {
            return;
         }

         var3 = (PlayerEntry)var2.next();
      } while(!var3.getPlayerId().equals(var1));

      var3.setRemoved(true);
   }
}
