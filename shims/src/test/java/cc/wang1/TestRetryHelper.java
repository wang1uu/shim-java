package cc.wang1;

public class TestRetryHelper {
    public static void main(String[] args) {
        RetryHelper<String> retryHelper = RetryHelper.RetryHelperBuilder.newBuilder(String.class)
                .retryWithListener(System.out::println)
                .build();

        retryHelper.call(() -> {
            System.out.println(1);
            return "1";
        });
    }
}
