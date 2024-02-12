package converter

import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import verify


class TestConverter {
    @Language("XML") val testTemplate =
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

    @Test
    fun testFull() {
        FodtConverter {
            html = "<div><p>Some paragraph</p></div>"
            xpath = "/html/body/div"
            parse()
            generatePre()
            template = testTemplate
            generateFodt()
            fodt!!.verify()
        }
    }
}