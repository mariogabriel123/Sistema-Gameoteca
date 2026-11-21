package br.com.gameoteca.controle;

import br.com.gameoteca.modelo.Emprestimo;
import br.com.gameoteca.modelo.Jogo;
import br.com.gameoteca.modelo.Usuario;
import br.com.gameoteca.regras.GameotecaService;
import java.util.List;

/** Entrada das ações da interface. As regras continuam no serviço. */
public class GameotecaController {
    private final GameotecaService service;

    public GameotecaController(GameotecaService service) { this.service = service; }

    public boolean primeiroAcesso() { return service.primeiroAcesso(); }
    public Usuario usuarioAtual() { return service.usuarioAtual(); }
    public void sair() { service.sair(); }
    public void entrar(String email, char[] senha) { service.entrar(email, senha); }
    public void criarAdministrador(String nome, String email, char[] senha, char[] confirmacao) {
        service.criarAdministrador(nome, email, senha, confirmacao);
    }
    public void criarConta(String nome, String email, char[] senha, char[] confirmacao) {
        service.criarConta(nome, email, senha, confirmacao);
    }
    public List<Usuario> listarUsuarios() { return service.listarUsuarios(); }
    public void salvarUsuario(Long id, String nome, String email, boolean admin, boolean ativo, char[] senha) {
        service.salvarUsuario(id, nome, email, admin, ativo, senha);
    }
    public void excluirUsuario(long id) { service.excluirUsuario(id); }
    public List<Jogo> listarJogos() { return service.listarJogos(); }
    public void salvarJogo(Long id, String titulo, String genero, String plataforma, boolean fisico, int quantidade, boolean ativo) {
        service.salvarJogo(id, titulo, genero, plataforma, fisico, quantidade, ativo);
    }
    public void excluirJogo(long id) { service.excluirJogo(id); }
    public List<Usuario> usuariosParaEmprestimo() { return service.usuariosParaEmprestimo(); }
    public List<Emprestimo> listarEmprestimos() { return service.listarEmprestimos(); }
    public void emprestar(long usuarioId, long jogoId) { service.emprestar(usuarioId, jogoId); }
    public void devolver(long id) { service.devolver(id); }
    public int excluirHistorico(Long id) { return service.excluirHistorico(id); }
}
