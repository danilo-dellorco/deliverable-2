# ISW2 - Deliverable2 [![Build Status](https://travis-ci.com/danilo-dellorco/deliverable2.svg?branch=master)](https://travis-ci.com/danilo-dellorco/deliverable2) [![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=danilo-dellorco_deliverable-2&metric=code_smells)](https://sonarcloud.io/dashboard?id=danilo-dellorco_deliverable-2)
Codice relativo al secondo Deliverable. Consente la creazione del dataset e la valutazione dei classificatori su tali dataset.

# Configurazione
1. Creare un file ```paths.config```
2. Inserire nel file ```git-folder-path=PATH``` specificando dove si vuole clonare/aprire la repository in locale
3. Configurare nel file  ```src/main/java/utils/Parameters.java``` i nomi dei progetti che si vogliono analizzare

# Manuale
1. Lanciare ```src/main/java/dataset/CreateDataset.java``` per generare i dataset
2. Lanciare ```src/main/java/dataset/AnalyzeDataset.java``` per analizzare i dataset tramite i classificatori
I dataset ed i risultati dell'analisi vengono generati nella cartella ```output```
