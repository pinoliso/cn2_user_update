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
import java.util.Properties;

import com.fasterxml.jackson.databind.JsonNode;
import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.Scalars;
import graphql.schema.*;

import java.util.*;

public class GraphQLFunction {

    private static final List<User> userList = new ArrayList<>();
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final GraphQL graphQL;

    static {
        // Definir esquema GraphQL
        GraphQLObjectType userType = GraphQLObjectType.newObject()
            .name("User")
            .field(f -> f.name("id").type(Scalars.GraphQLString))
            .field(f -> f.name("name").type(Scalars.GraphQLString))
            .field(f -> f.name("email").type(Scalars.GraphQLString))
            .build();

        // Query
        GraphQLObjectType queryType = GraphQLObjectType.newObject()
            .name("Query")
            .field(f -> f.name("getUser")
                .type(userType)
                .argument(a -> a.name("id").type(Scalars.GraphQLString))
                .dataFetcher(env -> {
                    String id = env.getArgument("id");
                    return userList.stream().filter(u -> u.getId().equals(id)).findFirst().orElse(null);
                }))
            .field(f -> f.name("allUsers")
                .type(GraphQLList.list(userType))
                .dataFetcher(env -> userList))
            .build();

        // Mutation
        GraphQLObjectType mutationType = GraphQLObjectType.newObject()
            .name("Mutation")
            .field(f -> f.name("createUser")
                .type(userType)
                .argument(a -> a.name("id").type(Scalars.GraphQLString))
                .argument(a -> a.name("name").type(Scalars.GraphQLString))
                .argument(a -> a.name("email").type(Scalars.GraphQLString))
                .dataFetcher(env -> {
                    User user = new User(
                        env.getArgument("id"),
                        env.getArgument("username"),
                        env.getArgument("password"),
                        env.getArgument("name"),
                        env.getArgument("email"),
                        env.getArgument("rol"));
                    userList.add(user);
                    return user;
                }))
            .field(f -> f.name("updateUser")
                .type(userType)
                .argument(a -> a.name("id").type(Scalars.GraphQLString))
                .argument(a -> a.name("name").type(Scalars.GraphQLString))
                .argument(a -> a.name("email").type(Scalars.GraphQLString))
                .dataFetcher(env -> {
                    String id = env.getArgument("id");
                    for (User user : userList) {
                        if (user.getId().equals(id)) {
                            user.setName(env.getArgument("name"));
                            user.setEmail(env.getArgument("email"));
                            return user;
                        }
                    }
                    return null;
                }))
            .field(f -> f.name("deleteUser")
                .type(Scalars.GraphQLBoolean)
                .argument(a -> a.name("id").type(Scalars.GraphQLString))
                .dataFetcher(env -> userList.removeIf(u -> u.getId().equals(env.getArgument("id")))))
            .build();

        GraphQLSchema schema = GraphQLSchema.newSchema()
            .query(queryType)
            .mutation(mutationType)
            .build();

        graphQL = GraphQL.newGraphQL(schema).build();
    }

    @FunctionName("graphqlCrud")
    public HttpResponseMessage graphqlHandler(
        @HttpTrigger(name = "req", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.ANONYMOUS) 
        HttpRequestMessage<String> request, ExecutionContext context) {

        try {
            JsonNode body = mapper.readTree(request.getBody());
            String query = body.get("query").asText();
            JsonNode variables = body.get("variables");

            ExecutionInput input = ExecutionInput.newExecutionInput()
                .query(query)
                .variables(variables != null ? mapper.convertValue(variables, Map.class) : new HashMap<>())
                .build();

            Map<String, Object> result = graphQL.execute(input).toSpecification();

            return request.createResponseBuilder(HttpStatus.OK)
                          .header("Content-Type", "application/json")
                          .body(mapper.writeValueAsString(result))
                          .build();

        } catch (Exception e) {
            context.getLogger().severe("Error: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                          .body("Internal Error: " + e.getMessage())
                          .build();
        }
    }


    private void updateIntoOracle(User user, ExecutionContext context) throws Exception {

        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASSWORD");
        String dbAlias = System.getenv("DB_ALIAS");
        String walletPath = System.getenv("WALLET_PATH");

        String jdbcUrl = "jdbc:oracle:thin:@" + dbAlias + "?TNS_ADMIN=" + walletPath;

        Properties props = new Properties();
        props.put("user", dbUser);
        props.put("password", dbPass);
        props.put("oracle.net.ssl_server_dn_match", "true");

        try {
            context.getLogger().severe("Connect db: " + jdbcUrl);
            Connection conn = DriverManager.getConnection(jdbcUrl, props);
            context.getLogger().severe("Connected db: ");
            PreparedStatement stmt = conn.prepareStatement(
                "UPDATE users SET username = ?, password = ?, name = ?, rol = ? WHERE id = ?"
            );
            context.getLogger().severe("Prepared: ");
            stmt.setString(1, user.username);
            stmt.setString(2, user.password);
            stmt.setString(3, user.name);
            stmt.setString(4, user.rol);
            stmt.setLong(5, user.id);
            stmt.executeUpdate();
            
            context.getLogger().severe("Executed: ");
        }catch (Exception e) {
            context.getLogger().severe("Error: " + e.getMessage());
            throw new Exception("Error al actualizar: " + e.getMessage());
        }
    }
}
