package reader

import org.jsoup.Jsoup
import org.jsoup.nodes.Node as JsoupNode
import org.jsoup.nodes.Comment
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import org.jsoup.parser.Parser

class HtmlNode(private val jsoupNode: JsoupNode) {
    constructor(doc: String) : this(Jsoup.parse(doc))

    override fun toString(): String {
        return jsoupNode.toString()
    }

    fun id(): String? {
        return if (jsoupNode.hasAttr("id")) jsoupNode.attr("id") else null
    }

    fun isText(): Boolean {
        return jsoupNode is TextNode
    }

    fun isComment(): Boolean {
        return jsoupNode is Comment
    }

    fun nodeName(): String {
        return jsoupNode.nodeName()
    }

    fun attr(attributeKey: String): String {
        return jsoupNode.attr(attributeKey)
    }

    fun hasAttr(attributeKey: String): Boolean {
        return jsoupNode.hasAttr(attributeKey)
    }

    fun classNames(): MutableSet<String> {
        return if (jsoupNode is Element) {
            jsoupNode.classNames()
        } else {
            mutableSetOf()
        }
    }

    fun nodeNameOrText(): String {
        return if (jsoupNode is Element) jsoupNode.nodeName() else jsoupNode.toString()
    }

    fun nodeText(): String? {
        return if (jsoupNode is TextNode)
            Parser.unescapeEntities(jsoupNode.toString(), true) else null
    }

    fun selectAtXpath(xpath: String): HtmlNode? {
        return if (jsoupNode is Element && jsoupNode.selectXpath(xpath)[0] != null) HtmlNode(jsoupNode.selectXpath(xpath)[0])
        else null
    }

    fun firstChild(): HtmlNode? {
        val firstChild = jsoupNode.firstChild()
        return if (firstChild == null) null else HtmlNode(firstChild)
    }

    fun previousSibling(): HtmlNode? {
        val previousSibling = jsoupNode.previousSibling()
        return if (previousSibling == null) null else HtmlNode(previousSibling)
    }


    fun nextSibling(): HtmlNode? {
        val nextSibling = jsoupNode.nextSibling()
        return if (nextSibling == null) null else HtmlNode(nextSibling)
    }

    // If matched returns this node and the next node
    fun matchByExpression(
        expression: (htmlNode: HtmlNode) -> Boolean
    ): Pair<HtmlNode?, HtmlNode?> {
        val parentJsoupNode = if (jsoupNode.parentNode() is Element) jsoupNode.parentNode() as Element
        else return (Pair(null, null))
        val matchedNode = if (expression.invoke(this)) this else null
        val nextJsoupNodeSibling = matchedNode?.jsoupNode?.nextSibling()
        val nextHtmlNodeSibling = if (nextJsoupNodeSibling != null) HtmlNode(nextJsoupNodeSibling) else null
        return Pair(matchedNode, nextHtmlNodeSibling)
    }

}
