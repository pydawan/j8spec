package j8spec;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static j8spec.BlockExecutionFlag.*;
import static j8spec.ItBlock.newIgnoredItBlock;
import static j8spec.ItBlock.newItBlock;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;

/**
 * Representation of a "describe" block.
 * @since 2.0.0
 */
public final class DescribeBlock {

    @FunctionalInterface
    private interface ShouldBeIgnoredPredicate {
        boolean test(DescribeBlock describeBlock, ItBlockDefinition itBlockDefinition);
    }

    private static final String LS = System.getProperty("line.separator");

    private final DescribeBlock parent;
    private final String description;
    private final List<UnsafeBlock> beforeAllBlocks;
    private final List<UnsafeBlock> beforeEachBlocks;
    private final Map<String, ItBlockDefinition> itBlockDefinitions;
    private final List<DescribeBlock> describeBlocks = new LinkedList<>();
    private final Class<?> specClass;
    private final BlockExecutionFlag executionFlag;

    static DescribeBlock newRootDescribeBlock(
        Class<?> specClass,
        List<UnsafeBlock> beforeAllBlocks,
        List<UnsafeBlock> beforeEachBlocks,
        Map<String, ItBlockDefinition> itBlocks
    ) {
        return new DescribeBlock(specClass, beforeAllBlocks, beforeEachBlocks, itBlocks);
    }

    private DescribeBlock(
        Class<?> specClass,
        List<UnsafeBlock> beforeAllBlocks,
        List<UnsafeBlock> beforeEachBlocks,
        Map<String, ItBlockDefinition> itBlockDefinitions
    ) {
        this.parent = null;
        this.specClass = specClass;
        this.description = specClass.getName();
        this.beforeAllBlocks = unmodifiableList(beforeAllBlocks);
        this.beforeEachBlocks = unmodifiableList(beforeEachBlocks);
        this.itBlockDefinitions = unmodifiableMap(itBlockDefinitions);
        this.executionFlag = DEFAULT;
    }

    private DescribeBlock(
        DescribeBlock parent,
        String description,
        List<UnsafeBlock> beforeAllBlocks,
        List<UnsafeBlock> beforeEachBlocks,
        Map<String, ItBlockDefinition> itBlockDefinitions,
        BlockExecutionFlag executionFlag
    ) {
        this.parent = parent;
        this.specClass = parent.specClass;
        this.description = description;
        this.beforeAllBlocks = unmodifiableList(beforeAllBlocks);
        this.beforeEachBlocks = unmodifiableList(beforeEachBlocks);
        this.itBlockDefinitions = unmodifiableMap(itBlockDefinitions);
        this.executionFlag = executionFlag;
    }

    DescribeBlock addDescribeBlock(
        String description,
        List<UnsafeBlock> beforeAllBlocks,
        List<UnsafeBlock> beforeEachBlocks,
        Map<String, ItBlockDefinition> itBlocks
    ) {
        DescribeBlock describeBlock = new DescribeBlock(this, description, beforeAllBlocks, beforeEachBlocks, itBlocks, DEFAULT);
        describeBlocks.add(describeBlock);
        return describeBlock;
    }

    DescribeBlock addIgnoredDescribeBlock(
        String description,
        List<UnsafeBlock> beforeAllBlocks,
        List<UnsafeBlock> beforeEachBlocks,
        Map<String, ItBlockDefinition> itBlocks
    ) {
        DescribeBlock describeBlock = new DescribeBlock(this, description, beforeAllBlocks, beforeEachBlocks, itBlocks, IGNORED);
        describeBlocks.add(describeBlock);
        return describeBlock;
    }

    DescribeBlock addFocusedDescribeBlock(
        String description,
        List<UnsafeBlock> beforeAllBlocks,
        List<UnsafeBlock> beforeEachBlocks,
        Map<String, ItBlockDefinition> itBlocks
    ) {
        DescribeBlock describeBlock = new DescribeBlock(this, description, beforeAllBlocks, beforeEachBlocks, itBlocks, FOCUSED);
        describeBlocks.add(describeBlock);
        return describeBlock;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        toString(sb, "");
        return sb.toString();
    }

