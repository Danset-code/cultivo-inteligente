package com.monitoreo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class VincularDispositivoRequest {
    @NotBlank(message = "El identificador de hardware es obligatorio")
    @Size(max = 80)
    private String hardwareId;

    // Compatibilidad con códigos legacy de un solo uso.
    @Size(max = 64)
    private String pairingCode;

    // Token largo de la cuenta, reutilizable para dar de alta múltiples dispositivos.
    @Size(max = 64)
    private String userToken;

    public String getHardwareId() { return hardwareId; }
    public String getPairingCode() { return pairingCode; }
    public String getUserToken() { return userToken; }
    public void setHardwareId(String hardwareId) { this.hardwareId = hardwareId; }
    public void setPairingCode(String pairingCode) { this.pairingCode = pairingCode; }
    public void setUserToken(String userToken) { this.userToken = userToken; }
}
