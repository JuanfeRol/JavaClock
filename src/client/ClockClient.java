package client;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import remote.ClockService;
import util.TimeUtils;

public class ClockClient extends UnicastRemoteObject implements ClockService {
    private final String id;
    private long offsetMillis;

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
        System.out.println("[" + id + "] 📍 Recibiendo ajuste (ms): " + offsetMillis);
        this.offsetMillis += offsetMillis;
        long newLocal = System.currentTimeMillis() + this.offsetMillis;
        System.out.println("[" + id + "] ✅ Nuevo reloj local -> " + TimeUtils.fmt(newLocal) +
                " (offsetTotal(ms)=" + this.offsetMillis + ")");
    }

    @Override
    public String getId() throws RemoteException {
        return id;
    }
    
    @Override
    public boolean isAlive() throws RemoteException {
        return true;
    }

    // ⭐ Los métodos registrarCliente e iniciarSincronizacion NO se implementan aquí
    // (usarán la implementación por defecto que lanza UnsupportedOperationException)

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

            String serverUrl = "rmi://" + serverIP + ":" + port + "/ClockServer";
            ClockService servidor = (ClockService) Naming.lookup(serverUrl);
            
            // Registrar este cliente con el servidor
            servidor.registrarCliente(id, client);
            
            System.out.println("✅ Cliente " + id + " registrado en servidor " + serverIP);
            System.out.println("📍 Offset inicial: " + offsetMs + "ms");
            System.out.println("⏰ Hora local simulada: " + TimeUtils.fmt(System.currentTimeMillis() + offsetMs));
            System.out.println("👂 Esperando ajustes del servidor... (presiona Ctrl+C para salir)");

            Thread.sleep(Long.MAX_VALUE);

        } catch (Exception e) {
            System.err.println("❌ Error conectando con el servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}