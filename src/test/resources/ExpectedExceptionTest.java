import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ExpectedExceptionTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void throwsIllegalArgument() {
    this.thrown.expect(IllegalArgumentException.class);
    this.thrown.expectMessage("illegal argument");

    throw new IllegalArgumentException("illegal argument");
  }
}
