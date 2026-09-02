Game title: GraphRAG Online Movie Quiz

The purpose of the game is to explain and showcase the power of GraphRAG
over traditional RAG, and also the benefit of them both over non-RAG.

When the game starts the player gets to type their name, and then they
gets a question in the form "Who in XXX starred in another movie with YYY?". 

The player can enter their answer in a text field, which starts to get
populated with possible actors in the database whos name starts with what
they have typed.

If they answer correctly they get one point and move over to the next
question. If it is not correct they loose and their final score is
however many points they collected so far.

Each question has a 40-second time limit. The remaining seconds are shown in
the UI. If time expires, the game ends as if the player answered incorrectly.

If they get more points then number 3 on the high-score list they are
added to that list (which is seen on the left-side of the screen).

Below the answer-field it shows the question as a graph with the end
nodes (the XXX and YYY) represented by a clue card, but they are
grey meaning they can't be turned over.

See slide 1 in the mockup PDF.

To their help they have two different types of tokens (they have two
tokens of each sort at their disposal): RAG and GraphRAG tokens.

If they choose a RAG token (when chosen there should be an animation
showing it flipp over to a grey brick) it will enable the clue card
on the far right (the movie YYY) becomes enabled (changed from grey to
color) and if clicked it is turned over (again with an animation showing
it flip). There you will see the first 5 credited actors in that movie.
Right-clicking the flipped card will show a zoomed-in version as a
popup dialog. On a touch device, holding a finger pressed on the flipped
card provides the same zoom interaction. This token is now forfit for the
rest of the game.

See slides 2-3 in the mockup PDF.

If they choose a GraphRAG token it will reveal a third clue card which
is the common movie and it is revealed between the earlier cards. Both
this and the YYY card becomes enabled and can be turned to reveal the
actors in those two movies. Also these can be zoomed with a right-click.

See slides 4-6 in the mockup PDF.

If a player used a RAG token they can choose to use a GraphRAG token on
the same game (it will then consume both). However, if you used a GraphRAG
token you cannot use a RAG token after that since it would make no sense.
You can also not use another token of the same type that you already used
on the question.

Note that playing the tokens will unlock the cards, but not reveal them
until they are clicked. You can flip unlock cards back and forth (it
doesn't matter much for the game play, but is a nice UI feature).

After submitting an answer the clue cards disappears. If you were correct
you get a new question and new cards.

Note that tokens doesn't not affect score. You don't get more score for
not having used tokens.

Backing this we have a Neo4j graph in an Aura instance. This has Person
nodes and Movie nodes, and each question is dynamically generated from
this graph with a special query (see database.md).
