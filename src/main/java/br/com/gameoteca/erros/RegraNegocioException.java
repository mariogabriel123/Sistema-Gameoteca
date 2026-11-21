package br.com.gameoteca.erros;

/** Uma regra não foi atendida; a tela apresenta a mensagem ao usuário. */
public class RegraNegocioException extends RuntimeException {
    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
