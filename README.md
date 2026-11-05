# **Problema do Passeio do Cavalo**

[![CI - Testes](https://github.com/davidalain/PasseioDoCavaloTabuleiroNxN/actions/workflows/ci.yml/badge.svg)](https://github.com/davidalain/PasseioDoCavaloTabuleiroNxN/actions/workflows/ci.yml)

O **Problema do Passeio do Cavalo** é um problema clássico da matemática e da ciência da computação,
relacionado ao movimento do cavalo no tabuleiro de xadrez.

Dado um tabuleiro de xadrez (_normalmente de 8x8 casas_), o problema pergunta:

&nbsp;&nbsp;&nbsp;&nbsp;É possível mover um cavalo pelo tabuleiro de forma que ele visite cada casa exatamente uma vez?

Esse percurso é chamado de passeio do cavalo ([Knight's tour](https://en.wikipedia.org/wiki/Knight%27s_tour)).

O código deste repositório fornece diferentes implementações em **Java**, desenvolvidas pelo [Prof. David Alain do Nascimento](https://github.com/davidalain),
que calculam a quantidade de passeios possíveis em um tabuleiro de dimensão **N**x**N** usando técnicas de programação paralela e concorrente.

A quantidade de soluções desse problema é conhecida na literatura. A proposta deste repositório é exemplificar as técnicas que podem ser usadas para implementar a solução.

Fonte: [Number of tours - Knight's tour](https://en.wikipedia.org/wiki/Knight%27s_tour#Number_of_tours) (sequência [OEIS A165134](https://oeis.org/A165134))

| N | Quantidade de passeios possíveis |
| - | -------------------------------- |
| 1 | 1                                |
| 2 | 0                                |
| 3 | 0                                |
| 4 | 0                                |
| 5 | 1 728                            |
| 6 | 6 637 920                        |
| 7 | 165 575 218 320                  |
| 8 | 19 591 828 170 979 904           |

> O número cresce muito rápido: o 5x5 é calculado em fração de segundo, o 6x6 já exige bastante processamento,
> o 7x7 levaria muito tempo e o 8x8 é inviável por busca exaustiva. Por isso a API aceita, por padrão, no máximo **N = 6**.

## **As soluções**

Cada classe em [`solucao/`](src/main/java/br/com/davidalain/passeiocavalo/solucao) é um passo da evolução da técnica.
O código de cada uma tem comentários que explicam a lógica em detalhe.

| Classe | O que faz | Técnica |
| ------ | --------- | ------- |
| [`Solucao1`](src/main/java/br/com/davidalain/passeiocavalo/solucao/Solucao1.java) | Encontra **um** passeio | Busca gulosa com a heurística de Warnsdorff. Rápida, mas pode falhar (não volta atrás) |
| [`Solucao2`](src/main/java/br/com/davidalain/passeiocavalo/solucao/Solucao2.java) | Encontra **um** passeio | Backtracking + Warnsdorff. Sempre encontra, se existir |
| [`Solucao3`](src/main/java/br/com/davidalain/passeiocavalo/solucao/Solucao3.java) | Conta **todos** e guarda numa árvore | Busca exaustiva com `parallelStream()` em todos os níveis |
| [`Solucao4`](src/main/java/br/com/davidalain/passeiocavalo/solucao/Solucao4.java) | Conta todos e guarda numa árvore | Paraleliza só o topo da busca (tarefas maiores, menos custo de coordenação) |
| [`Solucao5`](src/main/java/br/com/davidalain/passeiocavalo/solucao/Solucao5.java) | Conta todos e guarda numa árvore | Casas visitadas em uma máscara de bits (`long`) no lugar de `boolean[][]` |
| [`Solucao6`](src/main/java/br/com/davidalain/passeiocavalo/solucao/Solucao6.java) | Só conta | Sem árvore e sem estado compartilhado: cada chamada retorna sua soma (`long`) |
| [`Solucao7`](src/main/java/br/com/davidalain/passeiocavalo/solucao/Solucao7.java) | Conta todas as casas do tabuleiro | Solução 6 + simetria: calcula só ~1/8 das casas. **É a usada pela API** |

As soluções 5, 6 e 7 suportam tabuleiros de até 8x8 (64 casas = 64 bits de um `long`).

---

## **Tutorial: como executar o projeto**

### 1. Pré-requisitos

Escolha **uma** das opções:

- **Java (JDK) 21** — o Maven não precisa ser instalado, porque o projeto traz o Maven Wrapper (`./mvnw`);
- **Docker** — não precisa de Java na máquina.

Confira o Java com:

```bash
java -version
```

### 2. Baixar o código

```bash
git clone https://github.com/davidalain/PasseioDoCavaloTabuleiroNxN.git
cd PasseioDoCavaloTabuleiroNxN
```

### 3. Rodar os testes

Os testes verificam todas as soluções em tabuleiros de 1x1 a 5x5 e todos os casos da API:

```bash
./mvnw test
```

No Windows, use `mvnw.cmd` no lugar de `./mvnw`.

### 4. Definir o token de acesso

A API só aceita requisições com um token. Ele é lido da variável de ambiente `API_TOKEN`
(sem ela, o servidor sobe, mas responde `503` no cálculo):

```bash
export API_TOKEN=meu-token-secreto
```

No Windows (PowerShell): `$env:API_TOKEN="meu-token-secreto"`.

### 5. Subir o servidor

**Opção A — com o Maven Wrapper (desenvolvimento):**

```bash
./mvnw spring-boot:run
```

**Opção B — gerando o jar:**

```bash
./mvnw clean package
```

```bash
java -jar target/passeio-cavalo-1.0.1-exec.jar
```

**Opção C — com Docker, compilando a imagem localmente:**

```bash
docker build -t passeio-cavalo .
```

```bash
docker run --rm -p 8080:8080 -e API_TOKEN=meu-token-secreto passeio-cavalo
```

**Opção D — com Docker, usando a imagem publicada no GitHub Packages:**

```bash
docker run --rm -p 8080:8080 -e API_TOKEN=meu-token-secreto ghcr.io/davidalain/passeiodocavalotabuleironxn:1.0.1
```

O servidor estará pronto quando aparecer no log `Tomcat started on port 8080`.

### 6. Testar a API

Verifique se o servidor está no ar:

```bash
curl http://localhost:8080/api/passeiocavalo/health
```

Calcule os passeios de um tabuleiro 5x5:

```bash
curl -X POST http://localhost:8080/api/passeiocavalo/calcular -H "Authorization: Bearer meu-token-secreto" -H "Content-Type: application/json" -d '{"dimensao": 5}'
```

Resposta esperada (o tempo varia de máquina para máquina):

```json
{
  "status": "Sucesso",
  "totalSolucoes": 1728,
  "dimensaoTabuleiro": "5x5",
  "tempoExecucaoMs": 159
}
```

### 7. Configurações

| Variável de ambiente | Padrão | Descrição |
| -------------------- | ------ | --------- |
| `API_TOKEN` | _(vazio)_ | Token exigido pelo endpoint `/calcular`. **Obrigatório** |
| `PASSEIO_DIMENSAO_MAXIMA` | `6` | Maior N aceito pela API (de 1 a 8) |
| `PORT` | `8080` | Porta HTTP do servidor |

---

## **Referência da API**

* **BASE_URL:** `http://{HOST}:8080/api/passeiocavalo`

### `GET {BASE_URL}/health`

Indica se o servidor está respondendo.

```json
{
  "status": "UP"
}
```

### `POST {BASE_URL}/calcular`

Calcula, de forma síncrona, o total de passeios do tabuleiro NxN.

* **Autenticação:** header `Authorization: Bearer <token>`.
  Por compatibilidade com a versão 1.0.0, o token também é aceito no campo `token` do corpo.
* **Corpo da requisição (JSON):**

```json
{
  "dimensao": 5
}
```

* **Resposta de sucesso (`200`):**

```json
{
  "status": "Sucesso",
  "totalSolucoes": 1728,
  "dimensaoTabuleiro": "5x5",
  "tempoExecucaoMs": 159
}
```

* **Respostas de erro:**

```json
{
  "erro": "Mensagem de erro"
}
```

| Status | Quando |
| ------ | ------ |
| `400` | `dimensao` ausente, menor que 1 ou acima do limite, ou JSON inválido |
| `401` | Token ausente ou inválido |
| `429` | Já existe um cálculo em andamento (a API faz um por vez) |
| `503` | O servidor foi iniciado sem a variável `API_TOKEN` |

---

## **Pacotes publicados (GitHub Packages)**

Cada release publica, pelo workflow [`publish-packages.yml`](.github/workflows/publish-packages.yml):

- a imagem Docker `ghcr.io/davidalain/passeiodocavalotabuleironxn:<versão>` (e `latest`);
- o pacote Maven `br.com.davidalain:passeio-cavalo:<versão>`.

Para usar o pacote Maven em outro projeto, adicione o repositório e a dependência ao `pom.xml`.
O GitHub Packages exige autenticação até para leitura: configure no `~/.m2/settings.xml` um `<server>`
com `<id>github</id>` e um token com permissão `read:packages`.

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/davidalain/PasseioDoCavaloTabuleiroNxN</url>
    </repository>
</repositories>

<dependency>
    <groupId>br.com.davidalain</groupId>
    <artifactId>passeio-cavalo</artifactId>
    <version>1.0.1</version>
</dependency>
```
