package converter

import fodt.FodtGenerator
import fodt.parseStringAsXML
import model.Document
import model.Node
import org.redundent.kotlin.xml.PrintOptions
import reader.GenericHtmlReader
import reader.HtmlNode
import writer.OdWriter
import writer.OdtStyleList

open class FodtConverter(init: FodtConverter.() -> Unit) {

    var html: String? = null
        set(value) {
            val fieldNotNullable = value ?: throw Exception("html value can't be null")
            field = fieldNotNullable; htmlNode = HtmlNode(fieldNotNullable)
        }

    var xpath: String = "/html/body"
    var observedClassList: Set<String> = setOf()
    var odtStyleList = OdtStyleList()
    var template : String? = null
    var ast: Node? = null
    var pre : String? = null
    var fodtGenerator: FodtGenerator? = null
    var fodt : String? = null
    private var htmlNode: HtmlNode? = null

    init {
        this.apply(init)
    }

    fun ast() : Node {
        return ast ?: throw Exception("AST is empty")
    }

    fun pre() : String {
        return pre ?: throw Exception("Preliminary fodt is empty")
    }

    fun fodt() : String {
        return fodt ?: throw Exception("Fodt is empty")
    }

    fun parse() {
        val localHtmlNode = htmlNode?.selectAtXpath(xpath ?: "")
            ?: throw Exception("Please set html and xpath if necessary")
        ast = Document()
        val localAst = ast ?: throw Exception("Error: ast variable was mutated")
        newReaderInstance(localAst, localHtmlNode, observedClassList).apply { iterateAll() }
    }

    fun generatePre() {
        pre = newOdWriterInstance(odtStyleList = odtStyleList).apply {
            val localAst = ast ?: throw Exception("Please set ast variable")
            localAst.write(this) }.preOdNode.toString(PrintOptions(pretty = false))
    }

    fun generateFodt() {
        val localTemplate = template ?: throw Exception("Please, set template variable")
        val localPre = pre ?: throw Exception("Please set pre variable")
        fodtGenerator = FodtGenerator(localPre.parseStringAsXML(), localTemplate.parseStringAsXML())
        serializeFodt()
    }

    fun convert() {
        parse(); generatePre(); generateFodt()
    }

    fun ast2fodt() {
        generatePre(); generateFodt()
    }

    fun serializeFodt() {
        fodt = fodtGenerator?.serialize()
    }

    open fun newReaderInstance(ast: Node, htmlNode: HtmlNode, observedClassList: Set<String>): GenericHtmlReader {
        return GenericHtmlReader(ast, htmlNode, observedClassList)
    }

    open fun newOdWriterInstance(odtStyleList: OdtStyleList) : OdWriter {
        return OdWriter(odtStyleList = odtStyleList)
    }
}