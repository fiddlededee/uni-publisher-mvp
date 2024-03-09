@file:DependsOn("ru.fiddlededee:uni-publisher:0.6")
@file:DependsOn("org.asciidoctor:asciidoctorj:2.5.11")
@file:DependsOn("com.helger:ph-css:7.0.1")

import com.helger.css.ECSSVersion
import com.helger.css.reader.CSSReader
import converter.FodtConverter
import model.OpenBlock
import model.Paragraph
import model.Span
import org.asciidoctor.Asciidoctor
import org.asciidoctor.Options
import org.asciidoctor.SafeMode
import writer.OdtStyle
import writer.OdtStyleList
import writer.textProperties
import java.io.File
import java.nio.charset.StandardCharsets

object AsciidocHtmlFactory {
    private val factory: Asciidoctor = Asciidoctor.Factory.create()
    fun getHtmlFromFile(file: File): String =
        factory.convertFile(
            file, Options.builder().backend("html5").sourcemap(true)
                .safe(SafeMode.UNSAFE).toFile(false).standalone(true).build()
        )
}

fun String.toFile(name: String): String {
    File(name).writeText(this)
    return this
}

val css = CSSReader.readFromFile(
    File("${__FILE__.parent}/syntax.css"), StandardCharsets.UTF_8, ECSSVersion.CSS30
)

val rougeStyles = css?.allStyleRules?.flatMap { styleRule ->
    styleRule.allSelectors.flatMap { selector ->
        styleRule.allDeclarations.map { declaration ->
            selector.allMembers.map { it.asCSSString } to
                    (declaration.property to declaration.expressionAsCSSString)
        }
    }
}?.filter {
    it.first.size == 3 && it.first[0] == ".highlight" &&
            it.first[2][0] == "."[0] && it.first[2].length <= 3 &&
            arrayOf("color", "background-color", "font-weight", "font-style")
                .contains(it.second.first)
}?.map { it.first[2].substring(1) to it.second }
    ?.groupBy { it.first }
    ?.map { it.key to it.value.associate { pair -> pair.second.first to pair.second.second } }
    ?.toMap() ?: mapOf()

val odtStyle = OdtStyleList(
    OdtStyle { node ->
        val condition = (node is Span) &&
                (node.ancestor { it is Paragraph && it.sourceTagName == "pre" }.isNotEmpty())
        if (!condition) return@OdtStyle
        rougeStyles.filter { node.roles.contains(it.key) }.forEach { style ->
            arrayOf("color", "background-color", "font-weight", "font-style").forEach {
                style.value[it]
                    ?.let { value -> textProperties { attribute("fo:$it", value) } }
            }
        }
    }
)

FodtConverter {
    html = AsciidocHtmlFactory.getHtmlFromFile(File("doc/pages/mvp.adoc"))
    odtStyleList = odtStyle
    template = File("${__FILE__.parent}/template.fodt").readText()
    parse()
    // tag::id-div-to-paragraph[]
    ast().descendant { it is OpenBlock && it.roles.contains("paragraph") }.forEach {
        it.children()[0].id = it.id
        it.id = null
    }
    // end::id-div-to-paragraph[]
    ast2fodt()
    fodt().toFile("${__FILE__.parent}/output/mvp-doc.fodt")
}

"Finished"