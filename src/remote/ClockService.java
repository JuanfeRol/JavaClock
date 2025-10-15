package remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interfaz remota para servicios de reloj (RMI)
 */
public interface ClockService extends Remote {
    // Devuelve la hora local del nodo en milisegundos
    long getTimeMillis() throws RemoteException;

    // Aplica un ajuste (offset en milisegundos). Offset puede ser positivo o negativo.
    void applyAdjustment(long offsetMillis) throws RemoteException;

    // Devuelve un identificador del nodo
    String getId() throws RemoteException;
    
    // ⭐ NUEVO: Método para que el servidor verifique que el cliente está activo
    boolean isAlive() throws RemoteException;
    
    // ⭐ NUEVO: Método SOLO PARA SERVIDOR - Registrar clientes
    default void registrarCliente(String clientId, ClockService cliente) throws RemoteException {
        throw new UnsupportedOperationException("Método solo disponible en el servidor");
    }
    
    // ⭐ NUEVO: Método SOLO PARA SERVIDOR - Iniciar sincronización
    default void iniciarSincronizacion() throws RemoteException {
        throw new UnsupportedOperationException("Método solo disponible en el servidor");
    }

    // ⭐ NUEVO: Método que el SERVIDOR usa para pedir a los clientes que impriman
    // un check-sync (impresión de su hora local). Implementado por los clientes.
    default void checkSync(long serverTime) throws RemoteException {
        throw new UnsupportedOperationException("Método solo disponible en clientes");
    }
}