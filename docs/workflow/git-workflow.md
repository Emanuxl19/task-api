

















# Git Workflow Visual

Para o passo a passo completo com comandos, veja [gitflow-guide.md](gitflow-guide.md).

## Branch Map

```mermaid
flowchart LR
    main["main<br/>producao"]:::stable
    develop["develop<br/>proxima release"]:::integration

    feature["feature/*"]:::work --> prFeature["PR para develop"]:::pr --> develop
    fix["fix/*"]:::work --> prFix["PR para develop"]:::pr --> develop
    chore["chore/*"]:::work --> prChore["PR para develop"]:::pr --> develop

    develop --> release["release/vX.Y.Z"]:::release
    release --> prRelease["PR para main"]:::pr --> main
    release --> backRelease["merge de volta para develop"]:::sync --> develop

    main --> hotfix["hotfix/*"]:::hotfix
    hotfix --> prHotfix["PR para main"]:::pr --> main
    hotfix --> backHotfix["merge de volta para develop"]:::sync --> develop

    classDef stable fill:#16324f,color:#fff,stroke:#16324f;
    classDef integration fill:#2a6041,color:#fff,stroke:#2a6041;
    classDef work fill:#e8f1f8,color:#111,stroke:#5b88b2;
    classDef release fill:#f0c36a,color:#111,stroke:#9d6b00;
    classDef hotfix fill:#d96c6c,color:#fff,stroke:#8f1f1f;
    classDef pr fill:#f4f4f4,color:#111,stroke:#666;
    classDef sync fill:#f9efe0,color:#111,stroke:#a67c52;
```

## Day To Day Flow

```mermaid
flowchart TD
    start["Nova tarefa"] --> branch["Criar branch<br/>feature/..., fix/..., chore/..."]
    branch --> work["Implementar e testar"]
    work --> push["Push da branch"]
    push --> pr["Abrir PR para develop"]
    pr --> ci["CI: mvn verify"]
    ci --> review{"CI verde e revisao ok?"}
    review -- nao --> work
    review -- sim --> merge["Merge em develop"]
    merge --> next{"Vai publicar?"}
    next -- nao --> start
    next -- sim --> rel["Criar release/vX.Y.Z"]
    rel --> harden["Ajustes finais e smoke test"]
    harden --> prMain["PR da release para main"]
    prMain --> tag["Merge em main + tag vX.Y.Z"]
    tag --> sync["Merge da release de volta para develop"]
```

## Quick Rules

- `main` guarda apenas codigo pronto para producao.
- `develop` junta o que vai entrar na proxima entrega.
- `feature/*`, `fix/*` e `chore/*` sempre nascem de `develop`.
- `release/*` nasce de `develop` quando voce vai publicar.
- `hotfix/*` nasce de `main` quando precisa corrigir producao com urgencia.

## Practical Examples

### New Feature

```text
develop
  -> feature/oauth2-login
  -> PR para develop
  -> merge em develop
```

### Scheduled Release

```text
develop
  -> release/v1.3.0
  -> PR para main
  -> merge em main
  -> tag v1.3.0
  -> merge da release para develop
```

### Urgent Production Fix

```text
main
  -> hotfix/login-rate-limit
  -> PR para main
  -> merge em main
  -> tag v1.3.1
  -> merge do hotfix para develop
```

## One-Line Summary

Pense assim:

- trabalho normal entra em `develop`
- publicacao sai de `develop` para `main`
- incidente de producao sai de `main` e volta para `develop`
