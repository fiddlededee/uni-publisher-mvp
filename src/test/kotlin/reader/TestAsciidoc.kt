package reader

import model.Document
import model.Node
import model.Text
import org.approvaltests.Approvals
import org.asciidoctor.Asciidoctor
import org.asciidoctor.Options
import org.junit.jupiter.api.Test

class TestAsciidoc {
    @Test
    fun testSimpleAsciidoc() {
        """
            = Sample Asciidoc 
           
            == Heading 1
            
            [.unnecessary]
            This paragraph should not appear in result
            
            This paragraph should appear in result
            
            == Heading 2
            
            Some more paragraph
        """
            .trimIndent()
            .asciidocAsHtml()
            .apply {println(this)}
            .htmlMarkupAsHtmlNode()
            .selectAtXpath("/html/body")!!
            .parseWithBasic().apply {
                this.descendant { it.roles.contains("unnecessary") }
                    .forEach { it.removeSelf() }
            }
            .output()
            .apply { Approvals.verify(this) }
    }
}

object AsciidocHtmlFactory {
    private val factory: Asciidoctor = Asciidoctor.Factory.create()
    fun getHtml(string: String): String {
        return factory.convert(
            string, Options.builder().backend("html5")
                .sourcemap(true).build()
        )
    }
}

fun String.asciidocAsHtml(): String {
    return AsciidocHtmlFactory.getHtml(this)
}

fun HtmlNode.parseWithBasic(): Document {
    val document = Document()
    GenericHtmlReader(document, this, setOf("unnecessary")).apply { iterateAll() }
    return document
}


fun String.htmlMarkupAsHtmlNode(): HtmlNode {
    return HtmlNode(this)
}


fun Document.output(): String {
    val astAsText = StringBuilder()
    fun outputNode(node: Node, level: Int): String {
        if (node is Text) {
            astAsText.appendLine("${" ".repeat(level * 2)}text: [${node.text}]")
        } else {
            astAsText.appendLine(
                "${" ".repeat(level * 2)}${node::class.java.simpleName} id=${node.id}" +
                        " roles=${node.roles.joinToString(",")}"
            )
            node.children.forEach {
                outputNode(it, level + 1)
            }
        }
        return astAsText.toString()
    }
    outputNode(this, 0)
    return astAsText.toString()
}
