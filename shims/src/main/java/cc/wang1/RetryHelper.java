package cc.wang1;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class RetryHelper<T> {

    /**
     * 重试条件
     */
    private final Predicate<T> retryOnCondition;

    /**
     * 异常重试条件
     */
    private final Set<Class<? extends Throwable>> retryOnException;

    /**
     * 重试回调
     */
    private final LinkedList<Consumer<T>> retryListeners;

    /**
     * 重试停顿策略
     */
    private final Consumer<T> blockStrategy;

    /**
     * 最大重试次数
     */
    private int maxRetryCallLimit = 0;

    private RetryHelper(Predicate<T> retryOnCondition,
                        Set<Class<? extends Throwable>> retryOnException,
                        LinkedList<Consumer<T>> retryListeners,
                        Consumer<T> blockStrategy,
                        int maxRetryCallLimit) {

        this.retryOnCondition = retryOnCondition;
        this.retryOnException = retryOnException;
        this.retryListeners = retryListeners;
        this.blockStrategy = blockStrategy;
        this.maxRetryCallLimit = maxRetryCallLimit;
    }

    public T call(Callable<T> task) {
        if (task == null) {
            throw new RuntimeException("The retry task is required.");
        }
        if (blockStrategy == null) {
            throw new RuntimeException("The block strategy is required.");
        }

        T result = null;

        for (int i=0; i <= maxRetryCallLimit; ++i) {
            try {
                result = task.call();

                if (!retryOnCondition.test(result)) {
                    return result;
                }
            }catch (Exception e) {
                if (!retryOnException.contains(e.getClass())) {
                    throw new RuntimeException(e);
                }
            }

            if (retryListeners != null && !retryListeners.isEmpty()) {
                for (Consumer<T> retryListener : retryListeners) {
                    retryListener.accept(result);
                }
            }

            blockStrategy.accept(result);
        }

        return result;
    }

    public static class RetryHelperBuilder<T> {
        /**
         * 重试条件
         */
        private Predicate<T> retryOnCondition = r -> false;

        /**
         * 异常重试条件
         */
        private final Set<Class<? extends Throwable>> retryOnException = new HashSet<>();

        /**
         * 重试回调
         */
        private final LinkedList<Consumer<T>> retryListeners = new LinkedList<>();

        /**
         * 重试停顿策略
         */
        private Consumer<T> blockStrategy = r -> { try { Thread.sleep(1000); } catch (InterruptedException ignored) {}};

        /**
         * 最大重试次数
         */
        private int maxRetryCallLimit = 0;

        public static <T> RetryHelperBuilder<T> newBuilder(Class<T> resultType) {
            return new RetryHelperBuilder<>();
        }

        public RetryHelper<T> build() {
            return new RetryHelper<>(retryOnCondition, retryOnException, retryListeners, blockStrategy, maxRetryCallLimit);
        }

        public RetryHelperBuilder<T> retryWithListener(Consumer<T> listener) {
            if (listener == null) {
                return this;
            }
            this.retryListeners.add(listener);
            return this;
        }

        public RetryHelperBuilder<T> retryWithBlockStrategy(Consumer<T> blockStrategy) {
            if (blockStrategy == null) {
                throw new RuntimeException("The blockStrategy is required.");
            }
            this.blockStrategy = blockStrategy;
            return this;
        }

        public RetryHelperBuilder<T> retryIfException(Class<? extends Throwable> exception) {
            if (exception == null) {
                return this;
            }
            this.retryOnException.add(exception);
            return this;
        }

        public RetryHelperBuilder<T> retryIfCondition(Predicate<T> predicate) {
            if (predicate == null) {
                return this;
            }
            this.retryOnCondition = this.retryOnCondition == null ? predicate : this.retryOnCondition.or(predicate);
            return this;
        }

        public RetryHelperBuilder<T> maxRetryCallLimit(int count) {
            if (count < 0) {
                throw new RuntimeException(String.format("illegal config [%s] on maxRetryLimit(count >= 0).", count));
            }
            this.maxRetryCallLimit = count;
            return this;
        }
    }
}
