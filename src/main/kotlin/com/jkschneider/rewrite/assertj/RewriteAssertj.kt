package com.jkschneider.rewrite.assertj

import com.netflix.rewrite.ast.*
import com.netflix.rewrite.ast.visitor.AstVisitor
import com.netflix.rewrite.refactor.RefactorVisitor

object RewriteAssertj {
    @JvmStatic
    fun refactor(cu: Tr.CompilationUnit, path: String): String? {
        val refactor = cu.refactor()
        for (classDecl in cu.classes) {
            for (field in classDecl.findFields("org.junit.rules.ExpectedException")) {
                refactor.deleteField(field)

                for (methodDecl in classDecl.methods()) {
                    if (!methodDecl.findAnnotations("@org.junit.Test").isEmpty()) {
                        methodDecl.body?.accept(object : AstVisitor<ExpectedExceptionParams>(ExpectedExceptionParams()) {
                            override fun visitMethodInvocation(meth: Tr.MethodInvocation): ExpectedExceptionParams {
                                when (meth.name.simpleName) {
                                    "expect" -> {
                                        refactor.run(methodDecl.body!!, DeleteStatement(meth))
                                        return ExpectedExceptionParams(meth.args.args.first(), null, meth)
                                    }
                                    "expectMessage" -> {
                                        refactor.run(methodDecl.body!!, DeleteStatement(meth))
                                        return ExpectedExceptionParams(null, meth.args.args.first(), meth)
                                    }
                                }
                                return super.visitMethodInvocation(meth)
                            }

                            override fun reduce(r1: ExpectedExceptionParams, r2: ExpectedExceptionParams): ExpectedExceptionParams {
                                var reduced = r1
                                if (r2.message != null)
                                    reduced = reduced.copy(message = r2.message)
                                if (r2.type != null)
                                    reduced = reduced.copy(type = r2.type)
                                if (r2.lastExpectStatement != null)
                                    reduced = reduced.copy(lastExpectStatement = r2.lastExpectStatement)
                                return reduced
                            }
                        })?.let { params ->
                            if (params.message != null || params.type != null) {
                                refactor.run(methodDecl.body!!, WrapWithExceptionAssertion(params))
                                refactor.addImport("org.assertj.core.api.AssertionsForClassTypes", "assertThatExceptionOfType")
                            }
                        }
                    }
                }
            }
        }

//        return refactor.diff().replace("([ab])/.*/\\w+\\.java".toRegex(), "\$1/$path");
        return refactor.fix().printTrimmed()
    }

    private data class ExpectedExceptionParams(val type: Expression? = null, val message: Expression? = null,
                                               val lastExpectStatement: Expression? = null)

    private class DeleteStatement(val statement: Statement,
                                  override val ruleName: String = "delete-statement") : RefactorVisitor<Tr.Block<Tree>>() {
        override fun visitBlock(block: Tr.Block<Tree>): List<AstTransform<Tr.Block<Tree>>> =
                transform { copy(statements = block.statements - statement) }
    }

    private class WrapWithExceptionAssertion(val params: ExpectedExceptionParams,
                                             override val ruleName: String = "assertj-exception-assertion") : RefactorVisitor<Tr.Block<Tree>>() {
        override fun visitBlock(block: Tr.Block<Tree>): List<AstTransform<Tr.Block<Tree>>> {
            return transform {
                val assertThatExceptionOfType = call(null, "assertThatExceptionOfType",
                        listOf(params.type!!))

                val isThrownBy = call(assertThatExceptionOfType, "isThrownBy",
                        listOf(Tr.Lambda(Tr.Lambda.Parameters(true, emptyList()),
                                Tr.Lambda.Arrow(), Tr.Block(null, block.statements, Formatting.Infer, ""),
                                null)))

                val withMessageContaining = call(isThrownBy, "withMessageContaining",
                        listOf(params.message!!))

                copy(statements = listOf(withMessageContaining))
            }
        }

        fun call(select: Expression?, methodName: String, args: List<Expression> = emptyList()) =
                Tr.MethodInvocation(select, null, Tr.Ident.build(methodName),
                        Tr.MethodInvocation.Arguments(args),
                        Type.Method.build(Type.Class.build("doesnotmatter"), methodName, null,
                                null, emptyList(), emptyList()),
                        Formatting.Infer)
    }
}
