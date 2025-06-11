package io.github.linkfgfgui.mcllm.command;

import com.bawnorton.mcchatgpt.config.Config;
import com.bawnorton.mcchatgpt.config.ConfigManager;
import com.bawnorton.mcchatgpt.store.SecureTokenStorage;
import com.bawnorton.mcchatgpt.util.Conversation;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.architectury.event.events.client.ClientCommandRegistrationEvent;
import dev.architectury.event.events.client.ClientCommandRegistrationEvent.ClientCommandSourceStack;
import io.github.linkfgfgui.mcllm.MCLLM;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Objects;

public class CommandHandler {
    public static void init() {
        ClientCommandRegistrationEvent.EVENT.register((dispatcher, registryAccess) -> {
//            MCLLM.LOGGER.warn("CommandHandler init");
            registerAskCommand(dispatcher);
            registerAuthCommand(dispatcher);
            registerListConversationsCommand(dispatcher);
            registerNextConversationCommand(dispatcher);
            registerPreviousConversationCommand(dispatcher);
            registerSetConversationCommand(dispatcher);
            registerSetContextLevelCommand(dispatcher);
            registerGetContextLevelCommand(dispatcher);
            registerSetTimeoutCommand(dispatcher);
            registerSetModelCommand(dispatcher);
            registerSetIsThinkModelCommand(dispatcher);
            registerSetIsShowCostCommand(dispatcher);
            registerSetBaseurlCommand(dispatcher);
            registerSetProxyCommand(dispatcher);
        });
    }

    private static void registerAskCommand(CommandDispatcher<ClientCommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<ClientCommandSourceStack> builder = ClientCommandRegistrationEvent.literal("ask")
                .then(ClientCommandRegistrationEvent.argument("question", StringArgumentType.greedyString())
                        .executes(context ->
                                {
                                    ClientCommandSourceStack source = context.getSource();
                                    String question = StringArgumentType.getString(context, "question");
                                    source.arch$sendSuccess(() -> (Component.literal("§7<" + Objects.requireNonNull(context.getSource().arch$getPlayer()).getDisplayName().getString() + "> " + question)), false);
                                    MCLLM.ask(question);
                                    return 1;
                                }
                        )
                );
        dispatcher.register(builder);
    }

    private static void registerAuthCommand(CommandDispatcher<ClientCommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<ClientCommandSourceStack> builder = ClientCommandRegistrationEvent.literal("mcllm-auth")
                .then(ClientCommandRegistrationEvent.argument("token", StringArgumentType.string()).executes(context -> {
                    ClientCommandSourceStack source = context.getSource();
                    String token = StringArgumentType.getString(context, "token");
                    Config.getInstance().token = SecureTokenStorage.encrypt(token);
                    ConfigManager.saveConfig();
                    MCLLM.startService();
                    source.arch$sendSuccess(() -> Component.translatable("mcllm.auth.success"), false);
                    return 1;
                }));
        dispatcher.register(builder);
    }

    private static void registerSetTimeoutCommand(CommandDispatcher<ClientCommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<ClientCommandSourceStack> builder = ClientCommandRegistrationEvent.literal("mcllm-settimeout")
                .then(ClientCommandRegistrationEvent.argument("timeout", IntegerArgumentType.integer()).executes(context -> {
                    ClientCommandSourceStack source = context.getSource();
                    Config.getInstance().timeout = IntegerArgumentType.getInteger(context, "timeout");
                    ConfigManager.saveConfig();
                    MCLLM.startService();
                    source.arch$sendSuccess(() -> Component.translatable("mcllm.set.success"), false);
                    return 1;
                }));
        dispatcher.register(builder);
    }

    private static void registerSetModelCommand(CommandDispatcher<ClientCommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<ClientCommandSourceStack> builder = ClientCommandRegistrationEvent.literal("mcllm-setmodel")
                .then(ClientCommandRegistrationEvent.argument("model", StringArgumentType.string()).executes(context -> {
                    ClientCommandSourceStack source = context.getSource();
                    Config.getInstance().model = StringArgumentType.getString(context, "model");
                    ConfigManager.saveConfig();
                    MCLLM.startService();
                    source.arch$sendSuccess(() -> Component.translatable("mcllm.set.success"), false);
                    return 1;
                }));
        dispatcher.register(builder);
    }

    private static void registerSetBaseurlCommand(CommandDispatcher<ClientCommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<ClientCommandSourceStack> builder = ClientCommandRegistrationEvent.literal("mcllm-setbaseurl")
                .then(ClientCommandRegistrationEvent.argument("baseurl", StringArgumentType.string()).executes(context -> {
                    ClientCommandSourceStack source = context.getSource();
                    Config.getInstance().baseurl = StringArgumentType.getString(context, "baseurl");
                    ConfigManager.saveConfig();
                    MCLLM.startService();
                    source.arch$sendSuccess(() -> Component.translatable("mcllm.set.success"), false);
                    return 1;
                }));
        dispatcher.register(builder);
    }
    private static void registerSetProxyCommand(CommandDispatcher<ClientCommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<ClientCommandSourceStack> builder = ClientCommandRegistrationEvent.literal("mcllm-setproxy")
                .then(ClientCommandRegistrationEvent.argument("proxy", StringArgumentType.string()).executes(context -> {
                    ClientCommandSourceStack source = context.getSource();
                    Config.getInstance().proxy = StringArgumentType.getString(context, "proxy");
                    ConfigManager.saveConfig();
                    MCLLM.startService();
                    source.arch$sendSuccess(() -> Component.translatable("mcllm.set.success"), false);
                    return 1;
                }));
        dispatcher.register(builder);
    }
    private static void registerListConversationsCommand(CommandDispatcher<ClientCommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<ClientCommandSourceStack> builder = ClientCommandRegistrationEvent.literal("listconversations").executes(context -> {
            ClientCommandSourceStack source = context.getSource();
            List<Conversation> conversations = MCLLM.getConversations();
            source.arch$sendSuccess(() -> Component.translatable("mcllm.conversation.list"), false);
            for (int i = 0; i < conversations.size(); i++) {
                Conversation conversation = conversations.get(i);
                if (conversation.messageCount() < 2) continue;
                String lastQuestion = conversation.getPreviewMessage().toString();
                int finalI = i;
                source.arch$sendSuccess(() -> Component.literal("§b[mcllm]: §r" + (finalI + 1) + ": " + lastQuestion), false);
            }
            return 1;
        });
        dispatcher.register(builder);
    }

