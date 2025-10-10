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
}