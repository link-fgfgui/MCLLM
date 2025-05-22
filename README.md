MC ChatGPT Compatible
================
### A mod that provides a LLM interface inside minecraft

### This model knows it's inside Minecraft, and will respond accordingly.
- The model is limited by ChatGPT's training data, which ended around when 1.17 was released.
- ChatGPT gpt-3.5-turbo这个模型只知道1.17及以前的事情,其它模型的知识库截止日期请自行查询

两个版本硬编码都一套一套的,只能保证deepseek的基本使用

### Commands
- Use `/mcchatgpt-auth <token>` with your OpenAI token to authenticate with the API
  - You can get a token from [OpenAI API Keys](https://platform.openai.com/account/api-keys)
- 使用 `/mcchatgpt-auth <token>` 填入token
  - deepseek token 获取 [deepseek API Keys](https://platform.deepseek.com/api_keys)
- Use `/ask <question>` to ask the model a question, the model will respond in the chat with context from the last 10 messages.
  - Hover over the message to see the number of tokens used and approximate cost of the API request.
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
1. 关闭游戏
2. 去`.minecraft/config/mcchatgpt.json`修改配置文件
- deepseek
  - baseurl `https://api.deepseek.com/`
  - model `deepseek-chat`&`deepseek-reasoner`
- 讯飞星火 
  - baseurl `https://spark-api-open.xf-yun.com/`
  - model `Lite`(Spark Lite)

欢迎补充


- model: The model used for the API requests. 
  - Must support Chat Completions otherwise an error will occur.
  - `gpt-3.5-turbo` is the default value.
    - If changed, you may need to change the estimated_cost_per_token value in the config to match the new model.
  - You can find a list of models [here](https://platform.openai.com/docs/models/overview).
- temperature: The temperature used for the API requests.
  - Between 0 and 2, higher values will result in more creative responses, while lower values will result in more deterministic responses.
  - `1.0` is the default value.

### Installation
1. Download the latest version of the mod from the [releases page](https://github.com/link-fgfgui/MCChatGPT/releases)
   - If you are using the fabric version, please download the latest version of [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api)
2. Place the downloaded mod files in your mods folder
3. Launch Minecraft

### Reporting Bugs
我个人没有在Minecraft中使用LLM的需求,fork仅用做deepseek测试,可以去issues提bug但是我不会修,欢迎PR,当有人接手原项目时此仓库将存档

I personally don't have a need to use LLM in Minecraft. The fork is only used for deepseek testing. You can go to issues to report bugs, but I won't fix them. PRs are welcome. This repository will be archived when someone takes over the [original repo](https://github.com/Bawnorton/MCChatGPT).