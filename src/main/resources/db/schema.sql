CREATE TABLE IF NOT EXISTS usuarios (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  nome TEXT NOT NULL CHECK(length(trim(nome)) BETWEEN 1 AND 100),
  email TEXT NOT NULL COLLATE NOCASE UNIQUE,
  senha_hash TEXT NOT NULL,
  perfil TEXT NOT NULL CHECK(perfil IN ('ADMIN', 'MEMBRO')),
  ativo INTEGER NOT NULL DEFAULT 1 CHECK(ativo IN (0, 1))
);
CREATE TABLE IF NOT EXISTS jogos (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  titulo TEXT NOT NULL CHECK(length(trim(titulo)) BETWEEN 1 AND 120),
  genero TEXT NOT NULL CHECK(length(trim(genero)) BETWEEN 1 AND 60),
  plataforma TEXT NOT NULL CHECK(plataforma IN ('PC','PS4','PS5','XBOX_ONE','XBOX_SERIES','SWITCH')),
  modalidade TEXT NOT NULL CHECK(modalidade IN ('FISICO','LOCAL')),
  quantidade INTEGER NOT NULL CHECK(quantidade BETWEEN 1 AND 999),
  ativo INTEGER NOT NULL DEFAULT 1 CHECK(ativo IN (0, 1)),
  UNIQUE(titulo, plataforma, modalidade)
);
CREATE TABLE IF NOT EXISTS emprestimos (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  usuario_id INTEGER NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT,
  jogo_id INTEGER NOT NULL REFERENCES jogos(id) ON DELETE RESTRICT,
  retirada TEXT NOT NULL,
  prevista TEXT NOT NULL CHECK(prevista >= retirada),
  devolucao TEXT CHECK(devolucao IS NULL OR devolucao >= retirada)
);
CREATE INDEX IF NOT EXISTS idx_emprestimos_usuario ON emprestimos(usuario_id, devolucao);
CREATE INDEX IF NOT EXISTS idx_emprestimos_jogo ON emprestimos(jogo_id, devolucao);
CREATE INDEX IF NOT EXISTS idx_emprestimos_prevista ON emprestimos(prevista) WHERE devolucao IS NULL;
