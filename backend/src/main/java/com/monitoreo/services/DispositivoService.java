package com.monitoreo.services;

import com.monitoreo.dto.CodigoVinculacionResponse;
import com.monitoreo.dto.DispositivoResumen;
import com.monitoreo.dto.DispositivoVinculadoResponse;
import com.monitoreo.dto.VincularDispositivoRequest;
import com.monitoreo.models.Dispositivo;
import com.monitoreo.models.Usuario;
import com.monitoreo.repositories.DispositivoRepository;
import com.monitoreo.repositories.GraficoRepository;
import com.monitoreo.repositories.LecturaRepository;
import com.monitoreo.repositories.SensorRepository;
import com.monitoreo.repositories.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.UUID;

@Service
public class DispositivoService {
    private final DispositivoRepository repository;
    private final SensorRepository sensorRepository;
    private final LecturaRepository lecturaRepository;
    private final GraficoRepository graficoRepository;
    private final UsuarioRepository usuarioRepository;

    public DispositivoService(DispositivoRepository repository, SensorRepository sensorRepository,
                              LecturaRepository lecturaRepository, GraficoRepository graficoRepository,
                              UsuarioRepository usuarioRepository) {
        this.repository = repository;
        this.sensorRepository = sensorRepository;
        this.lecturaRepository = lecturaRepository;
        this.graficoRepository = graficoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public CodigoVinculacionResponse crearCodigo(Usuario owner) {
        String alias = siguienteAlias(owner);
        Dispositivo d = new Dispositivo();
        d.setAlias(alias);
        d.setUsuario(owner);
        d.setApiKey(UUID.randomUUID().toString());
        d.setCodigoVinculacion(UUID.randomUUID().toString());
        repository.save(d);
        return new CodigoVinculacionResponse(alias, d.getCodigoVinculacion(),
                "Configure pairingCode en el microcontrolador. En el primer arranque, envíe su hardwareId y el código a POST /api/dispositivos/vincular; guarde el deviceKey devuelto en memoria persistente.");
    }

    @Transactional
    public DispositivoVinculadoResponse vincular(VincularDispositivoRequest request) {
        String hardwareId = request.getHardwareId() == null ? "" : request.getHardwareId().trim();
        String pairingCode = request.getPairingCode() == null ? "" : request.getPairingCode().trim();
        String userToken = request.getUserToken() == null ? "" : request.getUserToken().trim();
        if (hardwareId.isBlank() || hardwareId.length() > 80) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "hardwareId es obligatorio y no puede superar 80 caracteres");
        }
        if (pairingCode.isBlank() == userToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Envíe userToken o pairingCode, pero no ambos");
        }
        if (repository.findByHardwareId(hardwareId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este hardwareId ya está vinculado a una cuenta");
        }

        Dispositivo d;
        if (!userToken.isBlank()) {
            Usuario owner = usuarioRepository.findByTokenDispositivo(userToken).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token de usuario inválido o regenerado"));
            d = new Dispositivo();
            d.setAlias(siguienteAlias(owner));
            d.setUsuario(owner);
            d.setApiKey(UUID.randomUUID().toString());
            d.setHardwareId(hardwareId);
            d = repository.save(d);
            return new DispositivoVinculadoResponse(true, d.getAlias(), hardwareId, d.getApiKey(),
                    "Dispositivo vinculado. Guarde deviceKey en memoria persistente y utilícelo para las lecturas.");
        }

        // Flujo heredado: código generado para un solo dispositivo.
        d = repository.findByCodigoVinculacion(pairingCode).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Código de vinculación inválido o ya utilizado"));
        if (d.getHardwareId() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El código ya fue utilizado");
        }
        d.setHardwareId(hardwareId);
        d.setCodigoVinculacion(null);
        repository.save(d);
        return new DispositivoVinculadoResponse(true, d.getAlias(), hardwareId, d.getApiKey(),
                "Dispositivo vinculado. Guarde deviceKey de forma persistente y no lo publique.");
    }

    @Transactional
    public void eliminar(Long id, Usuario owner) {
        Dispositivo dispositivo = repository.findByIdAndUsuario_Id(id, owner.getId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Dispositivo no encontrado en tu cuenta"));
        if (dispositivo.getHardwareId() != null) {
            List<com.monitoreo.models.Sensor> sensores = sensorRepository
                    .findAllByDeviceIdAndUsuario_IdOrderByIdAsc(dispositivo.getHardwareId(), owner.getId());
            for (com.monitoreo.models.Sensor sensor : sensores) {
                graficoRepository.deleteBySensorId(sensor.getId());
                lecturaRepository.deleteBySensorId(sensor.getId());
                sensorRepository.delete(sensor);
            }
        }
        repository.delete(dispositivo);
    }

    public List<DispositivoResumen> listar(Usuario owner) {
        return repository.findByUsuario_IdOrderByIdAsc(owner.getId()).stream().map(DispositivoResumen::new).toList();
    }

    public Dispositivo validarClave(String key) {
        if (key == null || key.isBlank()) return null;
        Dispositivo d = repository.findByApiKey(key.trim()).orElse(null);
        return d != null && d.getHardwareId() != null ? d : null;
    }

    @Transactional
    public Dispositivo asegurarDispositivoLegacy(Usuario owner, String hardwareId) {
        String id = hardwareId == null ? "" : hardwareId.trim();
        if (id.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El identificador físico es obligatorio");
        Dispositivo existing = repository.findByHardwareId(id).orElse(null);
        if (existing != null) {
            if (!existing.getUsuario().getId().equals(owner.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El dispositivo pertenece a otra cuenta");
            }
            return existing;
        }
        Dispositivo d = new Dispositivo();
        d.setHardwareId(id);
        d.setAlias(siguienteAlias(owner));
        d.setUsuario(owner);
        d.setApiKey(UUID.randomUUID().toString());
        return repository.save(d);
    }

    private String siguienteAlias(Usuario owner) {
        int n = 1;
        while (repository.existsByUsuario_IdAndAlias(owner.getId(), "device" + n)) n++;
        return "device" + n;
    }
}
