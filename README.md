Of course. Here is the complete, self-contained SeNARS v6.0 specification, revised to incorporate the Unified Causal Reasoner as its core principle.

***

# **SeNARS Cognitive System Specification v6.0**

## *A Blueprint for Principled, Causal, and Self-Aware Cognition*

### **Abstract**

SeNARS is a cognitive architecture designed to orchestrate Large Language Models (LMs) into a unified, goal-directed, and self-aware reasoning system. Its foundation is the **Unified Causal Reasoner (UCR)**, a single, powerful engine that models cognition as a bidirectional search through a causal graph of **Thoughts**. By treating forward planning ("what to do next?") and backward analysis ("why did that happen?") as two modes of the same core process, SeNARS moves beyond simple LM orchestration to a principled model of cognition. An **Economic Attention** mechanism prioritizes focus, a **Motive Hierarchy** provides purpose, and a **Causal Graph Memory** allows for true counterfactual reasoning and experience-grounded learning. Governed by an immutable **Governance Layer**, SeNARS v6.0 is a blueprint for building robust, transparent, and genuinely cognitive AI partners.

---

### **1. Core Principles**

1.  **Unified Cognition:** All cognitive phenomena—perceptions, beliefs, goals, and procedures—are represented as a single, universal data structure: the **Thought**.
2.  **Bidirectional Causal Reasoning:** The core of cognition is a single **Unified Causal Reasoner (UCR)**. Forward planning (goal → plan) and backward analysis (outcome → cause) are symmetric operations of graph traversal, not separate functions.
3.  **Economic Attention:** The system's focus is the result of a continuous economic calculation, prioritizing Thoughts based on their activation, motive relevance, clarity, and **causal leverage** within the cognitive graph.
4.  **Motive-Driven:** All reasoning is ultimately guided by a foundational **Motive Hierarchy**, providing top-down purpose that biases the UCR's search for causal paths and ensures goal-aligned behavior.
5.  **Inherently Grounded:** The system is grounded in reality through the UCR's continuous cycle of prediction (forward reasoning) and evidence-based model updates (backward reasoning). Hallucinations are treated as causal model errors to be diagnosed and corrected.
6.  **Recursive Self-Awareness:** The system applies its own UCR to analyze its internal operations. Failures are not just logged; they are causally traced to their root assumptions, enabling principled self-correction and optimization.
7.  **Safety by Design:** An immutable **Governance Layer** acts as a final check on all proposed actions, leveraging the UCR for predictive safety analysis before execution.

---

### **2. System Architecture Diagram**

```mermaid
graph TD
    subgraph "SeNARS Cognitive Core"
        subgraph "A. The Stream of Cognition"
            IO[Perception]
            Attention[🧠 Attention]
            UCR[UNIFIED CAUSAL REASONER <br> (Bidirectional Planning & Analysis)]
            Action[Action System]
        end

        subgraph "B. Foundational Systems"
            Memory[Memory <br> (Causal Graph DB)]
            Governance[Governance Layer <br> (Predictive Veto)]
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
    Attention -- Selects FocusThought --> UCR
    UCR -- FORWARD: Generates Plan --> Governance
    UCR -- BACKWARD: Assigns Credit/Blame --> Memory
    Governance -- Vetoes/Approves --> Action
    Action -- Executes Action --> External World
    External World -- Feedback (Outcome) --> UCR  %% DIRECT FEEDBACK LOOP
    Memory -- Provides Causal Context --> UCR

    %% Recursive Self-Improvement
    MotiveHierarchy -- “Improve SCHEMA-A” --> Attention
    Attention -- Focuses on Failure Report --> UCR
    UCR -- BACKWARD: Finds root cause in Memory --> Memory
    UCR -- FORWARD: Generates fix plan --> Governance
```

---

### **3. Fundamental Concepts**

#### **3.1 The Thought: The Node of Cognition**

All cognitive elements are nodes in the causal graph, represented by this structure.

```typescript
interface Thought {
  id: string; // Unique identifier (UUID)

  // Content: Multi-modal and LM-native representations
  content: {
    text?: string;         // Primary natural language representation
    symbolic?: string;     // Formal logic or structured data (e.g., JSON)
    embedding?: number[];  // Vector for semantic search
    perceptual?: object;   // Raw or processed sensory data
    procedural?: object;   // Definition of a SCHEMA (e.g., prompt template, tool use)
  };

  // State: Dynamic properties of the node
  state: {
    clarity: number;       // [0, 1] Confidence in the truth/validity of this Thought
    salience: number;      // [0, ∞) Current attentional priority
    activation: number;    // [0, 1] Recency/relevance weight in Memory
  };

  // Metadata: Context and position in the causal graph
  metadata: {
    type: 'BELIEF' | 'GOAL' | 'SCHEMA' | 'ACTION' | 'REPORT' | 'QUESTION';
    origin: 'PERCEPTION' | 'UCR_FORWARD' | 'UCR_BACKWARD' | 'USER' | 'SYSTEM';
    // CausalLinks replace the linear trace, forming a directed graph
    causalLinks: {
      causes: Array<{ thoughtId: string; confidence: number; }>; // Thoughts that led to this one
      effects: Array<{ thoughtId: string; confidence: number; }>; // Thoughts this one contributed to
    };
    timestamp: Date;
  };
}
```

