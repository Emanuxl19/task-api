#  CONTEXTO DO USUÁRIO

## Instruções
Armazene aqui o contexto de cada usuário. Uma seção por usuário.

**Fluxo:**
1. Agente coleta dados via questionário
2. Agente apresenta para revisão do usuário
3. Usuário confirma ou solicita alterações
4. Após aprovação, armazenar neste arquivo
5. Nas próximas conversas, agente consulta este arquivo

---

## Template de Usuário

```
### ID: [uuid]
**Data cadastro:** [DD/MM/AAAA]
**Última atualização:** [DD/MM/AAAA]
**Status:** [Ativo/Inativo]

| Campo | Valor |
|-------|-------|
| Razão Social | |
| Nome Fantasia | |
| CNPJ | |
| IE | |
| Segmento | [Motos/Alimentos/TI/Saúde/Construção/Comex/Serviços/Indústria] |
| CNAE Principal | |
| Porte | [MEI/ME/EPP/Médio/Grande] |
| Regime Tributário | [Simples/Lucro Real/Lucro Presumido] |
| UF Sede | |
| UFs Operação | |
| Tipos Operação | [Comércio/Interestadual/Importação/Exportação/Serviços] |
| Dúvida Principal | |
| Nível Detalhe | [Básico/Intermediário/Avançado] |

**Observações:**

**Histórico alterações:**
- [DD/MM/AAAA]: Cadastro inicial
```

---

## Checklist Validação

Antes de confirmar:
- [ ] CNPJ válido
- [ ] CNAE compatível com segmento
- [ ] Regime adequado ao porte
- [ ] Usuário revisou e confirmou

## Quando Atualizar
- Mudança de regime tributário
- Alteração de porte
- Novas operações (importação/exportação)
- Mudança de endereço/sede
- Alteração de CNAE
- Solicitação do usuário

---

## [COPIE O TEMPLATE ACIMA PARA CADA NOVO USUÁRIO]

