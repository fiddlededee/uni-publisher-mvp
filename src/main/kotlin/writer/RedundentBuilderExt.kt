package writer

import org.redundent.kotlin.xml.Node as XmlNode

fun XmlNode.textProperties(textStyle: XmlNode.() -> Unit) {
    val textStyleNodes =
        children.filterIsInstance<XmlNode>().filter { it.nodeName == "style:text-properties" }
    if (textStyleNodes.isEmpty())
        "style:text-properties" { textStyle.invoke(this) }
    else {
        textStyleNodes[0].apply { textStyle.invoke(this) }
    }
}

fun XmlNode.paragraphProperties(paragraphStyle: XmlNode.() -> Unit) {
    val paragraphStyleNodes =
        children.filterIsInstance<XmlNode>().filter { it.nodeName == "style:paragraph-properties" }
    if (paragraphStyleNodes.isEmpty())
        "style:paragraph-properties" { paragraphStyle.invoke(this) }
    else {
        paragraphStyleNodes[0].apply { paragraphStyle.invoke(this) }
    }
}
fun XmlNode.tableProperties(tableStyle: XmlNode.() -> Unit) {
    val tableStyleNodes =
        children.filterIsInstance<XmlNode>().filter { it.nodeName == "style:table-properties" }
    if (tableStyleNodes.isEmpty())
        "style:table-properties" { tableStyle.invoke(this) }
    else {
        tableStyleNodes[0].apply { tableStyle.invoke(this) }
    }
}
fun XmlNode.tableCellProperties(tableCellStyle: XmlNode.() -> Unit) {
    val tableCellStyleNodes =
        children.filterIsInstance<XmlNode>().filter { it.nodeName == "style:table-cell-properties" }
    if (tableCellStyleNodes.isEmpty())
        "style:table-cell-properties" { tableCellStyle.invoke(this) }
    else {
        tableCellStyleNodes[0].apply { tableCellStyle.invoke(this) }
    }
}

fun XmlNode.tableColumnProperties(tableColumnStyle: XmlNode.() -> Unit) {
    val tableColumnStyleNodes =
        children.filterIsInstance<XmlNode>().filter { it.nodeName == "style:table-cell-properties" }
    if (tableColumnStyleNodes.isEmpty())
        "style:table-cell-properties" { tableColumnStyle.invoke(this) }
    else {
        tableColumnStyleNodes[0].apply { tableColumnStyle.invoke(this) }
    }
}
