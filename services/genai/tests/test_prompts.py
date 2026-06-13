from genai.chain import AllowedSummary, format_summaries


def test_format_summaries_includes_all_allowed_episodes():
    summaries = [
        AllowedSummary(
            episodeIndex=1,
            season=1,
            episodeNumber=1,
            title="Pilot",
            summary="A boy vanishes.",
        ),
        AllowedSummary(
            episodeIndex=2,
            season=1,
            episodeNumber=2,
            title="Chapter Two",
            summary="Eleven appears.",
        ),
    ]

    block = format_summaries(summaries, max_chars=10_000)

    assert "[S1E1]" in block
    assert "episode_index=1" in block
    assert "A boy vanishes." in block
    assert "[S1E2]" in block
    assert "Eleven appears." in block


def test_format_summaries_respects_char_cap():
    summaries = [
        AllowedSummary(
            episodeIndex=1,
            season=1,
            episodeNumber=1,
            title="One",
            summary="x" * 100,
        ),
        AllowedSummary(
            episodeIndex=2,
            season=1,
            episodeNumber=2,
            title="Two",
            summary="y" * 100,
        ),
    ]

    block = format_summaries(summaries, max_chars=120)

    assert "[S1E1]" in block
    assert "[S1E2]" not in block
