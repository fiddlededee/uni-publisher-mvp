package model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonTypeInfo(include = JsonTypeInfo.As.WRAPPER_OBJECT, use = JsonTypeInfo.Id.NAME)
abstract class Node() {

    // todo: next/previous-sibling
    // todo: ancestors(), nextSiblings(), previousSiblings()
    // todo: is firtst, is last
    var id: String? = null


    @get:JsonIgnore
    abstract val isInline: Boolean

    @JsonIgnore
    private var parent: Node? = null

    val children: ArrayList<Node> = arrayListOf()
    val roles = arrayListOf<String>()
    fun <T : Node> addChild(childNode: T): T {
        val oldParent = childNode.parent
        children.add(childNode)
        childNode.parent = this
        oldParent?.removeChild(childNode)
        return childNode
    }

    // todo : implement all builder functions

    fun table(init: Table.() -> Unit = {}): Table {
        return addChild(Table().apply(init))
    }

    fun tableRowGroup(type: TRG, init: TableRowGroup.() -> Unit = {}): TableRowGroup {
        return addChild(TableRowGroup(type).apply(init))
    }

    fun tr(init: TableRow.() -> Unit = {}): TableRow {
        return addChild(TableRow().apply(init))
    }

    fun td(init: TableCell.() -> Unit = {}): TableCell {
        return addChild(TableCell().apply(init))
    }

    fun h(level: Int, init: Header.() -> Unit = {}): Header {
        return addChild(Header(level).apply(init))
    }


    fun li(init: ListItem.() -> Unit = {}): ListItem {
        return addChild(ListItem().apply(init))
    }

    fun p(init: Paragraph.() -> Unit = {}): Paragraph {
        return addChild(Paragraph().apply(init))
    }

    fun span(init: Span.() -> Unit = {}): Span {
        return addChild(Span().apply(init))
    }

    fun a(href: String, init: Anchor.() -> Unit = {}): Anchor {
        return addChild(Anchor(href).apply(init))
    }

    fun text(text: String): Text {
        return addChild(Text(text))
    }

    operator fun String.unaryPlus(): Text {
        return this@Node.addChild(Text(this))
    }

    private fun removeChild(childNode: Node) {
        children.remove(childNode)
    }

    fun removeSelf() {
        val parentNode = parent
        if (parentNode != null) {
            parentNode.removeChild(this)
        } else throw Exception("Node doesn't hava a parent")
    }

    fun addAfter(node: Node): Node {
        val oldParent = node.parent
        val parent = this.parent
            ?: throw Exception("Can't add node after another if it has no parent")
        val index = parent.children.indexOf(this)
        if (index == -1) {
            throw Exception("Didn't find object to add after")
        }
        parent.children.add(index + 1, node)
        node.parent = parent
        oldParent?.removeChild(node)
        return node
    }

    fun addBefore(node: Node): Node {
        val oldParent = node.parent
        val parent = this.parent
            ?: throw Exception("Can't add node before another if it has no parent")
        val index = parent.children.indexOf(this)
        if (index == -1) {
            throw Exception("Didn't find object to add after")
        }
        parent.children.add(index, node)
        node.parent = parent
        oldParent?.removeChild(node)
        return node
    }


    // todo abstract ?
    abstract fun write(bw: BackendWriter)

    private fun ArrayList<Node>.fillWithDescendants(node: Node, filter: (Node) -> Boolean) {
        node.children.forEach {
            if (filter(it)) {
                add(it)
            }
            fillWithDescendants(it, filter)
        }
    }

    fun descendant(filter: (Node) -> Boolean = { true }): ArrayList<Node> {
        return arrayListOf<Node>().apply { fillWithDescendants(this@Node, filter) }
    }

    fun children(filter: (Node) -> Boolean = { true }): ArrayList<Node> {
        return arrayListOf<Node>()
            .apply { children.forEach { if (filter(it)) this.add(it) } }
    }

    fun parent(): Node? {
        return parent
    }

    inline fun <reified T : Node> T.roles(vararg role: String): T {
        role.forEach { roles.add(it) }
        return this
    }

    fun toYamlString(): String {
        return ObjectMapper(YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER))
            .writeValueAsString(this)
    }

}

