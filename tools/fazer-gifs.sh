#!/usr/bin/env bash
# Monta um GIF por cena a partir dos quadros gravados pelo gradlew runScreenshots
# (adaptado do mod Auras). Precisa do ffmpeg no PATH. Os GIFs vão para previas/gifs/.
# Uso: tools/fazer-gifs.sh [pasta-do-run]   (padrão: run-screenshots)
set -e
RUN="${1:-run-screenshots}"
mkdir -p previas/gifs
for pasta in "$RUN"/quadros/*/; do
    nome=$(basename "$pasta")
    ffmpeg -y -loglevel error -framerate 20 -i "$pasta/%03d.png" \
        -vf "fps=20,scale=480:-1:flags=lanczos,split[a][b];[a]palettegen=max_colors=256:stats_mode=full[p];[b][p]paletteuse=dither=sierra2_4a" \
        -loop 0 "previas/gifs/$nome.gif"
    echo "previas/gifs/$nome.gif"
done
