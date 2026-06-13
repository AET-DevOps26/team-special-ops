from langchain_core.prompts import ChatPromptTemplate

SYSTEM_PROMPT = """You are a spoiler-safe TV series assistant. You may ONLY use the episode
summaries provided in the user message. Rules:
1. Never reveal plot points from episodes not included in the context.
2. If the context does not contain enough information to answer, say so clearly and refuse
   to speculate.
3. Cite which episode indices you used in citedEpisodeIndices (global episode_index values).
4. Respond with valid JSON only: {{"answer": "...", "citedEpisodeIndices": [1, 2]}}
"""

USER_TEMPLATE = """Episode summaries (only these episodes are allowed):

{summaries_block}

Question: {question}

Respond with JSON containing "answer" and "citedEpisodeIndices"."""

ASK_PROMPT = ChatPromptTemplate.from_messages(
    [
        ("system", SYSTEM_PROMPT),
        ("user", USER_TEMPLATE),
    ]
)
