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

package com.megaease.easeagent.core.plugin;

import com.megaease.easeagent.core.plugin.annotation.Index;
import com.megaease.easeagent.plugin.api.interceptor.MethodInfo;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

/**
 * uniform interceptor for static method
 * get interceptor chain thought index generated by interceptor registry
 */
public class CommonInlineStaticAdvice {
    private static final String CONTEXT = "easeagent_context";

    @Advice.OnMethodEnter
    public static MethodInfo enter(@Index long index,
                                   @Advice.Origin("#m") String method,
                                   @Advice.AllArguments Object[] args,
                                   @Advice.Local(CONTEXT) Object context) {
        MethodInfo methodInfo = MethodInfo.builder()
            .invoker(null)
            .method(method)
            .args(args)
            .build();

        System.out.println("enter :" + method + ", index:" +  index);
        context = "start at :" + System.currentTimeMillis();

        // call chain
        // Dispatcher.enter(index, methodInfo, context);

        return methodInfo;
    }

    @Advice.OnMethodExit(onThrowable = Exception.class)
    public static void exit(@Index long index,
                            @Advice.Enter MethodInfo methodInfo,
                            @Advice.Return(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object result,
                            @Advice.Thrown Throwable throwable,
                            @Advice.Local(CONTEXT) Object context) {
        methodInfo.setThrowable(throwable);
        methodInfo.setRetValue(result);

        System.out.println("exit with :" + result);
        System.out.println("exit with context :" + context);

        // Dispatcher.exit(index, methodInfo, context);
    }
}
