package de.greenman999.librariantradefinder.mixin;

import de.greenman999.librariantradefinder.LibrarianTradeFinder;
import de.greenman999.librariantradefinder.TradeFinder;
import de.greenman999.librariantradefinder.TradeState;
import de.greenman999.librariantradefinder.config.TradeFinderConfig;
import io.netty.channel.ChannelHandlerContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.Holder;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicBoolean;

@Environment(EnvType.CLIENT)
@Mixin(Connection.class)
public class ClientConnectionMixin {
    @Inject(
        method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onChannelRead0(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        if (packet instanceof ClientboundOpenScreenPacket openScreenPacket) {
            if (openScreenPacket.getType() == MenuType.MERCHANT && TradeFinder.state != TradeState.IDLE) {
                Minecraft.getInstance().execute(() -> {
                    ClientPacketListener connection = Minecraft.getInstance().getConnection();
                    if (connection != null) {
                        connection.send(new ServerboundContainerClosePacket(openScreenPacket.getContainerId()));
                    }
                });
                ci.cancel();
            }
        } else if (packet instanceof ClientboundMerchantOffersPacket offersPacket
            && TradeFinder.state == TradeState.WAITING_FOR_PACKET) {
            AtomicBoolean found = new AtomicBoolean(false);
            for (MerchantOffer tradeOffer : offersPacket.getOffers()) {
                if (!tradeOffer.getResult().getItem().equals(Items.ENCHANTED_BOOK)) continue;

                EnchantmentHelper.getEnchantmentsForCrafting(tradeOffer.getResult()).entrySet().forEach(enchantmentEntry -> {
                    Enchantment enchantment = enchantmentEntry.getKey().value();
                    int level = enchantmentEntry.getIntValue();
                    int maxPrice;
                    int minLevel;

                    if (TradeFinder.searchAll) {
                        TradeFinderConfig.EnchantmentOption option = LibrarianTradeFinder.getConfig().enchantments.get(enchantment);
                        if (option == null || !option.isEnabled()) return;
                        maxPrice = option.getMaxPrice();
                        minLevel = option.getLevel();
                    } else {
                        if (TradeFinder.enchantment == null || !Enchantment.getFullname(
                            Holder.direct(enchantment), enchantment.getMaxLevel()).equals(Enchantment.getFullname(
                            Holder.direct(TradeFinder.enchantment), enchantment.getMaxLevel()))) return;
                        maxPrice = TradeFinder.maxBookPrice;
                        minLevel = TradeFinder.minLevel;
                    }

                    if (tradeOffer.getBaseCostA().getCount() <= maxPrice && level >= minLevel) {
                        foundEnchantment(found, tradeOffer, enchantment, level);
                    }
                });
            }

            if (!found.get()) {
                TradeFinder.state = TradeState.BREAK;
                TradeFinder.tries++;
            }
        }
    }

    @Unique
    private void foundEnchantment(AtomicBoolean found, MerchantOffer tradeOffer, Enchantment enchantment, int level) {
        int attempts = TradeFinder.tries;
        TradeFinder.stop();
        found.set(true);

        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.sendSystemMessage(Component.translatable(
                    "librarian-trade-finder.found",
                    Enchantment.getFullname(Holder.direct(enchantment), level),
                    tradeOffer.getBaseCostA().getCount(),
                    Component.literal(String.valueOf(attempts)).withStyle(style -> style.withColor(0xcc1141))
                ).withStyle(ChatFormatting.GREEN));
            }
        });
    }
}
