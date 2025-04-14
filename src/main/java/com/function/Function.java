package com.function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Optional;
import java.util.Properties;

/**
 * Azure Functions with HTTP Trigger.
 */
public class Function {
    /**
     * This function listens at endpoint "/api/HttpExample". Two ways to invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/HttpExample
     * 2. curl "{your host}/api/HttpExample?name=HTTP%20Query"
     */
    @FunctionName("usersInsert")
    public HttpResponseMessage usersInsert(
        @HttpTrigger(
            name = "req",
            methods = {HttpMethod.POST},
            route = "usuarios",
            authLevel = AuthorizationLevel.ANONYMOUS)
        HttpRequestMessage<Optional<String>> request,
        final ExecutionContext context) {

        context.getLogger().info("Java HTTP trigger ejecutado");

        try {
            ObjectMapper mapper = new ObjectMapper();
            String json = request.getBody().orElse("{}");
            User user = mapper.readValue(json, User.class);

            crearUsuario(user, context);
            return request.createResponseBuilder(HttpStatus.CREATED)
                .body("Usuario creado correctamente.")
                .build();

        } catch (Exception e) {
            context.getLogger().severe("Error: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error en la operación: " + e.getMessage())
                    .build();
        }
    }

    @FunctionName("usersUpdate")
    public HttpResponseMessage usersUpdate(
        @HttpTrigger(
            name = "req",
            methods = {HttpMethod.PUT},
            route = "usuarios",
            authLevel = AuthorizationLevel.ANONYMOUS)
        HttpRequestMessage<Optional<String>> request,
        final ExecutionContext context) {

        context.getLogger().info("Java HTTP trigger ejecutado");

        try {
            ObjectMapper mapper = new ObjectMapper();
            String json = request.getBody().orElse("{}");
            User user = mapper.readValue(json, User.class);

            actualizarUsuario(user, context);
            return request.createResponseBuilder(HttpStatus.OK)
                .body("Usuario actualizado correctamente.")
                .build();

        } catch (Exception e) {
            context.getLogger().severe("Error: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error en la operación: " + e.getMessage())
                    .build();
        }
    }

    @FunctionName("usersDelete")
    public HttpResponseMessage usersDelete(
        @HttpTrigger(
            name = "req",
            methods = {HttpMethod.DELETE},
            route = "usuarios",
            authLevel = AuthorizationLevel.ANONYMOUS)
        HttpRequestMessage<Optional<String>> request,
        final ExecutionContext context) {

        context.getLogger().info("Java HTTP trigger ejecutado");

        try {
            ObjectMapper mapper = new ObjectMapper();
            String json = request.getBody().orElse("{}");
            User user = mapper.readValue(json, User.class);
            
            eliminarUsuario(user.id, context);
            return request.createResponseBuilder(HttpStatus.OK)
                .body("Usuario eliminado correctamente.")
                .build();

        } catch (Exception e) {
            context.getLogger().severe("Error: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error en la operación: " + e.getMessage())
                    .build();
        }
    }

    private void crearUsuario(User user, ExecutionContext context) throws Exception {
        Connection conn = conectarOracle(context);
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO users (username, password, name, rol) VALUES (?, ?, ?, ?)");
        stmt.setString(1, user.username);
        stmt.setString(2, user.password);
        stmt.setString(3, user.name);
        stmt.setString(4, user.rol);
        stmt.executeUpdate();
    }

    private void actualizarUsuario(User user, ExecutionContext context) throws Exception {
        Connection conn = conectarOracle(context);
        PreparedStatement stmt = conn.prepareStatement("UPDATE users SET username = ?, password = ?, name = ?, rol = ? WHERE id = ?");
        stmt.setString(1, user.username);
        stmt.setString(2, user.password);
        stmt.setString(3, user.name);
        stmt.setString(4, user.rol);
        stmt.setLong(5, user.id);
        stmt.executeUpdate();
    }

    private void eliminarUsuario(Long id, ExecutionContext context) throws Exception {
        Connection conn = conectarOracle(context);
        PreparedStatement stmt = conn.prepareStatement("DELETE FROM users WHERE id = ?");
        stmt.setLong(1, id);
        stmt.executeUpdate();
    }

    private Connection conectarOracle(ExecutionContext context) throws Exception {
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASSWORD");
        String dbAlias = System.getenv("DB_ALIAS");
        String walletPath = System.getenv("WALLET_PATH");

        String jdbcUrl = "jdbc:oracle:thin:@" + dbAlias + "?TNS_ADMIN=" + walletPath;

        Properties props = new Properties();
        props.put("user", dbUser);
        props.put("password", dbPass);
        props.put("oracle.net.ssl_server_dn_match", "true");

        context.getLogger().info("Conectando a DB: " + jdbcUrl);
        return DriverManager.getConnection(jdbcUrl, props);
    }
}
