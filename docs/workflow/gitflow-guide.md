# Guia Completo de GitFlow

Este projeto usa um GitFlow pragmatico: simples o suficiente para o dia a dia, mas organizado o bastante para feature, release e hotfix sem virar bagunca.

Se voce quiser primeiro uma visao rapida, veja [git-workflow.md](git-workflow.md). Este arquivo aqui e o guia completo com comandos.

Observacao: este guia usa `git switch`, que e o comando moderno para trocar e criar branches. Se sua maquina estiver com Git antigo, voce pode trocar por `git checkout`.

## 1. O que e GitFlow

GitFlow e uma estrategia de branches em que cada tipo de trabalho tem um lugar certo:

- `main`: codigo pronto para producao
- `develop`: codigo em integracao para a proxima entrega
- `feature/*`: novas funcionalidades
- `fix/*`: correcao normal que nao e incidente em producao
- `chore/*`: manutencao, CI, build, docs, infra
- `release/*`: preparacao de versao
- `hotfix/*`: correcao urgente saindo de `main`

Pense assim:

- trabalho normal entra em `develop`
- publicacao sai de `develop` para `main`
- incidente de producao sai de `main` e volta para `develop`

## 2. Quando usar cada branch

### `main`

Use para:

- estado publicado
- tags de release
- hotfixes que realmente vao para producao

Nao use para:

- desenvolver feature direto
- fazer experimento
- subir codigo sem PR

### `develop`

Use para:

- juntar tudo que vai entrar na proxima release
- receber PRs de `feature/*`, `fix/*` e `chore/*`

### `feature/*`

Use para:

- endpoint novo
- suporte a Kafka
- novo provider OAuth2
- nova regra de negocio

Exemplos:

- `feature/kafka-task-events`
- `feature/oauth2-github-login`
- `feature/task-export-csv`

### `fix/*`

Use para:

- bug normal encontrado durante desenvolvimento ou QA

Exemplos:

- `fix/task-owner-validation`
- `fix/jwt-expiration-parsing`

### `chore/*`

Use para:

- pipeline
- docker
- dependencia
- formatacao
- docs

Exemplos:

- `chore/github-actions-ci`
- `chore/update-testcontainers`

### `release/*`

Use para:

- congelar a release antes de publicar
- ajustes pequenos finais
- smoke test
- documentacao final
- versionamento e tag

Exemplo:

- `release/v1.2.0`

### `hotfix/*`

Use para:

- bug urgente em producao
- regressao em login, seguranca, rate limit, OAuth2, etc.

Exemplo:

- `hotfix/oauth2-redirect-fix`

## 3. Fluxo visual resumido

```text
main
  <- release/vX.Y.Z <- develop <- feature/*, fix/*, chore/*
  <- hotfix/*

Depois:
- release volta para develop
- hotfix volta para develop
```

## 4. Setup inicial do repositorio

Se o repositorio ainda nao tem `develop` no remoto:

```bash
git clone https://github.com/Emanuxl19/task-api.git
cd task-api

git switch main
git pull origin main

git switch -c develop
git push -u origin develop
```

Se `develop` ja existe no remoto:

```bash
git clone https://github.com/Emanuxl19/task-api.git
cd task-api

git fetch origin
git switch main
git pull origin main

git switch -c develop --track origin/develop
```

Para conferir o estado das branches:

```bash
git branch -a
git status
git remote -v
```

## 5. Fluxo normal de feature

Esse e o fluxo mais comum no projeto.

### Passo 1: atualizar `develop`

```bash
git switch develop
git pull origin develop
```

### Passo 2: criar a branch da feature

```bash
git switch -c feature/kafka-task-events
```

### Passo 3: trabalhar normalmente

Comandos uteis durante o desenvolvimento:

```bash
git status
git diff
git add .
git commit -m "feat: publish task created event"
```

Se preferir adicionar arquivos especificos:

