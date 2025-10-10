# Sincronización de Reloj Distribuida con RMI (Java)

Este proyecto implementa un **algoritmo de sincronización de reloj tipo Berkeley** utilizando **Java RMI**. El sistema consta de un **servidor coordinador** y múltiples **clientes**, donde el servidor coordina la sincronización de tiempo y calcula los ajustes necesarios para todos los nodos.

---

## Descripción del Proyecto

- **Servidor (ClockServer)**:
  - Coordina la sincronización de reloj.
  - Solicita la hora a todos los clientes y ajusta por RTT (Round-Trip Time).
  - Calcula el desfase de cada nodo respecto a su propio tiempo.
  - Calcula el promedio de desfases y envía ajustes a los clientes.
  - Actualiza su propio reloj para mantener la consistencia.

- **Clientes (ClockClient)**:
  - Implementan la interfaz remota `ClockService`.
  - Registran su objeto RMI en la IP local de la máquina.
  - Reciben ajustes del servidor y aplican cambios a su reloj local.

- **Interfaz Remota (ClockService)**:
  - Define los métodos para obtener la hora (`getTimeMillis()`), aplicar ajustes (`applyAdjustment()`) y obtener un identificador de nodo (`getId()`).

- **Compatibilidad remota**:
  - Ahora tanto servidor como clientes pueden funcionar en diferentes dispositivos de la misma red, usando la **IP real** de cada máquina.

---

## Requisitos

- Java 8 o superior.
- Acceso a la red local para que los clientes puedan conectarse al servidor.
- Puertos abiertos (por defecto **1099**) para RMI.

---

## Compilación

Desde la raíz del proyecto:

mkdir -p build
javac -d build src/remote/ClockService.java src/util/TimeUtils.java src/client/ClockClient.java src/server/ClockServer.java

---

## Ejecución

### 1. Iniciar el servidor

java -cp build server.ClockServer client1 client2 client3     

- El servidor imprimirá su **IP real** y la URL RMI que los clientes deben usar.  
- Espera unos segundos para que los clientes se registren.

### 2. Iniciar cada cliente

java -cp build client.ClockClient <ID_CLIENTE> <OFFSET_SEC> <SERVER_IP> [PORT]


- **ID_CLIENTE**: Identificador único del cliente.  
- **OFFSET_SEC**: Offset inicial del reloj en segundos (puede ser negativo).  
- **SERVER_IP**: IP real del servidor.  
- **PORT** (opcional): Puerto del RMIRegistry (por defecto 1099).

**Ejemplo**:

java -cp build client.ClockClient client1 -29 192.168.1.100 1099
java -cp build client.ClockClient client2 61 192.168.1.100 1099
java -cp build client.ClockClient client3 -74 192.168.1.100 1099


### 3. Sincronización

- Una vez que todos los clientes estén activos, el servidor ejecuta el algoritmo de sincronización:
  - Solicita la hora a los clientes y ajusta por RTT.
  - Calcula desfases y promedio.
  - Envía los ajustes a cada nodo.
  - Imprime tablas de **desfases**, **ajustes** y **nueva hora**.

---

## Tablas impresas por el servidor

1. **Tabla de Entrada y Desfase**  
   Muestra la hora original de cada nodo y su desfase respecto al servidor.

2. **Tabla de Promedio y Ajuste**  
   Muestra el cálculo del promedio de desfases y el ajuste enviado a cada nodo.

3. **Tabla de Nueva Hora**  
   Muestra la hora final de cada nodo después de aplicar los ajustes.

---

## Notas

- Asegúrate de que las IPs usadas sean accesibles entre los dispositivos.  
- Mantén abierto el puerto 1099 o el que uses para RMI.  
- Los clientes deben ejecutarse antes de que el servidor sincronice los relojes.  

---

## Autor

Proyecto realizado como práctica de sincronización de relojes distribuidos en Java usando RMI.

