package server;

import java.net.InetAddress;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import remote.ClockService;
import util.TimeUtils;

/**
 * Servidor Coordinador del algoritmo de sincronización de reloj (tipo Berkeley).
 * Ahora soporta conexión remota usando IP real.
 */
public class ClockServer extends UnicastRemoteObject implements ClockService {
    private final String id = "server";
    private long offsetMillis = 0; // offset local (inicialmente 0)
    private final DecimalFormat df = new DecimalFormat("0.00");

    protected ClockServer() throws RemoteException {
        super();
    }

    @Override
    public long getTimeMillis() throws RemoteException {
        long now = System.currentTimeMillis() + offsetMillis;
        System.out.println("[Servidor] getTimeMillis() -> " + TimeUtils.fmt(now));
        return now;
    }

    @Override
    public void applyAdjustment(long offsetMillis) throws RemoteException {
        System.out.println("[Servidor] Aplicando ajuste (ms): " + offsetMillis);
        this.offsetMillis += offsetMillis;
    }

    @Override
    public String getId() throws RemoteException {
        return id;
    }

    public void sincronizar(String[] clientes) {
        try {
            Map<String, Long> tiempos = new HashMap<>();
            Map<String, Long> desfases = new HashMap<>();

            long tiempoServidor = getTimeMillis();
            tiempos.put(id, tiempoServidor);
            desfases.put(id, 0L);

            // Paso 2: solicitar tiempo a clientes
            for (String nombreCliente : clientes) {
                try {
                    long inicio = System.currentTimeMillis();
                    // Buscar el cliente por su nombre en el RMI registry (host: localhost, puerto: 1099)
                    ClockService cliente = (ClockService) Naming.lookup("rmi://" + InetAddress.getLocalHost().getHostAddress() + ":1099/" + nombreCliente);
                    long horaCliente = cliente.getTimeMillis();
                    long fin = System.currentTimeMillis();

                    long rtt = fin - inicio;
                    long horaAjustada = horaCliente + (rtt / 2);
                    tiempos.put(nombreCliente, horaAjustada);

                    long desfase = horaAjustada - tiempoServidor;
                    desfases.put(nombreCliente, desfase);

                } catch (Exception e) {
                    System.err.println("Error al contactar con " + nombreCliente + ": " + e);
                }
            }

            // Mostrar tabla de desfases
            System.out.println("\n=== TABLA DE ENTRADA Y DESFASE ===");
            for (String k : tiempos.keySet()) {
                System.out.println(k + "\tHora(ms): " + tiempos.get(k) + "\tDesfase(ms): " + desfases.get(k));
            }

            // Paso 4: calcular promedio de desfases
            double suma = 0;
            for (long d : desfases.values()) {
                suma += d;
            }
            double promedio = suma / desfases.size();

            System.out.println("\nPromedio de desfases: " + df.format(promedio) + " ms");

            // Paso 5: enviar ajustes
            System.out.println("\n=== TABLA DE PROMEDIO Y AJUSTE ===");
            for (String k : desfases.keySet()) {
                double ajuste = promedio - desfases.get(k);
                System.out.println(k + "\tDesfase: " + desfases.get(k) + "\tAjuste: " + df.format(ajuste));

                if (!k.equals(id)) {
                    // Buscar el cliente por su nombre en el RMI registry del servidor
                    ClockService cliente = (ClockService) Naming.lookup("rmi://" + InetAddress.getLocalHost().getHostAddress() + ":1099/" + k);
                    cliente.applyAdjustment((long) ajuste);
                } else {
                    applyAdjustment((long) ajuste);
                }
            }

            // Mostrar nueva hora del servidor
            System.out.println("\n=== TABLA DE NUEVA HORA ===");
            long nuevaHoraServidor = System.currentTimeMillis() + offsetMillis;
            System.out.println("Servidor\tAjuste(ms): " + df.format(promedio - 0) +
                    "\tNueva hora: " + TimeUtils.fmt(nuevaHoraServidor));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            ClockServer server = new ClockServer();

            // Detectar IP real de la máquina
            String hostAddress = InetAddress.getLocalHost().getHostAddress();
            int port = 1099;

            // Crear registro RMI en el puerto y registrar el servidor
            LocateRegistry.createRegistry(port);
            String url = "rmi://" + hostAddress + ":" + port + "/ClockServer";
            Naming.rebind(url, server);
            System.out.println("Servidor registrado en RMIRegistry como 'ClockServer'");
            System.out.println("✅ URL de conexión para clientes: " + url);

            if (args.length == 0) {
                System.err.println("Debe proporcionar los nombres de los clientes como argumentos.");
                System.exit(1);
            }

            // Esperar hasta que los clientes se registren en el RMI registry (con timeout)
            try {
                java.rmi.registry.Registry registry = LocateRegistry.getRegistry(hostAddress, port);
                final int timeoutMs = 30_000; // 30 segundos timeout
                final int intervalMs = 500; // chequear cada 500ms
                int waited = 0;

                boolean allBound = false;
                while (waited < timeoutMs) {
                    try {
                        String[] bound = registry.list();
                        allBound = true;
                        for (String cliente : args) {
                            boolean found = false;
                            for (String b : bound) {
                                if (b.equals(cliente)) { found = true; break; }
                            }
                            if (!found) { allBound = false; break; }
                        }
                        if (allBound) break;
                    } catch (RemoteException re) {
                        // puede fallar temporalmente, se reintentará
                    }
                    try { Thread.sleep(intervalMs); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                    waited += intervalMs;
                }

                if (!allBound) {
                    System.err.println("Advertencia: no todos los clientes se registraron en el tiempo de espera. Procediendo con los que sí están.");
                } else {
                    System.out.println("Todos los clientes reportados están registrados. Iniciando sincronización.");
                }
            } catch (Exception e) {
                // Si hay algún error consultando el registry, seguimos adelante y dejamos que sincronizar maneje NotBoundException.
                System.err.println("Aviso: no se pudo comprobar el registro de clientes: " + e.getMessage());
            }

            server.sincronizar(args);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
