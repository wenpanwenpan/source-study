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
package org.redisson.client;

import java.net.URI;

import org.redisson.misc.URIBuilder;

/**
 * 
 * @author Nikita Koksharov
 *
 */
public class RedisRedirectException extends RedisException {

    private static final long serialVersionUID = 181505625075250011L;

    private final int slot;
    private final URI url;

    public RedisRedirectException(int slot, String url) {
        this.slot = slot;
        this.url = URIBuilder.create("redis://" + url);
    }

    public int getSlot() {
        return slot;
    }

    public URI getUrl() {
        return url;
    }

}
