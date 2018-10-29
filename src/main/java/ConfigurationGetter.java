import fi.iki.elonen.NanoHTTPD.IHTTPSession;

import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class ConfigurationGetter {
    private final SqlDatabase database;

    public ConfigurationGetter(SqlDatabase database) {
        this.database = database;
    }

    public ResponseWrapper get(IHTTPSession session)
    {
        try {
            List<Map<String, Object>> results = database.query("SELECT values FROM configurations.configValues WHERE token = ?",
                UUID.fromString(session.getParms().get("token")));
            if(results.size() == 0)
                return new ResponseWrapper(404, "DoesNotExistException",
                    "Configuration for this token was not found");
            return new ResponseWrapper(results.get(0).get("values"));
        } catch (IllegalArgumentException ex) {
            return new ResponseWrapper(400, "IllegalArgumentException", "Expected token type is uuid");
        } catch (Exception ex) {
            return new ResponseWrapper(500, "UnexpectedException", "getValues");
        }
    }
}
