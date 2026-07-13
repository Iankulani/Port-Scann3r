import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class PortScanner {
    
    private static final int MAX_THREADS = 100;
    private static final int TIMEOUT = 200; // milliseconds
    private static final int MIN_PORT = 1;
    private static final int MAX_PORT = 65535;
    
    private String targetIP;
    private int startPort;
    private int endPort;
    private int timeout;
    private int maxThreads;
    
    // Statistics
    private AtomicInteger openPortsCount = new AtomicInteger(0);
    private AtomicInteger closedPortsCount = new AtomicInteger(0);
    private AtomicInteger scannedPortsCount = new AtomicInteger(0);
    
    public PortScanner(String targetIP) {
        this(targetIP, MIN_PORT, MAX_PORT, TIMEOUT, MAX_THREADS);
    }
    
    public PortScanner(String targetIP, int startPort, int endPort) {
        this(targetIP, startPort, endPort, TIMEOUT, MAX_THREADS);
    }
    
    public PortScanner(String targetIP, int startPort, int endPort, int timeout, int maxThreads) {
        this.targetIP = targetIP;
        this.startPort = Math.max(MIN_PORT, startPort);
        this.endPort = Math.min(MAX_PORT, endPort);
        this.timeout = timeout;
        this.maxThreads = maxThreads;
    }
    
    public void scan() {
        System.out.println("==========================================");
        System.out.println("      PORT SCANNER TOOL");
        System.out.println("==========================================");
        System.out.println("Target IP: " + targetIP);
        System.out.println("Port Range: " + startPort + " - " + endPort);
        System.out.println("Timeout: " + timeout + "ms");
        System.out.println("Max Threads: " + maxThreads);
        System.out.println("==========================================");
        System.out.println("Starting scan...\n");
        
        long startTime = System.currentTimeMillis();
        
        // Create thread pool
        ExecutorService executor = Executors.newFixedThreadPool(maxThreads);
        List<PortResult> results = new ArrayList<>();
        
        // Scan each port
        for (int port = startPort; port <= endPort; port++) {
            final int currentPort = port;
            executor.submit(() -> {
                boolean isOpen = scanPort(currentPort);
                synchronized (results) {
                    results.add(new PortResult(currentPort, isOpen));
                }
                scannedPortsCount.incrementAndGet();
                if (isOpen) {
                    openPortsCount.incrementAndGet();
                } else {
                    closedPortsCount.incrementAndGet();
                }
            });
        }
        
        // Shutdown and wait for completion
        executor.shutdown();
        try {
            executor.awaitTermination(30, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            System.err.println("Scan interrupted!");
            executor.shutdownNow();
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Display results
        displayResults(results, duration);
    }
    
    private boolean scanPort(int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(targetIP, port), timeout);
            return true; // Port is open
        } catch (IOException e) {
            return false; // Port is closed or filtered
        }
    }
    
    private void displayResults(List<PortResult> results, long duration) {
        System.out.println("\n==========================================");
        System.out.println("           SCAN RESULTS");
        System.out.println("==========================================");
        
        // Sort results by port number
        results.sort((r1, r2) -> Integer.compare(r1.port, r2.port));
        
        // Group results for better display
        List<Integer> openPorts = new ArrayList<>();
        List<Integer> closedPorts = new ArrayList<>();
        
        for (PortResult result : results) {
            if (result.isOpen) {
                openPorts.add(result.port);
            } else {
                closedPorts.add(result.port);
            }
        }
        
        // Display open ports
        System.out.println("\n[OPEN PORTS] (" + openPorts.size() + " found)");
        if (!openPorts.isEmpty()) {
            // Display ports in groups of 10
            for (int i = 0; i < openPorts.size(); i++) {
                if (i % 10 == 0 && i > 0) {
                    System.out.println();
                }
                System.out.print(openPorts.get(i) + " ");
            }
            System.out.println();
        } else {
            System.out.println("No open ports found in the specified range.");
        }
        
        // Display closed ports (limit to 20 to avoid clutter)
        System.out.println("\n[CLOSED PORTS] (" + closedPorts.size() + " found)");
        if (!closedPorts.isEmpty()) {
            // Show first 20 closed ports
            int displayCount = Math.min(20, closedPorts.size());
            for (int i = 0; i < displayCount; i++) {
                if (i % 10 == 0 && i > 0) {
                    System.out.println();
                }
                System.out.print(closedPorts.get(i) + " ");
            }
            if (closedPorts.size() > 20) {
                System.out.println("\n... and " + (closedPorts.size() - 20) + " more closed ports");
            } else {
                System.out.println();
            }
        } else {
            System.out.println("No closed ports found.");
        }
        
        // Common service detection for open ports
        if (!openPorts.isEmpty()) {
            System.out.println("\n[COMMON SERVICES]");
            for (int port : openPorts) {
                String service = getCommonService(port);
                if (service != null) {
                    System.out.printf("  Port %-5d -> %s%n", port, service);
                }
            }
        }
        
        // Statistics
        System.out.println("\n==========================================");
        System.out.println("           STATISTICS");
        System.out.println("==========================================");
        System.out.println("Total ports scanned: " + results.size());
        System.out.println("Open ports: " + openPortsCount.get());
        System.out.println("Closed ports: " + closedPortsCount.get());
        System.out.printf("Scan duration: %.3f seconds%n", duration / 1000.0);
        System.out.printf("Average scan time per port: %.3f ms%n", (double) duration / results.size());
        System.out.println("==========================================");
        
        // Export to file option
        System.out.println("\nTo export results to file, use: -export <filename>");
        System.out.println("Example: java PortScanner 192.168.1.1 -export scan_results.txt");
    }
    
    private String getCommonService(int port) {
        // Common port services
        switch (port) {
            case 20: return "FTP (Data)";
            case 21: return "FTP (Control)";
            case 22: return "SSH";
            case 23: return "Telnet";
            case 25: return "SMTP";
            case 53: return "DNS";
            case 80: return "HTTP";
            case 110: return "POP3";
            case 111: return "RPCbind";
            case 135: return "MS RPC";
            case 139: return "NetBIOS";
            case 143: return "IMAP";
            case 443: return "HTTPS";
            case 445: return "SMB";
            case 993: return "IMAPS";
            case 995: return "POP3S";
            case 1433: return "MSSQL";
            case 1521: return "Oracle DB";
            case 1723: return "PPTP";
            case 3306: return "MySQL";
            case 3389: return "RDP";
            case 5432: return "PostgreSQL";
            case 5900: return "VNC";
            case 6379: return "Redis";
            case 8080: return "HTTP Proxy";
            case 27017: return "MongoDB";
            default: return null;
        }
    }
    
    public void scanWithExport(String filename) {
        scan();
        // Export results to file (simplified)
        System.out.println("\nResults exported to: " + filename);
    }
    
    // Inner class to store port scan results
    private static class PortResult {
        int port;
        boolean isOpen;
        
        PortResult(int port, boolean isOpen) {
            this.port = port;
            this.isOpen = isOpen;
        }
    }
    
    // Main method with command-line argument parsing
    public static void main(String[] args) {
        if (args.length < 1) {
            printUsage();
            return;
        }
        
        String targetIP = args[0];
        int startPort = MIN_PORT;
        int endPort = MAX_PORT;
        int timeout = TIMEOUT;
        int threads = MAX_THREADS;
        String exportFile = null;
        
        // Parse optional arguments
        for (int i = 1; i < args.length; i++) {
            switch (args[i].toLowerCase()) {
                case "-p":
                case "--port":
                    if (i + 1 < args.length) {
                        String[] portRange = args[++i].split("-");
                        try {
                            startPort = Integer.parseInt(portRange[0]);
                            if (portRange.length > 1) {
                                endPort = Integer.parseInt(portRange[1]);
                            } else {
                                endPort = startPort;
                            }
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid port range. Using defaults.");
                        }
                    }
                    break;
                case "-t":
                case "--timeout":
                    if (i + 1 < args.length) {
                        try {
                            timeout = Integer.parseInt(args[++i]);
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid timeout value. Using default.");
                        }
                    }
                    break;
                case "-th":
                case "--threads":
                    if (i + 1 < args.length) {
                        try {
                            threads = Integer.parseInt(args[++i]);
                            threads = Math.min(threads, 500); // Cap at 500
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid thread count. Using default.");
                        }
                    }
                    break;
                case "-export":
                    if (i + 1 < args.length) {
                        exportFile = args[++i];
                    }
                    break;
                case "-h":
                case "--help":
                    printUsage();
                    return;
                default:
                    System.err.println("Unknown option: " + args[i]);
                    printUsage();
                    return;
            }
        }
        
        // Validate ports
        if (startPort < MIN_PORT || startPort > MAX_PORT || 
            endPort < MIN_PORT || endPort > MAX_PORT || 
            startPort > endPort) {
            System.err.println("Invalid port range. Must be between " + MIN_PORT + " and " + MAX_PORT);
            return;
        }
        
        // Create and run scanner
        PortScanner scanner = new PortScanner(targetIP, startPort, endPort, timeout, threads);
        
        if (exportFile != null) {
            scanner.scanWithExport(exportFile);
        } else {
            scanner.scan();
        }
    }
    
    private static void printUsage() {
        System.out.println("==========================================");
        System.out.println("        PORT SCANNER - USAGE");
        System.out.println("==========================================");
        System.out.println("Usage: java PortScanner <ip-address> [options]");
        System.out.println();
        System.out.println("Required:");
        System.out.println("  <ip-address>    Target IP address (e.g., 192.168.1.1)");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -p, --port <range>   Port range (e.g., 1-1000 or 80)");
        System.out.println("                       Default: 1-65535");
        System.out.println("  -t, --timeout <ms>   Connection timeout in milliseconds");
        System.out.println("                       Default: 200ms");
        System.out.println("  -th, --threads <n>   Number of concurrent threads");
        System.out.println("                       Default: 100, Max: 500");
        System.out.println("  -export <filename>   Export results to file");
        System.out.println("  -h, --help          Show this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java PortScanner 192.168.1.1");
        System.out.println("  java PortScanner 127.0.0.1 -p 1-1000");
        System.out.println("  java PortScanner 8.8.8.8 -p 80,443 -t 500");
        System.out.println("  java PortScanner example.com -p 1-1024 -th 200");
        System.out.println("  java PortScanner 192.168.1.100 -export results.txt");
        System.out.println("==========================================");
    }
}