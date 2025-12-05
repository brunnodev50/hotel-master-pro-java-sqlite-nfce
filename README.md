# 🏨 Hotel Master Pro

> Sistema completo de Gestão Hoteleira e Frente de Caixa (PDV) desenvolvido em Java.

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![SQLite](https://img.shields.io/badge/SQLite-07405E?style=for-the-badge&logo=sqlite&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)
![Status](https://img.shields.io/badge/Status-Concluído-green?style=for-the-badge)

## 📋 Sobre o Projeto

O **Hotel Master Pro** é uma aplicação desktop robusta desenvolvida para simular o ambiente real de gestão de um hotel com ponto de venda integrado. O foco do projeto foi criar uma arquitetura resiliente, com persistência de dados local e geração avançada de documentos fiscais simulados.

Diferente de sistemas acadêmicos simples, este projeto implementa regras de negócio reais, como cálculo de troco, validação de estoque, status de produtos e emissão de comprovantes não fiscais (NFC-e) com códigos de barras legíveis por leitores a laser.

---

## 🚀 Funcionalidades Principais

### 🏨 Gestão Hoteleira
* **Mapa de Ocupação:** Visualização em tempo real dos quartos (Livre/Ocupado) com filtros de busca.
* **Check-in Rápido:** Alocação de hóspedes e cálculo automático de diárias.
* **Cadastro de Hóspedes:** CRUD completo com validação de CPF e máscaras de entrada.

### 🛒 Frente de Caixa (PDV Avançado)
* **Busca Inteligente:** Localização de produtos por Nome, Marca ou **Código de Barras (EAN)**.
* **Gestão de Venda:** Cálculo automático de subtotal, total e **troco**.
* **Validação de Estoque:** Impede a venda de itens sem saldo ou inativos.
* **Documento Auxiliar:** Geração automática de recibo em PDF (Layout 80mm - Térmico).

### 📄 Emissão de Documentos (PDF)
* Geração de **NFC-e (Simulada)** utilizando a biblioteca iText.
* Renderização vetorial de **Código de Barras (Barcode 128)** compatível com leitores físicos.
* Geração de **QR Code** integrado via API.
* Layout responsivo alinhado (descrição à esquerda, valores à direita).

### ⚙️ Administração e Segurança
* **Controle de Estoque:** Cadastro com EAN e status (Ativo/Inativo).
* **Backup e Restore:** Ferramenta integrada para salvar e restaurar o banco de dados SQLite.
* **Configurações:** Personalização de Razão Social e CNPJ da empresa.

---

## 📸 Screenshots

*(Adicione aqui prints do seu sistema para valorizar o portfólio)*

| Dashboard / Mapa | Frente de Caixa (PDV) |
|:---:|:---:|
| ![Dashboard](https://via.placeholder.com/400x300?text=Print+do+Dashboard) | ![Caixa](https://via.placeholder.com/400x300?text=Print+do+Caixa) |

| Recibo Gerado (PDF) | Controle de Estoque |
|:---:|:---:|
| ![PDF Recibo](https://via.placeholder.com/400x300?text=Print+do+PDF) | ![Estoque](https://via.placeholder.com/400x300?text=Print+do+Estoque) |

---

## 🛠️ Tecnologias Utilizadas

* **Linguagem:** Java 17 (JDK)
* **Interface Gráfica:** Swing (Customizado com Flat Design System)
* **Banco de Dados:** SQLite (JDBC)
* **Relatórios/PDF:** iText (OpenPDF)
* **Gerenciamento de Dependências:** Maven
* **Empacotamento:** jpackage (Gerador de executável .exe)

---

## 📦 Como Rodar o Projeto

### Pré-requisitos
* Java JDK 17 ou superior.
* Maven instalado.

### Passo a passo
1.  Clone o repositório:
    ```bash
    git clone [https://github.com/brunnodev50/hotel-master-pro-java-sqlite-nfce.git](https://github.com/brunnodev50/hotel-master-pro-java-sqlite-nfce.git)
    ```
2.  Entre na pasta do projeto:
    ```bash
    cd hotel-master-pro-java-sqlite-nfce
    ```
3.  Compile e execute com Maven:
    ```bash
    mvn clean install
    java -jar target/HotelMaster-1.0-SNAPSHOT.jar
    ```

*Nota: O sistema cria automaticamente o banco de dados `hotel_db_novo.db` na primeira execução.*

---

## 🧠 Desafios e Aprendizados

Durante o desenvolvimento da **Versão V47.0**, os principais desafios superados foram:

1.  **Manipulação de PDF:** Desenhar um layout térmico (80mm) "na unha" usando Java, garantindo que o código de barras não ficasse distorcido.
2.  **Lógica de Estoque:** Implementar a lógica de "Status Ativo/Inativo" para manter o histórico de vendas mesmo após um produto sair de linha.
3.  **Persistência de Dados:** Criar um mecanismo de migração automática que adiciona colunas novas ao banco de dados SQLite sem perder os dados do usuário antigo.

---

## 🤝 Contato

Se você gostou deste projeto ou quer trocar uma ideia sobre desenvolvimento Java, me chame no LinkedIn!

<a href="https://www.linkedin.com/in/brunno-henrique-4a514b14a/" target="_blank">
<img src="https://img.shields.io/badge/-LinkedIn-%230077B5?style=for-the-badge&logo=linkedin&logoColor=white" target="_blank">
</a>
