package j8spec;

import j8spec.annotation.DefinedOrder;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static j8spec.J8Spec.*;
import static j8spec.UnsafeBlock.NOOP;
import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class J8SpecTest {

    static class EmptySpec {}

    static class BadSpec {
        private BadSpec() {}
    }

    static class ExampleOverwrittenSpec {{
        it("some text", UnsafeBlock.NOOP);
        it("some text", UnsafeBlock.NOOP);
    }}

    static class ExampleGroupOverwrittenSpec {{
        describe("some text", SafeBlock.NOOP);
        describe("some text", SafeBlock.NOOP);
    }}

    static class ContextBlockOverwrittenSpec {{
        context("some text", SafeBlock.NOOP);
        context("some text", SafeBlock.NOOP);
    }}

    static class ExampleWithCollectorOverwrittenSpec {{
        it("some text", c -> c, UnsafeBlock.NOOP);
        it("some text", UnsafeBlock.NOOP);
    }}

    static class ThreadThatSleeps2sSpec {{
        describe("forces thread to sleep", () -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            it("block", UnsafeBlock.NOOP);
        });
    }}

    static class ExpectedExceptionSpec {{
        it("block 1", c -> c.expected(Exception.class), UnsafeBlock.NOOP);
    }}

    @DefinedOrder
    static class SampleSpec {{
        it("block 1", UnsafeBlock.NOOP);
        it("block 2", UnsafeBlock.NOOP);

        describe("describe A", () -> {
            it("block A.1", UnsafeBlock.NOOP);
            it("block A.2", UnsafeBlock.NOOP);

            describe("describe A.A", () -> {
                it("block A.A.1", UnsafeBlock.NOOP);
                it("block A.A.2", UnsafeBlock.NOOP);
            });
        });

        context("context B", () -> {
            it("block B.1", UnsafeBlock.NOOP);
        });
    }}

    @Test
    public void reads_an_empty_spec() {
        assertThat(read(EmptySpec.class), is(emptyList()));
    }

    @Test
    public void composes_the_example_description_using_their_containers() {
        List<Example> examples = read(SampleSpec.class);

        assertThat(examples.get(0).containerDescriptions(), hasItems("j8spec.J8SpecTest$SampleSpec"));
        assertThat(examples.get(2).containerDescriptions(), hasItems("j8spec.J8SpecTest$SampleSpec", "describe A"));
        assertThat(examples.get(4).containerDescriptions(), hasItems("j8spec.J8SpecTest$SampleSpec", "describe A", "describe A.A"));
        assertThat(examples.get(6).containerDescriptions(), hasItems("j8spec.J8SpecTest$SampleSpec", "context B"));
    }

    @Test
    public void builds_an_example_using_excepted_exception_from_the_spec_definition() {
        List<Example> examples = read(ExpectedExceptionSpec.class);

        assertThat(examples.get(0).expected(), is(equalTo(Exception.class)));
    }

    @Test(expected = Exceptions.SpecInitializationFailed.class)
    public void throws_exception_when_fails_to_evaluate_spec() {
        read(BadSpec.class);
    }

    @Test(expected = Exceptions.IllegalContext.class)
    public void does_not_allow_describe_method_direct_invocation() {
        describe("some text", SafeBlock.NOOP);
    }

    @Test(expected = Exceptions.BlockAlreadyDefined.class)
    public void does_not_allow_an_example_group_to_be_replaced() {
        read(ExampleGroupOverwrittenSpec.class);
    }

    @Test(expected = Exceptions.IllegalContext.class)
    public void does_not_allow_before_all_method_direct_invocation() {
        beforeAll(NOOP);
    }

    @Test(expected = Exceptions.IllegalContext.class)
    public void does_not_allow_before_each_method_direct_invocation() {
        beforeEach(NOOP);
    }

    @Test(expected = Exceptions.IllegalContext.class)
    public void does_not_allow_after_all_method_direct_invocation() {
        afterAll(NOOP);
    }

    @Test(expected = Exceptions.IllegalContext.class)
    public void does_not_allow_after_each_method_direct_invocation() {
        afterEach(NOOP);
    }

    @Test(expected = Exceptions.IllegalContext.class)
    public void does_not_allow_context_method_direct_invocation() {
        context("some text", SafeBlock.NOOP);
    }

    @Test(expected = Exceptions.BlockAlreadyDefined.class)
    public void does_not_allow_context_block_to_be_replaced() {
        read(ContextBlockOverwrittenSpec.class);
    }

    @Test(expected = Exceptions.IllegalContext.class)
    public void does_not_allow_it_method_direct_invocation() {
        it("some text", UnsafeBlock.NOOP);
    }

    @Test(expected = Exceptions.IllegalContext.class)
    public void does_not_allow_it_method_direct_invocation_with_collector() {
        it("some text", c -> c, UnsafeBlock.NOOP);
    }

    @Test(expected = Exceptions.BlockAlreadyDefined.class)
    public void does_not_allow_an_example_to_be_replaced() {
        read(ExampleOverwrittenSpec.class);
    }

    @Test(expected = Exceptions.BlockAlreadyDefined.class)
    public void does_not_allow_an_example_with_collector_to_be_replaced() {
        read(ExampleWithCollectorOverwrittenSpec.class);
    }

    @Test(expected = Exceptions.IllegalContext.class)
    public void forgets_last_spec() {
        read(SampleSpec.class);
        describe("some text", SafeBlock.NOOP);
    }

    @Test(expected = Exceptions.IllegalContext.class)
    public void forgets_last_spec_after_the_last_spec_evaluation_fails() {
        try {
            read(ExampleOverwrittenSpec.class);
        } catch (Exceptions.BlockAlreadyDefined e) {
        }

        it("some text", UnsafeBlock.NOOP);
    }

    @Test()
    public void allows_multiple_threads_to_build_examples() throws InterruptedException {
        final Var<List<Example>> sleepExamples = var();

        Thread anotherSpecThread = new Thread(() -> {
            var(sleepExamples, read(ThreadThatSleeps2sSpec.class));
        });
        anotherSpecThread.start();

        Thread.sleep(1000);

        List<Example> emptyExampleList = read(EmptySpec.class);

        anotherSpecThread.join();

        assertThat(emptyExampleList, is(Collections.<Example>emptyList()));

        assertThat(var(sleepExamples).size(), is(1));
        assertThat(var(sleepExamples).get(0).description(), is("block"));
    }
}
