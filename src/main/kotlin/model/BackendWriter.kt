package model
interface BackendWriter {
    // pattern visitor
    fun write(openBlock: OpenBlock)
    fun write(table: Table)
    fun write(tableRowGroup: TableRowGroup)
    fun write(tr: TableRow)
    fun write(td: TableCell)
    fun write(colGroup: ColGroup)
    fun write(col: Col)
    fun write(ol: OrderedList)
    fun write(ul: UnorderedList)
    fun write(li: ListItem)
    fun write(h: Header)
    fun write(p: Paragraph)
//    fun write(pre: Pre)
//    fun write(code: Code)
    fun write(doc: Document)
    fun write(text: Text)
    fun write(span: Span)
    fun write(a: Anchor)
    fun write(dummyNode: Node)
}
