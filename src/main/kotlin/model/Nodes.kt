package model

class DummyNode() : Node() {
    override val isInline: Boolean get() = false
    override fun write(bw: BackendWriter) {
        bw.write(this)
    }
}

abstract class NoWriterNode() : Node() {
    override fun write(bw: BackendWriter) {
        throw Exception ("Writer in NoWriterNodes should be defined in CustomStyle")
    }
}

class Document() : Node() {
    override val isInline: Boolean get() = false
    override fun write(bw: BackendWriter) {
        bw.write(this)
    }

}

class OpenBlock() : Node() {
    override val isInline: Boolean get() = false
    override fun write(bw: BackendWriter) {
        bw.write(this)
    }
}

class Col (var width : Width)

enum class WidthUnit { rel, mm }

class Width(var value : Float, var unit : WidthUnit = WidthUnit.rel )

class Table() : Node() {
    val cols = arrayListOf<Col>();
    fun col(width : Width) {
        cols.add(Col(width))
    }
    // TODO: implement cols property
    override val isInline: Boolean get() = false
    override fun write(bw: BackendWriter) {
        bw.write(this)
    }
}

enum class TRG {head, body, foot}

class TableRowGroup(val type : TRG) : Node() {
    override val isInline: Boolean get() = false
    override fun write(bw: BackendWriter) {
        bw.write(this)
    }
}

class TableRow() : Node() {
    override val isInline: Boolean get() = false
    override fun write(bw: BackendWriter) {
        bw.write(this)
    }
}

class TableCell() : Node() {
    override val isInline: Boolean get() = false
    override fun write(bw: BackendWriter) {
        bw.write(this)
    }
}

class OrderedList() : Node() {
    override val isInline: Boolean get() = false
    override fun write(bw: BackendWriter) {
        bw.write(this)
    }
}

class UnorderedList() : Node() {
    override val isInline: Boolean get() = false
    override fun write(bw: BackendWriter) {
        bw.write(this)
    }
}

class ListItem() : Node() {
    override val isInline: Boolean get() = false
    override fun write(bw: BackendWriter) {
        bw.write(this)
    }

    fun wrapListItemContents(): ListItem {
        // Wrapping each inline nodes chains into paragraph node
        fun wrap(itemsToWrap: ArrayList<ArrayList<Node>>) {
            itemsToWrap.forEach { itemsChain ->
                if (itemsChain.isEmpty()) return@forEach
                itemsChain[0].addBefore(Paragraph())
                    .apply { itemsChain.forEach { itemToWrap -> addChild(itemToWrap) } }
            }
        }

        // Finding inline nodes chains
        val itemsToWrap = arrayListOf<ArrayList<Node>>()
        itemsToWrap.add(arrayListOf())
        this.children.forEach { listItemChild ->
            if (listItemChild.isInline) itemsToWrap.last().add(listItemChild)
            else if (itemsToWrap.last().isNotEmpty()) itemsToWrap.add(arrayListOf())
        }
        wrap(itemsToWrap)
        return this
    }
}

class Header(var level: Int) : Node() {
    override val isInline: Boolean get() = false
    override fun write(bw: BackendWriter) {
        bw.write(this)
    }
}

class Paragraph() : Node() {
    override val isInline: Boolean get() = false
    override fun write(bw: BackendWriter) {
        bw.write(this)
    }
}

class Span() : Node() {
    override val isInline: Boolean get() = true
    override fun write(bw: BackendWriter) {
        bw.write(this)
    }
}

class Anchor(var href: String) : Node() {
    override val isInline: Boolean get() = true
    override fun write(bw: BackendWriter) {
        bw.write(this)
    }
}

class Text(var text: String) : Node() {
    override val isInline: Boolean get() = true
    override fun write(bw: BackendWriter) {
        bw.write(this)
    }
}
