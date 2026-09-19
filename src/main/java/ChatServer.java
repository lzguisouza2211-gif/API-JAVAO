import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.util.*;
import java.util.Scanner;
import java.io.*;
import java.util.UUID;

public class ChatServer extends WebSocketServer {
    private static final String UPLOAD_DIR = "./arquivos";
    private Map<WebSocket, String> clientTokens = new HashMap<>();
    private Map<String, WebSocket> tokenToClient = new HashMap<>();
    private HttpServer httpServer;

    public ChatServer(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        String token = UUID.randomUUID().toString();
        clientTokens.put(conn, token);
        tokenToClient.put(token, conn);

        System.out.println("[SERVIDOR] Cliente conectado: " + conn.getRemoteSocketAddress());
        System.out.println("[SERVIDOR] Token: " + token);

        conn.send("[SYSTEM] Seu token: " + token);
        broadcast("[SISTEMA] Um novo cliente entrou no chat");
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("[SERVIDOR] Mensagem recebida: " + message);
        broadcast("[Cliente]: " + message);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String token = clientTokens.get(conn);
        if (token != null) {
            tokenToClient.remove(token);
            clientTokens.remove(conn);
        }
        System.out.println("[SERVIDOR] Cliente desconectado");
        broadcast("[SISTEMA] Um cliente saiu do chat");
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("[ERRO] " + ex.getMessage());
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("========================================");
        System.out.println("Servidor WebSocket iniciado na porta 8080");
        System.out.println("Servidor HTTP iniciado na porta 8081");
        System.out.println("Digite suas mensagens abaixo:");
        System.out.println("========================================");
    }

    public void startHttpServer() throws Exception {
        httpServer = HttpServer.create(new InetSocketAddress(8081), 0);

        // Endpoint para upload
        httpServer.createContext("/upload", exchange -> {
            if ("POST".equals(exchange.getRequestMethod())) {
                String token = exchange.getRequestHeaders().getFirst("Authorization");

                if (token == null || !tokenToClient.containsKey(token)) {
                    exchange.sendResponseHeaders(401, 0);
                    exchange.close();
                    return;
                }

                try {
                    // Cria pasta se não existir
                    Files.createDirectories(Paths.get(UPLOAD_DIR));

                    String filename = UUID.randomUUID().toString();
                    Path filepath = Paths.get(UPLOAD_DIR, filename);

                    InputStream is = exchange.getRequestBody();
                    Files.copy(is, filepath, StandardCopyOption.REPLACE_EXISTING);

                    WebSocket client = tokenToClient.get(token);
                    client.send("[SYSTEM] Arquivo enviado: " + filename);
                    broadcast("[SISTEMA] Um arquivo foi enviado!");

                    exchange.sendResponseHeaders(200, 0);
                    exchange.getResponseBody().write("OK".getBytes());
                    exchange.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    exchange.sendResponseHeaders(500, 0);
                    exchange.close();
                }
            } else {
                exchange.sendResponseHeaders(405, 0);
                exchange.close();
            }
        });

        // Endpoint para download
        httpServer.createContext("/download", exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) {
                String query = exchange.getRequestURI().getQuery();
                String token = null;
                String filename = null;

                if (query != null) {
                    String[] params = query.split("&");
                    for (String param : params) {
                        if (param.startsWith("token=")) {
                            token = param.substring(6);
                        }
                        if (param.startsWith("file=")) {
                            filename = param.substring(5);
                        }
                    }
                }

                if (token == null || !tokenToClient.containsKey(token) || filename == null) {
                    exchange.sendResponseHeaders(401, 0);
                    exchange.close();
                    return;
                }

                try {
                    Path filepath = Paths.get(UPLOAD_DIR, filename);

                    if (!Files.exists(filepath)) {
                        exchange.sendResponseHeaders(404, 0);
                        exchange.close();
                        return;
                    }

                    byte[] fileBytes = Files.readAllBytes(filepath);
                    exchange.sendResponseHeaders(200, fileBytes.length);
                    exchange.getResponseBody().write(fileBytes);
                    exchange.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    exchange.sendResponseHeaders(500, 0);
                    exchange.close();
                }
            } else {
                exchange.sendResponseHeaders(405, 0);
                exchange.close();
            }
        });

        httpServer.setExecutor(null);
        httpServer.start();
    }

    public static void main(String[] args) {
        try {
            ChatServer server = new ChatServer(8080);
            server.start();
            server.startHttpServer();

            Scanner scanner = new Scanner(System.in);
            System.out.print("Servidor: ");

            while (true) {
                String msg = scanner.nextLine();
                if (msg.equals("sair")) {
                    server.stop();
                    if (server.httpServer != null) {
                        server.httpServer.stop(0);
                    }
                    break;
                }
                server.broadcast(msg);
                System.out.print("Servidor: ");
            }
        } catch (Exception e) {
            System.err.println("Erro no servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}