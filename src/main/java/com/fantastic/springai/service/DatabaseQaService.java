package com.fantastic.springai.service;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;

@Service
public class DatabaseQaService {

    private static final int MAX_SQL_ROWS = 200;

    private final ChatClient chatClient;
    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate readOnlyTx;
    private final String databaseSchema;
    private final DatabaseConversationStore conversationStore;

    private volatile String cachedSchema;

    public DatabaseQaService(
            ChatClient.Builder chatClientBuilder,
            JdbcTemplate jdbcTemplate,
            DataSource dataSource,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager,
            DatabaseConversationStore conversationStore,
            @Value("${app.database.schema:shop}") String databaseSchema) {
        this.chatClient = chatClientBuilder.build();
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
        this.conversationStore = conversationStore;
        this.databaseSchema = databaseSchema;
        this.jdbcTemplate.setMaxRows(MAX_SQL_ROWS);
        this.readOnlyTx = new TransactionTemplate(transactionManager);
        this.readOnlyTx.setReadOnly(true);
    }

    public DatabaseAnswer answer(String userQuestion, String conversationIdIn) {
        String conversationId = conversationStore.resolveConversationId(conversationIdIn);
        String history = conversationStore.formatHistoryForPrompt(conversationId);

        String schema = schemaDescription();
        if (schema.isBlank()) {
            String msg = "Aucune table n'a été trouvée dans le schéma « " + databaseSchema
                    + " ». Créez des tables ou vérifiez la connexion PostgreSQL.";
            rememberTurn(conversationId, userQuestion, msg);
            return new DatabaseAnswer(msg, null, conversationId);
        }

        String sqlUserBlock = (history.isEmpty() ? "" : history + "\n")
                + "Schéma disponible :\n" + schema + "\n\nQuestion actuelle : " + userQuestion;

        String sqlRaw = chatClient.prompt()
                .system("""
                        Tu es un expert PostgreSQL. Tu génères une seule requête SQL en lecture seule.
                        Règles strictes :
                        - Réponds uniquement par du SQL PostgreSQL valide, sans markdown, sans texte avant ou après.
                        - Utilise uniquement les tables et colonnes listées ci-dessous ; qualifie chaque table avec le schéma %s (ex. %s.ma_table).
                        - La requête doit être un SELECT (éventuellement avec CTE WITH … SELECT).
                        - Pas de point-virgule à la fin.
                        - Si l'historique est présent, interprète les questions de suivi (références implicites, filtres supplémentaires).
                        """.formatted(databaseSchema, databaseSchema))
                .user(sqlUserBlock)
                .call()
                .content();

        String sql;
        try {
            sql = sanitizeAndValidateSql(sqlRaw);
        } catch (IllegalArgumentException | JSQLParserException e) {
            String attempted = extractSql(sqlRaw);
            String msg = "Impossible de valider le SQL généré : " + e.getMessage();
            String out = withSqlInReply(msg, attempted);
            rememberTurn(conversationId, userQuestion, out);
            return new DatabaseAnswer(out, attempted, conversationId);
        }

        List<Map<String, Object>> rows;
        try {
            rows = readOnlyTx.execute(status -> jdbcTemplate.query(sql, (rs, rowNum) -> rowToMap(rs)));
        } catch (Exception e) {
            String msg = "Erreur lors de l'exécution de la requête : " + e.getMessage();
            String out = withSqlInReply(msg, sql);
            rememberTurn(conversationId, userQuestion, out);
            return new DatabaseAnswer(out, sql, conversationId);
        }

        String rowsJson;
        try {
            rowsJson = objectMapper.writeValueAsString(rows);
        } catch (JsonProcessingException e) {
            String out = withSqlInReply("Erreur de sérialisation des résultats.", sql);
            rememberTurn(conversationId, userQuestion, out);
            return new DatabaseAnswer(out, sql, conversationId);
        }

        String summaryUserBlock = (history.isEmpty() ? "" : history + "\n")
                + "Question actuelle : " + userQuestion + "\n\nRésultats (JSON) :\n" + rowsJson;

        String reply = chatClient.prompt()
                .system("""
                        Tu réponds en français, de façon claire et concise.
                        Tu t'appuies uniquement sur les résultats JSON fournis (issues de la base).
                        Si les résultats sont vides ou insuffisants, dis-le honnêtement. N'invente pas de chiffres ni de lignes.
                        Tu peux t'appuyer sur l'historique pour comprendre une question de suivi.
                        """)
                .user(summaryUserBlock)
                .call()
                .content();

        String out = withSqlInReply(reply, sql);
        rememberTurn(conversationId, userQuestion, out);
        return new DatabaseAnswer(out, sql, conversationId);
    }

