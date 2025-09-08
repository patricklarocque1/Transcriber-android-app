# Architecture Graphs

## Module Dependencies

```mermaid
graph TD
  Transcriber_android_app[Transcriber-android-app]:::package
  wear[wear]:::library
  wristlingo[wristlingo]:::library
  wristlingo --> wear

  classDef service fill:#e1f5fe,stroke:#0277bd,stroke-width:2px
  classDef library fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px
  classDef package fill:#e8f5e8,stroke:#388e3c,stroke-width:2px
```

## Layered Architecture

```mermaid
graph TB
  subgraph Layer0 ["Layer 0"]
    Transcriber_android_app[Transcriber-android-app]
    wristlingo[wristlingo]
  end
  subgraph Layer1 ["Layer 1"]
    wear[wear]
  end
  wristlingo --> wear
```

## Technology Stack

```mermaid
graph LR
  subgraph gradle ["Gradle"]
    Transcriber_android_app[Transcriber-android-app]
  end
  subgraph kotlin ["Kotlin"]
    wear[wear]
  end
  subgraph kotlin_cpp ["Kotlin+Cpp"]
    wristlingo[wristlingo]
  end
```

## Api Surface

```mermaid
graph TD
  Transcriber_android_app[Transcriber-android-app]:::module
  wear[wear]:::module
  wear_MainActivity[MainActivity]:::class
  wear --> wear_MainActivity
  wristlingo[wristlingo]:::module
  wristlingo_ExportActivity[ExportActivity]:::class
  wristlingo --> wristlingo_ExportActivity
  wristlingo_SimpleVad[SimpleVad]:::class
  wristlingo --> wristlingo_SimpleVad
  wristlingo_WearBridge[WearBridge]:::class
  wristlingo --> wristlingo_WearBridge

  classDef module fill:#e3f2fd,stroke:#1976d2,stroke-width:2px
  classDef class fill:#fff3e0,stroke:#f57c00,stroke-width:1px
```

