package com.function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.EventGridTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Properties;


public class Function {

    @FunctionName("usersEventHandler")
    public void usersEventHandler(
        @EventGridTrigger(name = "event", dataType = "String")
        String eventJson,
        final ExecutionContext context) {

        context.getLogger().info("Event Grid trigger ejecutado: " + eventJson);

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(eventJson);
            String op = root.get("operation").asText();
            User user = mapper.treeToValue(root.get("user"), User.class);

            switch (op) {
                case "create":
                    crearUsuario(user, context);
                    break;
                case "update":
                    actualizarUsuario(user, context);
                    break;
                case "delete":
                    eliminarUsuario(user.id, context);
                    break;
                default:
                    context.getLogger().warning("Operación desconocida: " + op);
            }
        } catch (Exception e) {
            context.getLogger().severe("Error procesando evento: " + e.getMessage());
        }
    }

    private void crearUsuario(User user, ExecutionContext context) throws Exception {
        Connection conn = conectarOracle(context);
        PreparedStatement stmt = conn.prepareStatement(
            "INSERT INTO users (username, password, name, rol) VALUES (?, ?, ?, ?)");
        stmt.setString(1, user.username);
        stmt.setString(2, user.password);
        stmt.setString(3, user.name);
        stmt.setString(4, user.rol);
        stmt.executeUpdate();
    }

    private void actualizarUsuario(User user, ExecutionContext context) throws Exception {
        Connection conn = conectarOracle(context);
        PreparedStatement stmt = conn.prepareStatement(
            "UPDATE users SET username = ?, password = ?, name = ?, rol = ? WHERE id = ?");
        stmt.setString(1, user.username);
        stmt.setString(2, user.password);
        stmt.setString(3, user.name);
        stmt.setString(4, user.rol);
        stmt.setLong(5, user.id);
        stmt.executeUpdate();
    }

    private void eliminarUsuario(Long id, ExecutionContext context) throws Exception {
        Connection conn = conectarOracle(context);
        PreparedStatement stmt = conn.prepareStatement(
            "DELETE FROM users WHERE id = ?");
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
