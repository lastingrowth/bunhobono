package api.integration;

import api.support.IntegrationDatabase;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.*;
import static org.junit.jupiter.api.Assertions.*;

/** SQL/parameter/result-mapping contracts; business assertions are in separate workflow tests. */
class MapperReadIntegrationTest {
    static SqlSessionFactory factory;
    @BeforeAll static void setup() throws Exception {factory=IntegrationDatabase.factory();}
    static Stream<Arguments> queries() throws Exception {
        List<Arguments> queries=new ArrayList<>();
        for(String name:IntegrationDatabase.MAPPERS) {
            Class<?> mapper=Class.forName("api."+name);
            for(Method method:Arrays.stream(mapper.getDeclaredMethods()).sorted(Comparator.comparing(Method::getName)).toList()) {
                if(method.isAnnotationPresent(Select.class)) queries.add(Arguments.of(mapper,method));
            }
        }
        return queries.stream();
    }
    @ParameterizedTest(name="{index}: {1}") @MethodSource("queries")
    @DisplayName("IT-BE-MAPPERREAD-001 | 모든 Mapper SELECT의 PostgreSQL 구문·매개변수·결과 매핑 계약을 실행한다")
    void selectContract(Class<?> type,Method method) throws Throwable {
        try(SqlSession session=factory.openSession(false)) {
            IntegrationDatabase.guard(session.getConnection());
            Object[] args=new Object[method.getParameterCount()];
            for(int i=0;i<args.length;i++) args[i]=value(method.getParameters()[i]);
            try {
                Object result=method.invoke(session.getMapper(type),args);
                if(Collection.class.isAssignableFrom(method.getReturnType())) assertNotNull(result,"Collections must not be null");
                if(method.getReturnType()==int.class || method.getReturnType()==long.class) assertNotNull(result);
            } catch(InvocationTargetException error) {throw error.getCause();}
            finally {session.rollback(true);}
        }
    }
    private Object value(Parameter p) throws Exception {
        Class<?> t=p.getType(); String name=p.isAnnotationPresent(Param.class)?p.getAnnotation(Param.class).value():p.getName();
        if(t==String.class) return "__TEST_NO_MATCH__";
        if(t==int.class || t==Integer.class) return name.equals("offset")?0:name.equals("size")||name.equals("limit")?10:1;
        if(t==long.class || t==Long.class) return 1L;
        if(t==boolean.class || t==Boolean.class) return false;
        if(List.class.isAssignableFrom(t)) return List.of(1);
        return t.getDeclaredConstructor().newInstance();
    }
}
