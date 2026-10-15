@echo off
rem Converte um audio qualquer para OGG Vorbis mono, que e o formato que o Minecraft toca.
rem O jogo NAO le OGG Opus (audio de WhatsApp, por exemplo) nem mp3: so Vorbis.
rem
rem Uso: arraste o arquivo de audio para cima deste .cmd
rem      ou:  converter-ogg.cmd "C:\caminho\fala.mp3"
rem
rem Sai um "<nome>-vorbis.ogg" na mesma pasta do arquivo original, pronto para ir para
rem resourcepacks\Nexus Resources\assets\nexuscore\sounds\speech\.
rem
rem Mono e de proposito: so assim o som vem da direcao do holograma. Estereo o jogo toca
rem sem posicao no mundo.

setlocal
set "VLC=C:\Program Files\VideoLAN\VLC\vlc.exe"
if not exist "%VLC%" set "VLC=C:\Program Files (x86)\VideoLAN\VLC\vlc.exe"
if not exist "%VLC%" (
    echo VLC nao encontrado. Instale o VLC ou ajuste o caminho no topo deste arquivo.
    pause
    exit /b 1
)
if "%~1"=="" (
    echo Arraste o arquivo de audio para cima deste .cmd.
    pause
    exit /b 1
)

set "SAIDA=%~dp1%~n1-vorbis.ogg"
echo Convertendo "%~nx1" ...
"%VLC%" -I dummy --dummy-quiet --no-video --play-and-exit "%~f1" "--sout=#transcode{acodec=vorb,ab=96,channels=1,samplerate=44100}:standard{access=file,mux=ogg,dst='%SAIDA%'}"

if exist "%SAIDA%" (
    echo.
    echo Pronto: %SAIDA%
) else (
    echo.
    echo Nao saiu arquivo nenhum. Veja se o audio de entrada abre no VLC.
)
pause
