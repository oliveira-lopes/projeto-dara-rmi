# Implementação Distribuída do Jogo Dara via RMI em Java

## 1. Introdução

Este projeto consiste na implementação distribuída do jogo de estratégia africano **Dara**, desenvolvido como requisito avaliativo da disciplina de Programação Paralela e Distribuída do curso de Engenharia de Computação do Instituto Federal de Educação, Ciência e Tecnologia do Ceará (IFCE).

A aplicação utiliza a tecnologia **Java RMI (Remote Method Invocation)** para comunicação remota entre processos, substituindo o modelo tradicional baseado em sockets. Dessa forma, a troca de informações ocorre através da invocação direta de métodos remotos orientados a objetos, permitindo que partidas sejam executadas entre máquinas distintas conectadas em rede.

---

## 2. Regras do Jogo

O Dara é disputado em um tabuleiro de **5 linhas por 6 colunas**, onde cada jogador possui **12 peças**.

### 2.1 Fase de Posicionamento (Placement)

Os jogadores alternam turnos posicionando suas peças nas casas disponíveis do tabuleiro.

**Restrição:**

* Não é permitido formar alinhamentos de três peças durante esta fase.

### 2.2 Fase de Movimentação (Move)

Após o posicionamento das 24 peças:

* Cada jogador movimenta uma peça por turno;
* Os movimentos são permitidos apenas para casas adjacentes;
* São aceitos apenas deslocamentos horizontais e verticais.

### 2.3 Fase de Captura (Capture)

Quando um jogador forma um alinhamento exato de três peças da mesma cor:

* Ganha o direito de capturar uma peça adversária;
* A peça capturada é removida do tabuleiro.

### 2.4 Condição de Vitória

A partida termina quando um jogador permanece com apenas duas peças, tornando-se incapaz de formar novos alinhamentos.

O adversário é declarado vencedor.

---

## 3. Arquitetura do Sistema

A solução adota uma arquitetura distribuída baseada em **Java RMI**, eliminando a necessidade de serialização manual de mensagens e gerenciamento explícito de conexões TCP.

O subsistema RMI é responsável por:

* Transporte dos dados;
* Serialização automática dos objetos;
* Geração de stubs;
* Gerenciamento das chamadas remotas.

### 3.1 Componentes Principais

#### DaraServerRMI

Servidor principal da aplicação.

Responsabilidades:

* Inicializar o `LocateRegistry`;
* Publicar o serviço remoto;
* Gerenciar conexões dos jogadores;
* Coordenar a sessão da partida.

#### DaraServerInterface

Contrato remoto utilizado pelos clientes.

Define todas as operações que podem ser invocadas remotamente no servidor.

#### DaraClientInterface

Contrato remoto utilizado pelo mecanismo de callback.

Permite que o servidor envie atualizações diretamente aos clientes.

#### DaraEngine

Motor central do jogo.

Responsável por:

* Manter o estado do tabuleiro;
* Validar jogadas;
* Controlar fases da partida;
* Detectar alinhamentos;
* Processar capturas;
* Determinar vencedores.

As operações críticas são protegidas por sincronização (`synchronized`) para garantir consistência em ambientes concorrentes.

#### DaraClientFX

Cliente gráfico desenvolvido com JavaFX.

Responsável por:

* Exibição do tabuleiro;
* Chat entre jogadores;
* Interação do usuário;
* Recebimento de callbacks do servidor.

As atualizações gráficas são executadas utilizando:

```java
Platform.runLater(...)
```

garantindo segurança de acesso à JavaFX Application Thread.

---

## 4. Interfaces Remotas

### 4.1 Cliente → Servidor (`DaraServerInterface`)

| Método                                                                                | Descrição                          |
| ------------------------------------------------------------------------------------- | ---------------------------------- |
| `registrarCliente(DaraClientInterface cliente, String nome)`                          | Registra um jogador e seu callback |
| `enviarMensagemChat(String nomeJogador, String texto)`                                | Envia mensagem para o chat         |
| `jogarPeca(String nomeJogador, int linha, int coluna)`                                | Posiciona uma peça                 |
| `moverPeca(String nomeJogador, int rOrigem, int cOrigem, int rDestino, int cDestino)` | Move uma peça                      |
| `capturarPeca(String nomeJogador, int linha, int coluna)`                             | Remove uma peça adversária         |
| `desistir(String nomeJogador)`                                                        | Encerra a participação do jogador  |

