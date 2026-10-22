package com.monitoreo.repositories;

import com.monitoreo.models.RangoSensor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RangoSensorRepository extends JpaRepository<RangoSensor, Long> {
    List<RangoSensor> findByUsuario_IdOrderByTipoAsc(Long usuarioId);
    Optional<RangoSensor> findByUsuario_IdAndTipo(Long usuarioId, String tipo);
    void deleteByUsuario_Id(Long usuarioId);
}
