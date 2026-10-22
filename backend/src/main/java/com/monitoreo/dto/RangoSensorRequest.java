package com.monitoreo.dto;

public class RangoSensorRequest {
    private String tipo;
    private Double minimo;
    private Double maximo;

    public String getTipo() { return tipo; }
    public Double getMinimo() { return minimo; }
    public Double getMaximo() { return maximo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public void setMinimo(Double minimo) { this.minimo = minimo; }
    public void setMaximo(Double maximo) { this.maximo = maximo; }
}