```bash
git add pom.xml src/main/java/com/taskapi/service/TaskService.java
git commit -m "feat: add kafka producer for task events"
```

### Passo 4: publicar a branch

```bash
git push -u origin feature/kafka-task-events
```

### Passo 5: abrir PR para `develop`

Destino do PR:

```text
feature/kafka-task-events -> develop
```

### Passo 6: depois do merge

```bash
git switch develop
git pull origin develop

git branch -d feature/kafka-task-events
git push origin --delete feature/kafka-task-events
```

## 6. Fluxo de `fix/*`

O fluxo e igual ao de feature, mas a branch nasce de `develop` e volta para `develop`.

```bash
git switch develop
git pull origin develop

git switch -c fix/task-owner-validation
git push -u origin fix/task-owner-validation
```

PR:

```text
fix/task-owner-validation -> develop
```

## 7. Fluxo de `chore/*`

Tambem nasce de `develop` e volta para `develop`.

```bash
git switch develop
git pull origin develop

git switch -c chore/github-actions-ci
git push -u origin chore/github-actions-ci
```

PR:

```text
chore/github-actions-ci -> develop
```

## 8. Fluxo de release

Use release quando o que esta em `develop` esta pronto para ser preparado para producao.

### Passo 1: criar a release a partir de `develop`

```bash
git switch develop
git pull origin develop

git switch -c release/v1.2.0
git push -u origin release/v1.2.0
```

### Passo 2: fazer apenas ajustes finais

Aqui entram:

- correcao pequena de ultima hora
- docs
- versionamento
- smoke test

Aqui nao entram:

- feature nova
- refactor grande
- mudanca de arquitetura

### Passo 3: abrir PR da release para `main`

```text
release/v1.2.0 -> main
```

### Passo 4: depois do merge em `main`, criar a tag

```bash
git switch main
git pull origin main

git tag v1.2.0
git push origin v1.2.0
```

### Passo 5: voltar a release para `develop`

Voce pode fazer isso por PR:

```text
release/v1.2.0 -> develop
```

Depois limpe a branch:

```bash
git branch -d release/v1.2.0
git push origin --delete release/v1.2.0
```

## 9. Fluxo de hotfix

Hotfix sempre nasce de `main`, porque ele corrige o que esta em producao.

### Passo 1: criar a branch de hotfix

```bash
git switch main
git pull origin main

git switch -c hotfix/oauth2-redirect-fix
```

### Passo 2: corrigir o problema e commitar

```bash
git add .
git commit -m "fix: correct oauth2 redirect callback"
git push -u origin hotfix/oauth2-redirect-fix
```

### Passo 3: abrir PR para `main`

```text
hotfix/oauth2-redirect-fix -> main
```

### Passo 4: criar tag de patch release

```bash
git switch main
git pull origin main

git tag v1.2.1
git push origin v1.2.1
```

### Passo 5: levar o hotfix de volta para `develop`

```text
hotfix/oauth2-redirect-fix -> develop
```

Depois limpe a branch:

```bash
git branch -d hotfix/oauth2-redirect-fix
git push origin --delete hotfix/oauth2-redirect-fix
```

## 10. Como manter sua branch atualizada

Durante uma feature longa, `develop` continua andando. Sua branch precisa ser sincronizada.

### Opcao recomendada para branch compartilhada: merge

```bash
git fetch origin
git switch develop
git pull origin develop

git switch feature/kafka-task-events
git merge develop
```

### Opcao aceitavel para branch so sua: rebase

Use rebase apenas se voce entende que ele reescreve historico.

```bash
git fetch origin
git switch feature/kafka-task-events
git rebase origin/develop
```

Se voce ja fez push da branch e mais gente trabalha nela, prefira `merge`, nao `rebase`.

## 11. Como resolver conflito

Fluxo basico:

```bash
git status
```

Edite os arquivos em conflito, depois:

