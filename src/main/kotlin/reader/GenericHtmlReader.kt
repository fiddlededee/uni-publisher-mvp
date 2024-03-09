package reader

import model.*

open class GenericHtmlReader(
    node: Node, htmlNode: HtmlNode,
    private val observedClassList: Set<String> = setOf(),
) : HtmlReaderCommon(node, htmlNode) {

    fun parseNode(astNode: Node, confirmedHtmlNode: HtmlNode) {
        GenericHtmlReader(astNode, confirmedHtmlNode, observedClassList).iterateAll()
    }

    fun Node.setBasics(htmlNode: HtmlNode): Node {
        this.id = htmlNode.id()
        this.roles += htmlNode.classNames()
        return this
    }

    protected open val detectors = arrayOf(
        ::detectOpenBlock,
        ::detectTable,
        ::detectRowGroup,
        ::detectRow,
        ::detectColGroup,
        ::detectCol,
        ::detectCell,
        ::detectOl,
        ::detectUl,
        ::detectLi,
        ::detectH,
        ::detectP,
        ::detectPre,
        ::detectCode,
        ::passOtherDiv,
        ::passComment,
        ::passHr,
        ::detectSpan,
        ::detectA,
        ::detectText,
    )

    open fun iterateAll() {
        detectBy(*detectors)
    }

    open fun passComment() {
        detectByExpression({ it.isComment() }) {}
    }

    open fun passHr() {
        detectByExpression({ it.nodeName() == "hr" }) {}
    }

    open fun passOtherDiv() {
        detectByExpression({
            it.nodeName() == "div" && !(it.hasAttr("id") || it.classNames().intersect(observedClassList).isNotEmpty())
        }) { confirmedHtmlNode ->
            parseNode(this.node(), confirmedHtmlNode)
        }
    }

    open fun detectOpenBlock() {
        detectByExpression({
            it.nodeName() == "div" && (it.hasAttr("id") || it.classNames().intersect(observedClassList).isNotEmpty())
        }) { confirmedHtmlNode ->
            val newOpenBlock = OpenBlock().setBasics(confirmedHtmlNode)
//            newOpenBlock.roles += confirmedHtmlNode.classNames().intersect(observedClassList)
            parseNode(addToAST(newOpenBlock), confirmedHtmlNode)
        }
    }

    open fun detectTable() {
        detectByExpression({ it.nodeName() == "table" }) { confirmedNode ->
            parseNode(addToAST(Table()), confirmedNode)
        }
    }

    open fun detectRowGroup() {
        detectByExpression(
            { arrayOf("tbody", "thead", "tfoot").contains(it.nodeName()) }
        ) { confirmedNode ->
            val type = when (confirmedNode.nodeName()) {
                "thead" -> TRG.head
                "tfoot" -> TRG.foot
                else -> TRG.body
            }
            parseNode(addToAST(TableRowGroup(type).apply { roles(confirmedNode.nodeName()) }), confirmedNode)
        }
    }

    open fun detectRow() {
        detectByExpression({ it.nodeName() == "tr" }) { confirmedNode ->
            parseNode(addToAST(TableRow()), confirmedNode)
        }
    }

    open fun detectCell() {
        detectByExpression({ it.nodeName() == "td" }) { confirmedNode ->
            parseNode(addToAST(TableCell()), confirmedNode)
        }
    }

    open fun detectColGroup() {
        detectByExpression({ it.nodeName() == "colgroup" }) { confirmedNode ->
            parseNode(addToAST(ColGroup()), confirmedNode)
        }
    }

    open fun detectCol() {
        detectByExpression({ it.nodeName() == "col" }) { confirmedNode ->
            val width =
                if (confirmedNode.hasAttr("width")) {
                    val widthAttrValue = confirmedNode.attr("width")
                    """[0-9]""".toRegex().matchEntire(widthAttrValue)?.value?.toFloat() ?: 1F
                } else 1F
            parseNode(addToAST(Col(Width(width))), confirmedNode)
        }
    }

    open fun detectOl() {
        detectByExpression({ it.nodeName() == "ol" }) { confirmedNode ->
            parseNode(addToAST(OrderedList()), confirmedNode)
        }
    }

    open fun detectUl() {
        detectByExpression({ it.nodeName() == "ul" }) { confirmedNode ->
            parseNode(addToAST(UnorderedList()), confirmedNode)
        }
    }

    open fun detectLi() {
        detectByExpression({ it.nodeName() == "li" }) { confirmedNode ->
            parseNode(addToAST(ListItem()), confirmedNode)
        }
    }

    open fun detectH() {
        val regEx = "h([0-9])".toRegex()
        detectByExpression({ regEx.matches(it.nodeName()) }) { confirmedHtmlNode ->
            val level = regEx.matchEntire(confirmedHtmlNode.nodeName())?.groupValues?.get(1) ?: "0"
            val newHeader = Header(level = level.toInt()).setBasics(confirmedHtmlNode)
            parseNode(addToAST(newHeader), confirmedHtmlNode)
        }
    }

    open fun detectP() {
        detectByExpression({ it.nodeName() == "p" }) { confirmedNode ->
            parseNode(addToAST(Paragraph().setBasics(confirmedNode)), confirmedNode)
        }
    }

    open fun detectPre() {
        detectByExpression({ it.nodeName() == "pre" }) { confirmedNode ->
            parseNode(addToAST(Paragraph().apply { roles("pre"); sourceTagName = "pre" }), confirmedNode)
        }
    }

    open fun detectCode() {
        detectByExpression({ it.nodeName() == "code" }) { confirmedNode ->
            parseNode(addToAST(OpenBlock().apply { roles("code") }), confirmedNode)
        }
    }

    open fun detectSpan() {
        val spanTypes = "strong|em|i|span|br".split("|")
        detectByExpression({ spanTypes.contains(it.nodeName()) }) { confirmedHtmlNode ->
            val newSpan = Span().apply {
                sourceTagName = confirmedHtmlNode.nodeName()
                roles.add(confirmedHtmlNode.nodeName())
                setBasics(confirmedHtmlNode)
                if (confirmedHtmlNode.nodeName() == "br") +"\n"
            }
            parseNode(addToAST(newSpan), confirmedHtmlNode)
        }
    }

    open fun detectA() {
        detectByExpression({ it.nodeName() == "a" }) { confirmedHtmlNode ->
            val href = confirmedHtmlNode.attr("href")
            val newAnchor = Anchor(href)
            parseNode(addToAST(newAnchor), confirmedHtmlNode)
        }
    }

    open fun detectText() {
        detectByExpression({ htmlNode -> htmlNode.isText() }) { confirmedNode ->
            val text = Text(confirmedNode.nodeText() ?: "")
            if (text.text != "") {
                val newTextNode = Text(text.text)
                addToAST(newTextNode)
            }
        }
    }
}
