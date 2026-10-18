# Disparador WhatsApp (Android)

Aplicativo Android nativo desenvolvido em **Kotlin** e **Jetpack Compose** para gerenciamento, automação e despacho de relatórios periódicos e mensagens para grupos do WhatsApp com suporte a agendamento automático, múltiplos grupos de destino e regras inteligentes de anti-duplicação.

## Funcionalidades Recentes Adicionadas

- **Seleção de Múltiplos Grupos por Disparo**:
  - Selecione quantos grupos de WhatsApp desejar para um único disparo utilizando caixas de seleção interativas.
  - Botões para "Selecionar Todos" ou "Limpar" grupos de uma só vez.
  - Cartões dos grupos selecionados na tela de edição com botão de remoção rápida (`X`).
  - Despacho simultâneo para todos os grupos destinatários com registro consolidado e detalhado no histórico.
  - Entrada manual de JID com suporte à adição direta à lista de grupos selecionados.

- **Modo Segundo Plano Permanente & Prioridade Total (Anti-Kill Android)**:
  - **Foreground Service**: serviço em primeiro plano ativo (`SchedulerForegroundService`) com notificação permanente contínua no sistema.
  - **Execução Ininterrupta (`START_STICKY`)**: se o sistema necessitar de memória extrema, o serviço é reiniciado automaticamente pelo Android.
  - **WakeLock (`PARTIAL_WAKE_LOCK`)**: mantém a CPU ativa para que os agendamentos e verificações a cada minuto nunca sejam congelados pelo Doze Mode.
  - **Isenção de Otimização de Bateria**: atalho nativo para solicitar a isenção de economia de bateria no Android (`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`), impedindo que o fabricante ou sistema suspenda o app.
  - Chave de ativação/desativação dedicada na aba **Conexão**.

## Outras Funcionalidades

- **Gerenciador de Disparos (Dispatches)**:
  - Criação, edição, pausa e exclusão de disparos programados.
  - Anexos de documentos e relatórios (planilhas Excel `.xlsx`, relatórios PDF, arquivos CSV e Word `.docx`).
  - Legendas dinâmicas com substituição automática de tags (`{data}` para DD/MM/AAAA e `{hora}` para HH:MM).
  - Teste de envio manual imediato.

- **Motor de Agendamento Automático (Scheduler)**:
  - **Horários Fixos Diários**: múltiplos horários configuráveis (ex: 08:00, 18:00, 22:00) com atalhos rápidos.
  - **Intervalo Recorrente**: repetição a cada 15m, 30m, 1h, 2h, 4h, 6h, 12h ou 24h.
  - Execução periódica em segundo plano via corrotinas e Foreground Service.

- **Regras Inteligentes Anti-Duplicação**:
  - **Evitar Envio de Arquivos Idênticos**: cálculo de hash SHA-256 do arquivo; caso o relatório seja idêntico ao anterior dentro da janela configurada, o reenvio duplicado ao grupo é evitado.
  - **Substituição Automática de Mesmo Nome**: ao receber uma nova versão atualizada de um relatório com o mesmo nome, o aplicativo substitui a versão armazenada sem acumular duplicatas.
  - **Janela de Proteção Flexível**: 30 min, 1h, 2h, 24h ou sempre ativo.

- **Histórico Completo de Envios**:
  - Registro de cada disparo com data, hora, grupos, arquivo e status detalhado (entregue, falha ou duplicidade evitada).
  - Limpeza de histórico sob demanda.

- **Painel de Conexão WhatsApp & Baileys**:
  - Status em tempo real (Conectado, Aguardando QR, Conectando, Offline).
  - Pareamento via QR Code e código de telefone (8 caracteres).
  - Registrador de eventos / console de logs com marcação de hora.
  - Configurações de URL base e token de acesso da API (`x-token`).

## Arquitetura & Tecnologias

- **Linguagem**: Kotlin 2.2
- **UI Toolkit**: Jetpack Compose com Material Design 3 (tema escuro emerald WhatsApp)
- **Serviço em 2º Plano**: Android Foreground Service + WakeLock + NotificationChannel
- **Build System**: Gradle 9.3.1 (Kotlin DSL) com Android Gradle Plugin 9.1.1
- **Arquitetura**: MVVM (Model-View-ViewModel) com StateFlow e Coroutines
