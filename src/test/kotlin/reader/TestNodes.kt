package reader

import converter.FodtConverter
import org.junit.jupiter.api.Test

class TestNodes {

    @Test
    fun table() {
        FodtConverter {
            html = "<table><tr><td>A1</td><td>A2</td></tr></table>"
            parse()
            println(ast?.toYamlString())
        }
    }
}