    private void rememberTurn(String conversationId, String userQuestion, String replyToUser) {
        conversationStore.append(conversationId, userQuestion, stripSqlFooter(replyToUser));
    }

    private static String stripSqlFooter(String reply) {
        String marker = "\n\n---\nRequête SQL :\n";
        int idx = reply.indexOf(marker);
        return idx >= 0 ? reply.substring(0, idx) : reply;
    }

    private static String withSqlInReply(String reply, String sql) {
        if (sql == null || sql.isBlank()) {
            return reply;
        }
        return reply + "\n\n---\nRequête SQL :\n" + sql;
    }

    private String schemaDescription() {
        if (cachedSchema != null) {
            return cachedSchema;
        }
        synchronized (this) {
            if (cachedSchema != null) {
                return cachedSchema;
            }
            cachedSchema = loadSchema();
            return cachedSchema;
        }
    }

    private String loadSchema() {
        StringBuilder out = new StringBuilder();
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData md = connection.getMetaData();
            try (ResultSet tables = md.getTables(connection.getCatalog(), databaseSchema, "%",
                    new String[] { "TABLE", "VIEW" })) {
                while (tables.next()) {
                    String table = tables.getString("TABLE_NAME");
                    out.append("Table ").append(databaseSchema).append('.').append(table).append(" : ");
                    List<String> cols = new ArrayList<>();
                    try (ResultSet columns = md.getColumns(connection.getCatalog(), databaseSchema, table, "%")) {
                        while (columns.next()) {
                            String col = columns.getString("COLUMN_NAME");
                            String type = columns.getString("TYPE_NAME");
                            cols.add(col + " (" + type + ")");
                        }
                    }
                    out.append(String.join(", ", cols)).append('\n');
                }
            }
        } catch (SQLException e) {
            return "/* Erreur introspection : " + e.getMessage() + " */";
        }
        return out.toString();
    }

    static String sanitizeAndValidateSql(String raw) throws JSQLParserException {
        String sql = extractSql(raw).trim();
        if (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1).trim();
        }
        if (sql.contains(";")) {
            throw new IllegalArgumentException("Une seule instruction SQL est autorisée.");
        }
        Statement stmt = CCJSqlParserUtil.parse(sql);
        if (!(stmt instanceof Select)) {
            throw new IllegalArgumentException("Seules les requêtes SELECT sont autorisées.");
        }
        return sql;
    }

    static String extractSql(String raw) {
        String s = raw.trim();
        if (s.startsWith("```")) {
            int firstNl = s.indexOf('\n');
            int start = firstNl >= 0 ? firstNl + 1 : 3;
            int fence = s.lastIndexOf("```");
            if (fence > start) {
                s = s.substring(start, fence).trim();
            }
        }
        if (s.regionMatches(true, 0, "sql", 0, 3) && (s.length() == 3 || Character.isWhitespace(s.charAt(3)))) {
            s = s.substring(3).trim();
        }
        return s;
    }

    private static Map<String, Object> rowToMap(java.sql.ResultSet rs) throws SQLException {
        int n = rs.getMetaData().getColumnCount();
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 1; i <= n; i++) {
            row.put(rs.getMetaData().getColumnLabel(i), rs.getObject(i));
        }
        return row;
    }

    public record DatabaseAnswer(String reply, String executedSql, String conversationId) {
    }
}
