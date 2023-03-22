package com.komarov.osmgraphapp.handlers

import de.westnordost.osmapi.map.data.Way
import de.westnordost.osmapi.map.handler.ListOsmElementHandler

class RoadwaysHandler(
): ListOsmElementHandler<Way>(Way::class.java) {
    override fun handleElement(element: Way?) {
        super.handleElement(element)
    }

}