### 4.2 Servidor → Cliente (`DaraClientInterface`)

| Método                                                                            | Descrição                         |
| --------------------------------------------------------------------------------- | --------------------------------- |
| `receberAtualizacaoEstado(String tabuleiro, String turnoAtual, String faseAtual)` | Atualiza a interface gráfica      |
| `receberMensagemChat(String remetente, String texto)`                             | Recebe mensagens do chat          |
| `notificarVencedor(String vencedor)`                                              | Informa o encerramento da partida |

---

## 5. Estrutura do Projeto

```text
src/
└── br/
    └── edu/
        └── ifce/
            ├── shared/
            │   └── model/
            ├── server/
            │   ├── core/
            │   └── network/
            └── client/
                └── ui/
```

---

## 6. Pré-requisitos

* JDK 11 ou superior;
* Java RMI;
* JavaFX.

### 6.1 Linux (Debian/Ubuntu)

Instalação do JavaFX:

```bash
sudo apt update
sudo apt install openjfx
```

### 6.2 Windows

Baixe o SDK do JavaFX e extraia para um diretório local.

Exemplo:

```text
C:\javafx-sdk\
```

---

## 7. Compilação e Execução

### 7.1 Linux (JDK 11+)

#### Compilar

```bash
javac -encoding UTF-8 \
-cp "bin:/usr/share/java/*" \
-d bin \
src/br/edu/ifce/shared/model/*.java \
src/br/edu/ifce/server/core/*.java \
src/br/edu/ifce/server/network/*.java \
src/br/edu/ifce/client/ui/*.java
```

#### Executar o Servidor

```bash
java -cp bin br.edu.ifce.server.network.DaraServerRMI
```

#### Executar os Clientes

```bash
java -cp "bin:/usr/share/java/*" \
br.edu.ifce.client.ui.DaraMain
```

---

### 7.2 Windows (JDK 17+)

#### Compilar

```powershell
& "C:\Program Files\Microsoft\jdk-17.0.11.9-hotspot\bin\javac.exe" `
-encoding UTF-8 `
-cp bin `
--module-path "C:\javafx-sdk\lib" `
--add-modules javafx.controls `
-d bin `
src/br/edu/ifce/shared/model/*.java `
src/br/edu/ifce/server/core/*.java `
src/br/edu/ifce/server/network/*.java `
src/br/edu/ifce/client/ui/*.java
```

#### Executar o Servidor

```powershell
& "C:\Program Files\Microsoft\jdk-17.0.11.9-hotspot\bin\java.exe" `
-cp bin `
br.edu.ifce.server.network.DaraServerRMI
```

#### Executar os Clientes

```powershell
& "C:\Program Files\Microsoft\jdk-17.0.11.9-hotspot\bin\java.exe" `
-cp bin `
--module-path "C:\javafx-sdk\lib" `
--add-modules javafx.controls `
br.edu.ifce.client.ui.DaraClientFX
```

---

## 8. Fluxo de Execução

```text
Cliente 1
    │
    ▼
Servidor RMI
    ▲
    │
Cliente 2
```

1. O servidor é iniciado e publica o serviço RMI;
2. Os clientes conectam-se ao serviço remoto;
3. Os jogadores realizam suas ações;
4. O servidor valida e processa as jogadas;
5. Atualizações são enviadas via callback para todos os clientes;
6. O jogo continua até que uma condição de vitória seja atingida.

---

## 9. Tecnologias Utilizadas

* Java 11+;
* Java RMI;
* JavaFX;
* TCP/IP;
* Programação Distribuída;
* Programação Concorrente.

---

## 10. Objetivos Acadêmicos

Este projeto demonstra conceitos fundamentais de:

* Invocação remota de métodos (RMI);
* Comunicação distribuída orientada a objetos;
* Callbacks bidirecionais;
* Sincronização de estado compartilhado;
* Concorrência em aplicações distribuídas;
* Desenvolvimento de interfaces gráficas com JavaFX.

---

## 11. Licença

Projeto desenvolvido exclusivamente para fins acadêmicos na disciplina de Programação Paralela e Distribuída do Instituto Federal de Educação, Ciência e Tecnologia do Ceará (IFCE).
