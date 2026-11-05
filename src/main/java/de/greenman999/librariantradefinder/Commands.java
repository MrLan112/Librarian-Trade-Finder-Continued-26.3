package de.greenman999.librariantradefinder;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantment;

import static de.greenman999.librariantradefinder.LibrarianTradeFinder.openConfig;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

/** Client command tree for the Librarian Trade Finder Continued fork. */
public final class Commands {
    private Commands() {
    }

    public static void registerCommands() {
        register("tradefinder");
        register("tf");
    }

    private static void register(String rootName) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            var root = literal(rootName);

            var select = literal("select")
                .then(literal("manual").executes(context -> {
                    TradeFinder.selectManual();
                    return 1;
                }))
                .executes(context -> TradeFinder.select() ? 1 : 0);

            var enchantment = argument(
                "enchantment",
                ResourceArgument.resource(registryAccess, Registries.ENCHANTMENT)
            ).executes(context -> {
                Holder<Enchantment> holder = context.getArgument("enchantment", Holder.class);
                return TradeFinder.searchSingle(holder.value(), 1, 64);
            });

            var level = argument("level", IntegerArgumentType.integer(1, 5))
                .executes(context -> {
                    Holder<Enchantment> holder = context.getArgument("enchantment", Holder.class);
                    int levelValue = IntegerArgumentType.getInteger(context, "level");
                    return TradeFinder.searchSingle(holder.value(), levelValue, 64);
                });

            var maxPrice = argument("maxPrice", IntegerArgumentType.integer(1, 64))
                .executes(context -> {
                    Holder<Enchantment> holder = context.getArgument("enchantment", Holder.class);
                    int levelValue = IntegerArgumentType.getInteger(context, "level");
                    int maxPriceValue = IntegerArgumentType.getInteger(context, "maxPrice");
                    return TradeFinder.searchSingle(holder.value(), levelValue, maxPriceValue);
                });

            level.then(maxPrice);
            enchantment.then(level);

            var search = literal("search")
                .executes(context -> TradeFinder.searchList())
                .then(enchantment);

            var config = literal("config").executes(context -> {
                openConfig();
                return 1;
            });

            var stop = literal("stop").executes(context -> {
                TradeFinder.stop();
                context.getSource().sendFeedback(
                    Component.translatable("commands.tradefinder.stop.success")
                        .withStyle(ChatFormatting.GREEN)
                );
                return 1;
            });

            dispatcher.register(root.then(select).then(search).then(config).then(stop));
        });
    }
}
