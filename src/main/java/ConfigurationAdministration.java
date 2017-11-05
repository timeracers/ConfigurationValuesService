import fi.iki.elonen.NanoHTTPD.IHTTPSession;

import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigurationAdministration {
    private final SqlDatabase database;
    private final Authenticator authenticator;
    private final SecureRandom rng = new SecureRandom();

    public ConfigurationAdministration(Authenticator authenticator, SqlDatabase database) {
        this.database = database;
        this.authenticator = authenticator;
    }

    public ResponseWrapper getApps(IHTTPSession session) {
        return authenticator.authenticated(Optional.ofNullable(session.getHeaders().get("authorization")), () -> {
            try {
                List<Map<String, Object>> results = database.query("SELECT name FROM configurations.apps");
                List<Object> output = new ArrayList<>();
                for (Map<String, Object> result : results ) {
                    output.add(result.get("name"));
                }
                return new ResponseWrapper(output);
            } catch (SQLException ex) {
                return new ResponseWrapper(500, "UnexpectedException", "getApps");
            }
        });
    }

    public ResponseWrapper addApp(IHTTPSession session) {
        return authenticator.authenticated(Optional.ofNullable(session.getHeaders().get("authorization")), () -> {
            try {
                Matcher matches = Pattern.compile("\\/([^\\/]+)\\/?").matcher(session.getUri());
                matches.find();
                database.execute("INSERT INTO configurations.apps (name) VALUES ('" + matches.group(1) + "')");
                return new ResponseWrapper();
            } catch (SQLException ex) {
                if(ex.getSQLState() == "23505")
                    return new ResponseWrapper(409, "AlreadyExistsException",
                        "App already exists");
                return new ResponseWrapper(500, "UnexpectedException", "addApp");
            }
        });
    }

    public ResponseWrapper removeApp(IHTTPSession session) {
        return authenticator.authenticated(Optional.ofNullable(session.getHeaders().get("authorization")), () -> {
            try {
                Matcher matches = Pattern.compile("\\/([^\\/]+)\\/?").matcher(session.getUri());
                matches.find();
                int affected = database.execute("DELETE FROM configurations.apps WHERE name = '" + matches.group(1)
                    + "'");
                if(affected == 0)
                    return new ResponseWrapper(404, "DoesNotExistException",
                        "App not found");
                return new ResponseWrapper();
            } catch (SQLException ex) {
                return new ResponseWrapper(500, "UnexpectedException", "removeApp");
            }
        });
    }

    public ResponseWrapper getConfigs(IHTTPSession session) {
        return authenticator.authenticated(Optional.ofNullable(session.getHeaders().get("authorization")), () -> {
            try {
                Matcher matches = Pattern.compile("\\/([^\\/]+)\\/?").matcher(session.getUri());
                matches.find();
                List<Map<String, Object>> results = database.query("SELECT config FROM configurations.configProfiles "
                    + "WHERE app = '" + matches.group(1) + "'");
                if(results.size() == 0 && database.query("SELECT name FROM configurations.apps WHERE name ="
                    + " '" + matches.group(1) + "'").size() == 0)
                        return new ResponseWrapper(404, "DoesNotExistException",
                            "App does not exist");
                List<Object> output = new ArrayList<>();
                for (Map<String, Object> result : results ) {
                    output.add(result.get("config"));
                }
                return new ResponseWrapper(output);
            } catch (SQLException ex) {
                return new ResponseWrapper(500, "UnexpectedException", "getConfigs");
            }
        });
    }

    public ResponseWrapper addConfig(IHTTPSession session) {
        return authenticator.authenticated(Optional.ofNullable(session.getHeaders().get("authorization")), () -> {
            try {
                Matcher matches = Pattern.compile("\\/([^\\/]+)\\/([^\\/]+)\\/?").matcher(session.getUri());
                matches.find();
                UUID token = generateSequentialUuid();
                database.execute("INSERT INTO configurations.configProfiles (app, config, token) VALUES ('" +
                    matches.group(1) + "', '" + matches.group(2) + "', '" + token + "')");
                database.execute("INSERT INTO configurations.configValues (token, values) VALUES ('" + token
                    + "', '{}')");
                return new ResponseWrapper(token);
            } catch (SQLException ex) {
                if(ex.getSQLState() == "23503")
                    return new ResponseWrapper(404, "DoesNotExistException",
                        "App does not exist");
                return new ResponseWrapper(500, "UnexpectedException", "addConfig");
            }
        });
    }

    public ResponseWrapper removeConfig(IHTTPSession session) {
        return authenticator.authenticated(Optional.ofNullable(session.getHeaders().get("authorization")), () -> {
            try {
                Matcher matches = Pattern.compile("\\/([^\\/]+)\\/([^\\/]+)\\/?").matcher(session.getUri());
                matches.find();
                int affected = database.execute("DELETE FROM configurations.configProfiles WHERE app = '"
                    + matches.group(1) + "' AND config = '" + matches.group(2) + "'");
                if(affected == 0)
                    return new ResponseWrapper(404, "DoesNotExistException",
                        "Configuration not found");
                return new ResponseWrapper();
            } catch (SQLException ex) {
                return new ResponseWrapper(500, "UnexpectedException", "removeConfig");
            }
        });
    }

    public ResponseWrapper getToken(IHTTPSession session) {
        return authenticator.authenticated(Optional.ofNullable(session.getHeaders().get("authorization")), () -> {
            try {
                Matcher matches = Pattern.compile("\\/([^\\/]+)\\/([^\\/]+)\\/?").matcher(session.getUri());
                matches.find();
                List<Map<String, Object>> results = database.query("SELECT token FROM configurations.configProfiles "
                    + "WHERE app = '" + matches.group(1) + "' AND config = '" + matches.group(2) + "'");
                if(results.size() == 0)
                    return new ResponseWrapper(404, "DoesNotExistException",
                        "Configuration not found");
                return new ResponseWrapper(results.get(0).get("token"));
            } catch (SQLException ex) {
                return new ResponseWrapper(500, "UnexpectedException", "getToken");
            }
        });
    }

    private UUID generateSequentialUuid() {
        return new UUID(System.currentTimeMillis(), rng.nextLong());
    }
}
