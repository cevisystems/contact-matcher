# 🕵️‍♂️ contact-matcher
Java tool for detecting duplicate contacts in Excel using the Levenshtein algorithm with multi-criteria weighting (name, email, address). Includes text normalization, similarity score sorting, and classification accuracy.

Java/Spring Boot application that detects potentially duplicate contacts in Excel files using advanced matching techniques.

## 🔍 Comparison Method
- **Levenshtein Algorithm** for string similarity calculation
- **Multi-criteria weighting**:
  - Name (40% weight)
  - Email (30% weight)
  - Address (20% weight)
  - Postal code (10% weight)
- **Minimum threshold**: 0.5 similarity to consider duplicate

## 📊 Sorting Method
- Results sorted by **descending score** (highest similarity first)
- Precision classification:
  - **High**: ≥ 0.75
  - **Low**: ≥ 0.5 and < 0.75

## 🛠 Technologies
- Java 17 + Spring Boot
- Apache POI (Excel processing)
- Apache Commons Text (Levenshtein distance)
- Custom string normalization algorithm

## 📋 Features
1. Loads contacts from Excel files (.xlsx)
2. Detects duplicates based on semantic similarity
3. Generates structured report with match reasons
4. Flexible similarity threshold configuration

## 💻 Running in Visual Studio Code

### Prerequisites
- VS Code with:
  - [Java Extension Pack](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack)
  - [Spring Boot Extension](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-spring-initializr)
- JDK 17+ (recommended: [Eclipse Temurin](https://adoptium.net/))
- Maven 3.8+

### Setup Instructions

1. **Clone the repository**:
   ```bash
   git clone https://github.com/your-username/your-repo.git
   code your-repo

   
