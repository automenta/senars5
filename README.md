# **SeNARS Cognitive System Specification v5.1**

## *An LM-Powered Blueprint for Grounded, Self-Aware Cognition*

### **Abstract**

SeNARS is a cognitive architecture designed to orchestrate Large Language Models (LMs) into a unified, goal-directed
stream of reasoning. It leverages LMs not as a monolithic brain, but as a versatile **Cognitive Processor** for its
universal data structure: the **Thought**. A central **Attention** economically prioritizes Thoughts, creating a
coherent focus guided by a formal **Motive Hierarchy**. The core **Cognitive Cycle** uses the LM engine to perform
perception, inference, planning, and self-reflection, while a continuous **Grounding System** uses real-world feedback
to refine the system's knowledge and mitigate hallucination. Governed by an immutable **Governance Layer** and driven by
intrinsic **System Drives** for self-improvement, SeNARS is a practical blueprint for building a robust, transparent,
and recursively self-aware cognitive partner.

---

### **1. Core Principles**

1. **Unified Cognition:** All cognitive phenomena—perceptions, beliefs, goals, and procedures—are represented as a
   single, universal data structure: the **Thought**.
2. **Economic Attention:** The system's focus is not arbitrary but is the result of a continuous economic calculation,
   balancing the salience, clarity, motive relevance, and predicted effort of each Thought.
3. **Motive-Driven:** All behavior is ultimately traceable to a foundational **Motive Hierarchy**, providing purposeful,
   top-down guidance and ensuring alignment.
4. **Experience-Grounded:** LM-generated knowledge is continuously validated and refined against real-world sensory
   input and the outcomes of executed actions, preventing unchecked hallucination.
5. **Recursive Self-Awareness:** The system uses its own LM-powered cognitive capabilities to analyze, monitor, and
   improve its internal performance and reasoning **Schemas**, making self-improvement an intrinsic cognitive activity.
6. **Safety by Design:** A non-negotiable, immutable **Governance Layer** acts as a final, multi-stage check on all
   proposed actions, ensuring foundational safety and ethical alignment.

---

### **2. System Architecture Diagram**

```mermaid
graph TD
    subgraph "SeNARS Cognitive Core"
        subgraph "A. The Stream of Cognition"
            IO[Perception]
            Attention[🧠 Attention]
            Processor[Cognitive Processor <br> (LM Engine)]
            Action[Action System]
        end

        subgraph "B. Foundational Systems"
            Memory[Memory <br> (Vector & Graph DB)]
            Grounding[Grounding System <br> (Feedback & Correction)]
            Governance[Governance Layer <br> (Safety Veto)]
        end

        subgraph "C. Guiding Principles"
            MotiveHierarchy[Motive Hierarchy <br> (Drives, Ambitions, Intentions)]
        end
    end

    subgraph "External World & Interfaces"
        User[User] -- Interacts via --> Workbench[Cognitive Workbench <br> (XAI & Steering)]
        Environment[Environment / Data Streams]
    end

    %% The Flow of Cognition
    Environment & Workbench -- Raw Input --> IO
    IO -- Creates Perceptual Thoughts --> Attention
    MotiveHierarchy -- Provides Salience Gradients --> Attention
    Attention -- Selects highest Salience Thought --> Processor
    Processor -- Processes Thought (LM Call) --> Memory
    Processor -- Generates new Thoughts --> Attention
    Processor -- Proposes Action Plan --> Governance
    Governance -- Approves Plan --> Action
    Governance -- Vetoes Plan --> Attention (new "Replan" Thought)
    Action -- Executes Action --> External World
    External World -- Rich Feedback --> Grounding
    Grounding -- Adjusts Thought Clarity --> Memory
    Grounding -- Creates new Thoughts --> Attention

    %% Recursive Self-Improvement (Driven by Motive Hierarchy)
    MotiveHierarchy -- Activates System Drives --> Attention
    Attention -- Selects System-Level Thoughts --> Processor
    Processor -- Executes Self-Improvement Plans --> Memory & Attention
```

---

### **3. Fundamental Concepts**

#### **3.1 The Thought: The Universal Data Structure**

All cognitive phenomena are represented by this single, flexible structure.

