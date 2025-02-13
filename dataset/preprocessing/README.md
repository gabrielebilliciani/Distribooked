# Preprocessing

This folder contains preprocessing scripts for different components of the project. The preprocessing steps are divided into three main categories:

## 📚 Books Preprocessing (`books-preprocessing/`)
Scripts for cleaning and preparing book datasets.

- `01-chunker.ipynb` → Takes the Amazon Reviews data sets (JSON) and chunks them into smaller files.
- `02-preprocess_all_chunks.ipynb` → Preprocesses all chunks.
- `03-gutenberg_preprocessing.ipynb` → Preprocessing for Gutenberg dataset.
- `04-merge_datasets.ipynb` → Merges Gutenberg and Amazon datasets.
- `05-final_clean.ipynb` → Final cleaning and formatting before use.

## 📖 Libraries Preprocessing (`libraries-preprocessing/`)
Scripts for cleaning and preparing library-related datasets.

- `libraries_preprocessing.ipynb` → Processes and formats library-related data.

## 👥 Users Creation (`users-creation/`)
Scripts for generating synthetic user data.

- `users_generator.ipynb` → Generates synthetic user data for testing.

## 📦 Database Creation (`database-creation/`)
Scripts for creating the database starting from the preprocessed data, including embedded files included in the documents stored in MongoDB.

---

For further details on the preprocessing stage and data set preparation for the project, please refer to the project documentation.

