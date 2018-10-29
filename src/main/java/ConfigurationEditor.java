import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

public class ConfigurationEditor {
    private final SqlDatabase database;
    private final Authenticator authenticator;
    private final ObjectMapper mapper;

    public ConfigurationEditor(Authenticator authenticator, SqlDatabase database) {
        this.database = database;
        this.authenticator = authenticator;
        mapper = new ObjectMapper();
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    public ResponseWrapper setValues(IHTTPSession session) {
        return authenticator.authenticated(Optional.ofNullable(session.getHeaders().get("authorization")), () -> {
            Map<String, String> newValues;
            try {
                newValues = mapper.readValue(readBody(session).get(), Map.class);
            } catch (Exception ex) {
                return new ResponseWrapper(400, "JsonObjectExpectedException",
                    "Body is required to be a json object");
            }
            try {
                List<Map<String, Object>> results = database.query("SELECT values FROM configurations.configValues WHERE token = ?",
                    UUID.fromString(session.getParms().get("token")));
                if(results.size() == 0)
                    return new ResponseWrapper(404, "DoesNotExistException",
                        "Configuration for this token does not exist and therefore can not be altered");
                Map<String, String> values = mapper.readValue((String)results.get(0).get("values"), Map.class);
                for (Entry<String, String> pair : newValues.entrySet()) {
                    values.put(pair.getKey(), pair.getValue());
                }
                database.execute("UPDATE configurations.configValues SET values = ? WHERE token = ?",
                    mapper.writeValueAsString(values), UUID.fromString(session.getParms().get("token")));
                return new ResponseWrapper();
            } catch (IllegalArgumentException ex) {
                return new ResponseWrapper(400, "IllegalArgumentException", "Expected token type is uuid");
            } catch (Exception ex) {
                return new ResponseWrapper(500, "UnexpectedException", "setValues");
            }
        });
    }

    public ResponseWrapper clearValues(IHTTPSession session) {
        return authenticator.authenticated(Optional.ofNullable(session.getHeaders().get("authorization")), () -> {
            List<String> valuesToRemove;
            try {
                valuesToRemove = mapper.readValue(readBody(session).get(), List.class);
            } catch (Exception ex) {
                return new ResponseWrapper(400, "JsonArrayExpectedException",
                    "Body is required to be a json array");
            }
            try {
                List<Map<String, Object>> results = database.query("SELECT values FROM configurations.configValues WHERE token = ?",
                    UUID.fromString(session.getParms().get("token")));
                if(results.size() == 0)
                    return new ResponseWrapper(404, "DoesNotExistException",
                        "Configuration for this token does not exist and therefore can not be altered");
                Map<String, String> values = mapper.readValue((String)results.get(0).get("values"), Map.class);
                for (String value : valuesToRemove) {
                    values.remove(value);
                }
                database.execute("UPDATE configurations.configValues SET values = ? WHERE token = ?",
                    mapper.writeValueAsString(values), UUID.fromString(session.getParms().get("token")));
                return new ResponseWrapper();
            } catch (IllegalArgumentException ex) {
                return new ResponseWrapper(400, "IllegalArgumentException", "Expected token type is uuid");
            } catch (Exception ex) {
                return new ResponseWrapper(500, "UnexpectedException", "clearValues");
            }
        });
    }

    private Optional<String> readBody(IHTTPSession session) {
        try {
            int length = Integer.parseInt(session.getHeaders().get("content-length"));
            InputStream input = session.getInputStream();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int bytesLeft = length;
            byte[] data = new byte[4096];
            while (bytesLeft > 0) {
                int read = input.read(data, 0, Math.min(bytesLeft, data.length));
                if (read == -1) {
                    throw new EOFException("Unexpected end of data");
                }
                buffer.write(data, 0, read);
                bytesLeft -= read;
            }
            buffer.flush();
            return Optional.of(new String(buffer.toByteArray()));
        } catch (IOException ex) {
            return Optional.empty();
        }
    }
}
