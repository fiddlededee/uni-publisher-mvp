#!/usr/bin/env kotlin
@file:DependsOn("ru.fiddlededee:uni-publisher:0.6")
@file:DependsOn("mfietz:jhyphenator:1.0")

import converter.FodtConverter
import de.mfietz.jhyphenator.HyphenationPattern
import de.mfietz.jhyphenator.Hyphenator
import fodt.*
import model.*
import org.w3c.dom.Element
import writer.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

println("INFO: Started: " + SimpleDateFormat("dd/M/yyyy hh:mm:ss SSS").format(Date()))

// Custom node with builder introduced
// tag::custom-node[]
class PageRef(var href: String) : NoWriterNode() {
    override val isInline: Boolean get() = true
}

fun Node.pageRef(href: String, init: PageRef.() -> Unit = {}): PageRef {
    return addChild(PageRef(href).apply(init))
}
// end::custom-node[]

val fodtConverter = FodtConverter { template = File("${__FILE__.parent}/template.fodt").readText() }

val pagesIds =
    arrayOf("old:ps:118:start", *IntRange(0, 176).map { "old:ps:118:" + "00${it}".takeLast(3) }.toTypedArray())

data class InterpretationsReference(
    val part1: String, val part2: String, val part3: String, val part4: String, val part5: String?
)

fun interpretationsReference(href: String): InterpretationsReference? {
    val regex = """^[\/]?(old|new):([0-9a-z]+):([0-9]+):(start|[0-9]*)(?:#([a-z_0-9]+))?$""".toRegex()
    val matchResult = regex.matchEntire(href)
    val part1 = matchResult?.groupValues?.get(1)
    val part2 = matchResult?.groupValues?.get(2)
    val part3 = matchResult?.groupValues?.get(3)
    val part4 = matchResult?.groupValues?.get(4)
    val part5 = matchResult?.groupValues?.get(5)
    return if (part1 != null && part2 != null && part3 != null && part4 != null) {
        InterpretationsReference(
            part1, part2, part3, part4, if (part5 == "") null else part5
        )
    } else null
}

// files parsed
val parsedFiles = pagesIds //.take(2) //.subList(2, 3)
    .associateWith { pageId ->
        val fileName = "${__FILE__.parent}/source-html/${pageId.replace(":", "-")}.html"
        fodtConverter.apply {
            html = File(fileName).readText()
            xpath = "//div[@class='page']"
            observedClassList = setOf("plugin__pagenav")
            parse()
            ast().apply {
                descendant { it is ListItem }.map { it as ListItem }.forEach { it.wrapListItemContents() }
            }
        }.ast()
    }

// Dokuwiki contents tags removed
parsedFiles.forEach {
    it.value.descendant().forEach { node ->
        if (node.id == "dokuwiki__toc" || node.roles.contains("plugin__pagenav")) {
            node.removeSelf()
        }
    }
}

// Identifiers for headers set
parsedFiles.forEach { parsedFile ->
    parsedFile.value.descendant { it is Header }.map { it as Header }.forEach {
        val refAsKey = parsedFile.key.replace(":", "_")
        it.id = if (it.level == 1) refAsKey else "${refAsKey}_${it.id}"
    }
}

parsedFiles.forEach { parsedFile ->
    val pageId = parsedFile.key
    val ast = parsedFile.value
    if (pageId == "old:ps:118:start") {
        // Identifiers added to verses
        ast.descendant { it is Paragraph }.map { it as Paragraph }
            .forEachIndexed { index, paragraph -> paragraph.id = "old_ps_118_start_$index" }
    } else {
        // Level one headers updated: Page refs and header TOC added
        ast.descendant { it is Header && it.level == 1 }.map { it as Header }.forEach h1@{ headerLevel1 ->
            val verseNum = interpretationsReference(pageId)?.part4?.toInt() ?: 0
            headerLevel1.span { roles.add("page-ref"); +" ["; pageRef("old_ps_118_start_$verseNum"); +"]" }
            headerLevel1.addAfter(Paragraph()).apply {
                roles.add("sub-toc")
                ast.descendant { it is Header && it.level == 2 }.forEachIndexed h2@{ index, headerLevel2 ->
                    val headerLevel2Id = headerLevel2.id ?: return@h2
                    if (index != 0) +" — "
                    +headerLevel2.descendant { it is Text }.map { it as Text }.joinToString { it.text }
                    +" ["; pageRef(headerLevel2Id); +"]"
                }
            }
        }
    }
}


// All pages combined in one AST, nodes for title page added
val combinedAST = Document().apply {
    p { roles("title"); +"Псалтирь. Псалом 118" }
    p {
        roles("after-title")
        +"Исходный текст: "
        a("https://bible.optina.ru/old:ps:118:start") { +"https://bible.optina.ru/old:ps:118:start" }
    }
    p { roles("after-title"); +"(Толкования Священного Писания на bible.optina.ru)" }
    parsedFiles.forEach { this.addChild(it.value) }
}

