package org.http4k.filter

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.format.AutoMarshallingJson
import org.http4k.format.AutoMarshallingXml
import java.io.PrintStream

/**
 * This Filter is used to generate Data class definitions from a Response containing XML. The Filter will try and reduce
 * the number of class definitions by selecting the definition with the most fields (for cases where lists of items
 * have different fields).
 */
class GenerateXmlDataClasses<NODE : Any>(
    json: AutoMarshallingJson<NODE>,
    xml: AutoMarshallingXml,
    out: PrintStream = System.out,
    idGenerator: () -> Int = { Math.abs(java.util.Random().nextInt()) }) : Filter {
    private val chains = GenerateDataClasses(json, out, idGenerator).then(Filter { next ->
        {
            next(it).run { with(json.body().toLens() of json.asJsonObject(xml.asA(bodyString(), Map::class))) }
        }
    })

    override fun invoke(p1: HttpHandler): HttpHandler = { chains.then(p1)(it) }
}