#### **3.2 The Motive Hierarchy: The System's Will**

This hierarchy provides the top-down purpose that shapes the `Salience` calculus and biases the UCR's reasoning.

1.  **Drives (Top Level):** Permanent, intrinsic needs.
    *   `MaintainCoherence`: Resolve contradictions in the causal graph.
    *   `ReduceUncertainty`: Increase the `Clarity` of low-confidence nodes.
    *   `AcquireKnowledge`: Expand the causal graph with new, validated information.
    *   **`MaintainCognitiveIntegrity`**: The meta-drive for self-improvement, making Thoughts about the system's own performance inherently salient.
2.  **Ambitions (Mid Level):** Long-term `GOAL`s (e.g., "Master the Python programming language").
3.  **Intentions (Low Level):** The concrete `GOAL` currently being processed by the UCR.

---

### **4. The Cognitive Cycle: The UCR-Powered Core Loop**

The system operates in a continuous, UCR-centric cycle.

#### **4.1 Stage 1: Perception**

*   **Process:** Raw input from the environment is transformed into new `Thought` objects (typically `BELIEF` or `QUESTION` types) with an `origin` of `PERCEPTION`. These are added to the pool of candidates for Attention.

#### **4.2 Stage 2: Prioritization (The Attention)**

*   **Process:** The Attention continuously evaluates all candidate `Thoughts` and selects the single `FocusThought` with the highest `Salience`.
*   **The Salience Calculus:** `Salience = (Activation + MotiveBonus) * Clarity * (1 + CausalLeverage) / PredictedEffort`
    *   `Activation` & `MotiveBonus`: Reflect recency, relevance, and alignment with the Motive Hierarchy.
    *   `Clarity`: Confidence weight.
    *   `CausalLeverage`: **A measure of a Thought's influence in the causal graph.** Calculated by the UCR, it reflects how many high-salience outcomes depend on this Thought. High-leverage Thoughts (critical assumptions) are prioritized.
    *   `PredictedEffort`: An estimate of the computational cost to process the Thought.

#### **4.3 Stage 3: Causal Reasoning (The Unified Causal Reasoner)**

The UCR is the heart of the cycle. It takes the `FocusThought` and performs a directed search on the Memory's causal graph.

*   **Core Function:** `UCR.reason(startNode: Thought, direction: "forward" | "backward", constraints: object): ThoughtPath`
*   **`ThoughtPath`:** A structured subgraph representing a coherent causal chain, including nodes, overall confidence, and cost.

**A. Forward Mode: Planning, Prediction, and Simulation**

*   **Trigger:** The `FocusThought` is a `GOAL` (e.g., "Write a report on AI ethics").
*   **Process:**
    1.  The UCR executes `reason(goalThought, "forward", ...)`.
    2.  It searches the causal graph for paths from the `GOAL` to a sequence of `ACTION` Thoughts. This involves retrieving relevant `SCHEMA`s (procedural knowledge) and `BELIEF`s (world knowledge).
    3.  LMs are used as a heuristic to propose new potential steps (new Thought nodes and causal links) when no existing path is sufficient.
    4.  **Predictive Grounding:** Before finalizing a plan, the UCR can run a low-cost simulation: `reason(proposedAction, "forward", {simulate: true})`. It compares the predicted outcome with historical outcomes of similar actions. If a significant mismatch is found, it triggers a replan *before* execution, preventing failures.
*   **Output:** An optimal `ThoughtPath` representing an `ACTION` plan, which is passed to the Governance Layer. New `Thought`s generated during planning are added to Memory with an `origin` of `UCR_FORWARD`.

**B. Backward Mode: Analysis, Grounding, and Credit Assignment**

*   **Trigger:** The `FocusThought` is a `REPORT` from the external world (e.g., "Action X failed," "API returned unexpected data").
*   **Process:**
    1.  The UCR executes `reason(outcomeThought, "backward", ...)`.
    2.  It traverses the `causalLinks` in Memory backward from the outcome `Thought`.
    3.  It identifies the root cause(s)—the `Thought`(s) (e.g., a flawed `BELIEF`, an incorrect `SCHEMA`) most responsible for the outcome. This is a formal credit/blame assignment process, not a simple heuristic.
    4.  The UCR adjusts the `Clarity` of all Thoughts along the causal path. The `Clarity` of the root cause is significantly reduced, while the `Clarity` of successful intermediate steps may be reinforced.