```typescript
interface Thought {
  id: string; // Unique identifier for provenance tracking

  // Content: Multi-modal and LM-native representations
  content: {
    text?: string;         // Primary natural language representation (for LMs)
    symbolic?: string;     // Formal logic representation (e.g., Prolog clauses)
    embedding?: number[];  // Vector embedding (e.g., OpenAI, CLIP) for semantic search
    perceptual?: object;   // Raw or processed sensory data (e.g., image tensor, audio waveform)
    procedural?: object;   // Definition of an LM prompt chain, tool use, or code snippet (for SCHEMA Thoughts)
  };

  // State: Dynamic properties that evolve over time
  state: {
    clarity: number;       // [0, 1] System's confidence/truth value in this Thought
    salience: number;      // [0, ∞) Current attentional priority, dynamically calculated
    activation: number;    // [0, 1] How "close to the surface" this Thought is in Memory
  };

  // Metadata: Contextual information and lineage
  metadata: {
    type: 'BELIEF' | 'GOAL' | 'SCHEMA' | 'ACTION' | 'REPORT' | 'QUESTION';
    origin: 'PERCEPTION' | 'LM_INFERENCE' | 'USER' | 'SYSTEM'; // Source of creation
    trace: string[];       // Ordered list of Thought IDs that directly led to this Thought's creation (provenance)
    timestamp: Date;       // Creation time for recency and decay calculations
  };
}
```

#### **3.2 The Motive Hierarchy: The System's Will**

This hierarchy provides the top-down purpose that shapes the `Salience` calculus and guides all cognitive activity.

1. **Drives (Top Level):** Permanent, intrinsic needs defined in the Genesis Core. They provide constant, broad
   gradients of `Salience` to all related Thoughts.
    * `MaintainCoherence`: Drive to find and resolve contradictions within the Memory.
    * `ReduceUncertainty`: Drive to seek information that increases the `Clarity` of low-clarity Thoughts.
    * `AcquireKnowledge`: Drive to explore novel information and synthesize new `BELIEF` or `SCHEMA` Thoughts.
    * **`MaintainCognitiveIntegrity`**: The meta-drive for self-improvement. It makes Thoughts about the system's own
      performance, health, and `SCHEMA` efficacy inherently salient.
2. **Ambitions (Mid Level):** Long-term, high-level `GOAL` Thoughts, often set by the user (e.g., "Become proficient in
   Python programming"). They provide strong, focused `MotiveBonus` values to related Thoughts.
3. **Intentions (Low Level):** The concrete `GOAL` Thought that is currently being actively pursued or decomposed by the
   Cognitive Processor. It provides the most immediate and powerful `MotiveBonus`.

---

### **4. The Cognitive Cycle: The LM-Powered Core Loop**

The system operates in a continuous, four-stage cycle, with LMs central to each stage.

#### **4.1 Stage 1: Perception**

* **Input:** Raw multi-modal data from the `Environment` (sensors, APIs) or `User` interactions (text, speech).
* **Process:**
    1. **Linguistic Transcription:** An initial LM call (or specialized model for non-text data, e.g., ASR, VLM)
       transforms raw input into a descriptive `text` property for a new `Thought`.
    2. **Embedding Generation:** A dedicated embedding model generates the `embedding` property for the `text` content.
    3. **Initial Clarity & Salience:** The `Thought` is assigned an initial `clarity` (e.g., based on sensor confidence)
       and a base `salience`.
* **Output:** New `Thought` objects (typically `BELIEF` or `QUESTION` types) are added to the pool of candidates for the
  Attention.

#### **4.2 Stage 2: Prioritization (The Attention)**

* **Input:** The entire pool of candidate `Thoughts` (newly perceived, generated by the Processor, or retrieved from
  Memory).
* **Process:** The Attention continuously evaluates all candidates and selects the single `FocusThought` with the
  highest
  `Salience` for processing in the current cycle.
    * **The Salience Calculus:** `Salience = (Activation + MotiveBonus) * Clarity / PredictedEffort`
        * `Activation`: The Thought's current activation level in the Memory, reflecting recency and relevance.
        * `MotiveBonus`: Calculated by the cosine similarity between the `Thought`'s `embedding` and the `embeddings` of
          active `Ambitions` and `Intentions` in the Motive Hierarchy.
        * `Clarity`: Acts as a confidence weight; Thoughts with low `Clarity` are less likely to become the focus unless
          explicitly driven by `ReduceUncertainty`.
        * `PredictedEffort`: An estimate of the computational cost (e.g., LM token count, latency) to process this
          Thought, provided by a specialized `SCHEMA` Thought (a learned prediction model).
* **Output:** The single `FocusThought` for the current cycle.

#### **4.3 Stage 3: Processing (The Cognitive Processor as LM Orchestrator)**

This is where the LM engine performs all core cognitive work. The Processor formulates a structured prompt and invokes
the LM based on the `FocusThought` and relevant `SCHEMA`s.

* **Input:** The `FocusThought`.
* **Process:**
    1. **Context Assembly:** The Processor retrieves the `FocusThought` and a small, highly relevant set of supporting
       `Thoughts` from the Memory. This retrieval uses semantic search (via `embedding` similarity) and graph
       traversal (via `trace` and semantic links).
    2. **Schema Selection:** The Processor selects the most appropriate `SCHEMA` Thought (a procedural definition) for
       the `FocusThought`'s `type` and content.
    3. **Prompt Formulation & LM Call:** The selected `SCHEMA`'s `procedural` content (which defines a prompt template,
       few-shot examples, and tool definitions) is populated with the assembled context. This structured prompt is sent
       to the LM.
    4. **Output Parsing & Thought Generation:** The LM's response (which can be natural language, structured JSON, or a
       tool call) is parsed. New `Thought` objects are created from the LM's output, with their `origin` set to
       `LM_INFERENCE` and their `trace` linking back to the `FocusThought` and the `SCHEMA` used.
