package br.com.gameoteca.banco;

import java.util.List;

/** Interface: contrato comum dos cadastros. T é o tipo do objeto cadastrado. */
public interface CrudRepository<T> {
    List<T> listar();
    T buscarPorId(long id);
    T salvar(T objeto);
    void excluir(long id);
}
