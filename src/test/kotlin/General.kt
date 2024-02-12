import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import model.Node
import org.approvaltests.Approvals

fun Node.toYamlString() : String {
    return ObjectMapper(YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER))
        .writeValueAsString(this)
}

fun String.verify() {
    Approvals.verify(this)
}