package com.bawnorton.mcchatgpt.util;

import com.openai.models.chat.completions.ChatCompletionMessageParam;

import java.util.ArrayList;
import java.util.List;

public class Conversation {

    private final List<ChatCompletionMessageParam> messageList;
    private ChatCompletionMessageParam previewMessage;
    private int contextIndex;

    public Conversation() {
        this.messageList = new ArrayList<>();
        this.contextIndex = -1;
    }

    public void addMessage(ChatCompletionMessageParam message) {
        messageList.add(message);
    }

    public void setPreviewMessage(ChatCompletionMessageParam message) {
        previewMessage = message;
    }

    public ChatCompletionMessageParam getPreviewMessage() {
        return previewMessage;
    }

    public List<ChatCompletionMessageParam> getMessages() {
        return messageList;
    }

    public void resetContext() {
        if (contextIndex != -1) messageList.remove(contextIndex);
        contextIndex = -1;
    }

    public void setContext(ChatCompletionMessageParam message) {
        resetContext();
        messageList.add(message);
        contextIndex = messageList.size() - 1;
    }

    public int messageCount() {
        return messageList.size();
    }

    public void removeMessage(int i) {
        messageList.remove(i);
    }
}
