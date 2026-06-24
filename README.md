# Projeto Final — Sistema de Mensagens com Controle Offline

Disciplina **Programação Paralela e Distribuída** (IFCE — Eng. de Computação, 2025.2).
Sistema de troca de mensagens com entrega instantânea quando ambos os
contatos estão online (via **RMI**) e armazenamento em fila quando o destinatário está
offline (via **MOM / ActiveMQ**).

## Tecnologias

- **Java 21**
- **Java RMI**: comunicação online e acesso remoto ao servidor de mensagens
- **Apache ActiveMQ 5.18.6**: Middleware Orientado a Mensagens (uma fila por cliente)
- **Swing + FlatLaf 3.5.1**: interface gráfica (tema dark)

## Arquitetura

Um único processo **servidor** sobe, no mesmo JVM:

- o **broker ActiveMQ** embarcado (`tcp://localhost:61616`) : o "servidor de mensagens offline";
- o **registro/serviço RMI** (`rmi://localhost:1099/ChatService`).

Cada **cliente** (GUI) fala apenas via RMI. O servidor faz o roteamento:

- destinatário **online** → entrega instantânea via *callback* RMI;
- destinatário **offline** → mensagem vai para a fila `fila.<contato>` no broker;
- ao **reconectar**, o servidor drena a fila do cliente e entrega o que ficou pendente.

Se o **próprio** cliente está offline e envia uma mensagem, ela fica numa fila local e é
despachada assim que ele volta a ficar online.

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

Caso não queira compilar, basta redirecionar os comandos para o executável disponibilizado na raiz do projeto.

```bat
:: Terminal 1 — servidor
java -jar projeto-final.jar server

:: Terminais 2, 3, ... — clientes
java -jar projeto-final.jar client
```
Ou use os atalhos em `scripts\run-server.bat` e `scripts\run-client.bat`.

No cliente: informe o **nome de contato** e o **servidor** (`localhost`), adicione amigos pelo
nome (botão **+**), selecione um amigo e troque mensagens. Use **Ficar Offline / Ficar Online**
para alternar o estado.