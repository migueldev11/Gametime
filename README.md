# GameTime — tempo de jogo estilo Steam (Android)

App nativo (Kotlin + Jetpack Compose) que mostra quanto tempo você jogou cada jogo do celular.

## Recursos
- Biblioteca estilo Steam (tema escuro, "jogados recentemente", ordenação, busca, favoritos)
- Tempo total, últimas 2 semanas e última vez jogado por jogo
- Detecção automática de jogos + adicionar QUALQUER app instalado manualmente
- Jogos manuais (emulador/PC/console) com horas iniciais e registro de sessões
- Botão JOGAR, notas por jogo, marcos (1h, 10h ... 1000h)
- Resumo: hoje / 7 dias / 30 dias, gráfico de 14 dias, ranking, sequência, meta semanal
- Sincronização em segundo plano (WorkManager), backup/restauração em JSON
- Sem internet, sem anúncios, dados só no aparelho

## Como compilar

### Opção A — Android Studio (PC)
1. Abra a pasta `GameTime` no Android Studio (Koala 2024.1 ou mais novo) e aguarde o Gradle sincronizar.
2. Build > Build APK(s). O APK sai em `app/build/outputs/apk/debug/app-debug.apk`.

### Opção B — Só com o celular (GitHub Actions)
1. Crie um repositório no GitHub e envie todo o conteúdo desta pasta (inclusive `.github`).
2. Aba **Actions** > "Build APK" > Run workflow (ou ele roda sozinho no push).
3. Baixe o artefato `GameTime-debug-apk`, extraia e instale o APK.

### Opção C — Linha de comando
`gradle assembleDebug` (Gradle 8.7+, JDK 17).

## Primeiro uso
1. Abra o app e toque em **ATIVAR ACESSO** > ative "Acesso ao uso" para o GameTime.
2. Volte ao app: os jogos detectados aparecem. Use **+** para adicionar apps que não foram detectados.
3. (Recomendado) Ajustes > "Bateria: não otimizar", para a sincronização em segundo plano não ser cortada.

## Observações
- O Android guarda só ~7–10 dias de eventos detalhados; o GameTime guarda localmente a partir da instalação.
  O histórico anterior é uma estimativa (pode corrigir em "Definir tempo total").
- Em Android 11+ o app declara QUERY_ALL_PACKAGES para listar os apps instalados.
