package org.geysermc.hydraulic.metadata;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.BitSet;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class BlockMapping {
    private static final RuntimeMetadata EMPTY_METADATA = new RuntimeMetadata(Map.of(), null, null, false, null);

    private final Identifier javaIdentifier;
    private final List<BlockStateRule> rules;
    private final List<CompiledRule> compiledRules;
    private final Map<AnchorKey, int[]> ruleOrdinalsByAnchor;
    private final ConcurrentMap<BlockState, MatchResult> resolvedStates = new ConcurrentHashMap<>();
    private final ConcurrentMap<StateDefinition<?, ?>, Map<String, Property<?>>> propertiesByDefinition = new ConcurrentHashMap<>();

    public BlockMapping(@NotNull Identifier javaIdentifier, @NotNull List<BlockStateRule> rules) {
        this.javaIdentifier = javaIdentifier;
        this.rules = List.copyOf(rules);
        CompiledRules compiledRules = compileRules(this.rules);
        this.compiledRules = compiledRules.rules();
        this.ruleOrdinalsByAnchor = compiledRules.ruleOrdinalsByAnchor();
    }

    @NotNull
    public Identifier javaIdentifier() {
        return this.javaIdentifier;
    }

    @NotNull
    public List<BlockStateRule> rules() {
        return this.rules;
    }

    @NotNull
    public MatchResult resolve(@NotNull BlockState state) {
        return this.resolvedStates.computeIfAbsent(state, this::resolveUncached);
    }

    @Nullable
    public BlockStateRule findRule(@NotNull BlockState state) {
        return this.resolve(state).rule();
    }

    int cachedStateCount() {
        return this.resolvedStates.size();
    }

    int indexedRuleCount() {
        return this.compiledRules.size() - this.unconditionalRuleCount();
    }

    int anchorBucketCount() {
        return this.ruleOrdinalsByAnchor.size();
    }

    int unconditionalRuleCount() {
        int count = 0;
        for (CompiledRule compiledRule : this.compiledRules) {
            if (compiledRule.unconditional()) {
                count++;
            }
        }
        return count;
    }

    @NotNull
    private MatchResult resolveUncached(@NotNull BlockState state) {
        Map<String, Property<?>> propertiesByName = this.propertiesFor(state);
        Map<String, String> stateValues = this.stateValues(state, propertiesByName);
        BitSet candidateOrdinals = this.candidateOrdinals(stateValues);
        for (CompiledRule compiledRule : this.compiledRules) {
            if (!compiledRule.unconditional() && !candidateOrdinals.get(compiledRule.ordinal())) {
                continue;
            }

            BlockStateRule rule = compiledRule.rule();
            if (rule.matches(state, propertiesByName)) {
                Identifier resolvedIdentifier = rule.bedrockIdentifier() != null ? rule.bedrockIdentifier() : this.javaIdentifier;
                return new MatchResult(rule, resolvedIdentifier, rule.bedrockIdentifier() != null, RuntimeMetadata.fromRule(rule));
            }
        }
        return new MatchResult(null, this.javaIdentifier, false, EMPTY_METADATA);
    }

    @NotNull
    private Map<String, Property<?>> propertiesFor(@NotNull BlockState state) {
        return this.propertiesByDefinition.computeIfAbsent(state.getBlock().getStateDefinition(), ignored -> {
            Map<String, Property<?>> propertiesByName = new LinkedHashMap<>();
            for (Property<?> property : state.getProperties()) {
                propertiesByName.put(property.getName(), property);
            }
            return Map.copyOf(propertiesByName);
        });
    }

    @NotNull
    private Map<String, String> stateValues(@NotNull BlockState state, @NotNull Map<String, Property<?>> propertiesByName) {
        Map<String, String> stateValues = new LinkedHashMap<>();
        for (Map.Entry<String, Property<?>> entry : propertiesByName.entrySet()) {
            stateValues.put(entry.getKey(), state.getValue(entry.getValue()).toString());
        }
        return stateValues;
    }

    @NotNull
    private BitSet candidateOrdinals(@NotNull Map<String, String> stateValues) {
        BitSet candidateOrdinals = new BitSet(this.compiledRules.size());
        for (Map.Entry<String, String> entry : stateValues.entrySet()) {
            int[] ordinals = this.ruleOrdinalsByAnchor.get(new AnchorKey(entry.getKey(), entry.getValue()));
            if (ordinals == null) {
                continue;
            }
            for (int ordinal : ordinals) {
                candidateOrdinals.set(ordinal);
            }
        }
        return candidateOrdinals;
    }

    @NotNull
    private static CompiledRules compileRules(@NotNull List<BlockStateRule> rules) {
        List<CompiledRule> compiledRules = new ArrayList<>(rules.size());
        Map<AnchorKey, List<Integer>> ruleOrdinalsByAnchor = new LinkedHashMap<>();
        for (int ordinal = 0; ordinal < rules.size(); ordinal++) {
            BlockStateRule rule = rules.get(ordinal);
            AnchorKey anchor = anchorFor(rule);
            compiledRules.add(new CompiledRule(ordinal, rule, anchor == null));
            if (anchor != null) {
                ruleOrdinalsByAnchor.computeIfAbsent(anchor, ignored -> new ArrayList<>()).add(ordinal);
            }
        }

        Map<AnchorKey, int[]> finalizedOrdinals = new LinkedHashMap<>();
        for (Map.Entry<AnchorKey, List<Integer>> entry : ruleOrdinalsByAnchor.entrySet()) {
            int[] ordinals = new int[entry.getValue().size()];
            for (int index = 0; index < entry.getValue().size(); index++) {
                ordinals[index] = entry.getValue().get(index);
            }
            finalizedOrdinals.put(entry.getKey(), ordinals);
        }

        return new CompiledRules(List.copyOf(compiledRules), Map.copyOf(finalizedOrdinals));
    }

    @Nullable
    private static AnchorKey anchorFor(@NotNull BlockStateRule rule) {
        if (rule.javaWhen().isEmpty()) {
            return null;
        }

        return rule.javaWhen().entrySet().stream()
            .min(Comparator.comparing((Map.Entry<String, String> entry) -> entry.getKey()).thenComparing(entry -> entry.getValue()))
            .map(entry -> new AnchorKey(entry.getKey(), entry.getValue()))
            .orElse(null);
    }

    public record MatchResult(@Nullable BlockStateRule rule, @NotNull Identifier identifier, boolean overridden, @NotNull RuntimeMetadata metadata) {
    }

    public record RuntimeMetadata(@NotNull Map<String, String> bedrockState, @Nullable String geometryId, @Nullable String materialId, boolean behaviorRequired, @Nullable String behaviorTag) {
        public RuntimeMetadata {
            bedrockState = Map.copyOf(bedrockState);
        }

        @NotNull
        public static RuntimeMetadata fromRule(@NotNull BlockStateRule rule) {
            return new RuntimeMetadata(
                rule.bedrockState() != null ? rule.bedrockState() : Map.of(),
                rule.geometryId(),
                rule.materialId(),
                rule.behaviorRequired(),
                rule.behaviorTag()
            );
        }
    }

    private record CompiledRules(@NotNull List<CompiledRule> rules, @NotNull Map<AnchorKey, int[]> ruleOrdinalsByAnchor) {
    }

    private record CompiledRule(int ordinal, @NotNull BlockStateRule rule, boolean unconditional) {
    }

    private record AnchorKey(@NotNull String propertyName, @NotNull String propertyValue) {
    }
}