    private static void registerNextConversationCommand(CommandDispatcher<ClientCommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<ClientCommandSourceStack> builder = ClientCommandRegistrationEvent.literal("nextconversation").executes(context -> {
            try {
                ClientCommandSourceStack source = context.getSource();
                boolean newConversation = MCLLM.nextConversation();
                int index = MCLLM.getConversationIndex();
                if (newConversation) {
                    source.arch$sendSuccess(() -> Component.translatable("mcllm.conversation.new", index + 1), false);
                } else {
                    source.arch$sendSuccess(() -> Component.translatable("mcllm.conversation.continue", index + 1), false);
                }
                return 1;
            } catch (IllegalStateException e) {
                return 0;
            }
        });
        dispatcher.register(builder);
    }

    private static void registerPreviousConversationCommand(CommandDispatcher<ClientCommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<ClientCommandSourceStack> builder = ClientCommandRegistrationEvent.literal("previousconversation").executes(context -> {
            try {
                ClientCommandSourceStack source = context.getSource();
                MCLLM.previousConversation();
                int index = MCLLM.getConversationIndex();
                source.arch$sendSuccess(() -> Component.translatable("mcllm.conversation.continue", index + 1), false);
                return 1;
            } catch (IllegalStateException e) {
                return 0;
            }
        });
        dispatcher.register(builder);
    }

    private static void registerSetConversationCommand(CommandDispatcher<ClientCommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<ClientCommandSourceStack> builder = ClientCommandRegistrationEvent.literal("setconversation")
                .then(ClientCommandRegistrationEvent.argument("index", IntegerArgumentType.integer(1)).executes(context -> {
                    ClientCommandSourceStack source = context.getSource();
                    int index = IntegerArgumentType.getInteger(context, "index") - 1;
                    if (index >= MCLLM.getConversations().size()) {
                        source.arch$sendSuccess(() -> Component.translatable("mcllm.conversation.invalid"), false);
                        return 0;
                    }
                    MCLLM.setConversationIndex(index);
                    source.arch$sendSuccess(() -> Component.translatable("mcllm.conversation.continue", index + 1), false);
                    return 1;
                }));
        dispatcher.register(builder);
    }

    private static void registerSetContextLevelCommand(CommandDispatcher<ClientCommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<ClientCommandSourceStack> builder = ClientCommandRegistrationEvent.literal("setcontextlevel")
                .then(ClientCommandRegistrationEvent.argument("level", IntegerArgumentType.integer(0, 3)).executes(context -> {
                    ClientCommandSourceStack source = context.getSource();
                    int level = IntegerArgumentType.getInteger(context, "level");
                    Config.getInstance().contextLevel = level;
                    ConfigManager.saveConfig();
                    source.arch$sendSuccess(() -> Component.translatable("mcllm.context.level.set", level, Component.translatable("mcllm.context.level." + level).getString()), false);
                    return 1;
                }));
        dispatcher.register(builder);
    }

    private static void registerGetContextLevelCommand(CommandDispatcher<ClientCommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<ClientCommandSourceStack> builder = ClientCommandRegistrationEvent.literal("getcontextlevel").executes(context -> {
            ClientCommandSourceStack source = context.getSource();
            int level = Config.getInstance().contextLevel;
            source.arch$sendSuccess(() -> Component.translatable("mcllm.context.level.get", level, Component.translatable("mcllm.context.level." + level).getString()), false);
            return 1;
        });
        dispatcher.register(builder);
    }

    private static void registerSetIsThinkModelCommand(CommandDispatcher<ClientCommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<ClientCommandSourceStack> builder = ClientCommandRegistrationEvent.literal("setisthinkmodel")
                .then(ClientCommandRegistrationEvent.argument("type", BoolArgumentType.bool()).executes(context -> {
                    ClientCommandSourceStack source = context.getSource();
                    Config.getInstance().isThinkModel = BoolArgumentType.getBool(context, "type");
                    ConfigManager.saveConfig();
                    source.arch$sendSuccess(() -> Component.translatable("mcllm.set.success"), false);
                    return 1;
                }));
        dispatcher.register(builder);
    }


    private static void registerSetIsShowCostCommand(CommandDispatcher<ClientCommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<ClientCommandSourceStack> builder = ClientCommandRegistrationEvent.literal("setisshowcost")
                .then(ClientCommandRegistrationEvent.argument("type", BoolArgumentType.bool()).executes(context -> {
                    ClientCommandSourceStack source = context.getSource();
                    Config.getInstance().isShowCost = BoolArgumentType.getBool(context, "type");
                    ConfigManager.saveConfig();
                    source.arch$sendSuccess(() -> Component.translatable("mcllm.set.success"), false);
                    return 1;
                }));
        dispatcher.register(builder);
    }

}
