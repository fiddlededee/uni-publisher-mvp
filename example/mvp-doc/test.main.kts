@file:DependsOn("ru.fiddlededee:uni-publisher:0.6")

import converter.FodtConverter


FodtConverter {
    html = """
        <p><span>String &gt; 1</span>
        String 2</p>
    """.trimIndent()
    parse()
    println(ast().toYamlString())
}