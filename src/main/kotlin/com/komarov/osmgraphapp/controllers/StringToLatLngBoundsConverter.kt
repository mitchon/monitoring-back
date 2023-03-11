package com.komarov.osmgraphapp.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import com.komarov.osmgraphapp.services.LatLngBounds
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import java.util.*

@Component
class StringToLatLngBoundsConverter(
    private val objectMapper: ObjectMapper
): Converter<String, LatLngBounds> {
    override fun convert(source: String): LatLngBounds {
        val json = String(Base64.getMimeDecoder().decode(source))
        return objectMapper.readValue(json, LatLngBounds::class.java)
    }

}