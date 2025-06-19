# RAG_legal: Legal Question Answering System with Retrieval Augmented Generation

## Introduction
RAG_legal is a legal question answering system that leverages Retrieval Augmented Generation (RAG) techniques. It combines the power of large language models and vector databases to provide accurate and professional legal advice based on Chinese legal knowledge.

## Features
1. **Intelligent Question Answering**: Input your legal questions and get intelligent answers based on Chinese legal provisions, quickly resolving your legal doubts.
    - Based on a massive legal knowledge base.
    - Provide references to relevant legal provisions.
    - Support follow - up questions and context understanding.
2. **Legal Provision Retrieval**: Quickly search for various Chinese laws and regulations, obtain accurate legal provision content and interpretations to help you understand legal regulations in depth.
    - Cover multiple fields such as the Constitution, Civil Law, and Criminal Law.
    - Support keyword search and advanced retrieval.
    - Provide provision update reminders.
3. **Legal Risk Assessment**: Analyze your legal risks, provide professional risk assessment reports and response suggestions to help you effectively prevent legal risks.
    - Contract risk analysis.
    - Compliance check for business activities.
    - Customized risk response plans.

## Technical Stack
### Back - end
- **Spring Boot**: A framework for building Java - based enterprise applications, used to build RESTful APIs.
- **Milvus**: A high - performance vector database for storing and retrieving legal text vectors.
- **DashScope SDK**: Used to interact with the Qwen large - language model to generate answers.

### Front - end
- **Tailwind CSS**: A utility - first CSS framework for quickly building user interfaces.
- **Font Awesome**: A font and icon toolkit used to display icons on the page.
- **Chart.js**: A JavaScript library for creating charts, although not fully utilized in the current project.

## Directory Structure
```
RAGJurisChat/
├── .gitignore
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── org/
│   │   │   │   └── bigdata/
│   │   │   │       └── rag_legal/
│   │   │   │           ├── controller/
│   │   │   │           │   └── LegalController.java
│   │   │   │           ├── demos/
│   │   │   │           │   └── web/
│   │   │   │           │       ├── BasicController.java
│   │   │   │           │       ├── PathVariableController.java
│   │   │   │           │       └── User.java
│   │   │   │           ├── entity/
│   │   │   │           │   └── Reference.java
│   │   │   │           ├── service/
│   │   │   │           │   └── LegalService.java (not provided in the code snippet)
│   │   │   │           ├── utils/
│   │   │   │           │   ├── QwenClient.java
│   │   │   │           │   ├── VectorSearchResult.java
│   │   │   │           │   └── MilvusService.java
│   │   │   │           └── RagLegalApplication.java
│   │   └── resources/
│   │       └── static/
│   │           └── index.html
└── src/
    └── test/
        └── java/
            └── org/
                └── bigdata/
                    └── rag_legal/
                        └── RagLegalApplicationTests.java
```

## Installation and Setup
### Prerequisites
- Java 17
- Maven
- Milvus running on `localhost:19530`
- DashScope API key set as an environment variable `DASHSCOPE_API_KEY`

### Steps
1. **Clone the project**:
    ```bash
    git clone https://github.com/WindyStu/RAGJurisChat.git
    cd RAGJurisChat
    ```
2. **Build the project**:
    ```bash
    mvn clean package
    ```
3. **Run the application**:
    ```bash
    java -jar target/RAG_legal-0.0.1-SNAPSHOT.jar
    ```

## Usage
### API Endpoints
- **`/api/ask`**: Send a POST request with a legal question in the request body to get an answer.
    - Example:
    ```bash
    curl -X POST -H "Content-Type: text/plain" -d "What laws are violated if I obtain public property illegally and resell it?" http://127.0.0.1:8080/api/ask
    ```
### Web Interface
Open `http://127.0.0.1:8080/html` in your browser to access the web interface, where you can experience various legal consultation functions.

## Contributing
If you want to contribute to this project, please follow these steps:
1. Fork the repository.
2. Create a new branch for your feature or bug fix.
3. Make your changes and commit them with clear and concise commit messages.
4. Push your changes to your forked repository.
5. Open a pull request in the original repository.

## License
This project is licensed under the Apache License 2.0. See the `LICENSE` file for details.