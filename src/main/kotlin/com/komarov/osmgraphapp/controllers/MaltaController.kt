package com.komarov.osmgraphapp.controllers

import com.komarov.osmgraphapp.services.MaltaService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/malta")
class MaltaController(
    private val maltaService: MaltaService
) {
    @GetMapping("/")
    fun getMaltaShops() {
        maltaService.getMaltaShops()
    }
}