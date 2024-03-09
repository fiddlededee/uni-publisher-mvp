package writer

import model.*
import org.redundent.kotlin.xml.Node as XmlNode
import org.redundent.kotlin.xml.Namespace
import org.redundent.kotlin.xml.xml
import kotlin.math.roundToInt

open class OdWriter(
    preOdNode: XmlNode? = null,
    val odtStyleList: OdtStyleList
) : BackendWriter {
    fun XmlNode.process(node: Node, key: String? = null) {
        odtStyleList.applyStyle(node, this, key)
        node.children.forEach {
            val customProcessed = odtStyleList.applyCustomWriter(it, newInstance(this, odtStyleList))
            if (!customProcessed) it.write(newInstance(this, odtStyleList))
        }
    }

    val preOdNode = preOdNode ?: xml("root") {
        namespace(Namespace("text", "urn:oasis:names:tc:opendocument:xmlns:text:1.0"))
        namespace(Namespace("style", "urn:oasis:names:tc:opendocument:xmlns:style:1.0"))
        namespace(Namespace("fo", "urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0"))
        namespace(Namespace("xlink", "http://www.w3.org/1999/xlink"))
        namespace(Namespace("table", "urn:oasis:names:tc:opendocument:xmlns:table:1.0"))
    }

    open fun newInstance(xmlNode: XmlNode, odtStyleList: OdtStyleList): OdWriter {
        return OdWriter(xmlNode, odtStyleList)
    }

    override fun write(openBlock: OpenBlock) {
        preOdNode.apply {
            process(openBlock)
        }
    }

    override fun write(table: Table) {
        preOdNode.apply {
            "table:table" {
                table.cols.forEach {
                    "table:table-column" {
                        // TODO: be more careful, allow absolute units
                        // Open Document doesn't allow to mix rel and absolute units like html
                        // And we can't render styles to understand widths
                        // The approximate algorithm needed that ignores paddings
                        val relColumnWidth = it.width.value.roundToInt() * 100
                        tableProperties { attribute("style:rel-column-width", "$relColumnWidth*") }
                    }
                }
                process(table)
            }
        }
    }

    override fun write(tableRowGroup: TableRowGroup) {
        preOdNode.apply {
            if (tableRowGroup.type == TRG.head) {
                "table:table-header-rows" {
                    process(tableRowGroup)
                }
            } else process(tableRowGroup)
        }
    }

    override fun write(tr: TableRow) {
        preOdNode.apply {
            "table:table-row" {
                process(tr)
            }
        }
    }

    override fun write(td: TableCell) {
        preOdNode.apply {
            "table:table-cell" {
                process(td)
            }
        }
    }

    override fun write(colGroup: ColGroup) {}
    override fun write(col: Col) {}

    override fun write(ol: OrderedList) {
        preOdNode.apply {
            "text:list" {
                attribute("text:style-name", "Numbering 123")
                process(ol)
            }
        }
    }

    override fun write(ul: UnorderedList) {
        preOdNode.apply {
            "text:list" {
                attribute("text:style-name", "List 1")
                process(ul)
            }
        }
    }

    override fun write(li: ListItem) {
        preOdNode.apply {
            "text:list-item" {
                process(li)
            }
        }
    }

    override fun write(h: Header) {
        // todo -- to example
        preOdNode.apply {
            "text:h" {
                attribute("text:style-name", "Heading ${h.level}")
                val id = h.id
                if (id != null) {
                    "text:bookmark-start" {
                        attribute("text:name", id)
                    }
                    process(h)
                    "text:bookmark-end" {
                        attribute("text:name", id)
                    }
                } else process(h)
            }
        }
    }

    override fun write(p: Paragraph) {
        preOdNode.apply {
            "text:p" {
                when (p.sourceTagName) {
                    "pre" -> attribute("text:style-name", "Preformatted Text")
                    else -> attribute("text:style-name", "Text body")
                }
                val id = p.id
                if (id != null) {
                    "text:bookmark" { attribute("text:name", id) }
                }
                process(p)
            }
        }
    }

//    override fun write(pre: Pre) {
//        preOdNode.apply {
//            process(pre)
//        }
//    }
//
//    override fun write(code: Code) {
//        preOdNode.apply {
//            process(code)
//        }
//    }

    override fun write(doc: Document) {
        preOdNode.apply {
            process(doc)
        }
    }

    override fun write(text: Text) {
        preOdNode.apply {
            // todo: bug in XML builder?, outputs nothing for space between tags
            var textChunk = ""
            text.text.map { it.toString() }.forEachIndexed { i, character ->
                if (character == " ") {
                    if (i != 0 && text.text[i - 1].toString() != " "
                        && i < text.text.length - 1 && text.text[i + 1].toString() != " "
                    ) {
                        textChunk += " "
                    } else {
                        -textChunk
                        textChunk = ""
                        "text:s" { attribute("text:c", "1") }
                    }
                } else if (character == "\n") {
                    -textChunk
                    textChunk = ""
                    "text:line-break" {}
                } else {
                    textChunk += character
                }
            }
            if (textChunk != "") -textChunk
        }
    }

    override fun write(span: Span) {
        preOdNode.apply {
            "text:span" {
                textProperties {
                    if (span.roles.contains("em") or span.roles.contains("i")) {
                        attribute("fo:font-style", "italic")
                    }
                    if (span.roles.contains("strong") or span.roles.contains("b")) {
                        attribute("fo:font-weight", "bold")
                    }
                }
                process(span)
            }
        }
    }

    override fun write(a: Anchor) {
        preOdNode.apply {
            "text:a" {
                attribute("xlink:href", a.href)
                attribute("text:style-name", "Internet Link")
                attribute("text:visited-style-name", "Visited Internet Link")
                process(a)
            }
        }
    }

    override fun write(dummyNode: Node) {
        println("Error: no writer available")
    }
}
