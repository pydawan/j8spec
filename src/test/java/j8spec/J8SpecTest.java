package j8spec;

import org.junit.Test;

import java.util.Collections;

import static j8spec.J8Spec.*;
import static j8spec.Var.var;
import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class J8SpecTest {

    private static final Runnable NOOP = () -> {};

    private static final Runnable BEFORE_ALL_BLOCK = () -> {};
    private static final Runnable BEFORE_EACH_BLOCK = () -> {};
    private static final Runnable IT_BLOCK_1 = () -> {};
    private static final Runnable IT_BLOCK_2 = () -> {};
    private static final Runnable IT_BLOCK_3 = () -> {};

    private static final Runnable BEFORE_ALL_A_BLOCK = () -> {};
    private static final Runnable BEFORE_EACH_A_BLOCK = () -> {};
    private static final Runnable IT_BLOCK_A1 = () -> {};
    private static final Runnable IT_BLOCK_A2 = () -> {};

    private static final Runnable BEFORE_ALL_AA_BLOCK = () -> {};
    private static final Runnable BEFORE_EACH_AA_BLOCK = () -> {};
    private static final Runnable IT_BLOCK_AA1 = () -> {};
    private static final Runnable IT_BLOCK_AA2 = () -> {};

    private static final Runnable BEFORE_ALL_B_BLOCK = () -> {};
    private static final Runnable BEFORE_EACH_B_BLOCK = () -> {};
    private static final Runnable IT_BLOCK_B1 = () -> {};
    private static final Runnable IT_BLOCK_B2 = () -> {};

    static class EmptySpec {}

    static class BadSpec {
        private BadSpec() {}
    }

    static class ItBlockOverwrittenSpec {{
        it("some text", NOOP);
        it("some text", NOOP);
    }}

    static class ItBlockWithCollectorOverwrittenSpec {{
        it("some text", c -> c, NOOP);
        it("some text", NOOP);
    }}

    static class ThreadThatSleeps2sSpec {{
        describe("forces thread to sleep", () -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            it("block", NOOP);
        });
    }}

    static class ExpectedExceptionSpec {{
        it("block 1", c -> c.expected(Exception.class), IT_BLOCK_1);
        xit("block 2", c -> c.expected(Exception.class), IT_BLOCK_2);
        fit("block 3", c -> c.expected(Exception.class), IT_BLOCK_3);
    }}

    static class SampleSpec {{
        beforeAll(BEFORE_ALL_BLOCK);
        beforeEach(BEFORE_EACH_BLOCK);

        it("block 1", IT_BLOCK_1);
        it("block 2", IT_BLOCK_2);
        xit("block 3", IT_BLOCK_3);

        describe("describe A", () -> {
            beforeAll(BEFORE_ALL_A_BLOCK);
            beforeEach(BEFORE_EACH_A_BLOCK);

            it("block A.1", IT_BLOCK_A1);
            it("block A.2", IT_BLOCK_A2);

            describe("describe A.A", () -> {
                beforeAll(BEFORE_ALL_AA_BLOCK);
                beforeEach(BEFORE_EACH_AA_BLOCK);

                it("block A.A.1", IT_BLOCK_AA1);
                it("block A.A.2", IT_BLOCK_AA2);
            });
        });

        describe("describe B", () -> {
            beforeAll(BEFORE_ALL_B_BLOCK);
            beforeEach(BEFORE_EACH_B_BLOCK);

            it("block B.1", IT_BLOCK_B1);
            it("block B.2", IT_BLOCK_B2);
        });
    }}

    @Test
    public void builds_a_describe_block_using_the_given_spec_as_description() {
        DescribeBlock describeBlock = read(EmptySpec.class);

        assertThat(describeBlock.specClass(), equalTo(EmptySpec.class));
        assertThat(describeBlock.description(), is("j8spec.J8SpecTest$EmptySpec"));
    }

    @Test
    public void builds_a_describe_block_based_on_an_empty_spec() {
        DescribeBlock describeBlock = read(EmptySpec.class);

        assertThat(describeBlock.flattenItBlocks(), is(emptyList()));
    }

    @Test
    public void builds_a_describe_block_where_all_inner_describe_blocks_have_the_same_spec_class() {
        DescribeBlock rootDescribeBlock = read(SampleSpec.class);

        DescribeBlock describeA = rootDescribeBlock.describeBlocks().get(0);
        assertThat(describeA.specClass(), equalTo(SampleSpec.class));

        DescribeBlock describeAA = describeA.describeBlocks().get(0);
        assertThat(describeAA.specClass(), equalTo(SampleSpec.class));

        DescribeBlock describeB = rootDescribeBlock.describeBlocks().get(1);
        assertThat(describeB.specClass(), equalTo(SampleSpec.class));
    }

    @Test
    public void builds_a_describe_block_using_the_description_from_the_spec_definition() {
        DescribeBlock rootDescribeBlock = read(SampleSpec.class);

        DescribeBlock describeA = rootDescribeBlock.describeBlocks().get(0);
        assertThat(describeA.description(), is("describe A"));

        DescribeBlock describeAA = describeA.describeBlocks().get(0);
        assertThat(describeAA.description(), is("describe A.A"));

        DescribeBlock describeB = rootDescribeBlock.describeBlocks().get(1);
        assertThat(describeB.description(), is("describe B"));
    }

    @Test
    public void builds_a_describe_block_using_it_blocks_from_the_spec_definition() {
        DescribeBlock rootDescribeBlock = read(SampleSpec.class);

        assertThat(rootDescribeBlock.itBlock("block 1").body(), is(IT_BLOCK_1));
        assertThat(rootDescribeBlock.itBlock("block 2").body(), is(IT_BLOCK_2));
        assertThat(rootDescribeBlock.itBlock("block 3").body(), is(IT_BLOCK_3));

        DescribeBlock describeA = rootDescribeBlock.describeBlocks().get(0);
        assertThat(describeA.itBlock("block A.1").body(), is(IT_BLOCK_A1));
        assertThat(describeA.itBlock("block A.2").body(), is(IT_BLOCK_A2));

        DescribeBlock describeAA = describeA.describeBlocks().get(0);
        assertThat(describeAA.itBlock("block A.A.1").body(), is(IT_BLOCK_AA1));
        assertThat(describeAA.itBlock("block A.A.2").body(), is(IT_BLOCK_AA2));

        DescribeBlock describeB = rootDescribeBlock.describeBlocks().get(1);
        assertThat(describeB.itBlock("block B.1").body(), is(IT_BLOCK_B1));
        assertThat(describeB.itBlock("block B.2").body(), is(IT_BLOCK_B2));
    }

    @Test
    public void builds_a_describe_block_using_excepted_exceptions_from_the_spec_definition() {
        DescribeBlock describeBlock = read(ExpectedExceptionSpec.class);

        assertThat(describeBlock.itBlock("block 1").expected(), is(equalTo(Exception.class)));
        assertThat(describeBlock.itBlock("block 2").expected(), is(equalTo(Exception.class)));
        assertThat(describeBlock.itBlock("block 3").expected(), is(equalTo(Exception.class)));
    }

    @Test(expected = SpecInitializationException.class)
    public void throws_exception_when_fails_to_evaluate_spec() {
        read(BadSpec.class);
    }

    @Test(expected = IllegalContextException.class)
    public void does_not_allow_describe_method_direct_invocation() {
        describe("some text", NOOP);
    }

    @Test(expected = IllegalContextException.class)
    public void does_not_allow_it_method_direct_invocation() {
        it("some text", NOOP);
    }

    @Test(expected = IllegalContextException.class)
    public void does_not_allow_it_method_direct_invocation_with_collector() {
        it("some text", c -> c, NOOP);
    }

    @Test(expected = BlockAlreadyDefinedException.class)
    public void does_not_allow_it_block_to_be_replaced() {
        read(ItBlockOverwrittenSpec.class);
    }

    @Test(expected = BlockAlreadyDefinedException.class)
    public void does_not_allow_it_block_with_collector_to_be_replaced() {
        read(ItBlockWithCollectorOverwrittenSpec.class);
    }

    @Test(expected = IllegalContextException.class)
    public void forgets_last_spec() {
        read(SampleSpec.class);
        describe("some text", NOOP);
    }

    @Test(expected = IllegalContextException.class)
    public void forgets_last_spec_after_the_last_spec_evaluation_fails() {
        try {
            read(ItBlockOverwrittenSpec.class);
        } catch (BlockAlreadyDefinedException e) {
        }

        it("some text", NOOP);
    }

    @Test()
    public void allows_multiple_threads_to_build_describe_blocks() throws InterruptedException {
        final Var<DescribeBlock> sleepDescribe = var();

        Thread anotherDescribeBlockThread = new Thread(() -> {
            var(sleepDescribe, read(ThreadThatSleeps2sSpec.class));
        });
        anotherDescribeBlockThread.start();

        Thread.sleep(1000);

        DescribeBlock emptyDescribeBlock = read(EmptySpec.class);

        anotherDescribeBlockThread.join();

        assertThat(emptyDescribeBlock.flattenItBlocks(), is(Collections.<ItBlock>emptyList()));

        assertThat(var(sleepDescribe).flattenItBlocks().size(), is(1));
        assertThat(var(sleepDescribe).flattenItBlocks().get(0).description(), is("block"));
    }
}
