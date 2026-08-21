package api.predictive_maintenance_p;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.PGobject;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/** PostgreSQL JSONB와 센서값 Map 사이의 변환을 담당한다. */
public class SensorValuesJsonTypeHandler
        extends BaseTypeHandler<Map<String, Number>> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Number>> SENSOR_MAP_TYPE =
            new TypeReference<>() { };

    @Override
    public void setNonNullParameter(
            PreparedStatement statement,
            int index,
            Map<String, Number> parameter,
            JdbcType jdbcType
    ) throws SQLException {
        try {
            PGobject json = new PGobject();
            json.setType("jsonb");
            json.setValue(OBJECT_MAPPER.writeValueAsString(parameter));
            statement.setObject(index, json);
        } catch (Exception exception) {
            throw new SQLException("센서값을 JSONB로 변환하지 못했습니다.", exception);
        }
    }

    @Override
    public Map<String, Number> getNullableResult(ResultSet resultSet, String columnName)
            throws SQLException {
        return parse(resultSet.getString(columnName));
    }

    @Override
    public Map<String, Number> getNullableResult(ResultSet resultSet, int columnIndex)
            throws SQLException {
        return parse(resultSet.getString(columnIndex));
    }

    @Override
    public Map<String, Number> getNullableResult(CallableStatement statement, int columnIndex)
            throws SQLException {
        return parse(statement.getString(columnIndex));
    }

    private Map<String, Number> parse(String json) throws SQLException {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, SENSOR_MAP_TYPE);
        } catch (Exception exception) {
            throw new SQLException("JSONB 센서값을 읽지 못했습니다.", exception);
        }
    }
}
