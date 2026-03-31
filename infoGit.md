# Remove os arquivos que já foram commitados anteriormente (se houver)
git rm -r --cached uploads/
git rm -r --cached temp/
git rm -r --cached document/

# Ou para remover tudo de uma vez
git rm -r --cached .