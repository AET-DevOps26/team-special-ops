INSERT INTO series (id, title, seasons_count, episodes_count)
VALUES ('958661e6-226c-5117-9318-5e3265598767', 'Stranger Things', 1, 3);

INSERT INTO episode (id, series_id, season, episode_number, episode_index, title, summary) VALUES
('f0f21e77-e689-54af-a568-b9dd658f48f3', '958661e6-226c-5117-9318-5e3265598767', 1, 1, 1, 'Chapter One', 'Will vanishes. Eleven appears.'),
('8f01c6aa-e147-5603-84aa-16674faa4f88', '958661e6-226c-5117-9318-5e3265598767', 1, 2, 2, 'Chapter Two', 'Eleven stays with Mike. Barb is taken.'),
('3a95da46-c9bb-58b1-a6c4-8964f15b5815', '958661e6-226c-5117-9318-5e3265598767', 1, 3, 3, 'Chapter Three', 'Joyce uses Christmas lights to talk to Will.');
