# API REST - Sistema de Monitoreo de Cultivo Medicinal

## Direcciones base

Desarrollo local: `http://localhost:8080`

## 1. Autenticación y sesiones

### `POST /api/auth/register`

```json
{
  "nombre": "Usuario de prueba",
  "correo": "usuario@example.com",
  "password": "ClaveSegura123"
}
```

### `POST /api/auth/login`

```json
{
  "correo": "admin@monitoreo.com",
  "password": "Admin123"
}
```

La respuesta de inicio de sesión incluye `accessToken`. Para acceder a sensores, gráficos, lecturas, reportes y dispositivos desde Postman o desde otra herramienta, agrega este encabezado:

```text
Authorization: Bearer <accessToken>
```

El token se guarda en una tabla de sesiones del backend y expira en ocho horas. La interfaz web no lo almacena en `localStorage` ni en `sessionStorage`: al recargar la URL debe pedir autenticación nuevamente.

### `POST /api/auth/logout`

Invalida la sesión actual. Envía el token anterior en `Authorization`.

## 2. Sensores

Todas las rutas de este módulo requieren autenticación y filtran por la cuenta asociada a la sesión.

| Método | Endpoint | Función |
|---|---|---|
| GET | `/api/sensores` | Lista los sensores del usuario |
| GET | `/api/sensores/{id}` | Consulta un sensor propio |
| POST | `/api/sensores` | Crea un sensor para el usuario |
| PUT | `/api/sensores/{id}` | Actualiza un sensor propio |
| DELETE | `/api/sensores/{id}` | Elimina el sensor y sus lecturas/gráficos asociados |

Ejemplo de cuerpo para `POST /api/sensores`:

```json
{
  "nombre": "Temperatura de hoja",
  "tipo": "Temperatura de hoja",
  "unidad": "°C",
  "valorActual": 0
}
```

Para asociar un sensor con una placa vinculada se utilizan `deviceId` (ID de hardware registrado) y `canal`; en la interfaz se selecciona el alias automático y no se escribe el ID manualmente.

## 3. Gráficos

| Método | Endpoint | Función |
|---|---|---|
| GET | `/api/graficos` | Lista gráficos del usuario |
| GET | `/api/graficos/{id}` | Consulta un gráfico propio |
| POST | `/api/graficos` | Crea un gráfico sobre un sensor propio |
| PUT | `/api/graficos/{id}` | Actualiza un gráfico propio |
| PATCH | `/api/graficos/{id}/activo?activo=true` | Activa/desactiva un gráfico propio |
| DELETE | `/api/graficos/{id}` | Elimina un gráfico propio |

Ejemplo de `POST /api/graficos`:

```json
{
  "nombre": "Temperatura de hoja",
  "tipo": "LINEAL",
  "sensorId": 1,
  "activo": true
}
```

Tipos visuales usados por el frontend: `LINEAL`, `BARRAS` y `AREA`.

## 4. Lecturas

| Método | Endpoint | Función |
|---|---|---|
| GET | `/api/lecturas` | Lista lecturas del usuario |
| GET | `/api/lecturas/{id}` | Consulta una lectura propia |
| GET | `/api/lecturas/sensor/{sensorId}` | Lista lecturas de un sensor propio |
| POST | `/api/lecturas` | Registra una lectura propia o autenticada con clave de dispositivo |
| DELETE | `/api/lecturas/{id}` | Elimina una lectura propia |

Un navegador o Postman con sesión puede enviar una lectura asociada a un sensor propio:

```json
{
  "sensorId": 1,
  "valor": 25.8,
  "fecha": "2026-10-08T13:30:00"
}
```

Debe incluir el encabezado `Authorization: Bearer <accessToken>`.

## 5. Dispositivos físicos: nombres automáticos y token de vinculación

Las rutas de consulta y generación de tokens requieren sesión de usuario. La vinculación es pública, pero solo se autoriza con el token secreto de la cuenta o con un código legacy válido.

| Método | Endpoint | Función |
|---|---|---|
| GET | `/api/dispositivos` | Lista dispositivos de la cuenta |
| DELETE | `/api/dispositivos/{id}` | Elimina el dispositivo propio y sus sensores, gráficos e historial asociado |
| GET | `/api/configuracion/token-dispositivo` | Consulta el token de vinculación de la cuenta actual |
| POST | `/api/configuracion/token-dispositivo/generar` | Genera o rota un token aleatorio de 256 bits, único en la base de datos |
| GET | `/api/configuracion/rangos` | Lee rangos configurados para los tipos de sensor del usuario |
| PUT | `/api/configuracion/rangos` | Guarda los rangos generales por tipo de sensor para ese usuario |
| POST | `/api/dispositivos/codigo-vinculacion` | Genera un código legacy de un solo uso |
| POST | `/api/dispositivos/vincular` | Vincula una placa (ruta pública autenticada por secreto) |

El token de cuenta contiene 64 caracteres hexadecimales (256 bits aleatorios). Su valor se mantiene único mediante una restricción `UNIQUE` en la tabla `usuarios`. Solo se comparte con dispositivos que deban darse de alta; no es la clave para enviar todas las lecturas.

