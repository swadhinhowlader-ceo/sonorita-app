package com.sonorita.assistant.ai

import com.sonorita.assistant.ai.AIEngine

class MultiAgentSwarm(private val aiEngine: AIEngine) {

    data class Agent(
        val id: String,
        val name: String,
        val role: AgentRole,
        val specialization: String,
        val model: String = "default"
    )

    enum class AgentRole {
        RESEARCHER, WRITER, REVIEWER, CODER, ANALYST,
        TRANSLATOR, PLANNER, CRITIC, CREATOR, EDITOR
    }

    data class Task(
        val id: String,
        val description: String,
        val assignedAgent: Agent,
        val status: TaskStatus = TaskStatus.PENDING,
        val result: String? = null
    )

    enum class TaskStatus {
        PENDING, IN_PROGRESS, COMPLETED, FAILED
    }

    data class SwarmResult(
        val taskDescription: String,
        val agentResults: Map<String, String>,
        val finalResult: String,
        val executionTime: Long
    )

    private val agents = mutableListOf(
        Agent("researcher_1", "Researcher", AgentRole.RESEARCHER, "Deep research and fact-finding"),
        Agent("writer_1", "Writer", AgentRole.WRITER, "Content creation and formatting"),
        Agent("reviewer_1", "Reviewer", AgentRole.REVIEWER, "Quality check and feedback"),
        Agent("coder_1", "Coder", AgentRole.CODER, "Code generation and debugging"),
        Agent("analyst_1", "Analyst", AgentRole.ANALYST, "Data analysis and insights")
    )

    suspend fun executeSwarmTask(taskDescription: String): SwarmResult {
        val startTime = System.currentTimeMillis()

        // Break task into subtasks
        val subtasks = decomposeTask(taskDescription)

        // Assign agents to subtasks
        val assignments = assignAgents(subtasks)

        // Execute in parallel (simulated)
        val results = mutableMapOf<String, String>()

        for ((agent, subtask) in assignments) {
            try {
                val result = executeAgentTask(agent, subtask)
                results[agent.name] = result
            } catch (e: Exception) {
                results[agent.name] = "Failed: ${e.message}"
            }
        }

        // Synthesize results
        val finalResult = synthesizeResults(taskDescription, results)

        return SwarmResult(
            taskDescription = taskDescription,
            agentResults = results,
            finalResult = finalResult,
            executionTime = System.currentTimeMillis() - startTime
        )
    }

    private suspend fun decomposeTask(task: String): List<String> {
        val response = aiEngine.query(
            "Break this task into 3-5 subtasks that can be handled by different specialists: \"$task\"\n" +
            "Format: one subtask per line, numbered.",
            emptyList()
        )

        return response.content.lines()
            .filter { it.trim().matches(Regex("^\\d+\\..*")) }
            .map { it.replace(Regex("^\\d+\\.\\s*"), "").trim() }
            .ifEmpty { listOf(task) }
    }

    private fun assignAgents(subtasks: List<String>): Map<Agent, String> {
        val assignments = mutableMapOf<Agent, String>()

        subtasks.forEachIndexed { index, subtask ->
            val agent = agents[index % agents.size]
            assignments[agent] = subtask
        }

        return assignments
    }

    private suspend fun executeAgentTask(agent: Agent, subtask: String): String {
        val rolePrompt = when (agent.role) {
            AgentRole.RESEARCHER -> "You are a researcher. Find and present key information."
            AgentRole.WRITER -> "You are a writer. Create well-written, engaging content."
            AgentRole.REVIEWER -> "You are a reviewer. Analyze for quality, errors, and improvements."
            AgentRole.CODER -> "You are a coder. Write clean, efficient code."
            AgentRole.ANALYST -> "You are an analyst. Provide data-driven insights."
            else -> "You are a specialist. Complete this task expertly."
        }

        val response = aiEngine.query(
            "$rolePrompt\n\nTask: $subtask\n\nProvide your expert response.",
            emptyList()
        )

        return response.content
    }

    private suspend fun synthesizeResults(originalTask: String, results: Map<String, String>): String {
        val resultsText = results.entries.joinToString("\n\n") { (agent, result) ->
            "[$agent]:\n$result"
        }

        val response = aiEngine.query(
            "Synthesize these specialist responses into a cohesive final answer for: \"$originalTask\"\n\n" +
            "Specialist outputs:\n$resultsText\n\n" +
            "Combine all insights into a clear, actionable final result.",
            emptyList()
        )

        return response.content
    }

    // Pre-built swarm pipelines
    suspend fun researchAndWrite(topic: String): SwarmResult {
        return executeSwarmTask("Research and write a comprehensive article about: $topic")
    }

    suspend fun codeAndReview(description: String): SwarmResult {
        return executeSwarmTask("Write code for: $description, then review and improve it")
    }

    suspend fun analyzeAndReport(data: String): SwarmResult {
        return executeSwarmTask("Analyze this data and create a report: $data")
    }

    fun getAgentList(): String {
        return buildString {
            appendLine("🤖 Available Agents (${agents.size}):")
            agents.forEach { agent ->
                appendLine("• ${agent.name} (${agent.role}) — ${agent.specialization}")
            }
        }
    }

    fun addCustomAgent(name: String, role: AgentRole, specialization: String) {
        agents.add(Agent("custom_${agents.size}", name, role, specialization))
    }
}
