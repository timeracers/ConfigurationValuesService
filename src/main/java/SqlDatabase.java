import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SqlDatabase {
    private final String connectionUrl;

    public SqlDatabase(String connectionUrl) {
        this.connectionUrl = connectionUrl;
    }

    public boolean canConnect() {
        try {
            DriverManager.getConnection(connectionUrl);
            return true;
        } catch (SQLException ex) {
            return false;
        }
    }

    public int execute(String sql, Object... params) throws SQLException {
        Connection c = DriverManager.getConnection(connectionUrl);
        PreparedStatement s = c.prepareStatement(sql);
        for(int i = 0; i < params.length; i++) {
            s.setObject(i + 1, params[i]);
        }
        int affected = s.executeUpdate();
        s.close();
        c.close();
        return affected;
    }

    public List<Map<String, Object>> query(String sql, Object... params) throws SQLException {
        Connection c = DriverManager.getConnection(connectionUrl);
        PreparedStatement s = c.prepareStatement(sql);
        for(int i = 0; i < params.length; i++) {
            s.setObject(i + 1, params[i]);
        }
        ResultSet results = s.executeQuery();

        ArrayList<Map<String, Object>> entities = new ArrayList<>();
        ResultSetMetaData metaData = results.getMetaData();
        int columnCount = metaData.getColumnCount();
        while (results.next()) {
            Map<String, Object> entity = new HashMap<>();
            for (int i = 1; i <= columnCount; ++i) {
                String columnName = metaData.getColumnName(i).toLowerCase();
                Object object = results.getObject(i);
                entity.put(columnName, object);
            }
            entities.add(entity);
        }

        s.close();
        c.close();
        return entities;
    }
}
