See GraphRAG Movie Quiz mockups.pdf for mockups of the screens.

The backside and frontside of the cards will be provided as PNG images.
The text on the cards should be rendered with this font:
"Syne Neo", PLAIN, 42
Though it should be truncated if it doesn't fit
The color of the text should be RGB 1, 64, 99

The mockup shows a lot of information on the clue cards, but for the
actual game we will only have this
Backside of the card:
Movie name / Actor name under the CLUE text
Frontside of the card:
Movie name at the top followed by a list of (top 5) actors

Regarding the answer-field:
They must not choose an autocomplete option, but they must have exact
spelling (except casing) so it would probably be best to choose an
autocomplete option. The string matching should be case insensitive.
If two actors have the same name, it doesn't matter which one you choose.
You submit the guess by pressing enter, or clicking the arrow button.
The autocomplete should be alphabetically ordered.
The API for submitting an answer should only take the name.

The frontend will get the UUID of the game and need to remember that.
If the brower is refreshed it should fetch the same game and show the
game state as it was (if the game still exists).

Other things to note on the UI:
* Double-clicking a token still only consumes it once
* Double-clicking a card just flips it as a regular click

The frontend should make use of the Neo4j Needle framework:
https://neo4j.design/40a8cff71/p/159f18-needle-design-system
