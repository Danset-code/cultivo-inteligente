package com.monitoreo.controllers;

import com.monitoreo.dto.RangoSensorRequest;
import com.monitoreo.models.RangoSensor;
import com.monitoreo.models.Usuario;
import com.monitoreo.services.ConfiguracionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/configuracion")
public class ConfiguracionController {
    private final ConfiguracionService service;

    public ConfiguracionController(ConfiguracionService service) { this.service = service; }

    @GetMapping("/token-dispositivo")
    public ResponseEntity<?> obtenerToken(@RequestAttribute("currentUser") Usuario owner) {
        String token = owner.getTokenDispositivo();
        return ResponseEntity.ok(Map.of("configurado", token != null && !token.isBlank(), "token", token == null ? "" : token));
    }

    @PostMapping("/token-dispositivo/generar")
    public ResponseEntity<?> generarToken(@RequestAttribute("currentUser") Usuario owner) {
        String token = service.generarTokenDispositivo(owner.getId());
        return ResponseEntity.ok(Map.of("success", true, "token", token,
                "message", "Token generado. Puedes usarlo para vincular varios dispositivos."));
    }

    @GetMapping("/rangos")
    public ResponseEntity<List<RangoSensor>> listarRangos(@RequestAttribute("currentUser") Usuario owner) {
        return ResponseEntity.ok(service.listarRangos(owner.getId()));
    }

    @PutMapping("/rangos")
    public ResponseEntity<List<RangoSensor>> guardarRangos(@RequestBody List<RangoSensorRequest> requests,
                                                            @RequestAttribute("currentUser") Usuario owner) {
        return ResponseEntity.ok(service.guardarRangos(owner, requests));
    }
}
