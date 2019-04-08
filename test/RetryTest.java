package xxx;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import xxx.Retry.ThrowingSupplier;
import xxx.exception.RetryException;
import xxx.orgenum.RetryKey;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;

public class RetryTest {

  @Rule public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

  @InjectMocks private Retry<Object> retry;
  private ChunkContext chunkContext;

  @Mock private ThrowingSupplier<Object> throwingSupplier = () -> null;

  /** setup. */
  @Before
  public void before() {
    final String jobName = "jobName";
    final JobExecution jobExcution =
        new JobExecution(new JobInstance(123L, jobName), new JobParameters());

    chunkContext = new ChunkContext(new StepContext(new StepExecution("step", jobExcution)));
  }

  /**
   * 正常.
   *
   * @throws Exception 例外.
   */
  @Test
  public void execute() throws Exception {
    final int retryCount = 3;

    boolean actualExeptionCalled = false;
    try {
      // 処理実行.
      retry.execute(
          throwingSupplier, RetryKey.HOGE_KEY, retryCount, 0, chunkContext);
    } catch (Exception e) {
      actualExeptionCalled = true;
    }

    // アサーション 実行回数 リトライされていないこと.
    verify(throwingSupplier, times(1)).get();
    // アサーション リトライ回数を超過していないので、例外がthrowされていないこと.
    assertThat(actualExeptionCalled).isFalse();
  }

  /**
   * 例外によるリトライ.
   *
   * @throws Exception 例外.
   */
  @Test
  public void execute_retry() throws Exception {
    final int retryCount = 3;

    // スタブ 例外発生.
    doThrow(new RetryException("message")).when(throwingSupplier).get();

    boolean actualExeptionCalled = false;
    try {
      // 処理実行.
      retry.execute(
          throwingSupplier, RetryKey.HOGE_KEY, retryCount, 0, chunkContext);
    } catch (Exception e) {
      actualExeptionCalled = true;
    }

    // アサーション 実行回数 リトライされていること.
    verify(throwingSupplier, times(4)).get();
    // アサーション 例外がthrowされていること.
    assertThat(actualExeptionCalled).isTrue();
  }

  /**
   * 検査外の例外発生.
   *
   * @throws Exception 例外.
   */
  @Test
  public void execute_retry_unexpectedException() throws Exception {
    final int retryCount = 3;

    // スタブ 例外発生.
    doThrow(new Exception()).when(throwingSupplier).get();

    boolean actualExeptionCalled = false;
    try {
      // 処理実行.
      retry.execute(
          throwingSupplier, RetryKey.HOGE_KEY, retryCount, 0, chunkContext);
    } catch (Exception e) {
      actualExeptionCalled = true;
    }

    // アサーション 実行回数 リトライされていないこと.
    verify(throwingSupplier, times(1)).get();
    // アサーション 例外がthrowされていること.
    assertThat(actualExeptionCalled).isTrue();
  }
}

