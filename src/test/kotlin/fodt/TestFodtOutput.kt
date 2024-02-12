package fodt

import org.approvaltests.Approvals
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.w3c.dom.Document
import org.xml.sax.InputSource
import java.io.StringReader
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class TestFodtOutput {
    @Test
    fun testSimpleFodtOutput() {
        @Language("XML") val preFodt =
            """
<root xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0"
      xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0"
      xmlns:fo="urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0">
    <text:h text:style-name="Heading 1">
        <style:text-properties fo:font-size="14pt"/>
        Heading 1
    </text:h>
    <text:p text:style-name="Body Text">
        Some text
    </text:p>
    <text:h text:style-name="Heading 1">
        <style:text-properties fo:font-size="14pt"/>
        Heading 1
    </text:h>
</root>
        """

        @Language("XML") val template =
            """<office:document
        xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
        xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0"
        xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0"
        xmlns:fo="urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0">
    <office:automatic-styles>
        <this-tag-should-be-left/>
    </office:automatic-styles>
    <office:body>
        <office:text text:use-soft-page-breaks="true">
            <this-tag-should-be-left/>
            <text:h text:style-name="P1" text:outline-level="1">@start-include[default]</text:h>
            <text:h text:style-name="P1" text:outline-level="1">@end-include[default]</text:h>
            <this-tag-should-be-left/>
        </office:text>
    </office:body>
</office:document>
            """

        FodtGenerator(preFodt.parseStringAsXML(), template.parseStringAsXML())
            .enrichedTemplate
            .prettySerialize()
            .apply {
                Approvals.verify(this)
            }
    }

    fun Document.prettySerialize(): String {
        val transformerFactory = TransformerFactory.newInstance()
        transformerFactory.setAttribute("indent-number", 2);
        val trans = transformerFactory.newTransformer()
        trans.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        trans.setOutputProperty(OutputKeys.INDENT, "yes");
        val sw = StringWriter()
        val result = StreamResult(sw)
        val source = DOMSource(this.documentElement)
        trans.transform(source, result)
        return sw.toString()
            .replace(""" ([a-z-]+[:][a-z-]+=")""".toRegex(), "\n${" ".repeat(20)}$1")
            .replace("""[ ]*\n""".toRegex(), "\n")
            .replace("\n\n", "\n")
    }
}

