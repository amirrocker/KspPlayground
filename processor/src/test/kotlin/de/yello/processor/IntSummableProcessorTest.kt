package de.yello.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IntSummableProcessorTest {

    @Rule
    @JvmField
    var temporaryFolder: TemporaryFolder = TemporaryFolder()

    @Test
    internal fun `target is not a data class`() {
        val kotlinSource = SourceFile.kotlin(
            "file1.kt",
            """
                package de.yello.consumer
                
                import de.yello.processor.annotation.IntSummable
                
                @IntSummable
                class FooSummable(
                    val bar: Int = 212,
                    val foo: Int = 234 
                )
            """
        )

        val compilationResult = compile(kotlinSource)
        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, compilationResult.exitCode)
        val expectedMessage = "@IntSummable cannot target non-data classes de.yello.consumer.FooSummable"
        assertTrue("Expected $expectedMessage but was ${compilationResult.messages}") {
            compilationResult.messages.contains(expectedMessage)
        }
    }

    private fun compile(vararg source: SourceFile) = KotlinCompilation().apply {
        sources = source.toList()
        symbolProcessorProviders = listOf(IntSummableProcessorProvider())
        workingDir = temporaryFolder.root
        inheritClassPath = true
        verbose = true
    }.compile()
}
