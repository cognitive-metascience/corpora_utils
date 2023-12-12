# Corpora Utils
This repository contains utilities to process our corpora (implemented in Java because of the file folder structure). Now the repository contains three utilities:
* JSONIndexer – a Lucene index maker that relies on our metadata files (includes full-text from text files as well),
* JSONSearcher - a utility to search in the index, also from CSV files (for example, by looking at a CSV file with DOI numbers), 
* TextCorpusMaker – to export text-only contents (also by relying on regular expressions) from our corpora.


