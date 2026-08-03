-- Bootstrap minimo e publico exclusivo do perfil Community dev.
-- O usuario permite exercitar a autenticacao HTTP Basic e o shell sem acoplar
-- a aplicacao ao fixture legado, que possui tabelas e colunas nao migradas.
-- A senha BCrypt do usuario demo corresponde a "community-dev".
INSERT INTO user (id, active, password) VALUES ('demo', 1, '$2a$10$BkWEtwCqpQD0cBF/f4mQFusUSt4VlRpsqVRnXkgTxLiZhgyid2yPK');
INSERT INTO user_role (user_role_type, user_id) VALUES ('ROLE_ADMIN', 'demo');
INSERT INTO parametros_globais (id) VALUES ('0');