// Inner links converted to page references
// tag::inner-links-to-page-reference[]
combinedAST.descendant { it is Anchor }.map { it as Anchor }.forEach { anchor ->
    val iR = interpretationsReference(anchor.href) ?: return@forEach
    if (iR.part1 == "old" && iR.part2 == "ps" && iR.part3 == "118") {
        val idPart = if (iR.part5 == null) "" else "_${iR.part5}"
        val href = "${iR.part1}_${iR.part2}_${iR.part3}_${iR.part4}${idPart}"
        anchor.addAfter(Span()).apply { anchor.children().forEach { this.addChild(it) } }
            .addAfter(Span().apply { roles.add("page-ref") })
            .apply { text(" ["); pageRef(href); text("]") }
        anchor.removeSelf()
    } else {
        val idPart = if (iR.part5 == null) "" else "#${iR.part5}"
        anchor.href =
            "https://bible.optina.ru/${iR.part1}:${iR.part2}:${iR.part3}:${iR.part4}$idPart"
    }
}
// end::inner-links-to-page-reference[]

// Broken inner links checked, if possible fixed
combinedAST.descendant { it is PageRef }.map { it as PageRef }.filter { pageRef ->
    val hasOneTarget = combinedAST.descendant { it.id == pageRef.href }.size == 1
    !hasOneTarget
}.forEach { pageRef ->
    val possibleFix = pageRef.href.replace("""_([0-9]{1,2})$""".toRegex()) { "_0${it.groupValues[1]}" }
    if (combinedAST.descendant { it.id == possibleFix }.size == 1) {
        println("WARNING: Fixed ${pageRef.href} -> $possibleFix ")
        pageRef.href = possibleFix
    } else {
        println("ERROR: No target for ${pageRef.href}")
    }
}

// Styles for A4 output document
val basicOdtStyleList = OdtStyleList(
    // tag::ref-page-writer[]
    CustomWriter {
        if (it !is PageRef) return@CustomWriter
        preOdNode.apply {
            "text:bookmark-ref" {
                attribute("text:reference-format", "page")
                attribute("text:ref-name", it.href)
                -"-"
            }
        }
    },
    // end::ref-page-writer[]
    OdtStyle {
        if (it !is Span) return@OdtStyle
        textProperties { if (it.roles.contains("page-ref")) attribute("fo:color", "#c9211e") }
    }, OdtStyle {
        val condition = it is Paragraph && it.parent() is ListItem
        if (!condition) return@OdtStyle
        paragraphProperties { attribute("style:contextual-spacing", "true") }
    }, OdtStyle {
        if (!it.roles.contains("title")) return@OdtStyle
        attribute("style:master-page-name", "First Page")
        paragraphProperties {
            attribute("fo:margin-top", "10mm"); attribute("fo:margin-bottom", "2mm")
            attribute("fo:text-align", "center"); attribute("fo:text-indent", "0mm")
        }
        textProperties { attribute("fo:font-size", "26pt") }
    }, OdtStyle {
        if (!it.roles.contains("after-title")) return@OdtStyle
        paragraphProperties {
            attribute("fo:text-align", "center"); attribute("fo:margin-bottom", "0mm")
            attribute("fo:text-indent", "0mm")
        }
        textProperties { attribute("fo:font-size", "16pt") }
    }, OdtStyle {
        if (!it.roles.contains("sub-toc")) return@OdtStyle
        paragraphProperties {
            attribute("fo:text-indent", "0mm"); attribute("fo:text-align", "center")
        }
    })

// A4 outputed
fodtConverter.apply {
    ast = combinedAST
    odtStyleList = basicOdtStyleList
    ast2fodt()
    File("${__FILE__.parent}/output/ps-118.fodt").writeText(fodt())
}

// Styles for electronic book added
val ebookOdtStyleList = OdtStyleList(OdtStyle {
    if (it !is Paragraph) return@OdtStyle
    paragraphProperties { attribute("fo:line-height", "100%") }
    textProperties { attribute("fo:font-size", "16pt") }
}, OdtStyle {
    if (it !is Header) return@OdtStyle
    if (it.level == 2) textProperties { attribute("fo:font-size", "14pt") }
    if (it.level == 1) textProperties { attribute("fo:font-size", "18pt") }
}, OdtStyle {
    if (!it.roles.contains("sub-toc")) return@OdtStyle
    textProperties { attribute("fo:font-size", "14pt") }
}).add(basicOdtStyleList)

// Hyphenations for electronic book added
var h : Hyphenator = Hyphenator.getInstance(HyphenationPattern.lookup("ru"))
combinedAST.descendant { it is Text }.map { it as Text }.forEach { textNode ->
    textNode.text =
        textNode.text.replace("""[а-яА-Я]?[а-я]+""".toRegex()) { h.hyphenate(it.value).joinToString("\u00AD") }
}

// Electronic book outputed
fodtConverter.apply {
    odtStyleList = ebookOdtStyleList
    ast2fodt()
    // Final file postprocessed, the better idea here would be to preprocess template
    // tag::post-processing[]
    fodtGenerator?.enrichedTemplate?.apply {
        xpath("//style:page-layout-properties").iterable().map { it as Element }.forEach { el ->
            arrayOf(
                "page-width" to "105mm", "page-height" to "148mm",
                "margin-top" to "2mm", "margin-right" to "2mm",
                "margin-bottom" to "3mm", "margin-left" to "2mm",
            ).forEach { el.setAttributeNS(foNS, it.first, it.second) }
        }
        xpath("//style:footer-style/style:header-footer-properties").iterable().map { it as Element }
            .forEach { element -> element.setAttributeNS(foNS, "margin-top", "2mm") }
    }
    // end::post-processing[]
    serializeFodt()
    File("${__FILE__.parent}/output/ps-118-ebook.fodt").writeText(fodt())
}

println("INFO: Finished: " + SimpleDateFormat("dd/M/yyyy hh:mm:ss SSS").format(Date()))

