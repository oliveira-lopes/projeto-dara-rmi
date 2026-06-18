# Implementação Distribuída do Jogo Dara via Sockets em Java

## 1. Introdução e Contextualização
Este projeto consiste na implementação do jogo de estratégia de origem africana Dara, desenvolvido como requisito avaliativo para a disciplina de Programação Paralela e Distribuída do Curso de Engenharia de Computação do Instituto Federal de Educação, Ciência e Tecnologia do Ceará (IFCE).

O sistema adota uma arquitetura cliente-servidor distribuída baseada na API de Sockets da linguagem Java, permitindo que a partida ocorra entre processos situados na mesma máquina ou em computadores distintos interconectados via rede local.

## 2. Regras do Jogo Implementadas
O Dara é operado em um tabuleiro de dimensão 5x6 (linhas por colunas), onde cada jogador dispõe de 12 peças. O fluxo da partida é segmentado em três fases distintas gerenciadas pelo motor do jogo:

1. **Fase de Posicionamento (Placement):** Os jogadores alternam turnos posicionando suas peças nas interseções disponíveis. É proibida por regra a formação de alinhamentos de 3 peças nesta fase.
2. **Fase de Movimentação (Move):** Uma vez dispostas todas as 24 peças, os jogadores realizam movimentos ortogonais (horizontal ou vertical) de suas peças para casas adjacentes que estejam vazias.
3. **Fase de Captura (Capture):** Ao consolidar um alinhamento exato de 3 peças de mesma cor na horizontal ou na vertical, o jogador obtém o direito de remover uma peça adversária do tabuleiro.

**Condição de Vitória:** O jogo é encerrado quando um dos proponentes é reduzido a apenas 2 peças, impossibilitando-o de realizar novos alinhamentos. O oponente remanescente é declarado vencedor.

## 3. Arquitetura do Sistema e Modelo de Concorrência
O sistema é estruturado sobre o protocolo de transporte TCP (Transmission Control Protocol), garantindo a entrega ordenada, confiável e livre de erros das mensagens de controle do jogo.

### Componentes Principais:
* **DaraServer:** Atua como o nó central da aplicação. Escuta conexões na porta TCP 12345 e gerencia o ciclo de vida da partida. Dispõe de uma lista sincronizada de clientes e delega cada conexão a uma linha de execução independente.
* **ClientHandler:** Classe executável (Thread) instanciada no servidor para cada cliente conectado. É responsável por realizar o parsing dos comandos baseados em strings e encapsular as regras de concorrência essenciais para evitar condições de corrida (Race Conditions).
* **DaraEngine:** Componente puramente lógico residente no servidor que mantém a matriz do tabuleiro, valida a legitimidade dos movimentos, computa a formação de trilhas de captura e define o estado do jogo.
* **DaraClientFX:** Interface gráfica desenvolvida sobre a plataforma JavaFX. Adota um modelo de execução assíncrono, operando a lógica de recepção de dados de rede em uma thread secundária enquanto despacha atualizações visuais na thread principal da interface de usuário (JavaFX Application Thread) via `Platform.runLater()`.

## 4. Protocolo de Comunicação (Camada de Aplicação)
A comunicação se dá por meio de strings formatadas com delimitadores de ponto e vírgula (`;`).

### Comandos Cliente -> Servidor:
* `CHAT;[mensagem]` - Envia uma cadeia de caracteres para o canal de texto compartilhado.
* `PUT;[linha];[coluna]` - Solicita o posicionamento de uma peça nas coordenadas informadas.
* `MOVE;[linha_origem];[coluna_origem];[linha_destino];[coluna_destino]` - Solicita a translação de uma peça.
* `CAPTURE;[linha];[coluna]` - Solicita a remoção de uma peça adversária após uma linha de 3 ser formada.
* `DESISTENCIA` - Notifica a abdicação voluntária da partida.

### Comandos Servidor -> Cliente:
* `START;[PLAYER_1|PLAYER_2]` - Realiza o handshake inicial determinando a cor de cada jogador.
* `UPDATE;[tabuleiro_serializado];[turno_atual];[fase_atual]` - Transmite o estado completo do tabuleiro e metadados de sincronia de turno.
* `CHAT;[remetente];[mensagem]` - Replica mensagens de texto para os terminais gráficos.
* `WINNER;[PLAYER_1|PLAYER_2]` - Decreta o encerramento da sessão de jogo e expõe o vencedor.

## 5. Pré-requisitos e Instalação
Para compilar e executar o projeto, é necessário possuir o Java Development Kit (JDK 11 ou superior) e as bibliotecas do JavaFX instaladas no ambiente de execução.

Em sistemas baseados em Debian/Ubuntu, as dependências podem ser obtidas via:
```bash
sudo apt update
sudo apt install openjfx