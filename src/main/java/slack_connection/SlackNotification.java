package slack_connection;

import org.json.JSONObject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class SlackNotification {
    private static HttpClient slackClient = HttpClient.newHttpClient();
    private static final String url = "https://hooks.slack.com/services/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";

    public static void enviarMensagem(JSONObject content) throws IOException,InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
        .header("accept", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(content.toString())).build();
        HttpResponse<String> response = slackClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
