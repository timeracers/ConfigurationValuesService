import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static java.lang.System.*;

public class Main {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final java.io.BufferedReader In = new BufferedReader(new InputStreamReader(System.in));

    public static void main(String[] args) throws java.io.IOException {
        tryStartServer(args);
        while (true) {
            String command = In.readLine().toUpperCase();
            if(command.equals("EXIT") || command.equals("QUIT"))
                System.exit(0);
            else
                out.println("Type EXIT or QUIT to end this program.");
        }
    }

    private static void tryStartServer(String[] args) {
        String adminPassword = null;
        String connectionUrl = null;
        int port = -1;

        if(args.length == 0){
            out.println("Since no arguments were provided this will use the default config file location.");
            try {
                args = new String[] { new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath())
                    .getParent() + "\\config.json" };
            } catch (Exception ex) {
                err.println("This should never occur. Unexpected #1");
            }
        }
        if(args.length == 1){
            out.println("Using config file at " + args[0]);
            try {
                JsonNode root = mapper.readTree(new File(args[0]));

                if(root.get("password").isTextual()) {
                    adminPassword = root.get("password").textValue();
                } else {
                    err.println("\"password\" is required and must be a string");
                }

                if(root.get("connectionUrl").isTextual()) {
                    connectionUrl = root.get("connectionUrl").textValue();
                } else {
                    err.println("\"connectionUrl\" is required and must be a string");
                }

                if(root.hasNonNull("port")) {
                    if(root.get("port").isInt()) {
                        port = root.get("port").intValue();
                    } else {
                        err.println("\"port\" is required to be an integer");
                    }
                } else {
                    out.println("Using default hyper text transfer protocol port (80)");
                    port = 80;
                }
            } catch (JsonProcessingException ex) {
                err.println("Invalid Json");
            } catch (java.io.IOException ex) {
                err.println("File does not exist, or was otherwise unreadable");
            }
        } else {
            err.println("Invalid amount of arguments to start service.\r\n" + "");
        }
        if(adminPassword == null || connectionUrl == null || port == -1)
            System.exit(1);

        ConfigurationValuesService.create(adminPassword, connectionUrl, port);
        out.println("Successful Startup");
    }
}
