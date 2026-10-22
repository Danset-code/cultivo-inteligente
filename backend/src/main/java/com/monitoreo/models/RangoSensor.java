package com.monitoreo.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "rangos_sensor", uniqueConstraints = {
        @UniqueConstraint(name = "uk_rango_usuario_tipo", columnNames = {"usuario_id", "tipo"})
})
public class RangoSensor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String tipo;

    @Column(name = "minimo", nullable = false)
    private Double minimo;

    @Column(name = "maximo", nullable = false)
    private Double maximo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    public RangoSensor() {}

    public RangoSensor(String tipo, Double minimo, Double maximo, Usuario usuario) {
        this.tipo = tipo;
        this.minimo = minimo;
        this.maximo = maximo;
        this.usuario = usuario;
    }

    public Long getId() { return id; }
    public String getTipo() { return tipo; }
    public Double getMinimo() { return minimo; }
    public Double getMaximo() { return maximo; }
    @JsonIgnore public Usuario getUsuario() { return usuario; }
    public void setId(Long id) { this.id = id; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public void setMinimo(Double minimo) { this.minimo = minimo; }
    public void setMaximo(Double maximo) { this.maximo = maximo; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
}
