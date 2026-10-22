package com.monitoreo.services;

import com.monitoreo.models.Sensor;
import com.monitoreo.repositories.SensorRepository;
import com.monitoreo.websocket.SensorWebSocketHandler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class SensorEstadoScheduler {
    private static final long TIMEOUT_SECONDS = 10;
    private static final ZoneId ZONA_SISTEMA = ZoneId.of("America/Bogota");

    private final SensorRepository sensorRepository;
    private final SensorWebSocketHandler webSocketHandler;

    public SensorEstadoScheduler(SensorRepository sensorRepository,
                                 SensorWebSocketHandler webSocketHandler) {
        this.sensorRepository = sensorRepository;
        this.webSocketHandler = webSocketHandler;
    }

    @Scheduled(fixedRate = 1000)
    public void verificarEstados() {
        LocalDateTime limite = LocalDateTime.now(ZONA_SISTEMA).minusSeconds(TIMEOUT_SECONDS);
        List<Sensor> candidatos = sensorRepository.findSensoresFueraDeLinea(limite);

        for (Sensor candidato : candidatos) {
            // El UPDATE comprueba otra vez la fecha en la base de datos para
            // impedir que una lectura concurrente quede sobrescrita como INACTIVO.
            int actualizados = sensorRepository.marcarInactivoSiVencido(candidato.getId(), limite);
            if (actualizados == 0) {
                continue;
            }

            Sensor actualizado = sensorRepository.findById(candidato.getId()).orElse(null);
            if (actualizado == null || actualizado.getUltimaLectura() == null) {
                continue;
            }

            // Solo emitir INACTIVO si todavía han pasado más de 10 segundos.
            if ("INACTIVO".equalsIgnoreCase(actualizado.getEstado())
                    && actualizado.getUltimaLectura().isBefore(limite)) {
                broadcastEstado(actualizado);
            }
        }
    }

    private void broadcastEstado(Sensor sensor) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", "ESTADO_SENSOR");
        payload.put("sensorId", sensor.getId());
        payload.put("deviceId", sensor.getDeviceId());
        payload.put("deviceAlias", sensor.getDeviceAlias());
        payload.put("canal", sensor.getCanal());
        payload.put("tipo", sensor.getTipo());
        payload.put("unidad", sensor.getUnidad());
        payload.put("estado", sensor.getEstado());
        payload.put("valor", sensor.getValorActual());
        // Fecha de recepción del último dato, no la fecha del reloj del dispositivo.
        payload.put("fecha", sensor.getUltimaLectura());
        if (sensor.getUsuario() != null) {
            webSocketHandler.broadcast(payload, sensor.getUsuario().getId());
        }
    }
}
