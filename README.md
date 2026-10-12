# Vaadin AI Showcases

A demo application showing how AI can be integrated into a Vaadin Flow UI. It is built with Vaadin 25.4 (alpha),
Spring Boot 4, Spring AI (OpenAI) and Java 25, and runs on an in-memory H2 database seeded with sample customer data.

## Showcases

| View | Route | What it shows |
|------|-------|---------------|
| AI Chat | `/` | Chat with an LLM |
| Customers (draiv) | `/customers` | AI goal bar that drives the live UI (search, filter) via draiv |
| AI Form Filler (Text) | `/form` | Fills a form from free text |
| AI Form Filler (Image) | `/form/receipt` | Fills a form from a receipt image |
| AI Grid | `/grid` | Grid generated from a natural-language question (LLM writes SQL) |
| AI Chart | `/chart` | Chart generated from a natural-language question (LLM writes SQL) |
| AI Navigation | `/navigation` | Navigates the app from natural-language requests |

LLM-generated SQL runs against a read-only database account (`AI_READER`).

## Configuration

Set `OPENAI_API_KEY` before starting. Without it the app still starts, but AI features report themselves as disabled.
Optional: `OPENAI_MODEL` (default `gpt-5-mini`), `OPENAI_REASONING_EFFORT` (default `low`), `OPENAI_TEXT_MODEL`.

The project uses commercial Vaadin components (`vaadin-ee`, AI extensions), which require a Vaadin license.

## Running

To start the application in development mode, import it into your IDE and run the `Application` class. 
You can also start the application from the command line by running: 

```bash
./mvnw
```

To build the application in production mode, run:

```bash
./mvnw package
```

## Getting Started

The [Quick Start](https://vaadin.com/docs/v25/getting-started/quick-start) tutorial helps you get started with Vaadin in 
around 10 minutes. This tutorial walks you through building a simple application, introducing the core concepts along 
the way.
