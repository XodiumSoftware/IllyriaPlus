# Chiseled Bookshelf Storage

Peek inside chiseled bookshelves without opening the inventory.

## How It Works

**Left-click** the **front face** of a chiseled bookshelf to see what's stored in the slot you're looking at.
The contents appear as an action bar message.

## What You See

| Content Type                                                                                            | Display                        |
| :-----------------------------------------------------------------------------------------------------: | ------------------------------ |
| <img class="mc-icon" alt="Written Book" title="Written Book" src="../img/item/written_book.png">            | Book icon + title + author     |
| <img class="mc-icon" alt="Enchanted Book" title="Enchanted Book" src="../img/item/enchanted_book.png">     | Book icon + stored enchantments |
| <img class="mc-icon" alt="Book or any item" title="Book or any item" src="../img/item/book.png">            | Item icon only                 |

## Details

- Uses ray casting to detect exactly which slot (out of 6) you're pointing at
- Shows the sprite icon of the stored item
- Works in Survival and Adventure modes
- Only works when clicking the front face of the bookshelf
