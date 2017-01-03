/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.kafka.streams.processor.internals;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.StreamsMetrics;
import org.apache.kafka.streams.processor.StateRestoreCallback;
import org.apache.kafka.streams.processor.StateStore;
import org.apache.kafka.streams.processor.StreamPartitioner;
import org.apache.kafka.streams.processor.TaskId;
import org.apache.kafka.streams.state.internals.ThreadCache;
import java.io.File;
import java.util.Collections;
import java.util.Map;

public class StandbyContextImpl implements InternalProcessorContext, RecordCollector.Supplier {

    private static final RecordCollector NO_OP_COLLECTOR = new RecordCollector() {
        @Override
        public <K, V> void send(final ProducerRecord<K, V> record, final Serializer<K> keySerializer, final Serializer<V> valueSerializer) {

        }

        @Override
        public <K, V> void send(final ProducerRecord<K, V> record, final Serializer<K> keySerializer, final Serializer<V> valueSerializer, final StreamPartitioner<K, V> partitioner) {

        }

        @Override
        public void flush() {

        }

        @Override
        public void close() {

        }

        @Override
        public Map<TopicPartition, Long> offsets() {
            return Collections.emptyMap();
        }
    };

    private final TaskId id;
    private final String applicationId;
    private final StreamsMetrics metrics;
    private final ProcessorStateManager stateMgr;

    private final StreamsConfig config;
    private final Serde<?> keySerde;
    private final Serde<?> valSerde;
    private final ThreadCache zeroSizedCache = new ThreadCache(0);

    private boolean initialized;

    public StandbyContextImpl(TaskId id,
                              String applicationId,
                              StreamsConfig config,
                              ProcessorStateManager stateMgr,
                              StreamsMetrics metrics) {
        this.id = id;
        this.applicationId = applicationId;
        this.metrics = metrics;
        this.stateMgr = stateMgr;

        this.config = config;
        this.keySerde = config.keySerde();
        this.valSerde = config.valueSerde();

        this.initialized = false;
    }

    public void initialized() {
        this.initialized = true;
    }

    public ProcessorStateManager getStateMgr() {
        return stateMgr;
    }

    @Override
    public TaskId taskId() {
        return id;
    }

    @Override
    public String applicationId() {
        return applicationId;
    }

    @Override
    public RecordCollector recordCollector() {
        return NO_OP_COLLECTOR;
    }

    @Override
    public Serde<?> keySerde() {
        return this.keySerde;
    }

    @Override
    public Serde<?> valueSerde() {
        return this.valSerde;
    }

    @Override
    public File stateDir() {
        return stateMgr.baseDir();
    }

    @Override
    public StreamsMetrics metrics() {
        return metrics;
    }

    /**
     * @throws IllegalStateException
     */
    @Override
    public void register(StateStore store, boolean loggingEnabled, StateRestoreCallback stateRestoreCallback) {
        if (initialized)
            throw new IllegalStateException("Can only create state stores during initialization.");

        stateMgr.register(store, loggingEnabled, stateRestoreCallback);
    }

    /**
     * @throws UnsupportedOperationException
     */
    @Override
    public StateStore getStateStore(String name) {
        throw new UnsupportedOperationException("this should not happen: getStateStore() not supported in standby tasks.");
    }

    @Override
    public ThreadCache getCache() {
        return zeroSizedCache;
    }

    /**
     * @throws UnsupportedOperationException
     */
    @Override
    public String topic() {
        throw new UnsupportedOperationException("this should not happen: topic() not supported in standby tasks.");
    }

    /**
     * @throws UnsupportedOperationException
     */
    @Override
    public int partition() {
        throw new UnsupportedOperationException("this should not happen: partition() not supported in standby tasks.");
    }

    /**
     * @throws UnsupportedOperationException
     */
    @Override
    public long offset() {
        throw new UnsupportedOperationException("this should not happen: offset() not supported in standby tasks.");
    }

    /**
     * @throws UnsupportedOperationException
     */
    @Override
    public long timestamp() {
        throw new UnsupportedOperationException("this should not happen: timestamp() not supported in standby tasks.");
    }

    /**
     * @throws UnsupportedOperationException
     */
    @Override
    public <K, V> void forward(K key, V value) {
        throw new UnsupportedOperationException("this should not happen: forward() not supported in standby tasks.");
    }

    /**
     * @throws UnsupportedOperationException
     */
    @Override
    public <K, V> void forward(K key, V value, int childIndex) {
        throw new UnsupportedOperationException("this should not happen: forward() not supported in standby tasks.");
    }

    /**
     * @throws UnsupportedOperationException
     */
    @Override
    public <K, V> void forward(K key, V value, String childName) {
        throw new UnsupportedOperationException("this should not happen: forward() not supported in standby tasks.");
    }

    /**
     * @throws UnsupportedOperationException
     */
    @Override
    public void commit() {
        throw new UnsupportedOperationException("this should not happen: commit() not supported in standby tasks.");
    }

    /**
     * @throws UnsupportedOperationException
     */
    @Override
    public void schedule(long interval) {
        throw new UnsupportedOperationException("this should not happen: schedule() not supported in standby tasks.");
    }

    @Override
    public Map<String, Object> appConfigs() {
        return config.originals();
    }

    @Override
    public Map<String, Object> appConfigsWithPrefix(String prefix) {
        return config.originalsWithPrefix(prefix);
    }

    @Override
    public RecordContext recordContext() {
        throw new UnsupportedOperationException("this should not happen: recordContext not supported in standby tasks.");
    }

    @Override
    public void setRecordContext(final RecordContext recordContext) {
        throw new UnsupportedOperationException("this should not happen: setRecordContext not supported in standby tasks.");
    }


    @Override
    public void setCurrentNode(final ProcessorNode currentNode) {
        // no-op. can't throw as this is called on commit when the StateStores get flushed.
    }

    @Override
    public ProcessorNode currentNode() {
        throw new UnsupportedOperationException("this should not happen: currentNode not supported in standby tasks.");
    }
}