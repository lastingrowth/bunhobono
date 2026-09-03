package api.support;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.*;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.sql.*;
import java.util.*;

/** Only the explicitly approved isolated database is reachable through this helper. */
public final class IntegrationDatabase {
    public static final String URL="jdbc:postgresql://127.0.0.1:5432/test_bono";
    public static final String[] MAPPERS={
        "a_security_config.AuthMapper","bill_p.BillMapper","board_p.BoardMapper","cameradata_p.CameraDataMapper",
        "camera_p.CameraMapper","camera_pdm_p.CameraPdmMapper","carlog_p.CarLogMapper","faq_p.FaqMapper",
        "feerule_p.FeeRuleMapper","gate_p.GateMapper","gate_pdm_p.GatePdmMapper","inquiry_p.InquiryMapper",
        "kiosk_p.KioskMapper","memberarchive_p.MemberArchiveMapper","member_p.MemberMapper","mem_notice_p.MemNoticeMapper",
        "mem_purchase_p.MemPurchaseMapper","notice_p.NoticeMapper","parking_p.ParkingMapper","parking_space_p.ParkingSpaceMapper",
        "robot_log_p.RobotLogMapper","robot_p.RobotMapper","robot_pdm_p.RobotPdmMapper","robot_task_p.RobotTaskMapper",
        "trash_p.TrashMapper","vehicle_p.VehicleMapper"
    };
    public static DriverManagerDataSource dataSource() {
        String user=System.getenv("BONO_TEST_DB_USER"), password=System.getenv("BONO_TEST_DB_PASSWORD");
        if(user==null || password==null) throw new IllegalStateException("Dedicated test DB credentials are required; production URL fallback is forbidden");
        return new DriverManagerDataSource(URL,user,password);
    }
    public static void guard(Connection connection) throws SQLException {
        try(var statement=connection.createStatement(); var rs=statement.executeQuery("SELECT current_database()")) {
            if(!rs.next() || !"test_bono".equals(rs.getString(1))) throw new IllegalStateException("Not the approved test database");
        }
    }
    public static SqlSessionFactory factory() throws Exception {
        var ds=dataSource(); try(var connection=ds.getConnection()) {guard(connection);}
        Configuration config=new Configuration(new Environment("isolated-integration",new JdbcTransactionFactory(),ds));
        config.setMapUnderscoreToCamelCase(true);
        for(String mapper:MAPPERS) config.addMapper(Class.forName("api."+mapper));
        return new SqlSessionFactoryBuilder().build(config);
    }
    private IntegrationDatabase() {}
}
