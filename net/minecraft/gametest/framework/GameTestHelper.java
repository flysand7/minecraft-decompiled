package net.minecraft.gametest.framework;

import com.mojang.authlib.GameProfile;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.LongStream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class GameTestHelper {
   private final GameTestInfo testInfo;
   private boolean finalCheckAdded;

   public GameTestHelper(GameTestInfo var1) {
      this.testInfo = var1;
   }

   public ServerLevel getLevel() {
      return this.testInfo.getLevel();
   }

   public BlockState getBlockState(BlockPos var1) {
      return this.getLevel().getBlockState(this.absolutePos(var1));
   }

   @Nullable
   public BlockEntity getBlockEntity(BlockPos var1) {
      return this.getLevel().getBlockEntity(this.absolutePos(var1));
   }

   public void killAllEntities() {
      this.killAllEntitiesOfClass(Entity.class);
   }

   public void killAllEntitiesOfClass(Class var1) {
      AABB var2 = this.getBounds();
      List var3 = this.getLevel().getEntitiesOfClass(var1, var2.inflate(1.0D), (var0) -> {
         return !(var0 instanceof Player);
      });
      var3.forEach(Entity::kill);
   }

   public ItemEntity spawnItem(Item var1, float var2, float var3, float var4) {
      ServerLevel var5 = this.getLevel();
      Vec3 var6 = this.absoluteVec(new Vec3((double)var2, (double)var3, (double)var4));
      ItemEntity var7 = new ItemEntity(var5, var6.x, var6.y, var6.z, new ItemStack(var1, 1));
      var7.setDeltaMovement(0.0D, 0.0D, 0.0D);
      var5.addFreshEntity(var7);
      return var7;
   }

   public ItemEntity spawnItem(Item var1, BlockPos var2) {
      return this.spawnItem(var1, (float)var2.getX(), (float)var2.getY(), (float)var2.getZ());
   }

   public <E extends Entity> E spawn(EntityType<E> var1, BlockPos var2) {
      return this.spawn(var1, Vec3.atBottomCenterOf(var2));
   }

   public <E extends Entity> E spawn(EntityType<E> var1, Vec3 var2) {
      ServerLevel var3 = this.getLevel();
      Entity var4 = var1.create(var3);
      if (var4 == null) {
         throw new NullPointerException("Failed to create entity " + var1.builtInRegistryHolder().key().location());
      } else {
         if (var4 instanceof Mob) {
            Mob var5 = (Mob)var4;
            var5.setPersistenceRequired();
         }

         Vec3 var6 = this.absoluteVec(var2);
         var4.moveTo(var6.x, var6.y, var6.z, var4.getYRot(), var4.getXRot());
         var3.addFreshEntity(var4);
         return var4;
      }
   }

   public <E extends Entity> E spawn(EntityType<E> var1, int var2, int var3, int var4) {
      return this.spawn(var1, new BlockPos(var2, var3, var4));
   }

   public <E extends Entity> E spawn(EntityType<E> var1, float var2, float var3, float var4) {
      return this.spawn(var1, new Vec3((double)var2, (double)var3, (double)var4));
   }

   public <E extends Mob> E spawnWithNoFreeWill(EntityType<E> var1, BlockPos var2) {
      Mob var3 = (Mob)this.spawn(var1, var2);
      var3.removeFreeWill();
      return var3;
   }

   public <E extends Mob> E spawnWithNoFreeWill(EntityType<E> var1, int var2, int var3, int var4) {
      return this.spawnWithNoFreeWill(var1, new BlockPos(var2, var3, var4));
   }

   public <E extends Mob> E spawnWithNoFreeWill(EntityType<E> var1, Vec3 var2) {
      Mob var3 = (Mob)this.spawn(var1, var2);
      var3.removeFreeWill();
      return var3;
   }

   public <E extends Mob> E spawnWithNoFreeWill(EntityType<E> var1, float var2, float var3, float var4) {
      return this.spawnWithNoFreeWill(var1, new Vec3((double)var2, (double)var3, (double)var4));
   }

   public GameTestSequence walkTo(Mob var1, BlockPos var2, float var3) {
      return this.startSequence().thenExecuteAfter(2, () -> {
         Path var4 = var1.getNavigation().createPath((BlockPos)this.absolutePos(var2), 0);
         var1.getNavigation().moveTo(var4, (double)var3);
      });
   }

   public void pressButton(int var1, int var2, int var3) {
      this.pressButton(new BlockPos(var1, var2, var3));
   }

   public void pressButton(BlockPos var1) {
      this.assertBlockState(var1, (var0) -> {
         return var0.is(BlockTags.BUTTONS);
      }, () -> {
         return "Expected button";
      });
      BlockPos var2 = this.absolutePos(var1);
      BlockState var3 = this.getLevel().getBlockState(var2);
      ButtonBlock var4 = (ButtonBlock)var3.getBlock();
      var4.press(var3, this.getLevel(), var2);
   }

   public void useBlock(BlockPos var1) {
      this.useBlock(var1, this.makeMockPlayer());
   }

   public void useBlock(BlockPos var1, Player var2) {
      BlockPos var3 = this.absolutePos(var1);
      this.useBlock(var1, var2, new BlockHitResult(Vec3.atCenterOf(var3), Direction.NORTH, var3, true));
   }

   public void useBlock(BlockPos var1, Player var2, BlockHitResult var3) {
      BlockPos var4 = this.absolutePos(var1);
      BlockState var5 = this.getLevel().getBlockState(var4);
      InteractionResult var6 = var5.use(this.getLevel(), var2, InteractionHand.MAIN_HAND, var3);
      if (!var6.consumesAction()) {
         UseOnContext var7 = new UseOnContext(var2, InteractionHand.MAIN_HAND, var3);
         var2.getItemInHand(InteractionHand.MAIN_HAND).useOn(var7);
      }

   }

   public LivingEntity makeAboutToDrown(LivingEntity var1) {
      var1.setAirSupply(0);
      var1.setHealth(0.25F);
      return var1;
   }

   public Player makeMockSurvivalPlayer() {
      return new Player(this.getLevel(), BlockPos.ZERO, 0.0F, new GameProfile(UUID.randomUUID(), "test-mock-player")) {
         public boolean isSpectator() {
            return false;
         }

         public boolean isCreative() {
            return false;
         }
      };
   }

   public LivingEntity withLowHealth(LivingEntity var1) {
      var1.setHealth(0.25F);
      return var1;
   }

   public Player makeMockPlayer() {
      return new Player(this.getLevel(), BlockPos.ZERO, 0.0F, new GameProfile(UUID.randomUUID(), "test-mock-player")) {
         public boolean isSpectator() {
            return false;
         }

         public boolean isCreative() {
            return true;
         }

         public boolean isLocalPlayer() {
            return true;
         }
      };
   }

   public ServerPlayer makeMockServerPlayerInLevel() {
      ServerPlayer var1 = new ServerPlayer(this.getLevel().getServer(), this.getLevel(), new GameProfile(UUID.randomUUID(), "test-mock-player")) {
         public boolean isSpectator() {
            return false;
         }

         public boolean isCreative() {
            return true;
         }
      };
      this.getLevel().getServer().getPlayerList().placeNewPlayer(new Connection(PacketFlow.SERVERBOUND), var1);
      return var1;
   }

   public void pullLever(int var1, int var2, int var3) {
      this.pullLever(new BlockPos(var1, var2, var3));
   }

   public void pullLever(BlockPos var1) {
      this.assertBlockPresent(Blocks.LEVER, var1);
      BlockPos var2 = this.absolutePos(var1);
      BlockState var3 = this.getLevel().getBlockState(var2);
      LeverBlock var4 = (LeverBlock)var3.getBlock();
      var4.pull(var3, this.getLevel(), var2);
   }

   public void pulseRedstone(BlockPos var1, long var2) {
      this.setBlock(var1, Blocks.REDSTONE_BLOCK);
      this.runAfterDelay(var2, () -> {
         this.setBlock(var1, Blocks.AIR);
      });
   }

   public void destroyBlock(BlockPos var1) {
      this.getLevel().destroyBlock(this.absolutePos(var1), false, (Entity)null);
   }

   public void setBlock(int var1, int var2, int var3, Block var4) {
      this.setBlock(new BlockPos(var1, var2, var3), var4);
   }

   public void setBlock(int var1, int var2, int var3, BlockState var4) {
      this.setBlock(new BlockPos(var1, var2, var3), var4);
   }

   public void setBlock(BlockPos var1, Block var2) {
      this.setBlock(var1, var2.defaultBlockState());
   }

   public void setBlock(BlockPos var1, BlockState var2) {
      this.getLevel().setBlock(this.absolutePos(var1), var2, 3);
   }

   public void setNight() {
      this.setDayTime(13000);
   }

   public void setDayTime(int var1) {
      this.getLevel().setDayTime((long)var1);
   }

   public void assertBlockPresent(Block var1, int var2, int var3, int var4) {
      this.assertBlockPresent(var1, new BlockPos(var2, var3, var4));
   }

   public void assertBlockPresent(Block var1, BlockPos var2) {
      BlockState var3 = this.getBlockState(var2);
      Predicate var10002 = (var2x) -> {
         return var3.is(var1);
      };
      String var10003 = var1.getName().getString();
      this.assertBlock(var2, var10002, "Expected " + var10003 + ", got " + var3.getBlock().getName().getString());
   }

   public void assertBlockNotPresent(Block var1, int var2, int var3, int var4) {
      this.assertBlockNotPresent(var1, new BlockPos(var2, var3, var4));
   }

   public void assertBlockNotPresent(Block var1, BlockPos var2) {
      this.assertBlock(var2, (var3) -> {
         return !this.getBlockState(var2).is(var1);
      }, "Did not expect " + var1.getName().getString());
   }

   public void succeedWhenBlockPresent(Block var1, int var2, int var3, int var4) {
      this.succeedWhenBlockPresent(var1, new BlockPos(var2, var3, var4));
   }

   public void succeedWhenBlockPresent(Block var1, BlockPos var2) {
      this.succeedWhen(() -> {
         this.assertBlockPresent(var1, var2);
      });
   }

   public void assertBlock(BlockPos var1, Predicate<Block> var2, String var3) {
      this.assertBlock(var1, var2, () -> {
         return var3;
      });
   }

   public void assertBlock(BlockPos var1, Predicate<Block> var2, Supplier<String> var3) {
      this.assertBlockState(var1, (var1x) -> {
         return var2.test(var1x.getBlock());
      }, var3);
   }

   public <T extends Comparable<T>> void assertBlockProperty(BlockPos var1, Property<T> var2, T var3) {
      BlockState var4 = this.getBlockState(var1);
      boolean var5 = var4.hasProperty(var2);
      if (!var5 || !var4.getValue(var2).equals(var3)) {
         String var6 = var5 ? "was " + var4.getValue(var2) : "property " + var2.getName() + " is missing";
         String var7 = String.format(Locale.ROOT, "Expected property %s to be %s, %s", var2.getName(), var3, var6);
         throw new GameTestAssertPosException(var7, this.absolutePos(var1), var1, this.testInfo.getTick());
      }
   }

   public <T extends Comparable<T>> void assertBlockProperty(BlockPos var1, Property<T> var2, Predicate<T> var3, String var4) {
      this.assertBlockState(var1, (var2x) -> {
         if (!var2x.hasProperty(var2)) {
            return false;
         } else {
            Comparable var3x = var2x.getValue(var2);
            return var3.test(var3x);
         }
      }, () -> {
         return var4;
      });
   }

   public void assertBlockState(BlockPos var1, Predicate<BlockState> var2, Supplier<String> var3) {
      BlockState var4 = this.getBlockState(var1);
      if (!var2.test(var4)) {
         throw new GameTestAssertPosException((String)var3.get(), this.absolutePos(var1), var1, this.testInfo.getTick());
      }
   }

   public void assertRedstoneSignal(BlockPos var1, Direction var2, IntPredicate var3, Supplier<String> var4) {
      BlockPos var5 = this.absolutePos(var1);
      ServerLevel var6 = this.getLevel();
      BlockState var7 = var6.getBlockState(var5);
      int var8 = var7.getSignal(var6, var5, var2);
      if (!var3.test(var8)) {
         throw new GameTestAssertPosException((String)var4.get(), var5, var1, this.testInfo.getTick());
      }
   }

   public void assertEntityPresent(EntityType<?> var1) {
      List var2 = this.getLevel().getEntities(var1, this.getBounds(), Entity::isAlive);
      if (var2.isEmpty()) {
         throw new GameTestAssertException("Expected " + var1.toShortString() + " to exist");
      }
   }

   public void assertEntityPresent(EntityType<?> var1, int var2, int var3, int var4) {
      this.assertEntityPresent(var1, new BlockPos(var2, var3, var4));
   }

   public void assertEntityPresent(EntityType<?> var1, BlockPos var2) {
      BlockPos var3 = this.absolutePos(var2);
      List var4 = this.getLevel().getEntities(var1, new AABB(var3), Entity::isAlive);
      if (var4.isEmpty()) {
         throw new GameTestAssertPosException("Expected " + var1.toShortString(), var3, var2, this.testInfo.getTick());
      }
   }

   public void assertEntityPresent(EntityType<?> var1, Vec3 var2, Vec3 var3) {
      List var4 = this.getLevel().getEntities(var1, new AABB(var2, var3), Entity::isAlive);
      if (var4.isEmpty()) {
         throw new GameTestAssertPosException("Expected " + var1.toShortString() + " between ", BlockPos.containing(var2), BlockPos.containing(var3), this.testInfo.getTick());
      }
   }

   public void assertEntitiesPresent(EntityType<?> var1, BlockPos var2, int var3, double var4) {
      BlockPos var6 = this.absolutePos(var2);
      List var7 = this.getEntities(var1, var2, var4);
      if (var7.size() != var3) {
         throw new GameTestAssertPosException("Expected " + var3 + " entities of type " + var1.toShortString() + ", actual number of entities found=" + var7.size(), var6, var2, this.testInfo.getTick());
      }
   }

   public void assertEntityPresent(EntityType<?> var1, BlockPos var2, double var3) {
      List var5 = this.getEntities(var1, var2, var3);
      if (var5.isEmpty()) {
         BlockPos var6 = this.absolutePos(var2);
         throw new GameTestAssertPosException("Expected " + var1.toShortString(), var6, var2, this.testInfo.getTick());
      }
   }

   public <T extends Entity> List<T> getEntities(EntityType<T> var1, BlockPos var2, double var3) {
      BlockPos var5 = this.absolutePos(var2);
      return this.getLevel().getEntities(var1, (new AABB(var5)).inflate(var3), Entity::isAlive);
   }

   public void assertEntityInstancePresent(Entity var1, int var2, int var3, int var4) {
      this.assertEntityInstancePresent(var1, new BlockPos(var2, var3, var4));
   }

   public void assertEntityInstancePresent(Entity var1, BlockPos var2) {
      BlockPos var3 = this.absolutePos(var2);
      List var4 = this.getLevel().getEntities(var1.getType(), new AABB(var3), Entity::isAlive);
      var4.stream().filter((var1x) -> {
         return var1x == var1;
      }).findFirst().orElseThrow(() -> {
         return new GameTestAssertPosException("Expected " + var1.getType().toShortString(), var3, var2, this.testInfo.getTick());
      });
   }

   public void assertItemEntityCountIs(Item var1, BlockPos var2, double var3, int var5) {
      BlockPos var6 = this.absolutePos(var2);
      List var7 = this.getLevel().getEntities(EntityType.ITEM, (new AABB(var6)).inflate(var3), Entity::isAlive);
      int var8 = 0;
      Iterator var9 = var7.iterator();

      while(var9.hasNext()) {
         ItemEntity var10 = (ItemEntity)var9.next();
         ItemStack var11 = var10.getItem();
         if (var11.is(var1)) {
            var8 += var11.getCount();
         }
      }

      if (var8 != var5) {
         throw new GameTestAssertPosException("Expected " + var5 + " " + var1.getDescription().getString() + " items to exist (found " + var8 + ")", var6, var2, this.testInfo.getTick());
      }
   }

   public void assertItemEntityPresent(Item var1, BlockPos var2, double var3) {
      BlockPos var5 = this.absolutePos(var2);
      List var6 = this.getLevel().getEntities(EntityType.ITEM, (new AABB(var5)).inflate(var3), Entity::isAlive);
      Iterator var7 = var6.iterator();

      ItemEntity var9;
      do {
         if (!var7.hasNext()) {
            throw new GameTestAssertPosException("Expected " + var1.getDescription().getString() + " item", var5, var2, this.testInfo.getTick());
         }

         Entity var8 = (Entity)var7.next();
         var9 = (ItemEntity)var8;
      } while(!var9.getItem().getItem().equals(var1));

   }

   public void assertItemEntityNotPresent(Item var1, BlockPos var2, double var3) {
      BlockPos var5 = this.absolutePos(var2);
      List var6 = this.getLevel().getEntities(EntityType.ITEM, (new AABB(var5)).inflate(var3), Entity::isAlive);
      Iterator var7 = var6.iterator();

      ItemEntity var9;
      do {
         if (!var7.hasNext()) {
            return;
         }

         Entity var8 = (Entity)var7.next();
         var9 = (ItemEntity)var8;
      } while(!var9.getItem().getItem().equals(var1));

      throw new GameTestAssertPosException("Did not expect " + var1.getDescription().getString() + " item", var5, var2, this.testInfo.getTick());
   }

   public void assertEntityNotPresent(EntityType<?> var1) {
      List var2 = this.getLevel().getEntities(var1, this.getBounds(), Entity::isAlive);
      if (!var2.isEmpty()) {
         throw new GameTestAssertException("Did not expect " + var1.toShortString() + " to exist");
      }
   }

   public void assertEntityNotPresent(EntityType<?> var1, int var2, int var3, int var4) {
      this.assertEntityNotPresent(var1, new BlockPos(var2, var3, var4));
   }

   public void assertEntityNotPresent(EntityType<?> var1, BlockPos var2) {
      BlockPos var3 = this.absolutePos(var2);
      List var4 = this.getLevel().getEntities(var1, new AABB(var3), Entity::isAlive);
      if (!var4.isEmpty()) {
         throw new GameTestAssertPosException("Did not expect " + var1.toShortString(), var3, var2, this.testInfo.getTick());
      }
   }

   public void assertEntityTouching(EntityType<?> var1, double var2, double var4, double var6) {
      Vec3 var8 = new Vec3(var2, var4, var6);
      Vec3 var9 = this.absoluteVec(var8);
      Predicate var10 = (var1x) -> {
         return var1x.getBoundingBox().intersects(var9, var9);
      };
      List var11 = this.getLevel().getEntities(var1, this.getBounds(), var10);
      if (var11.isEmpty()) {
         throw new GameTestAssertException("Expected " + var1.toShortString() + " to touch " + var9 + " (relative " + var8 + ")");
      }
   }

   public void assertEntityNotTouching(EntityType<?> var1, double var2, double var4, double var6) {
      Vec3 var8 = new Vec3(var2, var4, var6);
      Vec3 var9 = this.absoluteVec(var8);
      Predicate var10 = (var1x) -> {
         return !var1x.getBoundingBox().intersects(var9, var9);
      };
      List var11 = this.getLevel().getEntities(var1, this.getBounds(), var10);
      if (var11.isEmpty()) {
         throw new GameTestAssertException("Did not expect " + var1.toShortString() + " to touch " + var9 + " (relative " + var8 + ")");
      }
   }

   public <E extends Entity, T> void assertEntityData(BlockPos var1, EntityType<E> var2, Function<? super E, T> var3, @Nullable T var4) {
      BlockPos var5 = this.absolutePos(var1);
      List var6 = this.getLevel().getEntities(var2, new AABB(var5), Entity::isAlive);
      if (var6.isEmpty()) {
         throw new GameTestAssertPosException("Expected " + var2.toShortString(), var5, var1, this.testInfo.getTick());
      } else {
         Iterator var7 = var6.iterator();

         while(var7.hasNext()) {
            Entity var8 = (Entity)var7.next();
            Object var9 = var3.apply(var8);
            if (var9 == null) {
               if (var4 != null) {
                  throw new GameTestAssertException("Expected entity data to be: " + var4 + ", but was: " + var9);
               }
            } else if (!var9.equals(var4)) {
               throw new GameTestAssertException("Expected entity data to be: " + var4 + ", but was: " + var9);
            }
         }

      }
   }

   public <E extends LivingEntity> void assertEntityIsHolding(BlockPos var1, EntityType<E> var2, Item var3) {
      BlockPos var4 = this.absolutePos(var1);
      List var5 = this.getLevel().getEntities(var2, new AABB(var4), Entity::isAlive);
      if (var5.isEmpty()) {
         throw new GameTestAssertPosException("Expected entity of type: " + var2, var4, var1, this.getTick());
      } else {
         Iterator var6 = var5.iterator();

         LivingEntity var7;
         do {
            if (!var6.hasNext()) {
               throw new GameTestAssertPosException("Entity should be holding: " + var3, var4, var1, this.getTick());
            }

            var7 = (LivingEntity)var6.next();
         } while(!var7.isHolding(var3));

      }
   }

   public <E extends Entity & InventoryCarrier> void assertEntityInventoryContains(BlockPos var1, EntityType<E> var2, Item var3) {
      BlockPos var4 = this.absolutePos(var1);
      List var5 = this.getLevel().getEntities(var2, new AABB(var4), (var0) -> {
         return ((Entity)var0).isAlive();
      });
      if (var5.isEmpty()) {
         throw new GameTestAssertPosException("Expected " + var2.toShortString() + " to exist", var4, var1, this.getTick());
      } else {
         Iterator var6 = var5.iterator();

         Entity var7;
         do {
            if (!var6.hasNext()) {
               throw new GameTestAssertPosException("Entity inventory should contain: " + var3, var4, var1, this.getTick());
            }

            var7 = (Entity)var6.next();
         } while(!((InventoryCarrier)var7).getInventory().hasAnyMatching((var1x) -> {
            return var1x.is(var3);
         }));

      }
   }

   public void assertContainerEmpty(BlockPos var1) {
      BlockPos var2 = this.absolutePos(var1);
      BlockEntity var3 = this.getLevel().getBlockEntity(var2);
      if (var3 instanceof BaseContainerBlockEntity && !((BaseContainerBlockEntity)var3).isEmpty()) {
         throw new GameTestAssertException("Container should be empty");
      }
   }

   public void assertContainerContains(BlockPos var1, Item var2) {
      BlockPos var3 = this.absolutePos(var1);
      BlockEntity var4 = this.getLevel().getBlockEntity(var3);
      if (!(var4 instanceof BaseContainerBlockEntity)) {
         throw new GameTestAssertException("Expected a container at " + var1 + ", found " + BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(var4.getType()));
      } else if (((BaseContainerBlockEntity)var4).countItem(var2) != 1) {
         throw new GameTestAssertException("Container should contain: " + var2);
      }
   }

   public void assertSameBlockStates(BoundingBox var1, BlockPos var2) {
      BlockPos.betweenClosedStream(var1).forEach((var3) -> {
         BlockPos var4 = var2.offset(var3.getX() - var1.minX(), var3.getY() - var1.minY(), var3.getZ() - var1.minZ());
         this.assertSameBlockState(var3, var4);
      });
   }

   public void assertSameBlockState(BlockPos var1, BlockPos var2) {
      BlockState var3 = this.getBlockState(var1);
      BlockState var4 = this.getBlockState(var2);
      if (var3 != var4) {
         this.fail("Incorrect state. Expected " + var4 + ", got " + var3, var1);
      }

   }

   public void assertAtTickTimeContainerContains(long var1, BlockPos var3, Item var4) {
      this.runAtTickTime(var1, () -> {
         this.assertContainerContains(var3, var4);
      });
   }

   public void assertAtTickTimeContainerEmpty(long var1, BlockPos var3) {
      this.runAtTickTime(var1, () -> {
         this.assertContainerEmpty(var3);
      });
   }

   public <E extends Entity, T> void succeedWhenEntityData(BlockPos var1, EntityType<E> var2, Function<E, T> var3, T var4) {
      this.succeedWhen(() -> {
         this.assertEntityData(var1, var2, var3, var4);
      });
   }

   public <E extends Entity> void assertEntityProperty(E var1, Predicate<E> var2, String var3) {
      if (!var2.test(var1)) {
         throw new GameTestAssertException("Entity " + var1 + " failed " + var3 + " test");
      }
   }

   public <E extends Entity, T> void assertEntityProperty(E var1, Function<E, T> var2, String var3, T var4) {
      Object var5 = var2.apply(var1);
      if (!var5.equals(var4)) {
         throw new GameTestAssertException("Entity " + var1 + " value " + var3 + "=" + var5 + " is not equal to expected " + var4);
      }
   }

   public void succeedWhenEntityPresent(EntityType<?> var1, int var2, int var3, int var4) {
      this.succeedWhenEntityPresent(var1, new BlockPos(var2, var3, var4));
   }

   public void succeedWhenEntityPresent(EntityType<?> var1, BlockPos var2) {
      this.succeedWhen(() -> {
         this.assertEntityPresent(var1, var2);
      });
   }

   public void succeedWhenEntityNotPresent(EntityType<?> var1, int var2, int var3, int var4) {
      this.succeedWhenEntityNotPresent(var1, new BlockPos(var2, var3, var4));
   }

   public void succeedWhenEntityNotPresent(EntityType<?> var1, BlockPos var2) {
      this.succeedWhen(() -> {
         this.assertEntityNotPresent(var1, var2);
      });
   }

   public void succeed() {
      this.testInfo.succeed();
   }

   private void ensureSingleFinalCheck() {
      if (this.finalCheckAdded) {
         throw new IllegalStateException("This test already has final clause");
      } else {
         this.finalCheckAdded = true;
      }
   }

   public void succeedIf(Runnable var1) {
      this.ensureSingleFinalCheck();
      this.testInfo.createSequence().thenWaitUntil(0L, var1).thenSucceed();
   }

   public void succeedWhen(Runnable var1) {
      this.ensureSingleFinalCheck();
      this.testInfo.createSequence().thenWaitUntil(var1).thenSucceed();
   }

   public void succeedOnTickWhen(int var1, Runnable var2) {
      this.ensureSingleFinalCheck();
      this.testInfo.createSequence().thenWaitUntil((long)var1, var2).thenSucceed();
   }

   public void runAtTickTime(long var1, Runnable var3) {
      this.testInfo.setRunAtTickTime(var1, var3);
   }

   public void runAfterDelay(long var1, Runnable var3) {
      this.runAtTickTime(this.testInfo.getTick() + var1, var3);
   }

   public void randomTick(BlockPos var1) {
      BlockPos var2 = this.absolutePos(var1);
      ServerLevel var3 = this.getLevel();
      var3.getBlockState(var2).randomTick(var3, var2, var3.random);
   }

   public int getHeight(Heightmap.Types var1, int var2, int var3) {
      BlockPos var4 = this.absolutePos(new BlockPos(var2, 0, var3));
      return this.relativePos(this.getLevel().getHeightmapPos(var1, var4)).getY();
   }

   public void fail(String var1, BlockPos var2) {
      throw new GameTestAssertPosException(var1, this.absolutePos(var2), var2, this.getTick());
   }

   public void fail(String var1, Entity var2) {
      throw new GameTestAssertPosException(var1, var2.blockPosition(), this.relativePos(var2.blockPosition()), this.getTick());
   }

   public void fail(String var1) {
      throw new GameTestAssertException(var1);
   }

   public void failIf(Runnable var1) {
      this.testInfo.createSequence().thenWaitUntil(var1).thenFail(() -> {
         return new GameTestAssertException("Fail conditions met");
      });
   }

   public void failIfEver(Runnable var1) {
      LongStream.range(this.testInfo.getTick(), (long)this.testInfo.getTimeoutTicks()).forEach((var2) -> {
         GameTestInfo var10000 = this.testInfo;
         Objects.requireNonNull(var1);
         var10000.setRunAtTickTime(var2, var1::run);
      });
   }

   public GameTestSequence startSequence() {
      return this.testInfo.createSequence();
   }

   public BlockPos absolutePos(BlockPos var1) {
      BlockPos var2 = this.testInfo.getStructureBlockPos();
      BlockPos var3 = var2.offset(var1);
      return StructureTemplate.transform(var3, Mirror.NONE, this.testInfo.getRotation(), var2);
   }

   public BlockPos relativePos(BlockPos var1) {
      BlockPos var2 = this.testInfo.getStructureBlockPos();
      Rotation var3 = this.testInfo.getRotation().getRotated(Rotation.CLOCKWISE_180);
      BlockPos var4 = StructureTemplate.transform(var1, Mirror.NONE, var3, var2);
      return var4.subtract(var2);
   }

   public Vec3 absoluteVec(Vec3 var1) {
      Vec3 var2 = Vec3.atLowerCornerOf(this.testInfo.getStructureBlockPos());
      return StructureTemplate.transform(var2.add(var1), Mirror.NONE, this.testInfo.getRotation(), this.testInfo.getStructureBlockPos());
   }

   public Vec3 relativeVec(Vec3 var1) {
      Vec3 var2 = Vec3.atLowerCornerOf(this.testInfo.getStructureBlockPos());
      return StructureTemplate.transform(var1.subtract(var2), Mirror.NONE, this.testInfo.getRotation(), this.testInfo.getStructureBlockPos());
   }

   public void assertTrue(boolean var1, String var2) {
      if (!var1) {
         throw new GameTestAssertException(var2);
      }
   }

   public void assertFalse(boolean var1, String var2) {
      if (var1) {
         throw new GameTestAssertException(var2);
      }
   }

   public long getTick() {
      return this.testInfo.getTick();
   }

   private AABB getBounds() {
      return this.testInfo.getStructureBounds();
   }

   private AABB getRelativeBounds() {
      AABB var1 = this.testInfo.getStructureBounds();
      return var1.move(BlockPos.ZERO.subtract(this.absolutePos(BlockPos.ZERO)));
   }

   public void forEveryBlockInStructure(Consumer<BlockPos> var1) {
      AABB var2 = this.getRelativeBounds();
      BlockPos.MutableBlockPos.betweenClosedStream(var2.move(0.0D, 1.0D, 0.0D)).forEach(var1);
   }

   public void onEachTick(Runnable var1) {
      LongStream.range(this.testInfo.getTick(), (long)this.testInfo.getTimeoutTicks()).forEach((var2) -> {
         GameTestInfo var10000 = this.testInfo;
         Objects.requireNonNull(var1);
         var10000.setRunAtTickTime(var2, var1::run);
      });
   }

   public void placeAt(Player var1, ItemStack var2, BlockPos var3, Direction var4) {
      BlockPos var5 = this.absolutePos(var3.relative(var4));
      BlockHitResult var6 = new BlockHitResult(Vec3.atCenterOf(var5), var4, var5, false);
      UseOnContext var7 = new UseOnContext(var1, InteractionHand.MAIN_HAND, var6);
      var2.useOn(var7);
   }
}
