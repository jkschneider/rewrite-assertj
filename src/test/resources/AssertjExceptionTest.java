import java.io.IOException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

public class AssertjExceptionTest {
  public void assertjStyle() {
      assertThatExceptionOfType(IOException.class).isThrownBy(() -> { throw new IOException("boom!"); })
        .withMessage("%s!", "boom")
        .withMessageContaining("boom")
        .withNoCause();
  }
}
