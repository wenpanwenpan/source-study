/**
 * Copyright (c) 2013-2019 Nikita Koksharov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.redisson.rx;

import java.util.concurrent.Callable;

import org.redisson.RedissonBlockingDeque;
import org.redisson.api.RFuture;

import io.reactivex.Flowable;

/**
 * 
 * @author Nikita Koksharov
 *
 * @param <V> - value type
 */
public class RedissonBlockingDequeRx<V> extends RedissonBlockingQueueRx<V> {

    private final RedissonBlockingDeque<V> queue;
    
    public RedissonBlockingDequeRx(RedissonBlockingDeque<V> queue) {
        super(queue);
        this.queue = queue;
    }

    public Flowable<V> takeFirstElements() {
        return ElementsStream.takeElements(new Callable<RFuture<V>>() {
            @Override
            public RFuture<V> call() throws Exception {
                return queue.takeFirstAsync();
            }
        });
    }
    
    public Flowable<V> takeLastElements() {
        return ElementsStream.takeElements(new Callable<RFuture<V>>() {
            @Override
            public RFuture<V> call() throws Exception {
                return queue.takeLastAsync();
            }
        });
    }
    
}
