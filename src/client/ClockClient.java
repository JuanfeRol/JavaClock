package client;

import java.net.InetAddress;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import remote.ClockService;
import util.TimeUtils;

/**
 * Cliente que implementa la interfaz remota ClockService.
 * Ejemplo de ejecución:
 * java -cp build client.ClockClient client1 -29 <SERVER_IP> <PORT>
 */
public class ClockClient extends UnicastRemoteObject implements ClockService {
    private final String id;
    private long offsetMillis; // offset en milisegundos que simula la diferencia de reloj local

    protected ClockClient(String id, long offsetMillis) throws RemoteException {
        super();
        this.id = id;
        this.offsetMillis = offsetMillis;
    }

    @Override
    public long getTimeMillis() throws RemoteException {
        long reported = System.currentTimeMillis() + offsetMillis;
        System.out.println("[" + id + "] getTimeMillis() -> " + TimeUtils.fmt(reported) + " (ms=" + reported + ")");
        return reported;
    }

    @Override
    public void applyAdjustment(long offsetMillis) throws RemoteException {
        System.out.println("[" + id + "] Recibiendo ajuste (ms): " + offsetMillis);
        this.offsetMillis += offsetMillis;
        long newLocal = System.currentTimeMillis() + this.offsetMillis;
        System.out.println("[" + id + "] Nuevo reloj local -> " + TimeUtils.fmt(newLocal) +
                " (offsetTotal(ms)=" + this.offsetMillis + ")");
    }

    @Override
    public String getId() throws RemoteException {
        return id;
    }

    public static void main(String[] args) {
        try {
            if (args.length < 3) {
                System.err.println("Uso: java client.ClockClient <ID> <OFFSET_SEC> <SERVER_IP> [PORT]");
                System.exit(1);
            }

            String id = args[0];
            long offsetSec = Long.parseLong(args[1]);
            String serverIP = args[2];
            int port = args.length > 3 ? Integer.parseInt(args[3]) : 1099;

            long offsetMs = offsetSec * 1000L;
            ClockClient client = new ClockClient(id, offsetMs);

            // Detectar IP local del cliente
            String clientIP = InetAddress.getLocalHost().getHostAddress();

            // Registrar el cliente en su propio RMIRegistry local
            Naming.rebind("//" + clientIP + "/" + id, client);
            System.out.println("Cliente registrado en RMIRegistry con nombre: " + id +
                    " | IP: " + clientIP + " | Offset inicial(ms): " + offsetMs);

            // Conectarse al servidor remoto para aparecer en la lista de clientes
            String serverUrl = "rmi://" + serverIP + ":" + port + "/ClockServer";
            ClockService server = (ClockService) Naming.lookup(serverUrl);
            System.out.println("Conectado al servidor: " + serverUrl);

            // Mantener el cliente vivo para recibir invocaciones remotas
            System.out.println("Esperando invocaciones remotas... (presiona Ctrl+C para salir)");
            Thread.sleep(Long.MAX_VALUE);

        } catch (NumberFormatException nfe) {
            System.err.println("El segundo argumento debe ser un número entero (offset en segundos).");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
