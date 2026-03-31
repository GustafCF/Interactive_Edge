@echo off
echo Iniciando a API Spring Boot...
echo.

cd /d c:\workspace\Interactive_Edge

REM Verifica se o Maven está disponível
where mvn >nul 2>nul
if %errorlevel% neq 0 (
    echo ERRO: Maven nao encontrado. Verifique se o Maven esta instalado e no PATH.
    pause
    exit /b 1
)

REM Inicia a aplicacao
echo Executando: mvn spring-boot:run
mvn spring-boot:run

echo.
echo A API foi encerrada.
pause