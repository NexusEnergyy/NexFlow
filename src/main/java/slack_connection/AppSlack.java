package slack_connection;

import org.json.JSONObject;
import java.io.IOException;

public class AppSlack {
    public static void main(String[] args) throws IOException,InterruptedException {
        JSONObject json = new JSONObject();

        json.put("text", "Deus é bom ✝ , e o diabo não presta \uD83D\uDC4E");
        SlackNotification.enviarMensagem(json);

    }
}
