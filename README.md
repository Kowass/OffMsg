# Projeto Final — Sistema de Mensagens com Controle Offline

Disciplina **Programação Paralela e Distribuída** (IFCE — Eng. de Computação, 2025.2).
Sistema de troca de mensagens estilo *WhatsApp* com entrega instantânea quando ambos os
contatos estão online (via **RMI**) e armazenamento em fila quando o destinatário está
offline (via **MOM / ActiveMQ**).

## Tecnologias

- **Java 21**
- **Java RMI** — comunicação online e acesso remoto ao servidor de mensagens
- **Apache ActiveMQ 5.18.6** — Middleware Orientado a Mensagens (uma fila por cliente)
- **Swing + FlatLaf 3.5.1** — interface gráfica (tema dark)
- **Maven** (com *wrapper*) + **Maven Shade** — build e *fat-jar* executável

## Arquitetura

Um único processo **servidor** sobe, no mesmo JVM:

- o **broker ActiveMQ** embarcado (`tcp://localhost:61616`) — o "servidor de mensagens offline";
- o **registro/serviço RMI** (`rmi://localhost:1099/ChatService`).

Cada **cliente** (GUI) fala apenas via RMI. O servidor faz o roteamento:

- destinatário **online** → entrega instantânea via *callback* RMI;
- destinatário **offline** → mensagem vai para a fila `fila.<contato>` no broker;
- ao **reconectar**, o servidor drena a fila do cliente e entrega o que ficou pendente.

Se o **próprio** cliente está offline e envia uma mensagem, ela fica numa *outbox* local e é
despachada assim que ele volta a ficar online (estilo WhatsApp).

## Como compilar

Na pasta do projeto:

```bat
.\mvnw.cmd clean package
```

Gera `target\projeto-final.jar` (auto-contido, com todas as dependências).

## Como executar

Abra **um** terminal para o servidor e **um por cliente**:

```bat
:: Terminal 1 — servidor
java -jar target\projeto-final.jar server

:: Terminais 2, 3, ... — clientes
java -jar target\projeto-final.jar client
```

Ou use os atalhos em `scripts\run-server.bat` e `scripts\run-client.bat`.

No cliente: informe o **nome de contato** e o **servidor** (`localhost`), adicione amigos pelo
nome (botão **+**), selecione um amigo e troque mensagens. Use **Ficar Offline / Ficar Online**
para alternar o estado.

## Roteiro de demonstração

1. Suba o servidor e dois clientes (`ana` e `bob`); cada um adiciona o outro como amigo.
2. **Ambos online:** `ana` envia para `bob` → chega na hora.
3. **Destinatário offline:** `bob` clica em *Ficar Offline*; `ana` envia → ao `bob` voltar
   *Online*, recebe as mensagens da fila.
4. **Remetente offline:** `ana` fica *Offline*, escreve para `bob` (vai para a outbox) e depois
   volta *Online* → as mensagens são enviadas e roteadas normalmente.

## Mapeamento dos requisitos

| Req | Onde |
|-----|------|
| 1. Nome de contato + lista de amigos sempre visível | `ui/ChatFrame` (painel de amigos) |
| 2. Alternar online/offline | `ChatClient.conectar/desconectar` + botão de estado |
| 3. Online → entrega instantânea | `ChatServiceImpl.sendMessage` (callback) |
| 4. Offline → servidor remoto (RMI) + fila | `ChatServiceImpl` + `OfflineQueueManager` |
| 5. Fila por cliente gerenciada por MOM | `OfflineQueueManager` (`fila.<contato>`) |
| 6. Contato offline → vai para a fila | `ChatServiceImpl.sendMessage` (ramo offline) |
| 7. Novo cliente solicita criação da fila | `ChatService.createQueue` (no `conectar`) |
| 8. Incluir/excluir contatos | `ChatClient.adicionarAmigo/removerAmigo` |
