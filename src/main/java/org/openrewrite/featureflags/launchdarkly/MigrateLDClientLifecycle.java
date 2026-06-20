/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.featureflags.launchdarkly;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;

@EqualsAndHashCode(callSuper = false)
@Value
public class MigrateLDClientLifecycle extends Recipe {

    private static final MethodMatcher CLOSE = new MethodMatcher("com.launchdarkly.sdk.server.LDClient close()");
    private static final MethodMatcher IS_INITIALIZED = new MethodMatcher("com.launchdarkly.sdk.server.LDClient isInitialized()");

    String displayName = "Migrate LaunchDarkly `LDClient` lifecycle calls to OpenFeature";

    String description = "Migrate `LDClient` lifecycle calls: `close()` becomes `OpenFeatureAPI.getInstance().shutdown()` " +
            "and `isInitialized()` becomes `OpenFeatureAPI.getInstance().getClient().getProviderState() == ProviderState.READY`. " +
            "OpenFeature manages provider state globally rather than per client instance, so the original receiver is dropped.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.or(new UsesMethod<>(CLOSE), new UsesMethod<>(IS_INITIALIZED)),
                new JavaVisitor<ExecutionContext>() {
                    @Override
                    public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                        if (CLOSE.matches(m)) {
                            maybeAddImport("dev.openfeature.sdk.OpenFeatureAPI");
                            return JavaTemplate.builder("OpenFeatureAPI.getInstance().shutdown()")
                                    .imports("dev.openfeature.sdk.OpenFeatureAPI")
                                    .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx, "sdk-1.+"))
                                    .build()
                                    .apply(getCursor(), m.getCoordinates().replace())
                                    .withPrefix(m.getPrefix());
                        }
                        if (IS_INITIALIZED.matches(m)) {
                            maybeAddImport("dev.openfeature.sdk.OpenFeatureAPI");
                            maybeAddImport("dev.openfeature.sdk.ProviderState");
                            return JavaTemplate.builder("OpenFeatureAPI.getInstance().getClient().getProviderState() == ProviderState.READY")
                                    .imports("dev.openfeature.sdk.OpenFeatureAPI", "dev.openfeature.sdk.ProviderState")
                                    .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx, "sdk-1.+"))
                                    .build()
                                    .apply(getCursor(), m.getCoordinates().replace())
                                    .withPrefix(m.getPrefix());
                        }
                        return m;
                    }
                });
    }
}
