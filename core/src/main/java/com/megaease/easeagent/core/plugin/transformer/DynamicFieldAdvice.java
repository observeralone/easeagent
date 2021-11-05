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
 */

package com.megaease.easeagent.core.plugin.transformer;

import com.megaease.easeagent.plugin.field.DynamicFieldAccessor;
import com.megaease.easeagent.plugin.field.NullObject;
import net.bytebuddy.asm.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynamicFieldAdvice {
    public static Logger log = LoggerFactory.getLogger(DynamicFieldAdvice.class);

    public static class DynamicInstanceInit {
        @Advice.OnMethodExit
        public static void exit(@Advice.This(optional = true) Object target) {
            if (target instanceof DynamicFieldAccessor) {
                DynamicFieldAccessor accessor = (DynamicFieldAccessor)target;
                if (accessor.getEaseAgent$$DynamicField$$Data() == null) {
                    accessor.setEaseAgent$$DynamicField$$Data(NullObject.NULL);
                }
            }
        }
    }

    public static class DynamicClassInit {
        @Advice.OnMethodExit
        public static void exit(@Advice.Origin("#m") String method) {
        }
    }
}
