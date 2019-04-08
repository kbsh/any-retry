package xxx.exception;

public class RetryException extends Exception {
  private static final long serialVersionUID = 1L;

  /**
   * コンストラクタ.
   *
   * @param message メッセージ.
   */
  public RetryException(String message) {
    super(message);
  }
}

