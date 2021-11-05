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

package com.megaease.easeagent.core.instrument;

import com.megaease.easeagent.core.Bootstrap;
import com.megaease.easeagent.core.plugin.CommonInlineAdvice;
import com.megaease.easeagent.core.plugin.PluginLoader;
import com.megaease.easeagent.core.plugin.interceptor.InterceptorPluginDecorator;
import com.megaease.easeagent.core.plugin.interceptor.SupplierChain;
import com.megaease.easeagent.core.plugin.matcher.MethodTransformation;
import com.megaease.easeagent.core.plugin.registry.QualifierRegistry;
import com.megaease.easeagent.core.utils.AgentAttachmentRule;
import com.megaease.easeagent.plugin.Interceptor;
import com.megaease.easeagent.plugin.MethodInfo;
import com.megaease.easeagent.plugin.api.Context;
import com.megaease.easeagent.plugin.field.AgentDynamicFieldAccessor;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.dynamic.scaffold.TypeWriter;
import net.bytebuddy.matcher.ElementMatchers;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StaticMethodTransformTest {
    private static final String FOO = "foo",
        BAR = "bar",
        QUX = "qux",
        CLASS_INIT = "<clinit>",
        FOO_STATIC = "fooStatic";

    @SuppressWarnings("unused")
    public static class Foo {
        static String clazzInitString = FOO;
        public static String fooStatic(String a) {
            return a;
        }
        public String foo(String a) {
            return a;
        }
    }

    public static class FooInterceptor implements Interceptor {
        @Override
        public void before(MethodInfo methodInfo, Context context) {
            Object [] args = methodInfo.getArgs();
            args[0] = QUX;
            methodInfo.markChanged();
        }

        @Override
        public void after(MethodInfo methodInfo, Context context) {
            methodInfo.setRetValue(methodInfo.getRetValue() + BAR);
        }
    }

    public static class FooClassInitInterceptor implements Interceptor {
        @Override
        public void before(MethodInfo methodInfo, Context context) {
        }

        @Override
        public void after(MethodInfo methodInfo, Context context) {
            Foo.clazzInitString = BAR;
        }
    }

    private static ClassLoader classLoader;
    private static String dumpFolder;
    private static AtomicInteger globalIndex = new AtomicInteger(0);

    @Rule
    public MethodRule agentAttachmentRule = new AgentAttachmentRule();

    @BeforeClass
    public static void setUp() {
        classLoader = new ByteArrayClassLoader.ChildFirst(
            NonStaticMethodTransformTest.class.getClassLoader(),
            ClassFileLocator.ForClassLoader.readToNames(Foo.class, CommonInlineAdvice.class),
            ByteArrayClassLoader.PersistenceHandler.MANIFEST);

        String path = "target/test-classes";
        File file = new File(path);
        dumpFolder = file.getAbsolutePath();
        System.out.println(dumpFolder);
        assertTrue(dumpFolder.endsWith("target/test-classes"));
    }

    private Set<MethodTransformation> getMethodTransformations(int index, String methodName) {
        Supplier<Interceptor> supplier = FooInterceptor::new;
        SupplierChain.Builder<Interceptor> chainBuilder = SupplierChain.builder();
        chainBuilder.addSupplier(InterceptorPluginDecorator.getInterceptorSupplier(new TestPlugin(), supplier));

        MethodTransformation methodTransformation = new MethodTransformation(index,
            ElementMatchers.named(methodName),
            chainBuilder);
        QualifierRegistry.addMethodTransformation(index, methodTransformation);

        Set<MethodTransformation> transformations = new HashSet<>();
        transformations.add(methodTransformation);

        return transformations;
    }

    @Test
    @AgentAttachmentRule.Enforce
    public void testStaticAdviceTransformer() throws Exception {
        System.setProperty(TypeWriter.DUMP_PROPERTY, dumpFolder);
        assertEquals(System.getProperty(TypeWriter.DUMP_PROPERTY), dumpFolder);

        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        AgentBuilder builder = Bootstrap.getAgentBuilder(null);

        Set<MethodTransformation> transformations = getMethodTransformations(globalIndex.incrementAndGet(), FOO_STATIC);

        ClassFileTransformer classFileTransformer = builder
            .type(ElementMatchers.is(Foo.class), ElementMatchers.is(classLoader))
            .transform(PluginLoader.compound(false, transformations))
            .installOnByteBuddyAgent();

        try {
            Class<?> type = classLoader.loadClass(Foo.class.getName());
            // check
            assertThat(type.getDeclaredMethod(FOO_STATIC, String.class)
                    .invoke(null, "kkk"),
                is(QUX + BAR));
        } finally {
            assertThat(ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer), is(true));
        }
    }

    @Test
    @AgentAttachmentRule.Enforce
    public void testTypeInitialAdviceTransformer() throws Exception {
        System.setProperty(TypeWriter.DUMP_PROPERTY, dumpFolder);
        assertEquals(System.getProperty(TypeWriter.DUMP_PROPERTY), dumpFolder);

        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        AgentBuilder builder = Bootstrap.getAgentBuilder(null);

        Supplier<Interceptor> supplier = FooClassInitInterceptor::new;
        SupplierChain.Builder<Interceptor> chainBuilder = SupplierChain.builder();
        chainBuilder.addSupplier(supplier);

        MethodTransformation methodTransformation = new MethodTransformation(globalIndex.incrementAndGet(),
            ElementMatchers.named(CLASS_INIT),
            chainBuilder);

        Set<MethodTransformation> transformations = new HashSet<>();
        transformations.add(methodTransformation);

        ClassFileTransformer classFileTransformer = builder
            .type(ElementMatchers.is(Foo.class), ElementMatchers.is(classLoader))
            .transform(PluginLoader.compound(false, transformations))
            .installOnByteBuddyAgent();

        try {
            Class<?> type = classLoader.loadClass(Foo.class.getName());
            // check, wait to finish
            // assertEquals(Foo.clazzInitString, BAR);
        } finally {
            assertThat(ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer), is(true));
        }
    }
}
