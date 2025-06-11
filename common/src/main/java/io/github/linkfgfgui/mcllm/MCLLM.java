package io.github.linkfgfgui.mcllm;

import com.openai.core.JsonValue;
import dev.architectury.event.events.client.ClientLifecycleEvent;
import com.bawnorton.mcchatgpt.util.Conversation;
import com.bawnorton.mcchatgpt.config.Config;
import com.bawnorton.mcchatgpt.config.ConfigManager;
import com.bawnorton.mcchatgpt.store.SecureTokenStorage;
import com.bawnorton.mcchatgpt.util.Context;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.completions.CompletionUsage;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import io.github.linkfgfgui.mcllm.markdownRenderer.MarkdownRenderer;
import io.github.linkfgfgui.mcllm.command.CommandHandler;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.math.MathContext;

public final class MCLLM {
    public static final String MOD_ID = "mcllm";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);


    private static final ExecutorService executor;
    //    private static OpenAIClient client;
    private static OpenAIClient client; //OpenAIClient
    private static List<Conversation> conversations;
    private static int conversationIndex = 0;

    private static double COST_PER_TOKEN; // $0.000002 per token (https://openai.com/pricing)

    static {
        executor = Executors.newFixedThreadPool(1);
    }

//    public static String getPlatformName() {
//        if (Platform.isFabric()) return "fabric";
//        if (Platform.isNeoForge()) return "neoforge";
//        return "unknown";
//    }

    public static void init() {
        conversations = new ArrayList<>();
        ConfigManager.loadConfig();
        COST_PER_TOKEN = Config.getInstance().estimatedCostPerToken;
        if (!Config.getInstance().token.isEmpty()) {
            startService();
        }
        CommandHandler.init();

        ClientLifecycleEvent.CLIENT_LEVEL_LOAD.register(client -> {
            if (!notAuthed(false)) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    mc.player.displayClientMessage(Component.translatable("mcllm.auth.success"), false);
                }
            }
        });
    }
    private static Proxy getProxy(String proxyStr) {
        Proxy proxy=Proxy.NO_PROXY;
        if (proxyStr==null){
            LOGGER.debug("No proxy");
            return proxy;
        }
        if (proxyStr.contains("@")){
            LOGGER.warn("Unsupported auth proxy, use no proxy");
            return proxy;
        }
        try {
            URI uri = new URI(proxyStr);
            Proxy.Type type;
            String scheme = uri.getScheme();
            if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                type = Proxy.Type.HTTP;
            } else if ("socks5".equalsIgnoreCase(scheme)) {
                type = Proxy.Type.SOCKS;
            }else{
                LOGGER.error("Unsupported scheme: {}", scheme);
                return proxy;
            }
            String host = uri.getHost();
            int port = uri.getPort();
            if (host == null || port == -1) {
                LOGGER.error("URI is missing host or port: {}", uri);
                return proxy;
            }
            return new Proxy(type, new InetSocketAddress(host, port));
        } catch (URISyntaxException e) {
            LOGGER.error("ProxyString incorrect: {}", e.getMessage());
            return proxy;
        }
    }
    public static void startService() {
        client = OpenAIOkHttpClient
                .builder()
                .apiKey(SecureTokenStorage.decrypt(Config.getInstance().secret, Config.getInstance().token))
                .baseUrl(Config.getInstance().baseurl)
                .timeout(Duration.ofSeconds(Config.getInstance().timeout))
                .proxy(getProxy(Config.getInstance().proxy))
                .build();

    }

    public static boolean notAuthed() {
        return notAuthed(true);
    }

    public static boolean notAuthed(boolean prompt) {
        if (client == null) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null && prompt) {
                player.displayClientMessage(Component.translatable("mcllm.auth.message1"), false);
                player.displayClientMessage(Component.translatable("mcllm.auth.message2"), false);
            }
            return true;
        }
        return false;
    }

    public static List<Conversation> getConversations() {
        return conversations;
    }

    public static int getConversationIndex() {
        return conversationIndex;
    }

    public static void setConversationIndex(int index) {
        if (index >= 0 && index < conversations.size()) {
            conversationIndex = index;
        }
    }

    public static boolean nextConversation() {
        if (notAuthed()) throw new IllegalStateException("Not authenticated");
        if (conversationIndex < conversations.size() - 1) {
            conversationIndex++;
            return false;
        }

        conversations.add(new Conversation());
        conversationIndex = conversations.size() - 1;
        String contentText = "Context: You are an AI assistant in the game Minecraft version " +
                SharedConstants.getCurrentVersion().getName() +
                ". Limit your responses to 256 characters. Assume the player cannot access commands unless" +
                " explicitly asked for them. Do not simulate conversations";
        ChatCompletionMessageParam params = ChatCompletionMessageParam
                .ofSystem(ChatCompletionSystemMessageParam
                        .builder()
                        .content(contentText).build());
        conversations.get(conversationIndex).addMessage(params);
        return true;
    }

    public static void previousConversation() {
        if (notAuthed()) throw new IllegalStateException("Not authenticated");
        if (conversationIndex > 0) {
            conversationIndex--;
        }
    }

    private static HitResult getLookingAt(LocalPlayer player) {
        Minecraft client = Minecraft.getInstance();
        MultiPlayerGameMode gameMode = client.gameMode;
        if (gameMode == null) return null;
        return player.pick(player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE), 1.0f, false);
    }

    private static void addContext(Conversation conversation) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) return;
        HitResult target = getLookingAt(player);
        Context.Builder contextBuilder = Context.builder();
        switch (Config.getInstance().contextLevel) {
            case 3:
                List<LivingEntity> nearbyEntities = player.level().getNearbyEntities(LivingEntity.class, TargetingConditions.DEFAULT.selector(entity -> entity != player), player, player.getBoundingBox().inflate(64));
                if (target instanceof EntityHitResult entityHitResult) {
                    Entity entity = entityHitResult.getEntity();
                    if (entity instanceof LivingEntity livingEntity) {
                        contextBuilder.addEntityTarget(livingEntity);
                    }
                }

                contextBuilder.addEntities(nearbyEntities);
            case 2:
                Holder<Biome> biome = player.level().getBiome(player.blockPosition());
                biome.unwrapKey().ifPresent(biomeKey -> contextBuilder.addBiome(biomeKey.location().getPath()));
                Block block = null;
                if (target instanceof BlockHitResult blockHitResult) {
                    block = player.level().getBlockState(blockHitResult.getBlockPos()).getBlock();
                }
                contextBuilder.addBlockTarget(block);
                Holder<DimensionType> dimension = player.level().dimensionTypeRegistration();
                dimension.unwrapKey().ifPresent(dimensionKey -> contextBuilder.addDimension(dimensionKey.location().getPath()));

            case 1:
                List<ItemStack> playerInventory = player.getInventory().items;
                List<ItemStack> playerMainInventory = playerInventory.subList(9, playerInventory.size());
                List<ItemStack> playerHotbar = playerInventory.subList(0, 9);

                contextBuilder
                        .addInventory("Player", playerMainInventory)
                        .addHotbar(playerHotbar)
                        .addArmor(player.getArmorSlots())
                        .addMainHand(player.getMainHandItem())
                        .addOffHand(player.getOffhandItem())
                        .addPlayerPosition(player.blockPosition());

            default:
                String contentText = contextBuilder.build(Config.getInstance().contextLevel).get();
                ChatCompletionMessageParam contextMessage = ChatCompletionMessageParam
                        .ofSystem(ChatCompletionSystemMessageParam
                                .builder()
                                .content(contentText).build());
                conversation.setContext(contextMessage);
        }
    }

    private static void askSync(String question) {
        if (conversations.isEmpty()) {
            nextConversation();
        }

        Conversation conversation = conversations.get(conversationIndex);
        addContext(conversation);

        ChatCompletionMessageParam questionMessage = ChatCompletionMessageParam.ofUser(ChatCompletionUserMessageParam.builder().content(question).build());
        conversation.addMessage(questionMessage);
//        conversation.setPreviewMessage(questionMessage);
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .messages(conversation.getMessages())
                .model(Config.getInstance().model)
                .build();
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        try {
            ChatCompletion chatCompletion = client.chat().completions().create(params);

            Optional<CompletionUsage> usage = chatCompletion.usage();
            long tokensUsed = 0;
            float cost = 0;
            if (usage.isPresent()) {
                tokensUsed = usage.get().totalTokens();
                MathContext sigfigContext = new MathContext(1);
                BigDecimal costDecimal = BigDecimal.valueOf((float) (tokensUsed * COST_PER_TOKEN));
                costDecimal = costDecimal.round(sigfigContext);
                cost = costDecimal.floatValue();

                LOGGER.info("Used {} tokens (${})", tokensUsed, cost);
            }
            String replyMessage = chatCompletion.choices().getFirst().message().content().orElse("null");
            JsonValue reasoning_content_json_value = chatCompletion.choices().getFirst().message()._additionalProperties().get("reasoning_content");
            String reasoning_content;
            if (reasoning_content_json_value == null) {
                reasoning_content = "";
            } else {
                reasoning_content = reasoning_content_json_value.toString();
            }
            LOGGER.debug("reply: {}", replyMessage);
            player.displayClientMessage(
                    MarkdownRenderer.render(replyMessage, usage.isPresent(), tokensUsed, cost, reasoning_content),
                    false);
            ChatCompletionMessageParam replyParam = ChatCompletionMessageParam
                    .ofAssistant(ChatCompletionAssistantMessageParam
                            .builder()
                            .content(replyMessage).build());
            conversation.addMessage(replyParam);
            if (conversation.messageCount() > 10) {
                conversation.removeMessage(1); // don't remove the first message, as it's the minecraft context
                conversation.removeMessage(1);
            }
        } catch (RuntimeException e) {
            LOGGER.error("Error while communicating with Server", e);
            if (e.getMessage().toLowerCase().contains("exceeded your current quota")) {
                player.displayClientMessage(Component.translatable("mcllm.ask.quota").setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://platform.openai.com/account/usage")).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("https://platform.openai.com/account/usage")))), false);
            } else if (e.getMessage().toLowerCase().contains("maximum context length")) {
                player.displayClientMessage(Component.translatable("mcllm.ask.excessive.context").setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(e.getMessage())))), false);
            } else {
                player.displayClientMessage(Component.translatable("mcllm.ask.error").setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(e.getMessage())))), false);
            }
        }
    }

    public static void ask(String question) {
        if (notAuthed()) return;
        executor.execute(() -> {
            try {
                askSync(question);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }


}
