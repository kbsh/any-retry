package xxx.orgenum;

public enum RetryKey {
  // any key1
  HOGE_KEY("HOGE_KEY"),
  // any key2
  FUGA_KEY("FUGA_KEY"),
  ;

  private String str;

  RetryKey(String str) {
    this.str = str;
  }

  public String getKey() {
    return str;
  }
}

