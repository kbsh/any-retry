package xxx;

import java.util.Optional;
import xxx.exception.RetryException;
import xxx.orgenum.RetryKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.stereotype.Component;

@Component
public class Retry<T> {

  private static final Logger LOGGER = LoggerFactory.getLogger(Retry.class);

  // 最大リトライ回数.
  private int retryCount;
  // リトライ時スリープ秒数.
  private int delaySeconds;

  /**
   * 処理を実行する.
   *
   * <p>RetryException 発生時、指定回数リトライする.
   *
   * @param func 実行関数.
   * @param retryKey 実行キー.
   * @param retryCount リトライ回数.
   * @param delaySeconds リトライ時スリープ秒数.
   * @throws Exception 回数リトライ超過.
   */
  public T execute(
      ThrowingSupplier<T> func,
      RetryKey retryKey,
      int retryCount,
      int delaySeconds,
      ChunkContext chunkContext)
      throws Exception {
    try {
      return func.get();
    } catch (Exception e) {
      // RetryException 発生時は指定回数リトライする.
      if (e instanceof RetryException) {
        // 失敗.

        // 実行回数 singletonのためキーごとにcontextで保持.
        int execCount =
            (int)
                Optional.ofNullable(
                        chunkContext
                            .getStepContext()
                            .getJobExecutionContext()
                            .get(retryKey.getKey()))
                    .orElse(0);

        // 実行回数を加算.
        execCount++;
        // 実行回数を context にセット.
        chunkContext
            .getStepContext()
            .getStepExecution()
            .getJobExecution()
            .getExecutionContext()
            .put(retryKey.getKey(), execCount);

        // sleep.
        Thread.sleep(1000 * delaySeconds);

        // 最大実効回数超過.
        if (execCount >= retryCount + 1) {
          LOGGER.error("[" + chunkContext.getStepContext().getJobName() + "]" + "リトライ回数上限を超えました。");
          throw new Exception();
        } else {
          // リトライログ.
          LOGGER.error(
              String.format(
                  "[" + chunkContext.getStepContext().getJobName() + "]" + "({RetryCount: %d})",
                  execCount));

          // リトライ.
          return execute(func, retryKey, retryCount, delaySeconds, chunkContext);
        }
      } else {
        throw e;
      }
    }
  }

  @FunctionalInterface
  public interface ThrowingSupplier<T> {
    T get() throws Exception;
  }
}

