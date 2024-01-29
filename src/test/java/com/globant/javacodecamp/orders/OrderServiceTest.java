package com.globant.javacodecamp.orders;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@Testcontainers
class OrderServiceTest {
    private OrderService orderService;

    @Container
    private MySQLContainer<?> mysql = getMySQLContainer();
   @NotNull
    private static MySQLContainer<?> getMySQLContainer(){

       return new MySQLContainer<>(DockerImageName.parse("mysql:8.0.32"))
               .withDatabaseName("shop")
               .withCopyFileToContainer(MountableFile.forClasspathResource("init.sql"),
                       "/docker-entrypoint-initdb.d/init.sql")
               .withUsername("root");
    }

    @Test
    void testOrderDispatched(){
       var jdbcUrl = mysql.getJdbcUrl();
       orderService = new OrderService(jdbcUrl);
        var order = orderService.dispatchOrder(1L);
        assertEquals(OrderState.DISPATCHED,order.getState());

        try (Connection connection = DriverManager.getConnection(mysql.getJdbcUrl(),"root","test")) {
            int actualStock = getActualStock(connection);
            assertEquals(98,actualStock);
        } catch (SQLException e) {
          fail();
        }
   }
    @Test
    void testDispatchOrderWhenNotPaid(){
        var jdbcUrl = mysql.getJdbcUrl();

        orderService = new OrderService(jdbcUrl);
       var exeption = assertThrows(RuntimeException.class,() ->orderService.dispatchOrder(4L));

       assertTrue(exeption.getMessage().contains("Not yet paid"));
   }
    private static int getActualStock(Connection connection) throws SQLException {
        var resultSet = connection.createStatement()
                            .executeQuery("SELECT * FROM item WHERE id = %d".formatted(1L));
        resultSet.next();
        return resultSet.getInt("stock");
    }
}
//        try {
//            Class.forName("com.mysql.cj.jdbc.Driver");
//        } catch (ClassNotFoundException e) {
//            throw new RuntimeException(e);
//        }