```bash
git add .
git commit
```

Se o conflito aconteceu durante `rebase`:

```bash
git add .
git rebase --continue
```

Para abortar:

```bash
git merge --abort
git rebase --abort
```

## 12. Comandos que voce vai usar toda semana

### Ver estado

```bash
git status
git branch -a
git log --oneline --decorate --graph -20
```

### Atualizar branch local

```bash
git fetch origin
git pull origin develop
```

### Criar branch

```bash
git switch -c feature/minha-feature
git switch -c fix/meu-bug
git switch -c chore/minha-tarefa
```

### Commitar

```bash
git add .
git commit -m "feat: add refresh token rotation"
```

### Publicar branch

```bash
git push -u origin feature/minha-feature
```

### Apagar branch local e remota

```bash
git branch -d feature/minha-feature
git push origin --delete feature/minha-feature
```

### Criar tag

```bash
git tag v1.2.0
git push origin v1.2.0
```

## 13. Convencao de commits

Padrao recomendado:

```text
feat: nova funcionalidade
fix: correcao de bug
test: atualiza ou adiciona testes
docs: documentacao
chore: manutencao
ci: pipeline
refactor: refatoracao sem mudar comportamento
```

Exemplos:

```text
feat: add kafka task event publisher
fix: prevent cross-user task access
test: cover refresh token revocation
docs: add complete gitflow guide
ci: run verify on pull requests
```

## 14. Erros comuns no GitFlow

### Erro 1: criar feature a partir de `main`

Errado:

```text
main -> feature/minha-feature
```

Certo:

```text
develop -> feature/minha-feature
```

### Erro 2: mandar hotfix para `develop` primeiro

Se o problema esta em producao, o hotfix tem que ir para `main` primeiro.

### Erro 3: esquecer de devolver release e hotfix para `develop`

Se nao voltar, a proxima release pode perder a correcao.

### Erro 4: colocar feature nova dentro de `release/*`

Release e para estabilizar, nao para expandir escopo.

### Erro 5: fazer push direto em `main`

Mesmo em repo solo, o caminho certo e PR + CI.

## 15. Regra pratica para decidir a branch

Pergunte:

- e trabalho normal? `feature/*`, `fix/*` ou `chore/*` a partir de `develop`
- e preparacao de entrega? `release/*` a partir de `develop`
- e incidente em producao? `hotfix/*` a partir de `main`

## 16. Exemplo completo usando este projeto

Vamos supor que voce queira adicionar Kafka para publicar o evento `task.created`.

### Desenvolvimento

```bash
git switch develop
git pull origin develop
git switch -c feature/kafka-task-events
```

Implemente:

- dependencia `spring-kafka`
- produtor
- evento `task.created`
- testes

Depois:

```bash
git add .
git commit -m "feat: publish task created event"
git push -u origin feature/kafka-task-events
```

PR:

```text
feature/kafka-task-events -> develop
```

### Release

Quando essa e outras features estiverem prontas:

```bash
git switch develop
git pull origin develop
git switch -c release/v1.3.0
git push -u origin release/v1.3.0
```

PR:

```text
release/v1.3.0 -> main
```

Tag:

```bash
git switch main
git pull origin main
git tag v1.3.0
git push origin v1.3.0
```

Volta para `develop`:

```text
release/v1.3.0 -> develop
```

## 17. Checklist rapido antes de abrir PR

- branch nasceu da base correta
- branch esta atualizada
- commits estao claros
- testes rodaram
- nada sensivel entrou no Git
- PR aponta para o destino correto

## 18. Resumo final

Use esta regra simples:

- `develop` recebe o trabalho do dia a dia
- `main` recebe apenas release e hotfix
- `release/*` prepara publicacao
- `hotfix/*` corrige producao

Se voce seguir isso com disciplina, o historico fica limpo, a release fica previsivel e o risco de perder correcao entre branches cai bastante.
