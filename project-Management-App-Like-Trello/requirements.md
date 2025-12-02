Requirements
User: Each user should have a userId, name, email.
Board: Each board should have a id, name, privacy (PUBLIC/PRIVATE), url, members, lists
List: Each list should have a id, name and cards
Card: Each card should have a id, name, description, assigned user
We should be able to create/delete boards, add/remove people from the members list and modify attributes. Deleting a board should delete all lists inside it.
We should be able to create/delete lists and modify attributes. Deleting a list should delete all cards inside it.
We should be able to create/delete cards, assign/unassign a member to the card and modify attributes
We should also be able to move cards across lists in the same board
Ability to show all boards, a single board, a single list and a single card
Default privacy should be public
Cards should be unassigned by default
Ids should be auto-generated for board/list/card
URLs should get created based on the id
