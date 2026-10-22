package com.monitoreo.services;

import com.monitoreo.dto.RangoSensorRequest;
import com.monitoreo.models.RangoSensor;
import com.monitoreo.models.Usuario;
import com.monitoreo.repositories.RangoSensorRepository;
import com.monitoreo.repositories.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;

@Service
public class ConfiguracionService {
    private final UsuarioRepository usuarioRepository;
    private final RangoSensorRepository rangoRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public ConfiguracionService(UsuarioRepository usuarioRepository, RangoSensorRepository rangoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.rangoRepository = rangoRepository;
    }

    @Transactional
    public String generarTokenDispositivo(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        // 256 bits aleatorios / 64 caracteres hexadecimales; la columna cuenta con restricción UNIQUE.
        for (int intento = 0; intento < 10; intento++) {
            byte[] bytes = new byte[32];
            secureRandom.nextBytes(bytes);
            String token = HexFormat.of().formatHex(bytes);
            if (!usuarioRepository.existsByTokenDispositivo(token)) {
                usuario.setTokenDispositivo(token);
                usuarioRepository.saveAndFlush(usuario);
                return token;
            }
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "No se pudo crear un token único. Inténtalo de nuevo.");
    }

    public List<RangoSensor> listarRangos(Long usuarioId) {
        return rangoRepository.findByUsuario_IdOrderByTipoAsc(usuarioId);
    }

    @Transactional
    public List<RangoSensor> guardarRangos(Usuario owner, List<RangoSensorRequest> requests) {
        if (requests == null || requests.size() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La lista de rangos no es válida");
        }
        for (RangoSensorRequest request : requests) {
            if (request == null || request.getTipo() == null || request.getTipo().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cada rango debe tener un tipo de sensor");
            }
            String tipo = request.getTipo().trim();
            if (tipo.length() > 100) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El tipo de sensor supera los 100 caracteres");
            }
            Double minimo = request.getMinimo();
            Double maximo = request.getMaximo();
            var existente = rangoRepository.findByUsuario_IdAndTipo(owner.getId(), tipo);
            if (minimo == null && maximo == null) {
                existente.ifPresent(rangoRepository::delete);
                continue;
            }
            if (minimo == null || maximo == null || !Double.isFinite(minimo) || !Double.isFinite(maximo)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Para el tipo " + tipo + " debes completar ambos límites con números válidos");
            }
            if (minimo > maximo) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El límite desde no puede superar el límite hasta para " + tipo);
            }
            RangoSensor rango = existente.orElseGet(RangoSensor::new);
            rango.setTipo(tipo);
            rango.setMinimo(minimo);
            rango.setMaximo(maximo);
            rango.setUsuario(owner);
            rangoRepository.save(rango);
        }
        rangoRepository.flush();
        return listarRangos(owner.getId());
    }
}
