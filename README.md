# Distributed System Experiments (Java)

Six lab experiments covering core distributed systems concepts with pure Java socket programming, RMI, and HTTP.

- **Exp 1** — UDP Datagram Chat: multi-user chat with sender name, timestamp, and message display
- **Exp 2** — TCP Stream Socket Chat: `MyStreamSocket` wrapper with image/file transfer support
- **Exp 3** — Daytime & Echo Services: both UDP and TCP implementations
- **Exp 4** — RMI Student Score System: full CRUD with CSV persistence
- **Exp 5** — HTTP Client via Raw Socket: GET/POST with Swing browser UI
- **Exp 6** — Simple Web Server: static file serving + CGI (GET/POST)

## Prerequisites

- Windows
- JDK 8+

## Build

From the project root:

```bash
javac -encoding UTF-8 -d out src/**/*.java
```

If PowerShell glob `**` does not expand, use:

```bash
Get-ChildItem -Recurse .\src\*.java | ForEach-Object { $_.FullName } | Set-Content sources.txt
javac --% -encoding UTF-8 -d out @sources.txt
```

## Running

> All classes are under the `exp` package.

### Dashboard GUI (Recommended)

Launch all six experiments from a single control panel:

```bash
java -cp out exp.gui.ExperimentDashboard
```

Features:

- Experiment navigation on the left, overview and commands on the right
- One-click server/client launch buttons
- Built-in live log panel
- Closing the window or clicking `Stop All Processes` terminates all child processes

### Exp 1: UDP Multi-User Chat

1. Start the server (port 9000):

```bash
java -cp out exp.exp1.udpchat.UdpChatServer 9000
```

2. Start multiple clients:

```bash
java -cp out exp.exp1.udpchat.UdpChatClient 127.0.0.1 9000 Alice 10001
java -cp out exp.exp1.udpchat.UdpChatClient 127.0.0.1 9000 Bob 10002
```

Type a message and press Enter to send.

### Exp 2: TCP Stream Socket Chat

1. Start the server (port 9100):

```bash
java -cp out exp.exp2.streamchat.StreamServer 9100
```

2. Start a client:

```bash
java -cp out exp.exp2.streamchat.StreamClient 127.0.0.1 9100 Alice
```

Client commands:

- Text message: type directly
- Send image: `/img <absolute-path>`
- Send file: `/file <absolute-path>`
- Quit: `/quit`

### Exp 3: Daytime & Echo

UDP Daytime:

```bash
java -cp out exp.exp3.daytime.udp.DaytimeUdpServer 9200
java -cp out exp.exp3.daytime.udp.DaytimeUdpClient 127.0.0.1 9200
```

TCP Daytime:

```bash
java -cp out exp.exp3.daytime.tcp.DaytimeTcpServer 9201
java -cp out exp.exp3.daytime.tcp.DaytimeTcpClient 127.0.0.1 9201
```

UDP Echo:

```bash
java -cp out exp.exp3.echo.udp.EchoUdpServer 9300
java -cp out exp.exp3.echo.udp.EchoUdpClient 127.0.0.1 9300
```

TCP Echo:

```bash
java -cp out exp.exp3.echo.tcp.EchoTcpServer 9301
java -cp out exp.exp3.echo.tcp.EchoTcpClient 127.0.0.1 9301
```

### Exp 4: RMI Student Score System

1. Start the RMI server (default port 1099, data file `data/student_scores.csv`):

```bash
java -cp out exp.exp4.rmi.server.RmiStudentServer 1099 data/student_scores.csv
```

2. Start a client:

```bash
java -cp out exp.exp4.rmi.client.RmiStudentClient 127.0.0.1 1099
```

Client commands:

- `add <id> <name> <chinese> <math> <english>`
- `update <id> <name> <chinese> <math> <english>`
- `delete <id>`
- `get <id>`
- `list`

### Exp 5: HTTP Socket Client

Launch the GUI browser:

```bash
java -cp out exp.exp5.httpclient.HttpSocketBrowserUI
```

- Supports `GET` and `POST` (`application/x-www-form-urlencoded`)
- Displays response headers and body in a split view

### Exp 6: Simple Web Server

1. Start the server (default port 8080, document root `wwwroot`):

```bash
java -cp out exp.exp6.webserver.SimpleWebServer 8080 wwwroot
```

2. Open in a browser:

- `http://127.0.0.1:8080/` (static page)
- `http://127.0.0.1:8080/cgi/echo?name=alice&score=95` (CGI GET)

3. CGI POST example (PowerShell):

```powershell
Invoke-WebRequest -Uri http://127.0.0.1:8080/cgi/echo -Method Post -Body "name=bob&score=100"
```

## Notes

- UDP examples use `SoTimeout` to avoid indefinite blocking.
- Chat programs use a dedicated receiver thread alongside the sender thread for full-duplex communication.
- Image/file transfer is Base64-encoded over the text stream protocol.
- The RMI server creates/connects to the `Registry` and binds the remote object as `StudentScoreService`.
- Exp 6 CGI invokes external Java programs via `ProcessBuilder`.
