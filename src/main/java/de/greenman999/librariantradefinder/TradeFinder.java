package de.greenman999.librariantradefinder;

import de.greenman999.librariantradefinder.config.TradeFinderConfig;
import de.greenman999.librariantradefinder.util.HudUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.HashedStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

@SuppressWarnings("DuplicatedCode")
public final class TradeFinder {
    private TradeFinder() {}

    public static TradeState state = TradeState.IDLE;
    public static Villager villager = null;
    public static BlockPos lecternPos = null;
    public static boolean searchAll = true;

    // Single-enchantment search parameters.
    public static Enchantment enchantment = null;
    public static int maxBookPrice = 0;
    public static int minLevel = 0;
    public static int tries = 0;

    private static Vec3 prevPos = null;
    private static int placeDelay = 3;
    private static int interactDelay = 2;
    private static boolean startedBreakLook = false;
    private static boolean startedPlaceLook = false;
    private static boolean startedCheckLook = false;
    private static boolean finishedBreakLook = false;
    private static boolean finishedPlaceLook = false;
    private static boolean finishedCheckLook = false;

    public static void stop() {
        state = TradeState.IDLE;
        enchantment = null;
        maxBookPrice = 0;
        minLevel = 0;
        tries = 0;
        Minecraft.getInstance().gui.hud.setOverlayMessage(Component.literal(""), false);
    }

    public static int searchList() {
        if (TradeFinderConfig.INSTANCE.enchantments.values().stream().noneMatch(e -> e.enabled)) {
            HudUtils.chatMessage(HudUtils.textTranslatable(ChatFormatting.RED, "commands.tradefinder.search.no-enchantments"));
            return 0;
        }
        searchAll = true;
        state = TradeState.CHECK;
        if (villager == null || lecternPos == null) {
            HudUtils.chatMessage(HudUtils.textTranslatable(ChatFormatting.RED, "commands.tradefinder.start.not-selected"));
            stop();
            return 0;
        }
        HudUtils.chatMessage(HudUtils.textTranslatable(ChatFormatting.GREEN, "commands.tradefinder.start.success-list"));
        tries = 0;
        return 1;
    }

    public static int searchSingle(Enchantment enchantment, int minLevel, int maxBookPrice) {
        TradeFinder.enchantment = enchantment;
        TradeFinder.minLevel = Math.min(minLevel, enchantment.getMaxLevel());
        TradeFinder.maxBookPrice = maxBookPrice;
        searchAll = false;
        state = TradeState.CHECK;
        tries = 0;

        if (villager == null || lecternPos == null) {
            HudUtils.chatMessage(HudUtils.textTranslatable(ChatFormatting.RED, "commands.tradefinder.start.not-selected"));
            stop();
            return 0;
        }
        HudUtils.chatMessage(HudUtils.textTranslatable(ChatFormatting.GREEN, "commands.tradefinder.start.success-single"));
        return 1;
    }

    public static boolean select() {
        Minecraft mc = Minecraft.getInstance();
        HitResult hitResult = mc.player == null ? null : mc.player.pick(3.0, 0.0F, false);
        if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) {
            HudUtils.chatMessage(HudUtils.textTranslatable(ChatFormatting.RED, "commands.tradefinder.select.not-looking-at-lectern"));
            return false;
        }

        BlockPos blockPos = ((BlockHitResult) hitResult).getBlockPos();
        if (mc.level == null) return false;
        Block block = mc.level.getBlockState(blockPos).getBlock();
        if (!(block instanceof LecternBlock)) {
            HudUtils.chatMessage(HudUtils.textTranslatable(ChatFormatting.RED, "commands.tradefinder.select.not-looking-at-lectern"));
            return false;
        }

        double closestDistance = Double.POSITIVE_INFINITY;
        Vec3 lecternCenter = Vec3.atCenterOf(blockPos);
        Entity closestEntity = null;
        for (Entity entity : mc.level.entitiesForRendering()) {
            Vec3 entityPos = entity.position();
            if (entity instanceof Villager villagerEntity
                && villagerEntity.getVillagerData().profession().is(VillagerProfession.LIBRARIAN)
                && entityPos.distanceTo(lecternCenter) < closestDistance) {
                closestDistance = entityPos.distanceTo(lecternCenter);
                closestEntity = entity;
            }
        }

        if (!(closestEntity instanceof Villager foundVillager)) {
            HudUtils.chatMessage(HudUtils.textTranslatable(ChatFormatting.RED, "commands.tradefinder.select.no-librarian-found"));
            return false;
        }

