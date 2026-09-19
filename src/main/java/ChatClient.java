import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Scanner;

public class ChatClient extends WebSocketClient {
    public ChatClient(URI serverUri) {
        super(serverUri);
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.println("========================================");
        System.out.println("Conectado ao servidor!");
        System.out.println("Digite suas mensagens abaixo:");
        System.out.println("========================================");
    }

    @Override
    public void onMessage(String message) {
        System.out.println("\n[Enviado]: " + message);
        System.out.print("Você: ");
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Desconectado do servidor");
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("[ERRO] " + ex.getMessage());
        ex.printStackTrace();
    }

    public static void main(String[] args) throws URISyntaxException, InterruptedException {
        String serverIp = "192.168.1.6";
        String wsUrl = "ws://" + serverIp + ":8080";

        System.out.println("Tentando conectar em: " + wsUrl);

        ChatClient client = new ChatClient(new URI(wsUrl));

        if (!client.connectBlocking()) {
            System.err.println("Falha ao conectar!");
            return;
        }

        Scanner scanner = new Scanner(System.in);
        System.out.print("Você: ");

        while (true) {
            String msg = scanner.nextLine();
            if (msg.equals("sair")) {
                client.close();
                break;
            }
            client.send(msg);
        }
    }
}