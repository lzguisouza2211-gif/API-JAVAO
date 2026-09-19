import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import java.net.InetSocketAddress;

public class ChatServer extends WebSocketServer {
    public ChatServer(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("[SERVIDOR] Cliente conectado: " + conn.getRemoteSocketAddress());
        broadcast("[SISTEMA] Um novo cliente entrou no chat");
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("[SERVIDOR] Mensagem recebida: " + message);
        broadcast("[Cliente]: " + message);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
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
        System.out.println("Aguardando conexões...");
        System.out.println("========================================");
    }

    public static void main(String[] args) {
        ChatServer server = new ChatServer(8080);
        server.start();
    }
}