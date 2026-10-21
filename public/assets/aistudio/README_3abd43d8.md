# DayQuest prompt + ML starter data

This bundle contains a research-informed master build prompt and two starter CSV files.

## Files

- `DAYQUEST_MASTER_PROMPT.md` — full product, architecture, UX, data, ML, privacy, test, and phase gates.
- `assets/ml/seed.csv` — 1,650 balanced synthetic rows across 11 categories and English/Hindi/Hinglish, with `family_id` to prevent translated variants leaking across train/test splits.
- `assets/ml/time_parser_cases.csv` — 51 test cases for English, Devanagari Hindi, and Hinglish time/duration/date/range parsing.
- `assets/ml/dataset_audit.json` — row counts, balance, duplicate check, dataset hashes, and limitations for this starter snapshot.
- `assets/ml/generate_seed.py` — reproducible generator for the synthetic seed CSV and parser cases.

## Important dataset note

The seed corpus is deliberately marked `synthetic_seed`. It is a useful balanced starting point and regression fixture, **not** a production-quality external benchmark and **not** evidence that a trained model exists or is accurate. Train a real model, evaluate on an independently authored review set, and report metrics honestly. Group on `family_id` when splitting. The duration/difficulty targets are synthetic heuristics and should be reviewed before using them as a quality benchmark.

## Suggested destination inside the app

Copy the CSVs to `assets/ml/seed.csv` and `assets/ml/time_parser_cases.csv`. The master prompt requires a real model artifact, tokenizer, label map, training/conversion scripts, and measured evaluation results; these CSVs do not replace those files.
