package br.com.gameoteca.apoio;

import br.com.gameoteca.erros.RegraNegocioException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** Cria as conexões e mantém as operações de banco em um lugar. */
public class BancoDados {
    private final Path arquivo;

    public BancoDados(Path arquivo) {
        this.arquivo = arquivo.toAbsolutePath().normalize();
    }

    public static Path caminhoPadrao(Class<?> entrada) {
        try {
            Path origem = Path.of(entrada.getProtectionDomain().getCodeSource().getLocation().toURI());
            Path base = Files.isDirectory(origem) ? origem : origem.getParent();
            if (Files.isDirectory(origem) && base.getFileName().toString().equals("classes")
                    && base.getParent().getFileName().toString().equals("target")) {
                base = base.getParent().getParent();
            } else if (base.getFileName().toString().equals("target")) {
                base = base.getParent();
            }
            return base.resolve("dados/gameoteca.db");
        } catch (Exception e) {
            return Path.of("dados/gameoteca.db");
        }
    }

    public Connection conectar() throws SQLException {
        Connection conexao = DriverManager.getConnection("jdbc:sqlite:" + arquivo);
        try (Statement comando = conexao.createStatement()) {
            comando.execute("PRAGMA foreign_keys = ON");
            comando.execute("PRAGMA busy_timeout = 5000");
        } catch (SQLException e) {
            conexao.close();
            throw e;
        }
        return conexao;
    }

    public void inicializar() {
        try {
            Files.createDirectories(arquivo.getParent());
            try (var recurso = BancoDados.class.getResourceAsStream("/db/schema.sql")) {
                if (recurso == null) throw new IOException("schema.sql não foi encontrado.");
                String script = new String(recurso.readAllBytes(), StandardCharsets.UTF_8);
                try (Connection conexao = conectar(); Statement comando = conexao.createStatement()) {
                    for (String sql : script.split(";")) {
                        if (!sql.isBlank()) comando.execute(sql);
                    }
                }
            }
        } catch (IOException | SQLException e) {
            throw new IllegalStateException("Não foi possível preparar o banco de dados.", e);
        }
    }

    @FunctionalInterface
    public interface Operacao<T> {
        T executar(Connection conexao) throws SQLException;
    }

    public <T> T consultar(Operacao<T> operacao) {
        try (Connection conexao = conectar()) {
            return operacao.executar(conexao);
        } catch (SQLException e) {
            throw traduzir(e);
        }
    }

    /** As leituras e a gravação do empréstimo precisam acontecer juntas. */
    public <T> T transacao(Operacao<T> operacao) {
        try (Connection conexao = conectar(); Statement comando = conexao.createStatement()) {
            comando.execute("BEGIN IMMEDIATE");
            try {
                T resultado = operacao.executar(conexao);
                comando.execute("COMMIT");
                return resultado;
            } catch (SQLException | RuntimeException e) {
                try { comando.execute("ROLLBACK"); }
                catch (SQLException falha) { e.addSuppressed(falha); }
                throw e;
            }
        } catch (SQLException e) {
            throw traduzir(e);
        }
    }

    private RuntimeException traduzir(SQLException e) {
        String mensagem = e.getMessage() == null ? "" : e.getMessage();
        if (mensagem.contains("UNIQUE constraint failed: usuarios.email")) {
            return new RegraNegocioException("Este e-mail já está cadastrado. Entre com sua conta ou use outro e-mail.");
        }
        if (mensagem.contains("UNIQUE constraint failed")) {
            return new RegraNegocioException("Já existe um cadastro com esse e-mail ou com esse jogo, plataforma e modalidade.");
        }
        if (mensagem.contains("FOREIGN KEY constraint failed")) {
            return new RegraNegocioException("Este cadastro tem histórico de empréstimos. Desative-o em vez de excluir.");
        }
        return new IllegalStateException("Não foi possível acessar o banco de dados.", e);
    }
}
