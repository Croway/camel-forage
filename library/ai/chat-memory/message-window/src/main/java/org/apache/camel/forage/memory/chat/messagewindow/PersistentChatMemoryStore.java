/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.forage.memory.chat.messagewindow;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dev.langchain4j.data.message.ChatMessageDeserializer.messagesFromJson;
import static dev.langchain4j.data.message.ChatMessageSerializer.messagesToJson;

public class PersistentChatMemoryStore implements ChatMemoryStore {
    private static final Logger LOG = LoggerFactory.getLogger(PersistentChatMemoryStore.class);
    private final Map<Object, String> memoryMap = new ConcurrentHashMap<>();

    public PersistentChatMemoryStore() {
        LOG.trace("Creating PersistentChatMemoryStore {}", Thread.currentThread().getId());
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String json = memoryMap.get(memoryId);
        return json != null ? messagesFromJson(json) : List.of();
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String json = messagesToJson(messages);
        memoryMap.put(memoryId, json);
        if (LOG.isTraceEnabled()) {
            LOG.trace("Updated PersistentChatMemoryStore {}: {}", Thread.currentThread().getId(), memoryMap);
        }
    }

    @Override
    public void deleteMessages(Object memoryId) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Deleted PersistentChatMemoryStore {}: {}", Thread.currentThread().getId(), memoryMap);
        }
        memoryMap.remove(memoryId);
    }

    public int getMemoryCount() {
        return memoryMap.size();
    }

    public void clearAll() {
        LOG.trace("Clearing PersistentChatMemoryStore {}", Thread.currentThread().getId());
        memoryMap.clear();
    }
}
