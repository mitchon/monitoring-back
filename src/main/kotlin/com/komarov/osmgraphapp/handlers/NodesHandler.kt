package com.komarov.osmgraphapp.handlers

import de.westnordost.osmapi.map.data.Node
import de.westnordost.osmapi.map.handler.ListOsmElementHandler

class NodesHandler(
): ListOsmElementHandler<Node>(Node::class.java) {

    override fun handleElement(element: Node?) {
        super.handleElement(element)
    }
}