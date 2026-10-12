# DIA DE SORTE CAMPEÃO V7 — MOTOR V5 WINDOWS

O aplicativo Android usa a mesma regra do arquivo Python V5 de Windows, e não a regra alterada da V6:

- 8 tirinhas (01-04, 05-08, ..., 29-31) e todas as 255 combinações;
- Candidatas: somente 1 ou 2 tirinhas; classificar por atraso atual desc., depois falhas desc.; exibir as 20 primeiras;
- Escolher as tirinhas para excluir, ou 0 para não excluir;
- **Jogo**: dentre as dezenas disponíveis, ordenar pela **maior frequência nos últimos 100 concursos**, empate pelo número crescente; escolher 7 com no máximo 4 pares e 4 ímpares, completando se necessário;
- **Mês da Sorte**: menor frequência nos últimos 100, desempate pelo maior atraso e pelo número do mês; usuário pode trocar;
- PDF colorido com as 7 dezenas marcadas, 12 meses, mês marcado, retiradas e 255 combinações em páginas adicionais.

## Importar resultados

Selecionar TXT em Downloads, com linhas como:
`1 - 03,05,08,09,19,21,30 - Fevereiro`

O aplicativo exige histórico contínuo e sem linhas inválidas; não altera os dados silenciosamente.

## Gerar APK

Enviar **o conteúdo desta pasta** para a raiz de um repositório GitHub, incluindo `.github/workflows/android-apk.yml`. Em Actions, executar `Gerar APK Dia de Sorte Campeao V7`. Baixar o artefato APK após o sucesso.

## PDF

Botão `SALVAR PDF COLORIDO + RANKING`: o Android pede a pasta de destino. Escolha Downloads. O arquivo contém volante marcado e ranking completo.

**Nota:** o estudo de atrasos não altera a probabilidade de sorteios independentes.