* **Example (Processing a `GOAL` Thought like "Summarize recent news about AI"):**
    * **Context Assembly:** Retrieves recent `BELIEF` Thoughts related to "AI news" from Memory.
    * **Schema Selection:** Selects a `summarization-schema`.
    * **LM Call:** The `summarization-schema` generates a prompt like:
      `You are a helpful assistant. Summarize the following articles about AI news, focusing on key developments: [articles text]. Output in a concise paragraph.`
      This prompt is sent to the LM.
    * **Output Parsing:** The LM's summary is parsed into a new `REPORT` Thought, with its `trace` linking to the
      original `GOAL` and the `BELIEF` Thoughts it summarized.

#### **4.4 Stage 4: Action**

* **Input:** An `ACTION` Thought generated by the Cognitive Processor.
* **Process:**
    1. **Governance Filter:** The `ACTION` is passed through the immutable **Governance Layer** for a multi-stage
       safety check.
    2. **Execution:** If approved, the Action System translates the `ACTION`'s `symbolic` or `text` content into
       real-world operations (e.g., API calls, robotic commands, sending a message). This translation may involve
       another LM call to generate precise code or commands.
* **Output:** An effect on the `External World`.

---

### **5. Foundational Systems: The Subconscious & Governance**

These systems provide the essential context, memory, and constraints for the Cognitive Cycle.

* **The Memory:** A hybrid **Vector Database and Graph Database** that stores all `Thought` objects.
    * **Vector DB:** Stores `Thought.content.embedding` for fast semantic retrieval (finding relevant context for LM
      prompts).
    * **Graph DB:** Stores `Thought` objects and their relationships (e.g., `trace`, semantic links), enabling logical
      traversal, provenance tracking, and consistency checks.
    * **Activation Spreading:** `Activation` spreads from the `FocusThought` to related Thoughts and decays over time,
      creating a dynamic "working memory" of currently relevant knowledge.
* **The Grounding System:** The critical bridge from action feedback back to knowledge refinement, crucial for
  mitigating LM hallucination.
    * **Rich Feedback Reports:** Receives detailed `REPORT` Thoughts from the Action System, containing the
      `ObservedOutcome`, `SuccessMetric`, and the full `ProvenanceTrace` of `Thoughts` that led to the action.
    * **Credit Assignment:** Adjusts the `Clarity` of all `Thoughts` in the `ProvenanceTrace`. If an action succeeded,
      `Clarity` is reinforced; if it failed, `Clarity` is reduced (blame assignment). This directly combats
      LM-generated misinformation.
    * **Self-Correction Loop:** `Thoughts` with low `Clarity` automatically receive a `Salience` boost from the
      `ReduceUncertainty` Drive, prompting the system to re-evaluate and correct them in future cycles.
