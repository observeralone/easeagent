/*
 * Copyright (c) 2021, MegaEase
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.megaease.easeagent.report.plugin;

import com.megaease.easeagent.plugin.api.config.Config;
import com.megaease.easeagent.plugin.report.ByteWrapper;
import com.megaease.easeagent.plugin.report.EncodedData;
import com.megaease.easeagent.plugin.report.Encoder;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class NoOpEncoder<S> implements Encoder<S> {
    public static final NoOpEncoder<?> INSTANCE = new NoOpEncoder<>();

    @Override
    public void init(Config config) {
        // ignored
    }

    @Override
    public String name() {
        return "noop";
    }

    @Override
    public int sizeInBytes(S input) {
        return input.toString().length();
    }

    @Override
    public EncodedData encode(S input) {
        return new ByteWrapper(input.toString().getBytes(StandardCharsets.US_ASCII));
    }

    @Override
    public EncodedData encodeList(List<EncodedData> encodedItems) {
        StringBuilder sb = new StringBuilder();
        encodedItems.forEach(sb::append);
        return new ByteWrapper(sb.toString().getBytes(StandardCharsets.US_ASCII));
    }

    @Override
    public int appendSizeInBytes(List<Integer> sizes, int newMsgSize) {
        return newMsgSize;
    }

    @Override
    public int packageSizeInBytes(List<Integer> sizes) {
        return sizes.stream().mapToInt(s -> s).sum();
    }
}
