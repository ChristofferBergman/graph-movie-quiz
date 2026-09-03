See GraphRAG Movie Quiz mockups.pdf for mockups of the screens.

Questions are displayed as "Who in XXX (xxxx) starred in another movie with
YYY (born yyyy)?", using the movie's release year and the person's birth year.

The backside and frontside of the cards will be provided as PNG images.
The text on the cards should be rendered with this font:
"Syne Neo", MEDIUM, 42
Though it should be truncated if it doesn't fit
The color of the text should be RGB 1, 64, 99

The mockup shows a lot of information on the clue cards, but for the
actual game we will only have this
Backside of the card:
Movie name / Actor name under the CLUE text
Frontside of the card:
Movie name at the top followed by a list of (top 5) actors

Card text should scale down when necessary to remain within the available
card area, particularly on the mobile layout. A flipped card can be zoomed by
right-clicking it on desktop or holding a finger pressed on it on a touch
device. The helper text should describe the interaction appropriate for the
current pointer type.

Regarding the answer-field:
They must not choose an autocomplete option, but they must have exact
spelling (except casing) so it would probably be best to choose an
autocomplete option. The string matching should be case insensitive.
If two actors have the same name, it doesn't matter which one you choose.
You submit the guess by pressing enter, or clicking the arrow button.
The autocomplete should be alphabetically ordered.
When suggestions are open, the up and down arrow keys should highlight an
option and Enter should select the highlighted option. Escape closes the
suggestions.
The API for submitting an answer should only take the name.

The frontend will get the UUID of the game and need to remember that.
If the brower is refreshed it should fetch the same game and show the
game state as it was (if the game still exists).

Other things to note on the UI:
* Double-clicking a token still only consumes it once
* Double-clicking a card just flips it as a regular click

The frontend should make use of the Neo4j Needle framework:
https://neo4j.design/40a8cff71/p/159f18-needle-design-system
