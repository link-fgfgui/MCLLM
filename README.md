MCLLM
================



<details>

<summary>中文-简体 zh-Hans</summary>

### 一个在Minecraft中提供LLM交互的模组

<!-- [![Modrinth](https://img.shields.io/modrinth/dt/mcchatgpt?color=00AF5C&label=downloads&logo=modrinth)](https://modrinth.com/mod/mcchatgpt)
[![CurseForge](https://cf.way2muchnoise.eu/full_835315_downloads.svg)](https://curseforge.com/minecraft/mc-mods/mcchatgpt) -->

### 这个模型知道自己在Minecraft中，并会根据此环境进行响应。
- 模型受限于LLM服务提供者的训练数据

### 命令
- 使用 `/mcllm-setbaseurl "https://api.deepseek.com/v1"` 切换到DeepSeek服务上
- 使用 `/mcllm-auth <token>` 配合你的DeepSeek密钥进行API认证
  - 你可以从 [DeepSeek API Keys](https://platform.deepseek.com/api_keys) 获取密钥
- 使用 `/ask <question>` 向模型提问，模型会在聊天窗口中根据最近10条消息的上下文进行回复。
  - 使用 `/setisshowcost true`让输出文本的末尾加一个`(Cost: $6.0E-4)`(默认为true)
    - 鼠标悬停在消息上可查看使用的令牌数和API请求的大约费用。(token对应的价格请自行在配置文件中更改)
- 使用 `/setcontextlevel <0-3>` 设置模型的上下文级别。
  - 上下文级别越高，模型获取的关于世界和玩家的信息越多，但每次API请求的费用也越高。
  - 0：无信息（默认）
  - 1：玩家信息
    - 每次请求增加约100个令牌
  - 2：玩家和世界信息
    - 每次请求增加约200个令牌
  - 3：玩家、世界和实体信息
    - 每次请求增加约1000个令牌
- 使用 `/nextconversation` 进入下一个对话，或与模型开始新对话。
- 使用 `/previousconversation` 返回上一个与模型的对话。
- 使用 `/setconversation <conversationid>` 将与模型的对话设置为特定对话。
- 使用 `/listconversations` 列出你与模型的所有对话。
  - 将显示对话ID以及你在该对话中发送的最后一条消息。

### 配置
- model：用于API请求的模型。
  - 必须支持Chat Completions，否则会报错。
  - 默认值为 `gpt-3.5-turbo`。
    - 如果更改模型，你可能需要调整配置文件中的 estimated_cost_per_token 值以匹配新模型。
  - 你可以在 [这里](https://platform.openai.com/docs/models/overview) 找到模型列表。
- temperature：API请求使用的温度参数。
  - 范围在0到2之间，值越高，回复越有创意；值越低，回复越确定。
  - 默认值为 `1.0`。

### 安装
1. 从 [发布页面](https://github.com/link-fgfgui/MCLLM/releases) 下载最新版本的模组
   - 下载architectury依赖 [Architectury API](https://modrinth.com/mod/architectury-api)
   - 下载kotlin依赖 [Kotlin for Forge](https://modrinth.com/mod/kotlin-for-forge) [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin)
   - 如果使用Fabric版本，请下载最新版本的 [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api)
2. 将下载的模组文件放入你的mods文件夹
3. 启动Minecraft

### 报告问题
如果你发现任何问题，请在 [问题追踪器](https://github.com/link-fgfgui/MCLLM/issues) 上报告。
但我忙,可能不会处理,欢迎PR

</details>


### A mod that provides a LLM interface inside minecraft

<!-- [![Modrinth](https://img.shields.io/modrinth/dt/mcchatgpt?color=00AF5C&label=downloads&logo=modrinth)](https://modrinth.com/mod/mcchatgpt)
[![CurseForge](https://cf.way2muchnoise.eu/full_835315_downloads.svg)](https://curseforge.com/minecraft/mc-mods/mcchatgpt) -->

### This model knows it's inside Minecraft and will respond accordingly.
- The model is limited by the training data of the LLM service provider.

### Commands
- Use `/mcllm-auth <token>` with your OpenAI token to authenticate with the API
  - You can get a token from [OpenAI API Keys](https://platform.openai.com/account/api-keys)
- Use `/ask <question>` to ask the model a question; the model will respond in the chat with context from the last 10 messages.
  - Use `/setisshowcost true` to append `(Cost: $6.0E-4)` to the end of the output text (default is true).
    - Hover over the message to see the number of tokens used and the approximate cost of the API request. (Adjust the token price in the configuration file as needed.)
- Use `/setcontextlevel <0-3>` to set the context level of the model.
  - Higher context levels will provide the model with more information about the world and the player but will cost more per API request.
  - 0: No information (default)
  - 1: Player information
    - \+ ~100 tokens per request
  - 2: Player and world information
    - \+ ~200 tokens per request
  - 3: Player, world, and entity information 
    - \+ ~1k tokens per request
- Use `/nextconversation` go to the next conversation, or start a new conversation with the model.
- Use `/previousconversation` to go back to the previous conversation with the model.
- Use `/setconversation <conversationid>` to set the conversation with the model to a specific conversation.
- Use `/listconversations` to list all the conversations you have had with the model.
  - This will provide the conversation id, and the last message you sent in the conversation.

### Config
- model: The model used for the API requests. 
  - Must support Chat Completions otherwise an error will occur.
  - `gpt-3.5-turbo` is the default value.
    - If changed, you may need to change the estimated_cost_per_token value in the config to match the new model.
  - You can find a list of models [here](https://platform.openai.com/docs/models/overview).
- temperature: The temperature used for the API requests.
  - Between 0 and 2, higher values will result in more creative responses, while lower values will result in more deterministic responses.
  - `1.0` is the default value.

### Installation
1. Download the latest version of the mod from the [releases page](https://github.com/link-fgfgui/MCLLM/releases).
   - Download the Architectury dependency [Architectury API](https://modrinth.com/mod/architectury-api).
   - Download the Kotlin dependency [Kotlin for Forge](https://modrinth.com/mod/kotlin-for-forge) [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin).
   - If using the Fabric version, download the latest version of [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api).
2. Place the downloaded mod files in your mods folder.
3. Launch Minecraft.

### Reporting Bugs
If you find any bugs, please report them on the [issue tracker](https://github.com/Benjamin-Norton/MCGPT/issues).
However, I’m busy and may not address them; PRs are welcome.
