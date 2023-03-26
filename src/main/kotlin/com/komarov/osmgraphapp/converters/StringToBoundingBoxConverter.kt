package com.komarov.osmgraphapp.converters

import com.fasterxml.jackson.databind.ObjectMapper
import com.komarov.osmgraphapp.models.BoundingBoxTemplate
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import java.util.*

@Component
class StringToBoundingBoxConverter(
    private val objectMapper: ObjectMapper
): Converter<String, BoundingBoxTemplate> {
    override fun convert(source: String): BoundingBoxTemplate {
        val json = String(Base64.getMimeDecoder().decode(source))
        return objectMapper.readValue(json, BoundingBoxTemplate::class.java)
    }

}