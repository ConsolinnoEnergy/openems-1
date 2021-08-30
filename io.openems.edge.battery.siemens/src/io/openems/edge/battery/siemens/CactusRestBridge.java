package io.openems.edge.battery.siemens;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * This is the custom Rest Bridge for the Cactus API that is being used by the Siemens JuneLight.
 */
public class CactusRestBridge {
    private final URL url;
    private final String token;
    private final String projectId;
    private final String productId;
    private final String serial;


    CactusRestBridge(String tenant_Id, String token, String projectId, String productId, String serial) throws MalformedURLException {
        this.url = new URL("https://v1.api.04fe8.3ec34.cactus.siemens.cloud/data/tenant/" + tenant_Id + "/graphql");
        this.token = token;
        this.projectId = projectId;
        this.productId = productId;
        this.serial = serial;
    }

    /**
     * Pings the Cloud.
     *
     * @return true if connection is present
     */
    boolean checkConnection() {
        try {
            HttpURLConnection connection = (HttpURLConnection) this.url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
            connection.setRequestProperty("Authorization", "Bearer " + this.token);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();
            return responseCode == HttpURLConnection.HTTP_OK;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Make a new Rest request to the Battery Cloud. The Curl Request is extremely bad but Cactus API has forced my hand.
     *
     * @param app_name Name of the Folder the Data is in the Cloud
     * @return All the content of the folder
     * @throws IOException This shouldn't happen
     */
    String makeReadRequest(String app_name) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) this.url.openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
        connection.setRequestProperty("Authorization", "Bearer " + this.token);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        OutputStream os = connection.getOutputStream();
        String curlRequest = "{\"query\":\"query {\\n device(\\n project: \\\""
                + this.projectId + "\\\"\\n product: \\\"" + this.productId + "\\\"\\n serial: \\\""
                + this.serial + "\\\"\\n ) {\\n shadow: shadow(app: \\\"" + app_name + "\\\")\\n }\\n}\\n\"}";
        os.write(curlRequest.getBytes());
        os.flush();
        os.close();
        //Task can check if everythings ok --> good for Controller etc; ---> Check Channel
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            return response.toString();
        }
        return "Error";

    }

    /**
     * Make a write request to the cloud.
     *
     * @param app_name  name of the folder on the cloud
     * @param parameter parameter that has to be set
     * @return response
     * @throws IOException This shouldn't happen
     */
    String makeWriteRequest(String app_name, String parameter) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) this.url.openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
        connection.setRequestProperty("Authorization", "Bearer " + this.token);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        String jsonInputString = "{\"query\":\"query {\\n device(\\n project: \\\""
                + this.projectId + "\\\"\\n product: \\\"" + this.productId + "\\\"\\n serial: \\\""
                + this.serial + "\\\"\\n )"
                + "{"
                + "event(payload: \\\"" + this.getPayload(app_name, parameter) + "\\\") }\\n}\\n\"}";
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
            os.flush();
        }

        //Response
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            return response.toString();
        }
        return "Error";


    }

    /**
     * Create Payload String for write Request.
     * Note: This is dumb. But Cactus Data Api wants this exactly this way. None of this is correctly documented by the way. Too bad.
     *
     * @param app_name  Name of the Folder on the Cloud
     * @param parameter Parameter that has to be set
     * @return stringified Payload
     */
    private String getPayload(String app_name, String parameter) {
        return "[\\\\n {\\\\n \\\\\\\"app\\\\\\\": \\\\\\\"" + app_name + "\\\\\\\",\\\\n\\\\\\\"version\\\\\\\":"
                + " \\\\\\\"1.0\\\\\\\",\\\\n \\\\\\\"parameters\\\\\\\": {\\\\n\\\\" + parameter + "\\\\n }\\\\n }\\\\n]";

    }

    /**
     * This is a helper method to create the neccessary Parameter String.
     *
     * @param parameters Parameters given by the Battery.
     * @return ParamterString for the request.
     */
    public String createParameterString(List<EventParameter> parameters) {
        int size = parameters.size();
        int counter = 1;
        String list = "\\\"" + parameters.get(0).toString();
        while (counter < size) {
            list += ",\\\\n \\\\\\\"" + parameters.get(counter).toString();
            counter++;
        }
        return list;
    }
}