Petición inicial recomendada a `POST /api/dispositivos/vincular`:

```json
{
  "hardwareId": "IDENTIFICADOR_UNICO_LEIDO_POR_EL_FIRMWARE",
  "userToken": "TOKEN_DE_64_CARACTERES_GENERADO_EN_CONFIGURACION"
}
```

Respuesta orientativa:

```json
{
  "success": true,
  "alias": "device1",
  "hardwareId": "IDENTIFICADOR_UNICO_LEIDO_POR_EL_FIRMWARE",
  "deviceKey": "CLAVE_PRIVADA_EXCLUSIVA_DE_ESTA_PLACA",
  "message": "Dispositivo vinculado"
}
```

El `userToken` puede utilizarse para vincular varios `hardwareId` distintos. El backend genera para cada dispositivo una `deviceKey` diferente. La placa debe guardar su `deviceKey` de forma persistente y utilizarla en el encabezado `X-Device-Key` para enviar lecturas. No debe enviar correo ni contraseña. Regenerar el token de cuenta invalida el anterior para nuevas vinculaciones, pero no cambia las claves de las placas ya vinculadas. Para revocar una placa existente, elimínala desde Dispositivos; las lecturas posteriores con su clave serán rechazadas.

El flujo anterior con `pairingCode` de un solo uso sigue disponible por compatibilidad.

## 6. Lecturas enviadas por una placa

Una placa vinculada envía `POST /api/lecturas` con este encabezado:

```text
X-Device-Key: <deviceKey>
Content-Type: application/json
```

Y un cuerpo como:

```json
{
  "deviceId": "IDENTIFICADOR_UNICO_LEIDO_POR_EL_FIRMWARE",
  "canal": "1",
  "tipo": "Temperatura del aire",
  "unidad": "°C",
  "valor": 26.8,
  "fecha": "2026-10-08T20:30:00"
}
```

Se utiliza la combinación `deviceId + canal` para encontrar el sensor. Una placa puede tener varios canales, incluso de un mismo tipo, conservando el mismo `deviceId` y usando un `canal` distinto. Si un canal aún no está registrado, el backend puede crear su sensor durante la primera lectura cuando se envían `tipo` y `unidad`.

Consulta [`DEVICE_LINKING.md`](DEVICE_LINKING.md) para consejos de obtención del identificador según ESP32, Raspberry Pi y Arduino.

### Rangos por tipo de sensor

`PUT /api/configuracion/rangos` recibe una lista. Para cada tipo se guarda un único par mínimo/máximo por usuario; por eso el límite se aplica a todos los sensores que tengan el mismo campo `tipo`.

```json
[
  {"tipo": "Temperatura del aire", "minimo": 20, "maximo": 32},
  {"tipo": "CO₂", "minimo": 380, "maximo": 520}
]
```

La interfaz incluye los 23 tipos definidos en `frontend/src/main/webapp/simulador.html` y propone inicialmente los mismos rangos de ese simulador. Los límites guardados se pueden consultar desde el simulador mediante `GET /api/configuracion/rangos`. En el Dashboard, un valor mayor que el máximo colorea el fondo de la tarjeta en rojo claro; un valor menor que el mínimo lo colorea en azul claro. El color del gráfico en sí no cambia.

## 7. Reportes y exportación

Las dos rutas requieren sesión y solo muestran lecturas de la cuenta autenticada:

- `GET /api/reportes`
- `GET /api/reportes/exportar`

Parámetros opcionales: `sensorId`, `desde=yyyy-MM-dd`, `hasta=yyyy-MM-dd`. La exportación genera un archivo XLSX real.

## 8. WebSocket

El navegador se conecta con el token de sesión:

`ws://localhost:8080/ws/sensores?token=<accessToken>`

El servidor publica cada lectura y cambio de estado únicamente a las sesiones WebSocket del propietario del sensor.

## 9. Catálogo de tipos de sensores

El catálogo de tipos/unidades es común para todas las cuentas; no contiene sensores instalados, gráficos ni lecturas. La interfaz principal utiliza un catálogo de variables integrado en el frontend.

- `GET /api/tipos-sensores`
- `GET /api/tipos-sensores?activos=false`
- `POST /api/tipos-sensores`
- `PUT /api/tipos-sensores/{id}`
- `PATCH /api/tipos-sensores/{id}/activo?activo=true|false`
- `DELETE /api/tipos-sensores/{id}`

## 10. Prueba de aislamiento recomendada

1. Inicia sesión como administrador y confirma que se muestran sus datos históricos.
2. Registra un usuario nuevo, inicia sesión y confirma que el dashboard está vacío.
3. Crea un sensor y un gráfico con el segundo usuario y registra una lectura.
4. Entra de nuevo como administrador. Los nuevos datos del segundo usuario no deben aparecer en su dashboard, lista de sensores, reportes ni WebSocket.
5. Intenta consultar el ID de un sensor de otra cuenta con el token del segundo usuario. La API debe rechazar la consulta o responder que el recurso no existe en su cuenta.
