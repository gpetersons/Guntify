# Sprotify Beta 9 Android Auto physical tests

1. Install signed Beta 9 over Beta 8; verify no loss of local data. Do not uninstall.
2. Open **Ievietot rindā** while 5+ songs are queued. Verify several songs can be scrolled, with correct track titles, artists and covers.
3. Check the now-playing indicator follows the current song, and the next tracks appear in the correct shared-phone order. Exact section headers depend on Android Auto.
4. Tap a **later** queue song; it should start that song, not a stale stream, and preserve the context/remaining queue.
5. Change the current song from the phone; the Android Auto queue/current indicator should update. Append more Radio recommendations, ensure the queue grows without navigating back to an unrelated browse screen.
6. Android Auto **text/browse search**: select a track. It should open an action list offering **Atskaņot tagad** and **Pievienot rindai**.
7. Choose **Pievienot rindai**. The existing song must continue at its previous position without restarting; the added song appears next in the shared queue. Do not silently replace the whole queue.
8. Choose **Atskaņot tagad** in search and verify the selected song starts. Voice **Play from search** must retain expected play behavior.
9. Repeat Add with an already-pending track: no duplicate/manual-queue corruption.
10. Verify Radio, three Shuffle modes, Repeat, Favorite, fast Next/steering wheel, reconnect and audio continuity still work.
11. If Android Auto does not expose the browsable search actions or groups the queue differently, record the car screenshot/Android Auto version; do not assume Media3 can force the host's UI labels.
