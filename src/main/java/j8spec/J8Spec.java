package j8spec;

import java.util.function.Function;

import static java.util.function.Function.identity;

/**
 * @since 1.0.0
 */
public final class J8Spec {

    private static final ThreadLocal<Context<DescribeBlockDefinition>> currentThread = new ThreadLocal<>();

    /**
     * @since 1.0.0
     */
    public static synchronized void describe(String description, Runnable body) {
        isValidContext("describe");
        currentThread.get().current().describe(description, body);
    }

    /**
     * @since 1.1.0
     */
    public static synchronized void xdescribe(String description, Runnable body) {
        isValidContext("xdescribe");
        currentThread.get().current().xdescribe(description, body);
    }

    /**
     * @since 1.1.0
     */
    public static synchronized void fdescribe(String description, Runnable body) {
        isValidContext("fdescribe");
        currentThread.get().current().fdescribe(description, body);
    }

    /**
     * @since 1.1.0
     */
    public static synchronized void beforeAll(Runnable body) {
        isValidContext("beforeAll");
        currentThread.get().current().beforeAll(body);
    }

    /**
     * @since 1.0.0
     */
    public static synchronized void beforeEach(Runnable body) {
        isValidContext("beforeEach");
        currentThread.get().current().beforeEach(body);
    }

    /**
     * @since 1.0.0
     */
    public static synchronized void it(String description, Runnable body) {
        it(description, identity(), body);
    }

    /**
     * @since 1.1.0
     */
    public static synchronized void it(
        String description,
        Function<ItBlockDefinitionBuilder, ItBlockDefinitionBuilder> collector,
        Runnable body
    ) {
        isValidContext("it");
        ItBlockDefinition itBlockDefinition = collector.apply(new ItBlockDefinitionBuilder())
            .body(body)
            .newItBlockDefinition();
        currentThread.get().current().it(description, itBlockDefinition);
    }

    /**
     * @since 1.1.0
     */
    public static synchronized void xit(String description, Runnable body) {
        xit(description, identity(), body);
    }

    /**
     * @since 1.1.0
     */
    public static synchronized void xit(
        String description,
        Function<ItBlockDefinitionBuilder, ItBlockDefinitionBuilder> collector,
        Runnable body
    ) {
        isValidContext("xit");
        ItBlockDefinition itBlockDefinition = collector.apply(new ItBlockDefinitionBuilder())
            .body(body)
            .newIgnoredItBlockDefinition();
        currentThread.get().current().it(description, itBlockDefinition);
    }

    /**
     * @since 1.1.0
     */
    public static synchronized void fit(String description, Runnable body) {
        fit(description, identity(), body);
    }

    /**
     * @since 1.1.0
     */
    public static synchronized void fit(
        String description,
        Function<ItBlockDefinitionBuilder, ItBlockDefinitionBuilder> collector,
        Runnable body
    ) {
        isValidContext("fit");
        ItBlockDefinition itBlockDefinition = collector.apply(new ItBlockDefinitionBuilder())
            .body(body)
            .newFocusedItBlockDefinition();
        currentThread.get().current().it(description, itBlockDefinition);
    }

    private static void isValidContext(final String methodName) {
        if (currentThread.get() == null) {
            throw new IllegalContextException(
                "'" + methodName + "' should not be invoked from outside a spec definition."
            );
        }
    }

    /**
     * @since 1.0.0
     */
    public static synchronized DescribeBlock read(Class<?> specClass) {
        currentThread.set(new Context<>());
        try {
            DescribeBlockDefinition describeBlockDefinition = new DescribeBlockDefinition(specClass);
            describeBlockDefinition.evaluate(currentThread.get());
            return describeBlockDefinition.toDescribeBlock();
        } finally {
            currentThread.set(null);
        }
    }

    private J8Spec() {}
}