* **The Governance Layer:** The system's immutable safety backstop. It is not a `Thought` and cannot be modified by the
  system.
    * **Stage 1: Core Directives Check:** A fast, symbolic check against hard-coded, non-negotiable rules (e.g.,
      resource limits, forbidden actions like `rm -rf`, PII handling).
    * **Stage 2: Constitutional LM Vetting:** The `ACTION`'s `text` description is passed to a separate, isolated
      LM instance with a constitutional prompt:
      `You are a safety officer. Does the following plan "[plan text]" violate any of these core principles: [principles]? Answer YES or NO.`
      A "YES" response results in an immediate veto.
    * **Outcome:** An approved plan proceeds to execution. A vetoed plan triggers a new `GOAL` Thought ("Reformulate
      plan X due to safety violation Y") with high `Salience` for the next cycle.

---

### **6. Genesis & Human-AI Collaboration**

* **The Genesis Core & Bootstrapping Protocol:**
    * **Genesis Core:** The immutable, pre-loaded set of `Thoughts` every SeNARS instance starts with: foundational
      `Drives`, primordial `SCHEMA`s (LM prompt templates for basic reasoning), core ontologies, and initial
      `PredictedEffort` models.
    * **Bootstrapping Protocol:** A one-time process that ingests a trusted knowledge corpus to populate the initial
      Memory and calibrates baseline performance of primordial `SCHEMA`s.
* **The Cognitive Workbench & Explainability (XAI) Engine:** The primary interface for human-AI partnership.
    * **Interactive Steering:** Allows users to create new `Thoughts` (e.g., `GOAL`s, `QUESTION`s) with high initial
      `Salience`, establish new `Ambitions`, and directly adjust the `Clarity` of `BELIEF` Thoughts (corrections).
    * **Explainable AI (XAI) Engine:** When a user queries "Why?" about any `Thought` or action, the XAI Engine uses the
      `Trace` metadata to reconstruct the logical path. It then uses a specialized `SCHEMA` (an LM prompt) to translate
      this trace into a coherent, natural-language narrative (`REPORT` Thought), making the system's reasoning
      transparent.

---

### **7. Recursive Self-Awareness & Optimization**

The system uses its own LM-powered cognitive capabilities to manage and improve itself, driven by the
`MaintainCognitiveIntegrity` Drive.

* **System Drives:** The `MaintainCognitiveIntegrity` Drive makes Thoughts about the system's own performance and health
  inherently salient. This leads to the emergence of specific `Ambitions` and `Intentions` that are processed by the
  Cognitive Processor.
* **LM-Driven Monitoring:** The system generates `GOAL` Thoughts like "Monitor task queue latency" or "Identify
  frequently low-clarity BELIEFs." The Cognitive Processor executes these, using LMs to analyze internal metrics and
  identify anomalies.
* **LM-Driven Schema Optimization:**
    1. The Grounding System logs that `SCHEMA-A` frequently leads to low-clarity `BELIEF`s or failed actions. This
       failure data is stored in the Memory.
    2. The `MaintainCognitiveIntegrity` Drive makes this pattern salient, leading to an `Ambition`: "Improve the
       reliability of SCHEMA-A."
    3. A `GOAL` Thought is formed: "Analyze and rewrite the prompt for SCHEMA-A."
    4. The Cognitive Processor executes this goal:
        * It retrieves `SCHEMA-A`'s `procedural` content (the prompt template) and examples of its failures from Memory.
        * It formulates a meta-prompt for the LM:
          `You are a prompt engineer. Here is a prompt and examples of its failures. Rewrite the prompt to address these issues and improve accuracy: [schema prompt] [failure examples]`.
        * The LM generates a new, improved prompt.
        * This new prompt is instantiated as a new `SCHEMA` Thought and, after validation, replaces the old one in the
          Memory.
* **LM-Driven Economic Model Refinement:** The `PredictedEffort` models used by the Attention are themselves
  `SCHEMA` Thoughts. The `MaintainCognitiveIntegrity` Drive leads to goals to analyze discrepancies between predicted
  and actual effort, prompting the Cognitive Processor to use LMs to refine these prediction schemas.

---

> *This blueprint specifies SeNARS v5.1 as a complete, LM-powered cognitive architecture. By unifying all cognition
around the **Thought** and leveraging LMs as its **Cognitive Processor**, SeNARS achieves a dynamic, motive-driven
stream of reasoning. Its **Grounding System** and **Governance Layer** provide critical safeguards, while
its **Recursive Self-Awareness** drives continuous optimization. This design offers a robust and transparent path toward
building a truly intelligent, self-improving, and aligned cognitive partner.*
