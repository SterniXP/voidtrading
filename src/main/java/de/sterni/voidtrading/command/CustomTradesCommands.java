package de.sterni.voidtrading.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import de.sterni.voidtrading.VoidTrading;
import de.sterni.voidtrading.customtrades.TradeBlackListEditor;
import de.sterni.voidtrading.customtrades.TradeMaterialsEditor;
import lombok.NonNull;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import static net.minecraft.server.command.CommandManager.*;
import static de.sterni.voidtrading.VoidTrading.CONFIG;

public class CustomTradesCommands {
    public static final TextColor TEAL = TextColor.fromRgb(0x008080);
    public static final TextColor RED = TextColor.fromRgb(0xFF0000);
    public static final TextColor YELLOW = TextColor.fromRgb(0xFFFF00);
    private static final int DEFAULT_MAX_USES = 12;

    private final TradeMaterialsEditor tradeMaterialsEditor = TradeMaterialsEditor.getInstance();
    private final TradeBlackListEditor tradeBlackListEditor = TradeBlackListEditor.getInstance();

    public static void registerCommands() {
        CustomTradesCommands instance = new CustomTradesCommands();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerListCommands(dispatcher, instance);
        });
    }

    private static void registerListCommands(CommandDispatcher<ServerCommandSource> dispatcher, CustomTradesCommands instance) {
        dispatcher.register(literal("customtrades")
                        .executes(context -> defaultCommandResponse(instance, context))
                .then(literal("list")
                        .executes(context -> instance.showTradeList(context, true))
                        .then(argument("onlyactive", BoolArgumentType.bool())
                                .executes(context -> instance.showTradeList(context, BoolArgumentType.getBool(context, "onlyactive")))
                        )
                )
                .then(literal("add")
                        .requires(source -> source.hasPermissionLevel(VoidTrading.PERMISSION_LEVEL))
                        .executes(instance::addNewTrade)
                )
                .then(literal("blacklist")
                        .executes(instance::showBlackList)
                )
        );
    }

    private static int defaultCommandResponse(CustomTradesCommands instance, CommandContext<ServerCommandSource> context) {
        instance.sendMessageToSender(context.getSource(),"Dieser Mod erlaubt es Spielern neue 'Custom Handel' zu Villagern hinzuzufügen.\n" +
                        "Mit dem Befehl '/customtrades list' kannst du eine Liste aller verfügbaren Custom Handel anzeigen lassen.",
                TEAL, false);
        return 1;
    }

    private void sendMessageToSender(ServerCommandSource source, String message, TextColor color, boolean broadcastToOps) {
        source.sendFeedback(() -> Text.literal(message).setStyle(Style.EMPTY.withColor(color)), broadcastToOps);
    }

    public int showTradeList(@NonNull CommandContext<ServerCommandSource> context, boolean showOnlyActive) {
        String intro = """
                            Du kannst jedem beliebigem Villager einen Custom Handel hinzufügen, indem du auf diesen, mit dem gewünschten Item in der Hand, rechtsklickst.
                            Das Item in deiner Hand stellt das Ergebnis des Handels dar und wird dabei verbraucht.
                            Ein Villager kann immer nur eine Kategorie von Custom Handel haben.
                            Das heißt, die natürlichen Handel eines Villagers werden nicht beeinflusst, aber die Custom Handel eines Villagers werden überschrieben, wenn du einen neuen Custom Handel hinzufügst.
                            
                            Wenn mehrere CustomTrades das selbe Item als Ziel haben""";
        if (CONFIG.enableCustomTradeCycling()) {
            intro += " und erneut mit demselben Item in der Hand mit dem Villager interagiert wird, werden die Custom Handel eines Villagers zyklisch innerhalb der Item Kategorie gewechselt. (Bzw. es passiert nichts, wenn es nur einen Custom Handel mit diesem Item als Ziel gibt und der Villager diesen Handel bereits hat.)";
        } else {
            intro += ", werden alle diese Handel dem Villager hinzugefügt. (Bzw. es passiert nichts, wenn der Villager bereits alle diese Custom Handel hat.)";
        }
        sendMessageToSender(context.getSource(), intro, TEAL, false);
        String tradeList = tradeMaterialsEditor.getTradesAsString(showOnlyActive);
        if (tradeList.isEmpty()) {
            sendMessageToSender(context.getSource(), "Die List der Custom Handel ist leer.",
                    TEAL, false);
        } else {
            sendMessageToSender(context.getSource(), "Verfügbare Custom Handel:\n"+tradeMaterialsEditor.getTradesAsString(),
                    TEAL, false);
        }
        return 1;
    }

    public int addNewTrade(@NonNull CommandContext<ServerCommandSource> context) {
        sendMessageToSender(context.getSource(), "", TEAL, false);
        return 1;
    }

    public int showBlackList(@NonNull CommandContext<ServerCommandSource> context) {
        String blackList = tradeBlackListEditor.getTradesAsString(false);
        if (blackList.isEmpty()) {
            sendMessageToSender(context.getSource(), "Die Liste der gebannten Handel ist leer.",
                    TEAL, false);
        } else {
            sendMessageToSender(context.getSource(), "Die Liste der gebannten Handel ist:\n" + tradeBlackListEditor.getTradesAsString(),
                    TEAL, false);
        }
        return 1;
    }
}
