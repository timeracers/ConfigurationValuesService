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

    public int execute(String sql) throws SQLException {
        Connection c = DriverManager.getConnection(connectionUrl);
        Statement s = c.createStatement();
        int affected = s.executeUpdate(sql);
        s.close();
        c.close();
        return affected;
    }

    public List<Map<String, Object>> query(String sql) throws SQLException {
        Connection c = DriverManager.getConnection(connectionUrl);
        Statement s = c.createStatement();
        ResultSet results = s.executeQuery(sql);

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
