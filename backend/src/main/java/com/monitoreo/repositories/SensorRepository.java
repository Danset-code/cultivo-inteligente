package com.monitoreo.repositories;

import com.monitoreo.models.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SensorRepository extends JpaRepository<Sensor, Long> {
    Optional<Sensor> findByDeviceIdAndCanal(String deviceId, String canal);
    boolean existsByDeviceIdAndCanal(String deviceId, String canal);
    List<Sensor> findAllByUsuario_IdOrderByIdAsc(Long usuarioId);
    List<Sensor> findAllByDeviceIdAndUsuario_IdOrderByIdAsc(String deviceId, Long usuarioId);
    List<Sensor> findByUsuarioIsNull();
    Optional<Sensor> findByIdAndUsuario_Id(Long id, Long usuarioId);
    Optional<Sensor> findByDeviceIdAndCanalAndUsuario_Id(String deviceId, String canal, Long usuarioId);
    boolean existsByDeviceIdAndCanalAndUsuario_Id(String deviceId, String canal, Long usuarioId);

    // Candidatos cuyo último dato recibido quedó fuera de línea.
    @Transactional(readOnly = true)
    @Query("select s from Sensor s where s.ultimaLectura is not null " +
           "and s.ultimaLectura < :limite and upper(s.estado) <> 'INACTIVO'")
    List<Sensor> findSensoresFueraDeLinea(@Param("limite") LocalDateTime limite);

    // Actualización condicional atómica: nunca sobrescribe una lectura nueva
    // si esta llega mientras el scheduler está verificando el estado.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("update Sensor s set s.estado = 'INACTIVO' " +
           "where s.id = :id and s.ultimaLectura is not null " +
           "and s.ultimaLectura < :limite and upper(s.estado) <> 'INACTIVO'")
    int marcarInactivoSiVencido(@Param("id") Long id, @Param("limite") LocalDateTime limite);
}
