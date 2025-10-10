package server;

import java.net.InetAddress;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import remote.ClockService;
import util.TimeUtils;

/**
 * Servidor Coordinador del algoritmo de sincronización de reloj (tipo Berkeley).
 * Arquitectura mejorada con registro explícito de clientes.
 */
public class ClockServer extends UnicastRemoteObject implements ClockService {
    private final String id = "server";
    private long offsetMillis = 0;
    private final DecimalFormat df = new DecimalFormat("0.00");
    private final Map<String, ClockService> clientesRegistrados = new ConcurrentHashMap<>();

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
    
    @Override
    public boolean isAlive() throws RemoteException {
        return true;
    }
    
    // ⭐ IMPLEMENTAR métodos del servidor
    @Override
    public void registrarCliente(String clientId, ClockService cliente) throws RemoteException {
        clientesRegistrados.put(clientId, cliente);
        System.out.println("✅ Cliente registrado: " + clientId + " (Total: " + clientesRegistrados.size() + ")");
    }
    
    @Override
    public void iniciarSincronizacion() throws RemoteException {
        System.out.println("\n🎯 Iniciando sincronización por solicitud...");
        sincronizar();
    }

    // Método de sincronización (igual que antes)
    public void sincronizar() {
        try {
            if (clientesRegistrados.isEmpty()) {
                System.out.println("⚠️  No hay clientes registrados para sincronizar.");
                return;
            }

            Map<String, Long> tiempos = new java.util.HashMap<>();
            Map<String, Long> desfases = new java.util.HashMap<>();

            long tiempoServidor = getTimeMillis();
            tiempos.put(id, tiempoServidor);
            desfases.put(id, 0L);

            // Solicitar tiempo a clientes registrados
            for (Map.Entry<String, ClockService> entry : clientesRegistrados.entrySet()) {
                String clientId = entry.getKey();
                ClockService cliente = entry.getValue();
                
                try {
                    if (!cliente.isAlive()) {
                        System.err.println("Cliente " + clientId + " no responde, removiendo...");
                        clientesRegistrados.remove(clientId);
                        continue;
                    }
                    
                    long inicio = System.currentTimeMillis();
                    long horaCliente = cliente.getTimeMillis();
                    long fin = System.currentTimeMillis();

                    long rtt = fin - inicio;
                    long horaAjustada = horaCliente + (rtt / 2);
                    tiempos.put(clientId, horaAjustada);

                    long desfase = horaAjustada - tiempoServidor;
                    desfases.put(clientId, desfase);

                    System.out.println("📡 " + clientId + " - Hora: " + TimeUtils.fmt(horaCliente) + 
                                     ", RTT: " + rtt + "ms, Desfase: " + desfase + "ms");

                } catch (Exception e) {
                    System.err.println("❌ Error al contactar con " + clientId + ": " + e.getMessage());
                    clientesRegistrados.remove(clientId);
                }
            }

            // Mostrar tabla de desfases
            System.out.println("\n=== TABLA DE ENTRADA Y DESFASE ===");
            for (String k : tiempos.keySet()) {
                System.out.println(k + "\tHora(ms): " + tiempos.get(k) + "\tDesfase(ms): " + desfases.get(k));
            }

            // Calcular promedio de desfases
            double suma = 0;
            for (long d : desfases.values()) {
                suma += d;
            }
            double promedio = suma / desfases.size();

            System.out.println("\n📊 Promedio de desfases: " + df.format(promedio) + " ms");

            // Enviar ajustes
            System.out.println("\n=== TABLA DE PROMEDIO Y AJUSTE ===");
            for (String k : desfases.keySet()) {
                double ajuste = promedio - desfases.get(k);
                System.out.println(k + "\tDesfase: " + desfases.get(k) + "\tAjuste: " + df.format(ajuste));

                if (!k.equals(id)) {
                    try {
                        ClockService cliente = clientesRegistrados.get(k);
                        if (cliente != null) {
                            cliente.applyAdjustment((long) ajuste);
                            System.out.println("✅ Ajuste enviado a " + k + ": " + df.format(ajuste) + "ms");
                        }
                    } catch (Exception e) {
                        System.err.println("❌ Error enviando ajuste a " + k + ": " + e.getMessage());
                    }
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

            String hostAddress = InetAddress.getLocalHost().getHostAddress();
            int port = 1099;

            LocateRegistry.createRegistry(port);
            String url = "rmi://" + hostAddress + ":" + port + "/ClockServer";
            Naming.rebind(url, server);
            System.out.println("✅ Servidor registrado en RMIRegistry como 'ClockServer'");
            System.out.println("📍 URL de conexión para clientes: " + url);
            System.out.println("👥 Esperando registro de clientes...");

            // Menu interactivo simple para administrar sincronizaciones y clientes
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
            String line = null;
            printMenuHelp();
            while (true) {
                System.out.print("[Servidor] > ");
                line = reader.readLine();
                if (line == null) break;
                String cmd = line.trim();
                if (cmd.isEmpty()) continue;

                try {
                    if (cmd.equalsIgnoreCase("help") || cmd.equalsIgnoreCase("h")) {
                        printMenuHelp();
                    } else if (cmd.equalsIgnoreCase("sync") || cmd.equalsIgnoreCase("s")) {
                        server.sincronizar();
                    } else if (cmd.equalsIgnoreCase("list") || cmd.equalsIgnoreCase("ls")) {
                        System.out.println("Clientes registrados: " + server.clientesRegistrados.size());
                        for (String k : server.clientesRegistrados.keySet()) {
                            System.out.println(" - " + k);
                        }
                    } else if (cmd.startsWith("remove ") || cmd.startsWith("rm ")) {
                        String[] parts = cmd.split("\\s+", 2);
                        if (parts.length > 1) {
                            String idToRemove = parts[1].trim();
                            server.clientesRegistrados.remove(idToRemove);
                            System.out.println("Cliente removido: " + idToRemove);
                        } else {
                            System.out.println("Uso: remove <clientId>");
                        }
                    } else if (cmd.equalsIgnoreCase("clear")) {
                        server.clientesRegistrados.clear();
                        System.out.println("Lista de clientes limpiada.");
                    } else if (cmd.equalsIgnoreCase("exit") || cmd.equalsIgnoreCase("quit") || cmd.equalsIgnoreCase("q")) {
                        System.out.println("Saliendo...");
                        break;
                    } else {
                        System.out.println("Comando desconocido. Escribe 'help' para ver opciones.");
                    }
                } catch (Exception ex) {
                    System.err.println("Error ejecutando comando: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }

            System.out.println("Servidor detenido.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void printMenuHelp() {
        System.out.println("Comandos disponibles:");
        System.out.println("  help|h        - Mostrar esta ayuda");
        System.out.println("  sync|s        - Iniciar sincronizacion ahora");
        System.out.println("  list|ls       - Listar clientes registrados");
        System.out.println("  remove|rm <id>- Remover cliente por id");
        System.out.println("  clear         - Limpiar lista de clientes");
        System.out.println("  exit|quit|q   - Salir del servidor");
    }
}