        villager = foundVillager;
        lecternPos = blockPos;
        HudUtils.chatMessage(HudUtils.textTranslatable(ChatFormatting.GREEN, "commands.tradefinder.select.success"));
        return true;
    }

    public static void selectManual() {
        villager = null;
        lecternPos = null;
        state = TradeState.SELECT_MANUAL;
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (state == TradeState.IDLE) return;
        LocalPlayer player = mc.player;
        if (player == null) return;

        if ((villager == null || lecternPos == null) && state != TradeState.SELECT_MANUAL) {
            HudUtils.chatMessage(HudUtils.textTranslatable(ChatFormatting.RED, "commands.tradefinder.start.not-selected"));
            return;
        }

        switch (state) {
            case CHECK -> HudUtils.overlayMessage(HudUtils.textTranslatable(ChatFormatting.GRAY, "librarian-trade-finder.actionbar.status.check", tries), false);
            case BREAK -> HudUtils.overlayMessage(HudUtils.textTranslatable(ChatFormatting.GRAY, "librarian-trade-finder.actionbar.status.break", tries), false);
            case PLACE -> HudUtils.overlayMessage(HudUtils.textTranslatable(ChatFormatting.GRAY, "librarian-trade-finder.actionbar.status.place", tries), false);
            case SELECT_MANUAL -> HudUtils.overlayMessage(HudUtils.textTranslatable(ChatFormatting.GRAY, "librarian-trade-finder.actionbar.status.select-manual"), false);
            default -> { }
        }

        if ((state == TradeState.CHECK || state == TradeState.WAITING_FOR_PACKET)
            && villager.getVillagerData().profession().is(VillagerProfession.LIBRARIAN)) {
            Vec3 villagerPosition = new Vec3(villager.getX(), villager.getY() + villager.getEyeHeight(Pose.STANDING), villager.getZ());

            if (LibrarianTradeFinder.getConfig().legitMode && LibrarianTradeFinder.getConfig().slowMode) {
                if (RotationTools.isRotated && !finishedCheckLook) {
                    finishedCheckLook = true;
                    startedCheckLook = false;
                    RotationTools.isRotated = false;
                } else if (!startedCheckLook && !finishedCheckLook) {
                    RotationTools.smoothLookAt(villagerPosition, 3);
                    startedCheckLook = true;
                    return;
                } else if (!finishedCheckLook) {
                    return;
                }
            } else if (LibrarianTradeFinder.getConfig().legitMode) {
                player.lookAt(EntityAnchorArgument.Anchor.EYES, villagerPosition);
            }

            if (LibrarianTradeFinder.getConfig().slowMode) {
                if (interactDelay > 0) {
                    interactDelay--;
                    return;
                }
                interactDelay = 2;
            }

            InteractionResult result = null;
            if (mc.gameMode != null) {
                EntityHitResult hitResult = new EntityHitResult(villager);
                result = mc.gameMode.interact(player, villager, hitResult, InteractionHand.MAIN_HAND);
                player.swing(InteractionHand.MAIN_HAND, player.getMainHandItem().getAttackAnimation(), false);
                player.connection.send(ServerboundPunchPacket.INSTANCE);
                player.connection.send(new net.minecraft.network.protocol.game.ServerboundInteractPacket(
                    villager.getId(), InteractionHand.MAIN_HAND, villager.getEyePosition(), false));
            }

            if (result == InteractionResult.SUCCESS) {
                finishedBreakLook = false;
                state = TradeState.WAITING_FOR_PACKET;
            } else {
                HudUtils.chatMessage(HudUtils.textTranslatable(ChatFormatting.RED, "librarian-trade-finder.check.interact.failed"));
                stop();
            }
        } else if (state == TradeState.BREAK) {
            BlockPos toPlace = lecternPos.below();
            if (LibrarianTradeFinder.getConfig().legitMode && LibrarianTradeFinder.getConfig().slowMode) {
                if (RotationTools.isRotated && !finishedBreakLook) {
                    finishedBreakLook = true;
                    startedBreakLook = false;
                    RotationTools.isRotated = false;
                } else if (!startedBreakLook && !finishedBreakLook) {
                    RotationTools.smoothLookAt(new Vec3(toPlace.getX() + 0.5, toPlace.getY() + 1.0, toPlace.getZ() + 0.5), 3);
                    startedBreakLook = true;
                    return;
                } else if (!finishedBreakLook) {
                    return;
                }
            } else if (LibrarianTradeFinder.getConfig().legitMode) {
                player.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(toPlace.getX() + 0.5, toPlace.getY() + 1.0, toPlace.getZ() + 0.5));
            }

            Inventory inventory = player.getInventory();
            ItemStack mainHand = inventory.getSelectedItem();
            if (mainHand.typeHolder().is(ItemTags.AXES)) {
                int remainingDurability = mainHand.getMaxDamage() - mainHand.getDamageValue();
                if (remainingDurability <= 5 && LibrarianTradeFinder.getConfig().preventAxeBreaking) {
                    stop();
                    HudUtils.chatMessage(HudUtils.textTranslatable(ChatFormatting.RED, "librarian-trade-finder.break.axe.breaking"));
                    return;
                }
            }

            if (mc.level != null && mc.level.getBlockState(lecternPos).getBlock() instanceof LecternBlock && mc.gameMode != null) {
                player.swing(InteractionHand.MAIN_HAND, player.getMainHandItem().getAttackAnimation(), false);
                mc.gameMode.continueDestroyBlock(lecternPos, Direction.UP);
                player.connection.send(ServerboundPunchPacket.INSTANCE);
            } else {
                finishedPlaceLook = false;
                state = TradeState.PLACE;
                if (LibrarianTradeFinder.getConfig().tpToVillager && mc.getConnection() != null) {
                    prevPos = player.position();
                    mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(villager.getX(), villager.getY(), villager.getZ(), true, false));
                }
            }
        } else if (state == TradeState.PLACE) {
            if (LibrarianTradeFinder.getConfig().tpToVillager && mc.getConnection() != null && prevPos != null) {
                mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(prevPos.x, prevPos.y, prevPos.z, true, false));
            }

            BlockPos toPlace = lecternPos.below();
            if (LibrarianTradeFinder.getConfig().legitMode && LibrarianTradeFinder.getConfig().slowMode) {
                if (RotationTools.isRotated && !finishedPlaceLook) {
                    finishedPlaceLook = true;
                    startedPlaceLook = false;
                    RotationTools.isRotated = false;
                } else if (!startedPlaceLook && !finishedPlaceLook) {
                    RotationTools.smoothLookAt(new Vec3(toPlace.getX() + 0.5, toPlace.getY() + 1.0, toPlace.getZ() + 0.5), 3);
                    startedPlaceLook = true;
                    return;
                } else if (!finishedPlaceLook) {
                    return;
                }
            } else if (LibrarianTradeFinder.getConfig().legitMode) {
                player.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(toPlace.getX() + 0.5, toPlace.getY() + 1.0, toPlace.getZ() + 0.5));
            }

            if (LibrarianTradeFinder.getConfig().slowMode) {
                if (placeDelay > 0) {
                    placeDelay--;
                    return;
                }
                placeDelay = 3;
            }

            if (!player.getOffhandItem().getItem().equals(Items.LECTERN) && mc.gameMode != null) {
                if (player.inventoryMenu == player.containerMenu) {
                    for (int i = 9; i < 45; i++) {
                        if (player.getInventory().getItem(i >= 36 ? i - 36 : i).getItem() == Items.LECTERN) {
                            boolean itemInOffhand = !player.getOffhandItem().isEmpty();
                            var container = player.containerMenu;
                            ContainerInput pickupInput = ContainerInput.PICKUP;
                            Int2ObjectMap<HashedStack> emptyChangedSlots = new Int2ObjectOpenHashMap<>();
                            HashedStack emptyCarried = HashedStack.EMPTY;
                            int stateId = container.getStateId();

                            player.connection.send(new ServerboundContainerClickPacket(
                                container.containerId, stateId, (short) i, (byte) 0, pickupInput, emptyChangedSlots, emptyCarried));
                            player.connection.send(new ServerboundContainerClickPacket(
                                container.containerId, stateId, (short) 45, (byte) 0, pickupInput, emptyChangedSlots, emptyCarried));

                            if (itemInOffhand) {
                                player.connection.send(new ServerboundContainerClickPacket(
                                    container.containerId, stateId, (short) i, (byte) 0, pickupInput, emptyChangedSlots, emptyCarried));
                            }
                            break;
                        }
                    }
                } else {
                    for (int i = 0; i < 9; i++) {
                        if (player.getInventory().getItem(i).getItem() == Items.LECTERN) {
                            if (i != player.getInventory().getSelectedSlot()) {
                                player.getInventory().setSelectedSlot(i);
                                player.connection.send(new ServerboundSetCarriedItemPacket(i));
                            }
                            player.connection.send(new ServerboundPlayerActionPacket(
                                ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
                            break;
                        }
                    }
                }
            }

            BlockHitResult hit = new BlockHitResult(
                new Vec3(lecternPos.getX(), lecternPos.getY(), lecternPos.getZ()),
                Direction.UP, lecternPos.below(), false);
            if (mc.gameMode != null) {
                mc.gameMode.useItemOn(player, InteractionHand.OFF_HAND, hit);
                player.swing(InteractionHand.OFF_HAND, player.getOffhandItem().getAttackAnimation(), false);
                player.connection.send(ServerboundPunchPacket.INSTANCE);
            }

            finishedCheckLook = false;
            state = TradeState.CHECK;
        } else if (state == TradeState.SELECT_MANUAL && villager != null && lecternPos != null) {
            state = TradeState.IDLE;
        }
    }
}
