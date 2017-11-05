import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

import static java.lang.System.err;
import static java.lang.System.out;

public class ConfigurationValuesService extends NanoHTTPD  {
    private final ConfigurationGetter getter;
    private final ConfigurationEditor editor;
    private final ConfigurationAdministration administration;
    private final ObjectMapper mapper;
    private Thread serverThread;

    private ConfigurationValuesService(String adminPassword, String connectionUrl, int port) {
        super(port);
        SqlDatabase database = new SqlDatabase(connectionUrl);
        if(!database.canConnect())
            err.println("Failed to connect to database at \"" + connectionUrl + "\"" +
                "\r\nIs the database offline, wrong port?");
        Authenticator auth = new Authenticator(adminPassword);
        getter = new ConfigurationGetter(database);
        editor = new ConfigurationEditor(auth, database);
        administration = new ConfigurationAdministration(auth, database);
        mapper = new ObjectMapper();
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    public void start() {
        serverThread = new Thread(() -> {
            try {
                start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            } catch (IOException ex) {
                err.println("Failed to start, port in use? Admin?");
                System.exit(1);
            }
        } );
        serverThread.start();
    }

    @Override
    public Response serve(IHTTPSession session) {
        String method = String.valueOf(session.getMethod());
        String uri = session.getUri();
        Map<String, String> params = session.getParms();

        if (Pattern.compile("\\/?").matcher(uri).matches() && params.containsKey("token")) {
            if (method.equals("GET")) {
                return newJsonResponse(getter.get(session));
            } else if (method.equals("PUT")) {
                return newJsonResponse(editor.setValues(session));
            } else if (method.equals("DELETE")) {
                return newJsonResponse(editor.clearValues(session));
            }
        } else if (Pattern.compile("\\/?").matcher(uri).matches()) {
            if (method.equals("GET")) {
                return newJsonResponse(administration.getApps(session));
            }
        } else if (Pattern.compile("\\/[^\\/]+\\/?").matcher(uri).matches()){
            if (method.equals("GET")) {
                return newJsonResponse(administration.getConfigs(session));
            } else if (method.equals("PUT")) {
                return newJsonResponse(administration.addApp(session));
            } else if (method.equals("DELETE")) {
                return newJsonResponse(administration.removeApp(session));
            }
        } else if (Pattern.compile("(\\/[^\\/]+){2}\\/?").matcher(uri).matches()){
            if (method.equals("GET")) {
                return newJsonResponse(administration.getToken(session));
            } else if (method.equals("PUT")) {
                return newJsonResponse(administration.addConfig(session));
            } else if (method.equals("DELETE")) {
                return newJsonResponse(administration.removeConfig(session));
            }
        }

        return newJsonResponse(new ResponseWrapper(404, "DoesNotExistException",
            "Resources can never be accessed by that url and method combination"));
    }

    public Response newJsonResponse(ResponseWrapper obj) {
        try {
            return newFixedLengthResponse(Response.Status.OK, "application/json",
                mapper.writeValueAsString(obj));
        } catch (IOException ex) {
            err.println("This should never occur. Unexpected #2");
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", "null");
        }
    }

    public static ConfigurationValuesService create(String adminPassword, String connectionString, int port) {
        ConfigurationValuesService x = new ConfigurationValuesService(adminPassword, connectionString, port);
        x.start();
        return x;
    }

    @Override
    protected void finalize() throws Throwable {
        serverThread.interrupt();
    }
}