    private void toString(StringBuilder sb, String indentation) {
        sb.append(indentation).append(description);

        for (Map.Entry<String, ItBlockDefinition> blocks : itBlockDefinitions.entrySet()) {
            sb.append(LS).append(indentation).append("  ").append(blocks.getKey());
        }

        for (DescribeBlock describeBlock : describeBlocks) {
            sb.append(LS);
            describeBlock.toString(sb, indentation + "  ");
        }
    }

    /**
     * @return spec class where all blocks were defined
     * @since 2.0.0
     */
    public Class<?> specClass() {
        return specClass;
    }

    /**
     * Flattens all "it" blocks into a list ready to be executed.
     * @return {@link ItBlock} list
     * @since 2.0.0
     */
    public List<ItBlock> flattenItBlocks() {
        LinkedList<ItBlock> blocksCollector = new LinkedList<>();
        collectItBlocks(blocksCollector, new LinkedList<>(), shouldBeIgnoredPredicate());
        return blocksCollector;
    }

    private ShouldBeIgnoredPredicate shouldBeIgnoredPredicate() {
        if (thereIsAtLeastOneFocusedBlock()) {
            return (p, b) -> !b.focused() && !p.containerFocused();
        }
        return (p, b) -> b.ignored() || p.containerIgnored();
    }

    private boolean thereIsAtLeastOneFocusedBlock() {
        return itBlockDefinitions.values().stream().anyMatch(ItBlockDefinition::focused)
            || describeBlocks.stream().anyMatch(b -> b.focused() || b.thereIsAtLeastOneFocusedBlock());
    }

    String description() {
        return description;
    }

    List<DescribeBlock> describeBlocks() {
        return new LinkedList<>(describeBlocks);
    }

    List<UnsafeBlock> beforeAllBlocks() {
        return beforeAllBlocks;
    }

    List<UnsafeBlock> beforeEachBlocks() {
        return beforeEachBlocks;
    }

    ItBlockDefinition itBlock(String itBlockDescription) {
        return itBlockDefinitions.get(itBlockDescription);
    }

    boolean ignored() {
        return IGNORED.equals(executionFlag);
    }

    boolean focused() {
        return FOCUSED.equals(executionFlag);
    }

    private void collectItBlocks(
        List<ItBlock> blocksCollector,
        List<BeforeBlock> parentBeforeAllBlocks,
        ShouldBeIgnoredPredicate shouldBeIgnored
    ) {
        this.beforeAllBlocks.stream().map(BeforeBlock::newBeforeAllBlock).forEach(parentBeforeAllBlocks::add);

        List<BeforeBlock> beforeBlocks = new LinkedList<>();
        beforeBlocks.addAll(parentBeforeAllBlocks);
        beforeBlocks.addAll(collectBeforeEachBlocks());

        for (ItBlockDefinition itBlock : this.itBlockDefinitions.values()) {
            if (shouldBeIgnored.test(this, itBlock)) {
                blocksCollector.add(newIgnoredItBlock(allContainerDescriptions(), itBlock.description()));
            } else {
                blocksCollector.add(newItBlock(allContainerDescriptions(), itBlock.description(), beforeBlocks, itBlock.block(), itBlock.expected()));
            }
        }

        for (DescribeBlock describeBlock : this.describeBlocks) {
            describeBlock.collectItBlocks(blocksCollector, parentBeforeAllBlocks, shouldBeIgnored);
        }
    }

    private boolean containerIgnored() {
        if (isRoot()) {
            return ignored();
        }
        return ignored() || parent.containerIgnored();
    }

    private boolean containerFocused() {
        if (isRoot()) {
            return focused();
        }
        return focused() || parent.containerFocused();
    }

    private List<String> allContainerDescriptions() {
        List<String> containerDescriptions;

        if (isRoot()) {
            containerDescriptions = new LinkedList<>();
        } else {
            containerDescriptions = parent.allContainerDescriptions();
        }

        containerDescriptions.add(description);
        return containerDescriptions;
    }

    private List<BeforeBlock> collectBeforeEachBlocks() {
        List<BeforeBlock> beforeEachBlocks;

        if (isRoot()) {
            beforeEachBlocks = new LinkedList<>();
        } else {
            beforeEachBlocks = parent.collectBeforeEachBlocks();
        }

        this.beforeEachBlocks.stream().map(BeforeBlock::newBeforeEachBlock).forEach(beforeEachBlocks::add);

        return beforeEachBlocks;
    }

    private boolean isRoot() {
        return parent == null;
    }
}
