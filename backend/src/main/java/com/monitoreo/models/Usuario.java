package com.monitoreo.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "usuarios")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, unique = true, length = 150)
    private String correo;

    @Column(nullable = false)
    private String password;

    // Token secreto de alta de dispositivos, distinto de las claves privadas por dispositivo.
    @Column(name = "token_dispositivo", unique = true, length = 64)
    private String tokenDispositivo;

    public Usuario() {}

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getCorreo() { return correo; }
    public String getPassword() { return password; }
    @JsonIgnore public String getTokenDispositivo() { return tokenDispositivo; }

    public void setId(Long id) { this.id = id; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setCorreo(String correo) { this.correo = correo; }
    public void setPassword(String password) { this.password = password; }
    public void setTokenDispositivo(String tokenDispositivo) { this.tokenDispositivo = tokenDispositivo; }
}
