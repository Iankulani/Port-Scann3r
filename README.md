# Port-Scann3r
Port-Scann3r



# Example Output:

text
==========================================
      PORT SCANNER TOOL
==========================================
Target IP: 192.168.1.1
Port Range: 1 - 65535
Timeout: 200ms
Max Threads: 100
==========================================
Starting scan...

==========================================
           SCAN RESULTS
==========================================

[OPEN PORTS] (5 found)
22 80 443 3306 8080 

[CLOSED PORTS] (65530 found)
1 2 3 4 5 6 7 8 9 10 
... and 65520 more closed ports

[COMMON SERVICES]
  Port 22    -> SSH
  Port 80    -> HTTP
  Port 443   -> HTTPS
  Port 3306  -> MySQL
  Port 8080  -> HTTP Proxy

==========================================
           STATISTICS
==========================================
Total ports scanned: 65535
Open ports: 5
Closed ports: 65530
Scan duration: 12.345 seconds
Average scan time per port: 0.188 ms
==========================================

# Performance considerations:

The scanner uses connection timeouts to quickly identify closed ports

Thread pool size can be adjusted based on your system capabilities

Maximum threads capped at 500 to prevent system overload

# Compile the program:

```bash
javac PortScanner.java
Run with basic usage:
```

bash
java PortScanner 192.168.1.1
Scan specific port range:

```bash
java PortScanner 127.0.0.1 -p 1-1000
Custom timeout and threads:
```
```bash
java PortScanner 8.8.8.8 -p 80,443 -t 500 -th 50
Export results to file:
```
```bash
java PortScanner 192.168.1.100 -export scan_results.txt
```


# 🌟 Star History