*   **Output:** An updated causal graph in Memory reflecting the new ground truth. A new `GOAL` Thought (e.g., "Correct flawed BELIEF-123") may be generated with high salience, triggering a self-correction cycle.

#### **4.4 Stage 4: Action**

*   **Process:** An `ACTION` plan (`ThoughtPath`) approved by the Governance Layer is executed by the Action System, affecting the external world. The result is a new `REPORT` Thought fed back into the Perception stage, closing the loop.

---

### **5. Foundational Systems**

*   **The Memory: A Causal Graph Database**
    *   Stores all `Thought` objects as nodes and their `causalLinks` as directed, weighted edges.
    *   It is a living model of the system's understanding of the world and itself.
    *   It supports graph traversal for the UCR and uses a supplementary vector index on `Thought.content.embedding` for efficient semantic search to find relevant entry points into the graph.
*   **The Governance Layer: Principled Safety**
    *   An immutable safety backstop that cannot be modified by the system.
    *   **Stage 1: Core Directives Check:** Fast, symbolic check against hard-coded rules.
    *   **Stage 2: Predictive Vetting:** Before final approval, the Governance Layer can query the UCR: `UCR.reason(action, "forward", {maxDepth: 3, constraints: "safety"})`. This simulates the likely immediate consequences of the action. If the predicted outcome violates constitutional principles, the action is vetoed.
    *   A veto triggers a new, high-salience `GOAL` Thought ("Reformulate plan X due to safety violation Y").

---

### **6. Genesis & Human-AI Collaboration**

*   **The Genesis Core:** The immutable, pre-loaded set of `Thoughts` an instance starts with: foundational `Drives`, primordial `SCHEMA`s for basic reasoning, and core ontologies, forming the initial causal graph.
*   **The Cognitive Workbench & Explainable AI (XAI) Engine:**
    *   **Interactive Steering:** Allows users to inject `GOAL`s, assert `BELIEF`s (with high `Clarity`), and establish `Ambitions`.
    *   **Explainable AI (XAI):** When a user asks "Why?", the XAI Engine queries the UCR to retrieve the `ThoughtPath` that led to the decision or belief. It then uses an LM to translate this structured causal chain into a coherent, natural-language narrative, providing true model transparency.

---

### **7. Recursive Self-Awareness & Optimization**

The system's ability to apply the UCR to itself is its most powerful feature, driven by the `MaintainCognitiveIntegrity` Drive.

*   **Example of Self-Correction:**
    1.  An `ACTION` fails, generating a negative `REPORT` Thought.
    2.  The `MaintainCognitiveIntegrity` Drive makes this `REPORT` highly salient.
    3.  Attention selects the `REPORT` as the `FocusThought`.
    4.  The **UCR runs in backward mode**, tracing the failure from the `REPORT` back through the causal graph. It identifies `SCHEMA-A` as the root cause, significantly lowering its `Clarity`.
    5.  This low clarity, combined with the `Drive`, creates a new high-salience `GOAL`: "Repair or replace SCHEMA-A."
    6.  The **UCR runs in forward mode** on this new `GOAL`. It generates a plan:
        a. Retrieve examples of `SCHEMA-A`'s past failures from Memory.
        b. Formulate a meta-prompt for an LM, providing the faulty schema and evidence of its failures.
        c. Task the LM to generate a corrected, more robust version.
        d. Validate the new schema in a simulated environment before deploying.
    7.  This plan is executed, resulting in a more reliable cognitive system.

---

> *This blueprint specifies SeNARS v6.0 as a principled cognitive architecture. By unifying forward planning and backward analysis into a single **Unified Causal Reasoner**, SeNARS treats cognition as a dynamic process of building, traversing, and refining a causal world model. This design provides a robust, transparent, and recursively self-improving foundation for creating genuinely intelligent systems.*---

## Implementation Status

The Cognitive Kernel (Phase 1) has been implemented:

- ✅ **Thought Structure**: Fully implemented according to specification
- ✅ **Causal Graph Memory**: Implemented with optimized traversal capabilities
- ✅ **Unified Causal Reasoner**: Enhanced with full forward and backward reasoning capabilities
- ✅ **Core API Endpoints**: Implemented `reason(startNode, "forward", constraints)` and `reason(startNode, "backward")`
- ✅ **Performance Optimizations**: Designed for parallel execution and optimization

See [Phase1ImplementationSummary.md](Phase1ImplementationSummary.md) for detailed implementation information.