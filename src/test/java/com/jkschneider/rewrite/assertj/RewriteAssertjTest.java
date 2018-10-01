package com.jkschneider.rewrite.assertj;

import com.netflix.rewrite.ast.Tr;
import com.netflix.rewrite.parse.OracleJdkParser;
import org.junit.jupiter.api.Test;

class RewriteAssertjTest {

  @Test
  void findAndFixIssues() {
    Tr.CompilationUnit cu = new OracleJdkParser().parse("" +
      "import org.junit.Rule;\n" +
      "import org.junit.Test;\n" +
      "import org.junit.rules.ExpectedException;\n" +
      "\n" +
      "public class ExpectedExceptionTest {\n" +
      "    @Rule\n" +
      "    public ExpectedException thrown = ExpectedException.none();\n" +
      "\n" +
      "    @Test\n" +
      "    public void throwsIllegalArgument() {\n" +
      "      this.thrown.expect(IllegalArgumentException.class);\n" +
      "      this.thrown.expectMessage(\"illegal argument\");\n" +
      "\n" +
      "      throw new IllegalArgumentException(\"illegal argument\");\n" +
      "    }\n" +
      "}\n");

    System.out.println(RewriteAssertj.refactor(cu, "src/main/java/ExpectedException.java"));
  }
}
