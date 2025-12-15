/*
 * Copyright 2024 the original author or authors.
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
import org.openrewrite.*;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.*;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.reverse;
import static java.util.Objects.requireNonNull;

@EqualsAndHashCode(callSuper = false)
@Value
public class MigrateUserToContext extends Recipe {
    private static final MethodMatcher NEW_USER = new MethodMatcher("com.launchdarkly.sdk.LDUser <constructor>(java.lang.String)");
    private static final MethodMatcher NEW_USER_BUILDER = new MethodMatcher("com.launchdarkly.sdk.LDUser.Builder <constructor>(java.lang.String)");

    private static final List<String> BASIC_ATTRIBUTES = Arrays.asList("avatar", "country", "email", "firstName", "ip", "lastName");
    private static final List<String> PRIVATE_ATTRIBUTES = Arrays.asList("privateAvatar", "privateCountry", "privateEmail", "privateFirstName", "privateIp", "privateLastName", "privateName");
    private static final MethodMatcher BUILTIN_ATTRIBUTE = new MethodMatcher("com.launchdarkly.sdk.LDUser.Builder *(java.lang.String)");
    private static final MethodMatcher BUILTIN_PRIVATE_ATTRIBUTE = new MethodMatcher("com.launchdarkly.sdk.LDUser.Builder private*(java.lang.String)");
    private static final MethodMatcher CUSTOM_ATTRIBUTES = new MethodMatcher("com.launchdarkly.sdk.LDUser.Builder custom(java.lang.String, ..)"); // FIXME: This really should be `*`
    private static final MethodMatcher PRIVATE_CUSTOM_ATTRIBUTES = new MethodMatcher("com.launchdarkly.sdk.LDUser.Builder privateCustom(java.lang.String, ..)"); // FIXME: This really should be `*`

    @Override
    public String getDisplayName() {
        return "Migrate `LDUser` to `LDContext`";
    }

    @Override
    public String getDescription() {
        return "Migrate from `LDUser` and `LDUser.Builder` to `LDContext` and `ContextBuilder`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new UsesType<>("com.launchdarkly.sdk.LDUser", null),
                new JavaVisitor<ExecutionContext>() {
                    @Override
                    public J visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                        if (NEW_USER.matches(newClass)) {
                            maybeRemoveImport("com.launchdarkly.sdk.LDUser");
                            maybeAddImport("com.launchdarkly.sdk.LDContext");
                            doAfterVisit(new ChangeType("com.launchdarkly.sdk.LDUser", "com.launchdarkly.sdk.LDContext", null).getVisitor());
                            return JavaTemplate.builder("LDContext.create(#{any(java.lang.String)})")
                                    .contextSensitive()
                                    .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx, "launchdarkly-java-server-sdk-6.+"))
                                    .imports("com.launchdarkly.sdk.LDContext")
                                    .build()
                                    .apply(getCursor(), newClass.getCoordinates().replace(), newClass.getArguments().get(0));
                        }
                        if (NEW_USER_BUILDER.matches(newClass)) {
                            maybeRemoveImport("com.launchdarkly.sdk.LDUser");
                            maybeAddImport("com.launchdarkly.sdk.LDContext");
                            doAfterVisit(new ChangeType("com.launchdarkly.sdk.LDUser", "com.launchdarkly.sdk.LDContext", null).getVisitor());
                            return JavaTemplate.builder("LDContext.builder(#{any(java.lang.String)})")
                                    .contextSensitive()
                                    .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx, "launchdarkly-java-server-sdk-6.+"))
                                    .imports("com.launchdarkly.sdk.LDContext")
                                    .build()
                                    .apply(getCursor(), newClass.getCoordinates().replace(), newClass.getArguments().get(0));
                        }

                        return super.visitNewClass(newClass, ctx);
                    }

                    @Override
                    public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                        if (BUILTIN_ATTRIBUTE.matches(m) && BASIC_ATTRIBUTES.contains(m.getSimpleName())) {
                            String code;
                            if (requireNonNull(m.getPadding().getSelect()).getAfter().getWhitespace().contains("\n")) {
                                code = "#{any(com.launchdarkly.sdk.ContextBuilder)}\n.set(#{any(java.lang.String)}, #{any()})";
                            } else {
                                code = "#{any(com.launchdarkly.sdk.ContextBuilder)}.set(#{any(java.lang.String)}, #{any()})";
                            }
                            return JavaTemplate.builder(code)
                                    .contextSensitive()
                                    .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx, "launchdarkly-java-server-sdk-6.+"))
                                    .imports("com.launchdarkly.sdk.ContextBuilder")
                                    .build()
                                    .apply(
                                            getCursor(),
                                            m.getCoordinates().replace(),
                                            m.getSelect(),
                                            new J.Literal(Tree.randomId(), Space.EMPTY, Markers.EMPTY, m.getSimpleName(), "\"" + m.getSimpleName() + "\"", null, JavaType.Primitive.String),
                                            m.getArguments().get(0)
                                    );
                        }
                        if (BUILTIN_PRIVATE_ATTRIBUTE.matches(m) && PRIVATE_ATTRIBUTES.contains(m.getSimpleName())) {
                            doAfterVisit(new UseVarargsForPrivateAttributes());

                            String code;
                            if (requireNonNull(m.getPadding().getSelect()).getAfter().getWhitespace().contains("\n")) {
                                code = "#{any(com.launchdarkly.sdk.ContextBuilder)}\n.set(#{any(java.lang.String)}, #{any()})\n.privateAttributes(#{any(java.lang.String)})";
                            } else {
                                code = "#{any(com.launchdarkly.sdk.ContextBuilder)}.set(#{any(java.lang.String)}, #{any()}).privateAttributes(#{any(java.lang.String)})";
                            }
                            String attributeName = StringUtils.uncapitalize(m.getSimpleName().replace("private", ""));
                            return JavaTemplate.builder(code)
                                    .contextSensitive()
                                    .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx, "launchdarkly-java-server-sdk-6.+"))
                                    .imports("com.launchdarkly.sdk.ContextBuilder")
                                    .build()
                                    .apply(
                                            getCursor(),
                                            m.getCoordinates().replace(),
                                            m.getSelect(),
                                            new J.Literal(Tree.randomId(), Space.EMPTY, Markers.EMPTY, attributeName, "\"" + attributeName + "\"", null, JavaType.Primitive.String),
                                            m.getArguments().get(0),
                                            new J.Literal(Tree.randomId(), Space.EMPTY, Markers.EMPTY, attributeName, "\"" + attributeName + "\"", null, JavaType.Primitive.String)
                                    );
                        }
                        if (CUSTOM_ATTRIBUTES.matches(m)) {
                            String code;
                            if (requireNonNull(m.getPadding().getSelect()).getAfter().getWhitespace().contains("\n")) {
                                code = "#{any(com.launchdarkly.sdk.ContextBuilder)}\n.set(#{any(java.lang.String)}, #{any()})";
                            } else {
                                code = "#{any(com.launchdarkly.sdk.ContextBuilder)}.set(#{any(java.lang.String)}, #{any()})";
                            }
                            return JavaTemplate.builder(code)
                                    .contextSensitive()
                                    .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx, "launchdarkly-java-server-sdk-6.+"))
                                    .imports("com.launchdarkly.sdk.ContextBuilder")
                                    .build()
                                    .apply(getCursor(), m.getCoordinates().replace(), m.getSelect(), m.getArguments().get(0), m.getArguments().get(1));
                        }
                        if (PRIVATE_CUSTOM_ATTRIBUTES.matches(m)) {
                            doAfterVisit(new UseVarargsForPrivateAttributes());

                            String code;
                            if (requireNonNull(m.getPadding().getSelect()).getAfter().getWhitespace().contains("\n")) {
                                code = "#{any(com.launchdarkly.sdk.ContextBuilder)}\n.set(#{any(java.lang.String)}, #{any()})\n.privateAttributes(#{any(java.lang.String)})";
                            } else {
                                code = "#{any(com.launchdarkly.sdk.ContextBuilder)}.set(#{any(java.lang.String)}, #{any()}).privateAttributes(#{any(java.lang.String)})";
                            }
                            return JavaTemplate.builder(code)
                                    .contextSensitive()
                                    .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx, "launchdarkly-java-server-sdk-6.+"))
                                    .imports("com.launchdarkly.sdk.ContextBuilder")
                                    .build()
                                    .apply(getCursor(), m.getCoordinates().replace(), m.getSelect(), m.getArguments().get(0), m.getArguments().get(1), m.getArguments().get(0));
                        }
                        return m;
                    }
                }
        );
    }

    private static class UseVarargsForPrivateAttributes extends JavaIsoVisitor<ExecutionContext> {
        private static final MethodMatcher CONTEXT_BUILDER_MATCHER = new MethodMatcher("com.launchdarkly.sdk.ContextBuilder *(..)");
        private static final MethodMatcher PRIVATE_ATTRIBUTES_STRING_VARARGS_MATCHER = new MethodMatcher("com.launchdarkly.sdk.ContextBuilder privateAttributes(java.lang.String...)");
        private static final MethodMatcher USER_BUILDER_BUILD_MATCHER = new MethodMatcher("com.launchdarkly.sdk.LDContext$Builder build()");
        private static final MethodMatcher CONTEXT_BUILDER_BUILD_MATCHER = new MethodMatcher("com.launchdarkly.sdk.ContextBuilder build()");

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
            if (!(USER_BUILDER_BUILD_MATCHER.matches(m) || CONTEXT_BUILDER_BUILD_MATCHER.matches(m))) {
                return m;
            }

            List<J.MethodInvocation> chain = computeChain(m);
            return unfold(m, chain);
        }

        private List<J.MethodInvocation> computeChain(J.MethodInvocation build) {
            List<J.MethodInvocation> chain = new ArrayList<>();
            if (!(build.getSelect() instanceof J.MethodInvocation)) {
                return chain;
            }

            List<Expression> attributes = new ArrayList<>();
            Expression select = build.getSelect();
            int privateAttributesInvocations = 0;
            int lastPrivateAttributesIdx = -1;
            while (CONTEXT_BUILDER_MATCHER.matches(select)) {
                J.MethodInvocation m = (J.MethodInvocation) select;
                if (PRIVATE_ATTRIBUTES_STRING_VARARGS_MATCHER.matches(m)) {
                    if (lastPrivateAttributesIdx == -1 && CONTEXT_BUILDER_MATCHER.matches(m.getSelect())) {
                        lastPrivateAttributesIdx = chain.size();
                        chain.add(m);
                    }
                    attributes.addAll(0, m.getArguments());
                    privateAttributesInvocations++;
                } else {
                    chain.add(m);
                }
                select = m.getSelect();
            }
            if (privateAttributesInvocations <= 1) {
                return emptyList();
            }
            for (int i = 1; i < attributes.size(); i++) {
                attributes.set(i, attributes.get(i).withPrefix(Space.SINGLE_SPACE));
            }
            chain.set(lastPrivateAttributesIdx, chain.get(lastPrivateAttributesIdx).withArguments(attributes));
            return chain;
        }

        private J.MethodInvocation unfold(J.MethodInvocation build, List<J.MethodInvocation> chain) {
            if (chain.isEmpty()) {
                return build;
            }
            reverse(chain);

            J.MethodInvocation select = chain.get(0);
            for (int i = 1; i < chain.size(); i++) {
                select = chain.get(i)
                        .withId(Tree.randomId())
                        .withSelect(select);
            }
            return build.withSelect(select);
        }
    }
}
