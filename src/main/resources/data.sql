INSERT INTO tb_roles (id, name, description, role_status) 
SELECT 1, 'ADMIN', 'Papel de administrador com acesso total', 1 
WHERE NOT EXISTS (SELECT 1 FROM tb_roles WHERE id = 1);

INSERT INTO tb_roles (id, name, description, role_status) 
SELECT 2, 'BASIC', 'Papel de usuário básico com acesso limitado', 2 
WHERE NOT EXISTS (SELECT 1 FROM tb_roles WHERE id = 2);