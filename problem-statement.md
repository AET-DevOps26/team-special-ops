**Problem Statement**
1. What is the main functionality?
This is a web app that lets users ask free text questions about a TV show they are currently watching and returns answers grounded in the show's content, guaranteed not to reference any episode beyond the user's stated watch progress. Users set their progress, ask a question in natural language, and receive a cited, spoiler-safe answer drawn only from episodes they have already seen.

2. Who are the intended users?
Anyone watching a TV show at their own pace and not yet caught up:
	•	First-time viewers picking up older shows
	•	Re-watchers who have forgotten details
	•	Couples or groups watching together where members are at different points
3. How will you integrate GenAI meaningfully?
The GenAI component is the chat feature. It ensures that users are not being spoiled while giving them all the previous context necessary to enjoy the show:
	1.	Answer generation requires an LLM because user questions are open-ended natural language. Keyword search cannot answer "why is Walt distant from his family right now?"
	2.	Retrieval-Augmented Generation ensures every answer consists the show's actual content, preventing the LLM from leaking spoilers from its training data (popular shows are well-represented in pre-training data). Also allows the app to retrieve the correct data to answer the user's query.
	3.	Metadata-filtered retrieval is the safety mechanism. Every content chunk in the vector store is tagged with its episode index. At query time, retrieval filters on episode_index ≤ user_progress before similarity search. The LLM physically cannot see future-episode content, because that content never enters its context window.
	4.	Agent architecture allows for adding additional information to raw episode summaries with structured metadata that it found by doing web search (entities, topic tags, "first revealed" episode for character information)

**Scenarios**
1. Catching up after a longer break. Maria is on Breaking Bad S2E4 and gets confused about a scene. She opens the app, confirms her progress, and asks "why is Walt hiding things from Skyler?" Retrieval pulls only chunks from episodes she has seen, the LLM produces a cited answer, no later plot points are referenced.
2. Avoiding accidental spoilers. Maria heard about Gus Fring recently and asks "who is Gus Fring?" while still at S2E4. Gus is introduced later. Retrieval returns no relevant chunks under the progress filter, and the system responds that this character has not appeared yet in the episodes she has watched. No partial information is leaked.
3. Synced rewatch. David is rewatching with a partner who is two seasons behind him. He sets his progress in the app to match their shared watching point — not his own completed status — and uses the app as a spoiler-safe reference during episodes they watch together.
